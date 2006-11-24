/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.core.fs.db;

import org.apache.jackrabbit.util.Text;
import org.apache.jackrabbit.util.TransientFileFactory;
import org.apache.jackrabbit.core.fs.FileSystemException;
import org.apache.jackrabbit.core.fs.FileSystemPathUtil;
import org.apache.jackrabbit.core.fs.RandomAccessOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.IOException;
import java.io.File;
import java.io.FilterOutputStream;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.RandomAccessFile;
import java.util.List;
import java.util.LinkedList;
import java.lang.reflect.Method;

/**
 * <code>OracleFileSystem</code> is a JDBC-based <code>FileSystem</code>
 * implementation for Jackrabbit that persists file system entries in an
 * Oracle database.
 * <p/>
 * It is configured through the following properties:
 * <ul>
 * <li><code>driver</code>: the FQN name of the JDBC driver class
 * (default: <code>"oracle.jdbc.OracleDriver"</code>)</li>
 * <li><code>schema</code>: type of schema to be used
 * (default: <code>"oracle"</code>)</li>
 * <li><code>url</code>: the database url (e.g.
 * <code>"jdbc:oracle:thin:@[host]:[port]:[sid]"</code>)</li>
 * <li><code>user</code>: the database user</li>
 * <li><code>password</code>: the user's password</li>
 * <li><code>schemaObjectPrefix</code>: prefix to be prepended to schema objects</li>
 * </ul>
 * See also {@link DbFileSystem}.
 * <p/>
 * The following is a fragment from a sample configuration:
 * <pre>
 *   &lt;FileSystem class="org.apache.jackrabbit.core.fs.db.OracleFileSystem"&gt;
 *       &lt;param name="url" value="jdbc:oracle:thin:@127.0.0.1:1521:orcl"/&gt;
 *       &lt;param name="user" value="scott"/&gt;
 *       &lt;param name="password" value="tiger"/&gt;
 *       &lt;param name="schemaObjectPrefix" value="rep_"/&gt;
 *  &lt;/FileSystem&gt;
 * </pre>
 */
public class OracleFileSystem extends DbFileSystem {

    /**
     * Logger instance
     */
    private static Logger log = LoggerFactory.getLogger(OracleFileSystem.class);

    private Class blobClass;
    private Integer DURATION_SESSION_CONSTANT;
    private Integer MODE_READWRITE_CONSTANT;

    /**
     * Creates a new <code>OracleFileSystem</code> instance.
     */
    public OracleFileSystem() {
        // preset some attributes to reasonable defaults
        schema = "oracle";
        driver = "oracle.jdbc.OracleDriver";
        schemaObjectPrefix = "";
        user = "";
        password = "";
        initialized = false;
    }

    //-----------------------------------------< DatabaseFileSystem overrides >
    /**
     * {@inheritDoc}
     * <p/>
     * Retrieve the <code>oracle.sql.BLOB</code> class via reflection, and
     * initialize the values for the <code>DURATION_SESSION</code> and
     * <code>MODE_READWRITE</code> constants defined there.
     * @see oracle.sql.BLOB#DURATION_SESSION
     * @see oracle.sql.BLOB#MODE_READWRITE
     */
    public void init() throws FileSystemException {
        super.init();

        // initialize oracle.sql.BLOB class & constants

        // use the Connection object for using the exact same
        // class loader that the Oracle driver was loaded with
        try {
            blobClass = con.getClass().getClassLoader().loadClass("oracle.sql.BLOB");
            DURATION_SESSION_CONSTANT =
                    new Integer(blobClass.getField("DURATION_SESSION").getInt(null));
            MODE_READWRITE_CONSTANT =
                    new Integer(blobClass.getField("MODE_READWRITE").getInt(null));
        } catch (Exception e) {
            String msg = "failed to load/introspect oracle.sql.BLOB";
            log.error(msg, e);
            throw new FileSystemException(msg, e);
        }
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Since Oracle treats emtpy strings and BLOBs as null values the SQL
     * statements had to be adapated accordingly. The following changes were
     * necessary:
     * <ul>
     * <li>The distinction between file and folder entries is based on
     * FSENTRY_LENGTH being null/not null rather than FSENTRY_DATA being
     * null/not null because FSENTRY_DATA of a 0-length (i.e. empty) file is
     * null in Oracle.</li>
     * <li>Folder entries: Since the root folder has an empty name (which would
     * be null in Oracle), an empty name is automatically converted and treated
     * as " ".</li>
     * </ul>
     */
    protected List initPreparedStatements() throws SQLException {
        List stmts = new LinkedList();
        stmts.add(insertFileStmt = con.prepareStatement("insert into "
                + schemaObjectPrefix + "FSENTRY "
                + "(FSENTRY_PATH, FSENTRY_NAME, FSENTRY_DATA, "
                + "FSENTRY_LASTMOD, FSENTRY_LENGTH) "
                + "values (?, ?, ?, ?, ?)"));

        stmts.add(insertFolderStmt = con.prepareStatement("insert into "
                + schemaObjectPrefix + "FSENTRY "
                + "(FSENTRY_PATH, FSENTRY_NAME, FSENTRY_LASTMOD, FSENTRY_LENGTH) "
                + "values (?, nvl(?, ' '), ?, null)"));

        stmts.add(updateDataStmt = con.prepareStatement("update "
                + schemaObjectPrefix + "FSENTRY "
                + "set FSENTRY_DATA = ?, FSENTRY_LASTMOD = ?, FSENTRY_LENGTH = ? "
                + "where FSENTRY_PATH = ? and FSENTRY_NAME = ? "
                + "and FSENTRY_LENGTH is not null"));

        stmts.add(updateLastModifiedStmt = con.prepareStatement("update "
                + schemaObjectPrefix + "FSENTRY set FSENTRY_LASTMOD = ? "
                + "where FSENTRY_PATH = ? and FSENTRY_NAME = ? "
                + "and FSENTRY_LENGTH is not null"));

        stmts.add(selectExistStmt = con.prepareStatement("select 1 from "
                + schemaObjectPrefix + "FSENTRY where FSENTRY_PATH = ? "
                + "and FSENTRY_NAME = nvl(?, ' ')"));

        stmts.add(selectFileExistStmt = con.prepareStatement("select 1 from "
                + schemaObjectPrefix + "FSENTRY where FSENTRY_PATH = ? "
                + "and FSENTRY_NAME = ? and FSENTRY_LENGTH is not null"));

        stmts.add(selectFolderExistStmt = con.prepareStatement("select 1 from "
                + schemaObjectPrefix + "FSENTRY where FSENTRY_PATH = ? "
                + "and FSENTRY_NAME = nvl(?, ' ') and FSENTRY_LENGTH is null"));

        stmts.add(selectFileNamesStmt = con.prepareStatement("select FSENTRY_NAME from "
                + schemaObjectPrefix + "FSENTRY where FSENTRY_PATH = ? "
                + "and FSENTRY_LENGTH is not null"));

        stmts.add(selectFolderNamesStmt = con.prepareStatement("select FSENTRY_NAME from "
                + schemaObjectPrefix + "FSENTRY where FSENTRY_PATH = ? "
                + "and FSENTRY_NAME != ' ' "
                + "and FSENTRY_LENGTH is null"));

        stmts.add(selectFileAndFolderNamesStmt = con.prepareStatement("select FSENTRY_NAME from "
                + schemaObjectPrefix + "FSENTRY where FSENTRY_PATH = ? "
                + "and FSENTRY_NAME != ' '"));

        stmts.add(selectChildCountStmt = con.prepareStatement("select count(FSENTRY_NAME) from "
                + schemaObjectPrefix + "FSENTRY where FSENTRY_PATH = ?  "
                + "and FSENTRY_NAME != ' '"));

        stmts.add(selectDataStmt = con.prepareStatement("select nvl(FSENTRY_DATA, empty_blob()) from "
                + schemaObjectPrefix + "FSENTRY where FSENTRY_PATH = ? "
                + "and FSENTRY_NAME = ? and FSENTRY_LENGTH is not null"));

        stmts.add(selectLastModifiedStmt = con.prepareStatement("select FSENTRY_LASTMOD from "
                + schemaObjectPrefix + "FSENTRY where FSENTRY_PATH = ? "
                + "and FSENTRY_NAME = nvl(?, ' ')"));

        stmts.add(selectLengthStmt = con.prepareStatement("select nvl(FSENTRY_LENGTH, 0) from "
                + schemaObjectPrefix + "FSENTRY where FSENTRY_PATH = ? "
                + "and FSENTRY_NAME = ? and FSENTRY_LENGTH is not null"));

        stmts.add(deleteFileStmt = con.prepareStatement("delete from "
                + schemaObjectPrefix + "FSENTRY where FSENTRY_PATH = ? "
                + "and FSENTRY_NAME = ? and FSENTRY_LENGTH is not null"));

        stmts.add(deleteFolderStmt = con.prepareStatement("delete from "
                + schemaObjectPrefix + "FSENTRY where "
                + "(FSENTRY_PATH = ? and FSENTRY_NAME = nvl(?, ' ') and FSENTRY_LENGTH is null) "
                + "or (FSENTRY_PATH = ?) "
                + "or (FSENTRY_PATH like ?) "));

        stmts.add(copyFileStmt = con.prepareStatement("insert into "
                + schemaObjectPrefix + "FSENTRY "
                + "(FSENTRY_PATH, FSENTRY_NAME, FSENTRY_DATA, "
                + "FSENTRY_LASTMOD, FSENTRY_LENGTH) "
                + "select ?, ?, FSENTRY_DATA, "
                + "FSENTRY_LASTMOD, FSENTRY_LENGTH from "
                + schemaObjectPrefix + "FSENTRY where FSENTRY_PATH = ? "
                + "and FSENTRY_NAME = ? and FSENTRY_LENGTH is not null"));

        stmts.add(copyFilesStmt = con.prepareStatement("insert into "
                + schemaObjectPrefix + "FSENTRY "
                + "(FSENTRY_PATH, FSENTRY_NAME, FSENTRY_DATA, "
                + "FSENTRY_LASTMOD, FSENTRY_LENGTH) "
                + "select ?, FSENTRY_NAME, FSENTRY_DATA, "
                + "FSENTRY_LASTMOD, FSENTRY_LENGTH from "
                + schemaObjectPrefix + "FSENTRY where FSENTRY_PATH = ? "
                + "and FSENTRY_LENGTH is not null"));

        return stmts;
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Overridden because we need to use <code>oracle.sql.Blob</code>
     * and <code>PreparedStatement#setBlob</code> instead of just
     * <code>PreparedStatement#setBinaryStream</code>.
     */
    public OutputStream getOutputStream(final String filePath) throws FileSystemException {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        FileSystemPathUtil.checkFormat(filePath);

        final String parentDir = FileSystemPathUtil.getParentDir(filePath);
        final String name = FileSystemPathUtil.getName(filePath);

        if (!isFolder(parentDir)) {
            throw new FileSystemException("path not found: " + parentDir);
        }

        if (isFolder(filePath)) {
            throw new FileSystemException("path denotes folder: " + filePath);
        }

        try {
            TransientFileFactory fileFactory = TransientFileFactory.getInstance();
            final File tmpFile = fileFactory.createTransientFile("bin", null, null);

            return new FilterOutputStream(new FileOutputStream(tmpFile)) {

                public void close() throws IOException {
                    super.close();

                    PreparedStatement stmt = null;
                    InputStream in = null;
                    Blob blob = null;
                    try {
                        if (isFile(filePath)) {
                            stmt = updateDataStmt;
                            synchronized (stmt) {
                                long length = tmpFile.length();
                                in = new FileInputStream(tmpFile);
                                blob = createTemporaryBlob(in);
                                stmt.setBlob(1, blob);
                                stmt.setLong(2, System.currentTimeMillis());
                                stmt.setLong(3, length);
                                stmt.setString(4, parentDir);
                                stmt.setString(5, name);
                                stmt.executeUpdate();
                            }
                        } else {
                            stmt = insertFileStmt;
                            stmt.setString(1, parentDir);
                            stmt.setString(2, name);
                            long length = tmpFile.length();
                            in = new FileInputStream(tmpFile);
                            blob = createTemporaryBlob(in);
                            stmt.setBlob(3, blob);
                            stmt.setLong(4, System.currentTimeMillis());
                            stmt.setLong(5, length);
                            stmt.executeUpdate();
                        }

                    } catch (Exception e) {
                        throw new IOException(e.getMessage());
                    } finally {
                        if (stmt != null) {
                            resetStatement(stmt);
                        }
                        if (blob != null) {
                            try {
                                freeTemporaryBlob(blob);
                            } catch (Exception e1) {
                            }
                        }
                        if (in != null) {
                            try {
                                in.close();
                            } catch (Exception e1) {
                            }
                        }
                        // temp file can now safely be removed
                        tmpFile.delete();
                    }
                }
            };
        } catch (Exception e) {
            String msg = "failed to open output stream to file: " + filePath;
            log.error(msg, e);
            throw new FileSystemException(msg, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public RandomAccessOutputStream getRandomAccessOutputStream(final String filePath) throws FileSystemException, UnsupportedOperationException {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        FileSystemPathUtil.checkFormat(filePath);

        final String parentDir = FileSystemPathUtil.getParentDir(filePath);
        final String name = FileSystemPathUtil.getName(filePath);

        if (!isFolder(parentDir)) {
            throw new FileSystemException("path not found: " + parentDir);
        }

        if (isFolder(filePath)) {
            throw new FileSystemException("path denotes folder: " + filePath);
        }

        try {
            TransientFileFactory fileFactory = TransientFileFactory.getInstance();
            final File tmpFile = fileFactory.createTransientFile("bin", null, null);

            // @todo FIXME use java.sql.Blob

            if (isFile(filePath)) {
                // file entry exists, spool contents to temp file first
                InputStream in = getInputStream(filePath);
                OutputStream out = new FileOutputStream(tmpFile);
                try {
                    int read;
                    byte[] ba = new byte[8192];
                    while ((read = in.read(ba, 0, ba.length)) != -1) {
                        out.write(ba, 0, read);
                    }
                } finally {
                    out.close();
                    in.close();
                }
            }

            return new RandomAccessOutputStream() {
                private final RandomAccessFile raf =
                    new RandomAccessFile(tmpFile, "rw");

                public void close() throws IOException {
                    raf.close();

                    PreparedStatement stmt = null;
                    InputStream in = null;
                    Blob blob = null;
                    try {
                        if (isFile(filePath)) {
                            stmt = updateDataStmt;
                            synchronized (stmt) {
                                long length = tmpFile.length();
                                in = new FileInputStream(tmpFile);
                                blob = createTemporaryBlob(in);
                                stmt.setBlob(1, blob);
                                stmt.setLong(2, System.currentTimeMillis());
                                stmt.setLong(3, length);
                                stmt.setString(4, parentDir);
                                stmt.setString(5, name);
                                stmt.executeUpdate();
                            }
                        } else {
                            stmt = insertFileStmt;
                            stmt.setString(1, parentDir);
                            stmt.setString(2, name);
                            long length = tmpFile.length();
                            in = new FileInputStream(tmpFile);
                            blob = createTemporaryBlob(in);
                            stmt.setBlob(3, blob);
                            stmt.setLong(4, System.currentTimeMillis());
                            stmt.setLong(5, length);
                            stmt.executeUpdate();
                        }

                    } catch (Exception e) {
                        throw new IOException(e.getMessage());
                    } finally {
                        if (stmt != null) {
                            resetStatement(stmt);
                        }
                        if (blob != null) {
                            try {
                                freeTemporaryBlob(blob);
                            } catch (Exception e1) {
                            }
                        }
                        if (in != null) {
                            try {
                                in.close();
                            } catch (Exception e1) {
                            }
                        }
                        // temp file can now safely be removed
                        tmpFile.delete();
                    }
                }

                public void seek(long position) throws IOException {
                    raf.seek(position);
                }

                public void write(int b) throws IOException {
                    raf.write(b);
                }

                public void flush() /*throws IOException*/ {
                    // nop
                }

                public void write(byte[] b) throws IOException {
                    raf.write(b);
                }

                public void write(byte[] b, int off, int len) throws IOException {
                    raf.write(b, off, len);
                }
            };
        } catch (Exception e) {
            String msg = "failed to open output stream to file: " + filePath;
            log.error(msg, e);
            throw new FileSystemException(msg, e);
        }
    }

    //----------------------------------------< oracle-specific blob handling >
    /**
     * Creates a temporary oracle.sql.BLOB instance via reflection and spools
     * the contents of the specified stream.
     */
    protected Blob createTemporaryBlob(InputStream in) throws Exception {
        /*
        BLOB blob = BLOB.createTemporary(con, false, BLOB.DURATION_SESSION);
        blob.open(BLOB.MODE_READWRITE);
        OutputStream out = blob.getBinaryOutputStream();
        ...
        out.flush();
        out.close();
        blob.close();
        return blob;
        */
        Method createTemporary = blobClass.getMethod("createTemporary",
                new Class[]{Connection.class, Boolean.TYPE, Integer.TYPE});
        Object blob = createTemporary.invoke(null,
                new Object[]{con, Boolean.FALSE, DURATION_SESSION_CONSTANT});
        Method open = blobClass.getMethod("open", new Class[]{Integer.TYPE});
        open.invoke(blob, new Object[]{MODE_READWRITE_CONSTANT});
        Method getBinaryOutputStream =
                blobClass.getMethod("getBinaryOutputStream", new Class[0]);
        OutputStream out = (OutputStream) getBinaryOutputStream.invoke(blob, null);
        try {
            int read;
            byte[] buf = new byte[8192];
            while ((read = in.read(buf, 0, buf.length)) > -1) {
                out.write(buf, 0, read);
            }
        } finally {
            try {
                out.flush();
            } catch (IOException ioe) {
            }
            out.close();
        }
        Method close = blobClass.getMethod("close", new Class[0]);
        close.invoke(blob, null);
        return (Blob) blob;
    }

    /**
     * Frees a temporary oracle.sql.BLOB instance via reflection.
     */
    protected void freeTemporaryBlob(Object blob) throws Exception {
        // blob.freeTemporary();
        Method freeTemporary = blobClass.getMethod("freeTemporary", new Class[0]);
        freeTemporary.invoke(blob, null);
    }
}
