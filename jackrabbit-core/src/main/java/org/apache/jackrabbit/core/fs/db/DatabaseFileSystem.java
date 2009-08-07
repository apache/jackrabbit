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

import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.core.fs.FileSystem;
import org.apache.jackrabbit.core.fs.FileSystemException;
import org.apache.jackrabbit.core.fs.FileSystemPathUtil;
import org.apache.jackrabbit.core.fs.RandomAccessOutputStream;
import org.apache.jackrabbit.util.Text;
import org.apache.jackrabbit.util.TransientFileFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.FileInputStream;
import java.io.RandomAccessFile;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.HashMap;

/**
 * Base class for database file systems. This class contains common
 * functionality for database file system subclasses that normally differ only
 * in the way the database connection is acquired. Subclasses should override
 * the {@link #getConnection()} method to return the configured database
 * connection.
 * <p>
 * See the {@link DbFileSystem} for a detailed description of the available
 * configuration options and database behaviour.
 */
public class DatabaseFileSystem implements FileSystem {

    /**
     * Logger instance
     */
    private static Logger log = LoggerFactory.getLogger(DatabaseFileSystem.class);

    protected static final String SCHEMA_OBJECT_PREFIX_VARIABLE =
            "${schemaObjectPrefix}";

    protected boolean initialized;

    protected String schema;
    protected String schemaObjectPrefix;

    /**
     * Whether the schema check must be done during initialization.
     */
    private boolean schemaCheckEnabled = true;

    // initial size of buffer used to serialize objects
    protected static final int INITIAL_BUFFER_SIZE = 8192;

    // jdbc connection
    protected Connection con;

    // time to sleep in ms before a reconnect is attempted
    protected static final int SLEEP_BEFORE_RECONNECT = 10000;

    // the map of prepared statements (key: sql stmt, value: prepared stmt)
    private HashMap preparedStatements = new HashMap();

    // SQL statements
    protected String selectExistSQL;
    protected String selectFileExistSQL;
    protected String selectFolderExistSQL;
    protected String selectChildCountSQL;
    protected String selectDataSQL;
    protected String selectLastModifiedSQL;
    protected String selectLengthSQL;
    protected String selectFileNamesSQL;
    protected String selectFolderNamesSQL;
    protected String selectFileAndFolderNamesSQL;
    protected String deleteFileSQL;
    protected String deleteFolderSQL;
    protected String insertFileSQL;
    protected String insertFolderSQL;
    protected String updateDataSQL;
    protected String updateLastModifiedSQL;
    protected String copyFileSQL;
    protected String copyFilesSQL;

    /**
     * Default constructor
     */
    public DatabaseFileSystem() {
        schema = "default";
        schemaObjectPrefix = "";
        initialized = false;
    }

    //----------------------------------------------------< setters & getters >
    public String getSchemaObjectPrefix() {
        return schemaObjectPrefix;
    }

    public void setSchemaObjectPrefix(String schemaObjectPrefix) {
        // make sure prefix is all uppercase
        this.schemaObjectPrefix = schemaObjectPrefix.toUpperCase();
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    /**
     * @return whether the schema check is enabled
     */
    public final boolean isSchemaCheckEnabled() {
        return schemaCheckEnabled;
    }

    /**
     * @param enabled set whether the schema check is enabled
     */
    public final void setSchemaCheckEnabled(boolean enabled) {
        schemaCheckEnabled = enabled;
    }

    //-------------------------------------------< java.lang.Object overrides >
    /**
     * {@inheritDoc}
     */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj instanceof DatabaseFileSystem) {
            DatabaseFileSystem other = (DatabaseFileSystem) obj;
            return equals(schema, other.schema)
                && equals(schemaObjectPrefix, other.schemaObjectPrefix);
        } else {
            return false;
        }
    }

    private static boolean equals(Object a, Object b) {
        if (a == null && b == null) {
            return true;
        } else if (a == null || b == null) {
            return false;
        } else {
            return a.equals(b);
        }
    }

    /**
     * Returns zero to satisfy the Object equals/hashCode contract.
     * This class is mutable and not meant to be used as a hash key.
     *
     * @return always zero
     * @see Object#hashCode()
     */
    public int hashCode() {
        return 0;
    }

    //-----------------------------------------------------------< FileSystem >

    /**
     * {@inheritDoc}
     */
    public void init() throws FileSystemException {
        if (initialized) {
            throw new IllegalStateException("already initialized");
        }

        try {
            // setup jdbc connection
            initConnection();

            // make sure schemaObjectPrefix consists of legal name characters only
            prepareSchemaObjectPrefix();

            // check if schema objects exist and create them if necessary
            if (isSchemaCheckEnabled()) {
                checkSchema();
            }

            // build sql statements
            buildSQLStatements();

            // prepare statements
            initPreparedStatements();

            // finally verify that there's a file system root entry
            verifyRootExists();

            initialized = true;
        } catch (Exception e) {
            String msg = "failed to initialize file system";
            log.error(msg, e);
            throw new FileSystemException(msg, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void close() throws FileSystemException {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        try {
            // close shared prepared statements
            Iterator it = preparedStatements.values().iterator();
            while (it.hasNext()) {
                closeStatement((PreparedStatement) it.next());
            }
            preparedStatements.clear();

            // close jdbc connection
            closeConnection(con);
        } catch (SQLException e) {
            String msg = "error closing file system";
            log.error(msg, e);
            throw new FileSystemException(msg, e);
        } finally {
            initialized = false;
        }
    }

    /**
     * {@inheritDoc}
     */
    public void createFolder(String folderPath) throws FileSystemException {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        FileSystemPathUtil.checkFormat(folderPath);

        if (!exists(folderPath)) {
            createDeepFolder(folderPath);
        } else {
            throw new FileSystemException("file system entry already exists: " + folderPath);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void deleteFile(String filePath) throws FileSystemException {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        FileSystemPathUtil.checkFormat(filePath);

        String parentDir = FileSystemPathUtil.getParentDir(filePath);
        String name = FileSystemPathUtil.getName(filePath);

        int count = 0;
        synchronized (deleteFileSQL) {
            try {
                Statement stmt = executeStmt(
                        deleteFileSQL, new Object[]{parentDir, name});
                count = stmt.getUpdateCount();
            } catch (SQLException e) {
                String msg = "failed to delete file: " + filePath;
                log.error(msg, e);
                throw new FileSystemException(msg, e);
            }
        }

        if (count == 0) {
            throw new FileSystemException("no such file: " + filePath);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void deleteFolder(String folderPath) throws FileSystemException {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        FileSystemPathUtil.checkFormat(folderPath);

        if (folderPath.equals(FileSystem.SEPARATOR)) {
            throw new FileSystemException("cannot delete root");
        }

        String parentDir = FileSystemPathUtil.getParentDir(folderPath);
        String name = FileSystemPathUtil.getName(folderPath);

        int count = 0;
        synchronized (deleteFolderSQL) {
            try {
                Statement stmt = executeStmt(deleteFolderSQL, new Object[]{
                        parentDir,
                        name,
                        folderPath,
                        folderPath + FileSystem.SEPARATOR + "%"});
                count = stmt.getUpdateCount();
            } catch (SQLException e) {
                String msg = "failed to delete folder: " + folderPath;
                log.error(msg, e);
                throw new FileSystemException(msg, e);
            }
        }

        if (count == 0) {
            throw new FileSystemException("no such folder: " + folderPath);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean exists(String path) throws FileSystemException {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        FileSystemPathUtil.checkFormat(path);

        String parentDir = FileSystemPathUtil.getParentDir(path);
        String name = FileSystemPathUtil.getName(path);

        synchronized (selectExistSQL) {
            ResultSet rs = null;
            try {
                Statement stmt = executeStmt(
                        selectExistSQL, new Object[]{parentDir, name});
                rs = stmt.getResultSet();

                // a file system entry exists if the result set
                // has at least one entry
                return rs.next();
            } catch (SQLException e) {
                String msg = "failed to check existence of file system entry: " + path;
                log.error(msg, e);
                throw new FileSystemException(msg, e);
            } finally {
                closeResultSet(rs);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean isFile(String path) throws FileSystemException {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        FileSystemPathUtil.checkFormat(path);

        String parentDir = FileSystemPathUtil.getParentDir(path);
        String name = FileSystemPathUtil.getName(path);

        synchronized (selectFileExistSQL) {
            ResultSet rs = null;
            try {
                Statement stmt = executeStmt(
                        selectFileExistSQL, new Object[]{parentDir, name});
                rs = stmt.getResultSet();

                // a file exists if the result set has at least one entry
                return rs.next();
            } catch (SQLException e) {
                String msg = "failed to check existence of file: " + path;
                log.error(msg, e);
                throw new FileSystemException(msg, e);
            } finally {
                closeResultSet(rs);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean isFolder(String path) throws FileSystemException {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        FileSystemPathUtil.checkFormat(path);

        String parentDir = FileSystemPathUtil.getParentDir(path);
        String name = FileSystemPathUtil.getName(path);

        synchronized (selectFolderExistSQL) {
            ResultSet rs = null;
            try {
                Statement stmt = executeStmt(
                        selectFolderExistSQL, new Object[]{parentDir, name});
                rs = stmt.getResultSet();

                // a folder exists if the result set has at least one entry
                return rs.next();
            } catch (SQLException e) {
                String msg = "failed to check existence of folder: " + path;
                log.error(msg, e);
                throw new FileSystemException(msg, e);
            } finally {
                closeResultSet(rs);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public long lastModified(String path) throws FileSystemException {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        FileSystemPathUtil.checkFormat(path);

        String parentDir = FileSystemPathUtil.getParentDir(path);
        String name = FileSystemPathUtil.getName(path);

        synchronized (selectLastModifiedSQL) {
            ResultSet rs = null;
            try {
                Statement stmt = executeStmt(
                        selectLastModifiedSQL, new Object[]{parentDir, name});
                rs = stmt.getResultSet();
                if (!rs.next()) {
                    throw new FileSystemException("no such file system entry: " + path);
                }
                return rs.getLong(1);
            } catch (SQLException e) {
                String msg = "failed to determine lastModified of file system entry: " + path;
                log.error(msg, e);
                throw new FileSystemException(msg, e);
            } finally {
                closeResultSet(rs);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public long length(String filePath) throws FileSystemException {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        FileSystemPathUtil.checkFormat(filePath);

        String parentDir = FileSystemPathUtil.getParentDir(filePath);
        String name = FileSystemPathUtil.getName(filePath);

        synchronized (selectLengthSQL) {
            ResultSet rs = null;
            try {
                Statement stmt = executeStmt(
                        selectLengthSQL, new Object[]{parentDir, name});
                rs = stmt.getResultSet();
                if (!rs.next()) {
                    throw new FileSystemException("no such file: " + filePath);
                }
                return rs.getLong(1);
            } catch (SQLException e) {
                String msg = "failed to determine length of file: " + filePath;
                log.error(msg, e);
                throw new FileSystemException(msg, e);
            } finally {
                closeResultSet(rs);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasChildren(String path) throws FileSystemException {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        FileSystemPathUtil.checkFormat(path);

        if (!exists(path)) {
            throw new FileSystemException("no such file system entry: " + path);
        }

        synchronized (selectChildCountSQL) {
            ResultSet rs = null;
            try {
                Statement stmt = executeStmt(selectChildCountSQL, new Object[]{path});
                rs = stmt.getResultSet();
                if (!rs.next()) {
                    return false;
                }
                int count = rs.getInt(1);
                if (FileSystemPathUtil.denotesRoot(path)) {
                    // ingore file system root entry
                    count--;
                }
                return (count > 0);
            } catch (SQLException e) {
                String msg = "failed to determine child count of file system entry: " + path;
                log.error(msg, e);
                throw new FileSystemException(msg, e);
            } finally {
                closeResultSet(rs);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public String[] list(String folderPath) throws FileSystemException {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        FileSystemPathUtil.checkFormat(folderPath);

        if (!isFolder(folderPath)) {
            throw new FileSystemException("no such folder: " + folderPath);
        }

        synchronized (selectFileAndFolderNamesSQL) {
            ResultSet rs = null;
            try {
                Statement stmt = executeStmt(
                        selectFileAndFolderNamesSQL, new Object[]{folderPath});
                rs = stmt.getResultSet();
                ArrayList names = new ArrayList();
                while (rs.next()) {
                    String name = rs.getString(1);
                    if (name.length() == 0
                            && FileSystemPathUtil.denotesRoot(folderPath)) {
                        // this is the file system root entry, skip...
                        continue;
                    }
                    names.add(name);
                }
                return (String[]) names.toArray(new String[names.size()]);
            } catch (SQLException e) {
                String msg = "failed to list child entries of folder: " + folderPath;
                log.error(msg, e);
                throw new FileSystemException(msg, e);
            } finally {
                closeResultSet(rs);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public String[] listFiles(String folderPath) throws FileSystemException {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        FileSystemPathUtil.checkFormat(folderPath);

        if (!isFolder(folderPath)) {
            throw new FileSystemException("no such folder: " + folderPath);
        }

        synchronized (selectFileNamesSQL) {
            ResultSet rs = null;
            try {
                Statement stmt = executeStmt(
                        selectFileNamesSQL, new Object[]{folderPath});
                rs = stmt.getResultSet();
                ArrayList names = new ArrayList();
                while (rs.next()) {
                    names.add(rs.getString(1));
                }
                return (String[]) names.toArray(new String[names.size()]);
            } catch (SQLException e) {
                String msg = "failed to list file entries of folder: " + folderPath;
                log.error(msg, e);
                throw new FileSystemException(msg, e);
            } finally {
                closeResultSet(rs);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public String[] listFolders(String folderPath) throws FileSystemException {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        FileSystemPathUtil.checkFormat(folderPath);

        if (!isFolder(folderPath)) {
            throw new FileSystemException("no such folder: " + folderPath);
        }

        synchronized (selectFolderNamesSQL) {
            ResultSet rs = null;
            try {
                Statement stmt = executeStmt(
                        selectFolderNamesSQL, new Object[]{folderPath});
                rs = stmt.getResultSet();
                ArrayList names = new ArrayList();
                while (rs.next()) {
                    String name = rs.getString(1);
                    if (name.length() == 0
                            && FileSystemPathUtil.denotesRoot(folderPath)) {
                        // this is the file system root entry, skip...
                        continue;
                    }
                    names.add(name);
                }
                return (String[]) names.toArray(new String[names.size()]);
            } catch (SQLException e) {
                String msg = "failed to list folder entries of folder: " + folderPath;
                log.error(msg, e);
                throw new FileSystemException(msg, e);
            } finally {
                closeResultSet(rs);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void touch(String filePath) throws FileSystemException {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        FileSystemPathUtil.checkFormat(filePath);

        String parentDir = FileSystemPathUtil.getParentDir(filePath);
        String name = FileSystemPathUtil.getName(filePath);

        int count = 0;
        synchronized (updateLastModifiedSQL) {
            try {
                Statement stmt = executeStmt(updateLastModifiedSQL, new Object[]{
                        new Long(System.currentTimeMillis()),
                        parentDir,
                        name});
                count = stmt.getUpdateCount();
            } catch (SQLException e) {
                String msg = "failed to touch file: " + filePath;
                log.error(msg, e);
                throw new FileSystemException(msg, e);
            }
        }

        if (count == 0) {
            throw new FileSystemException("no such file: " + filePath);
        }
    }

    /**
     * {@inheritDoc}
     */
    public InputStream getInputStream(String filePath) throws FileSystemException {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        FileSystemPathUtil.checkFormat(filePath);

        String parentDir = FileSystemPathUtil.getParentDir(filePath);
        String name = FileSystemPathUtil.getName(filePath);

        synchronized (selectDataSQL) {
            try {
                Statement stmt = executeStmt(
                        selectDataSQL, new Object[]{parentDir, name});

                final ResultSet rs = stmt.getResultSet();
                if (!rs.next()) {
                    throw new FileSystemException("no such file: " + filePath);
                }
                InputStream in = rs.getBinaryStream(1);
                /**
                 * return an InputStream wrapper in order to
                 * close the ResultSet when the stream is closed
                 */
                return new FilterInputStream(in) {
                    public void close() throws IOException {
                        super.close();
                        // close ResultSet
                        closeResultSet(rs);
                    }
                };
            } catch (SQLException e) {
                String msg = "failed to retrieve data of file: " + filePath;
                log.error(msg, e);
                throw new FileSystemException(msg, e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public OutputStream getOutputStream(final String filePath)
            throws FileSystemException {
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

                public void write(byte[] bytes, int off, int len) throws IOException {
                    out.write(bytes, off, len);
                }

                public void close() throws IOException {
                    out.flush();
                    ((FileOutputStream) out).getFD().sync();
                    out.close();

                    InputStream in = null;
                    try {
                        if (isFile(filePath)) {
                            synchronized (updateDataSQL) {
                                long length = tmpFile.length();
                                in = new FileInputStream(tmpFile);
                                executeStmt(updateDataSQL,
                                        new Object[]{
                                            new SizedInputStream(in, length),
                                            new Long(System.currentTimeMillis()),
                                            new Long(length),
                                            parentDir,
                                            name
                                        });
                            }
                        } else {
                            synchronized (insertFileSQL) {
                                long length = tmpFile.length();
                                in = new FileInputStream(tmpFile);
                                executeStmt(insertFileSQL,
                                        new Object[]{
                                            parentDir,
                                            name,
                                            new SizedInputStream(in, length),
                                            new Long(System.currentTimeMillis()),
                                            new Long(length)
                                        });
                            }
                        }

                    } catch (Exception e) {
                        IOException ioe = new IOException(e.getMessage());
                        ioe.initCause(e);
                        throw ioe;
                    } finally {
                        if (in != null) {
                            in.close();
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
    public RandomAccessOutputStream getRandomAccessOutputStream(final String filePath)
            throws FileSystemException, UnsupportedOperationException {
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
                    IOUtils.copy(in, out);
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

                    InputStream in = null;
                    try {
                        if (isFile(filePath)) {
                            synchronized (updateDataSQL) {
                                long length = tmpFile.length();
                                in = new FileInputStream(tmpFile);
                                executeStmt(updateDataSQL,
                                        new Object[]{
                                            new SizedInputStream(in, length),
                                            new Long(System.currentTimeMillis()),
                                            new Long(length),
                                            parentDir,
                                            name
                                        });
                            }
                        } else {
                            synchronized (insertFileSQL) {
                                long length = tmpFile.length();
                                in = new FileInputStream(tmpFile);
                                executeStmt(insertFileSQL,
                                        new Object[]{
                                            parentDir,
                                            name,
                                            new SizedInputStream(in, length),
                                            new Long(System.currentTimeMillis()),
                                            new Long(length)
                                        });
                            }
                        }

                    } catch (Exception e) {
                        IOException ioe = new IOException(e.getMessage());
                        ioe.initCause(e);
                        throw ioe;
                    } finally {
                        if (in != null) {
                            in.close();
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

    /**
     * {@inheritDoc}
     */
    public void copy(String srcPath, String destPath) throws FileSystemException {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        FileSystemPathUtil.checkFormat(srcPath);
        FileSystemPathUtil.checkFormat(destPath);

        if (isFolder(srcPath)) {
            // src is a folder
            copyDeepFolder(srcPath, destPath);
        } else {
            // src is a file
            copyFile(srcPath, destPath);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void move(String srcPath, String destPath) throws FileSystemException {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        FileSystemPathUtil.checkFormat(srcPath);
        FileSystemPathUtil.checkFormat(destPath);

        // @todo optimize move (use sql update stmts)
        copy(srcPath, destPath);
        if (isFile(srcPath)) {
            deleteFile(srcPath);
        } else {
            deleteFolder(srcPath);
        }
    }

    //----------------------------------< misc. helper methods & overridables >

    /**
     * Initializes the database connection used by this file system.
     * <p>
     * Subclasses should normally override the {@link #getConnection()}
     * method instead of this one. The default implementation calls
     * {@link #getConnection()} to get the database connection and disables
     * the autocommit feature.
     *
     * @throws Exception if an error occurs
     */
    protected void initConnection() throws Exception {
        con = getConnection();
        // JCR-1013: Setter may fail unnecessarily on a managed connection
        if (!con.getAutoCommit()) {
            con.setAutoCommit(true);
        }
    }

    /**
     * Abstract factory method for creating a new database connection. This
     * method is called by {@link #initConnection()} when the file system is
     * started. The returned connection should come with the default JDBC
     * settings, as the {@link #initConnection()} method will explicitly set
     * the <code>autoCommit</code> and other properties as needed.
     * <p>
     * Note that the returned database connection is kept during the entire
     * lifetime of the file system, after which it is closed by
     * {@link #close()} using the {@link #closeConnection(Connection)} method.
     *
     * @return new connection
     * @throws Exception if an error occurs
     */
    protected Connection getConnection() throws Exception {
        throw new UnsupportedOperationException("Override in a subclass!");
    }

    /**
     * Closes the given database connection. This method is called by
     * {@link #close()} to close the connection acquired using
     * {@link #getConnection()} when the file system was started.
     * <p>
     * The default implementation just calls the {@link Connection#close()}
     * method of the given connection, but subclasses can override this
     * method to provide more extensive database and connection cleanup.
     *
     * @param connection database connection
     * @throws SQLException if an error occurs
     */
    protected void closeConnection(Connection connection) throws SQLException {
        connection.close();
    }

    /**
     * Re-establishes the database connection. This method is called by
     * {@link #executeStmt(String, Object[])} after a <code>SQLException</code>
     * had been encountered.
     *
     * @return true if the connection could be successfully re-established,
     *         false otherwise.
     */
    protected synchronized boolean reestablishConnection() {
        // in any case try to shut down current connection
        // gracefully in order to avoid potential memory leaks

        // close shared prepared statements
        Iterator it = preparedStatements.values().iterator();
        while (it.hasNext()) {
            closeStatement((PreparedStatement) it.next());
        }
        try {
            closeConnection(con);
        } catch (Exception ignore) {
        }

        // sleep for a while to give database a chance
        // to restart before a reconnect is attempted

        try {
            Thread.sleep(SLEEP_BEFORE_RECONNECT);
        } catch (InterruptedException ignore) {
        }

        // now try to re-establish connection

        try {
            initConnection();
            initPreparedStatements();
            return true;
        } catch (Exception e) {
            log.error("failed to re-establish connection", e);
            // reconnect failed
            return false;
        }
    }

    /**
     * Executes the given SQL statement with the specified parameters.
     * If a <code>SQLException</code> is encountered <i>one</i> attempt is made
     * to re-establish the database connection and re-execute the statement.
     *
     * @param sql    statement to execute
     * @param params parameters to set
     * @return the <code>Statement</code> object that had been executed
     * @throws SQLException if an error occurs
     */
    protected Statement executeStmt(String sql, Object[] params)
            throws SQLException {
        int trials = 2;
        while (true) {
            PreparedStatement stmt = (PreparedStatement) preparedStatements.get(sql);
            try {
                for (int i = 0; i < params.length; i++) {
                    if (params[i] instanceof SizedInputStream) {
                        SizedInputStream in = (SizedInputStream) params[i];
                        stmt.setBinaryStream(i + 1, in, (int) in.getSize());
                    } else {
                        stmt.setObject(i + 1, params[i]);
                    }
                }
                stmt.execute();
                resetStatement(stmt);
                return stmt;
            } catch (SQLException se) {
                if (--trials == 0) {
                    // no more trials, re-throw
                    throw se;
                }
                log.warn("execute failed, about to reconnect...", se.getMessage());

                // try to reconnect
                if (reestablishConnection()) {
                    // reconnect succeeded; check whether it's possible to
                    // re-execute the prepared stmt with the given parameters
                    for (int i = 0; i < params.length; i++) {
                        if (params[i] instanceof SizedInputStream) {
                            SizedInputStream in = (SizedInputStream) params[i];
                            if (in.isConsumed()) {
                                // we're unable to re-execute the prepared stmt
                                // since an InputStream paramater has already
                                // been 'consumed';
                                // re-throw previous SQLException
                                throw se;
                            }
                        }
                    }

                    // try again to execute the statement
                    continue;
                } else {
                    // reconnect failed, re-throw previous SQLException
                    throw se;
                }
            }
        }
    }

    /**
     * Makes sure that <code>schemaObjectPrefix</code> does only consist of
     * characters that are allowed in names on the target database. Illegal
     * characters will be escaped as necessary.
     *
     * @throws Exception if an error occurs
     */
    protected void prepareSchemaObjectPrefix() throws Exception {
        DatabaseMetaData metaData = con.getMetaData();
        String legalChars = metaData.getExtraNameCharacters();
        legalChars += "ABCDEFGHIJKLMNOPQRSTUVWXZY0123456789_";

        String prefix = schemaObjectPrefix.toUpperCase();
        StringBuffer escaped = new StringBuffer();
        for (int i = 0; i < prefix.length(); i++) {
            char c = prefix.charAt(i);
            if (legalChars.indexOf(c) == -1) {
                escaped.append("_x");
                String hex = Integer.toHexString(c);
                escaped.append("0000".toCharArray(), 0, 4 - hex.length());
                escaped.append(hex);
                escaped.append("_");
            } else {
                escaped.append(c);
            }
        }
        schemaObjectPrefix = escaped.toString();
    }

    /**
     * Checks if the required schema objects exist and creates them if they
     * don't exist yet.
     *
     * @throws Exception if an error occurs
     */
    protected void checkSchema() throws Exception {
        DatabaseMetaData metaData = con.getMetaData();
        String tableName = schemaObjectPrefix + "FSENTRY";
        if (metaData.storesLowerCaseIdentifiers()) {
            tableName = tableName.toLowerCase();
        } else if (metaData.storesUpperCaseIdentifiers()) {
            tableName = tableName.toUpperCase();
        }
        ResultSet rs = metaData.getTables(null, null, tableName, null);
        boolean schemaExists;
        try {
            schemaExists = rs.next();
        } finally {
            rs.close();
        }

        if (!schemaExists) {
            // read ddl from resources
            InputStream in = DatabaseFileSystem.class.getResourceAsStream(schema + ".ddl");
            if (in == null) {
                String msg = "Configuration error: unknown schema '" + schema + "'";
                log.debug(msg);
                throw new RepositoryException(msg);
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            Statement stmt = con.createStatement();
            try {
                String sql = reader.readLine();
                while (sql != null) {
                    // Skip comments and empty lines
                    if (!sql.startsWith("#") && sql.length() > 0) {
                        // replace prefix variable
                        sql = createSchemaSql(sql);
                        stmt.executeUpdate(sql);
                    }
                    // read next sql stmt
                    sql = reader.readLine();
                }
            } finally {
                IOUtils.closeQuietly(in);
                closeStatement(stmt);
            }
        }
    }

    /**
     * Replace wildcards.
     */
    protected String createSchemaSql(String sql) {
        sql = Text.replace(sql, SCHEMA_OBJECT_PREFIX_VARIABLE, schemaObjectPrefix);
        return sql;
    }

    /**
     * Builds the SQL statements
     */
    protected void buildSQLStatements() {
        insertFileSQL = "insert into "
                + schemaObjectPrefix + "FSENTRY "
                + "(FSENTRY_PATH, FSENTRY_NAME, FSENTRY_DATA, "
                + "FSENTRY_LASTMOD, FSENTRY_LENGTH) "
                + "values (?, ?, ?, ?, ?)";

        insertFolderSQL = "insert into "
                + schemaObjectPrefix + "FSENTRY "
                + "(FSENTRY_PATH, FSENTRY_NAME, FSENTRY_LASTMOD, FSENTRY_LENGTH) "
                + "values (?, ?, ?, 0)";

        updateDataSQL = "update "
                + schemaObjectPrefix + "FSENTRY "
                + "set FSENTRY_DATA = ?, FSENTRY_LASTMOD = ?, FSENTRY_LENGTH = ? "
                + "where FSENTRY_PATH = ? and FSENTRY_NAME = ? "
                + "and FSENTRY_DATA is not null";

        updateLastModifiedSQL = "update "
                + schemaObjectPrefix + "FSENTRY set FSENTRY_LASTMOD = ? "
                + "where FSENTRY_PATH = ? and FSENTRY_NAME = ? "
                + "and FSENTRY_DATA is not null";

        selectExistSQL = "select 1 from "
                + schemaObjectPrefix + "FSENTRY where FSENTRY_PATH = ? "
                + "and FSENTRY_NAME = ?";

        selectFileExistSQL = "select 1 from "
                + schemaObjectPrefix + "FSENTRY where FSENTRY_PATH = ? "
                + "and FSENTRY_NAME = ? and FSENTRY_DATA is not null";

        selectFolderExistSQL = "select 1 from "
                + schemaObjectPrefix + "FSENTRY where FSENTRY_PATH = ? "
                + "and FSENTRY_NAME = ? and FSENTRY_DATA is null";

        selectFileNamesSQL = "select FSENTRY_NAME from "
                + schemaObjectPrefix + "FSENTRY where FSENTRY_PATH = ? "
                + "and FSENTRY_DATA is not null";

        selectFolderNamesSQL = "select FSENTRY_NAME from "
                + schemaObjectPrefix + "FSENTRY where FSENTRY_PATH = ? "
                + "and FSENTRY_DATA is null";

        selectFileAndFolderNamesSQL = "select FSENTRY_NAME from "
                + schemaObjectPrefix + "FSENTRY where FSENTRY_PATH = ?";

        selectChildCountSQL = "select count(FSENTRY_NAME) from "
                + schemaObjectPrefix + "FSENTRY where FSENTRY_PATH = ?  ";

        selectDataSQL = "select FSENTRY_DATA from "
                + schemaObjectPrefix + "FSENTRY where FSENTRY_PATH = ? "
                + "and FSENTRY_NAME = ? and FSENTRY_DATA is not null";

        selectLastModifiedSQL = "select FSENTRY_LASTMOD from "
                + schemaObjectPrefix + "FSENTRY where FSENTRY_PATH = ? "
                + "and FSENTRY_NAME = ?";

        selectLengthSQL = "select FSENTRY_LENGTH from "
                + schemaObjectPrefix + "FSENTRY where FSENTRY_PATH = ? "
                + "and FSENTRY_NAME = ? and FSENTRY_DATA is not null";

        deleteFileSQL = "delete from "
                + schemaObjectPrefix + "FSENTRY where FSENTRY_PATH = ? "
                + "and FSENTRY_NAME = ? and FSENTRY_DATA is not null";

        deleteFolderSQL = "delete from "
                + schemaObjectPrefix + "FSENTRY where "
                + "(FSENTRY_PATH = ? and FSENTRY_NAME = ? and FSENTRY_DATA is null) "
                + "or (FSENTRY_PATH = ?) "
                + "or (FSENTRY_PATH like ?) ";

        copyFileSQL = "insert into "
                + schemaObjectPrefix + "FSENTRY "
                + "(FSENTRY_PATH, FSENTRY_NAME, FSENTRY_DATA, "
                + "FSENTRY_LASTMOD, FSENTRY_LENGTH) "
                + "select ?, ?, FSENTRY_DATA, "
                + "FSENTRY_LASTMOD, FSENTRY_LENGTH from "
                + schemaObjectPrefix + "FSENTRY where FSENTRY_PATH = ? "
                + "and FSENTRY_NAME = ? and FSENTRY_DATA is not null";

        copyFilesSQL = "insert into "
                + schemaObjectPrefix + "FSENTRY "
                + "(FSENTRY_PATH, FSENTRY_NAME, FSENTRY_DATA, "
                + "FSENTRY_LASTMOD, FSENTRY_LENGTH) "
                + "select ?, FSENTRY_NAME, FSENTRY_DATA, "
                + "FSENTRY_LASTMOD, FSENTRY_LENGTH from "
                + schemaObjectPrefix + "FSENTRY where FSENTRY_PATH = ? "
                + "and FSENTRY_DATA is not null";
    }

    /**
     * Initializes the map of prepared statements.
     *
     * @throws SQLException if an error occurs
     */
    protected void initPreparedStatements() throws SQLException {
        preparedStatements.put(
                selectExistSQL, con.prepareStatement(selectExistSQL));
        preparedStatements.put(
                selectFileExistSQL, con.prepareStatement(selectFileExistSQL));
        preparedStatements.put(
                selectFolderExistSQL, con.prepareStatement(selectFolderExistSQL));
        preparedStatements.put(
                selectChildCountSQL, con.prepareStatement(selectChildCountSQL));
        preparedStatements.put(
                selectDataSQL, con.prepareStatement(selectDataSQL));
        preparedStatements.put(
                selectLastModifiedSQL, con.prepareStatement(selectLastModifiedSQL));
        preparedStatements.put(
                selectLengthSQL, con.prepareStatement(selectLengthSQL));
        preparedStatements.put(
                selectFileNamesSQL, con.prepareStatement(selectFileNamesSQL));
        preparedStatements.put(
                selectFolderNamesSQL, con.prepareStatement(selectFolderNamesSQL));
        preparedStatements.put(
                selectFileAndFolderNamesSQL, con.prepareStatement(selectFileAndFolderNamesSQL));
        preparedStatements.put(
                deleteFileSQL, con.prepareStatement(deleteFileSQL));
        preparedStatements.put(
                deleteFolderSQL, con.prepareStatement(deleteFolderSQL));
        preparedStatements.put(
                insertFileSQL, con.prepareStatement(insertFileSQL));
        preparedStatements.put(
                insertFolderSQL, con.prepareStatement(insertFolderSQL));
        preparedStatements.put(
                updateDataSQL, con.prepareStatement(updateDataSQL));
        preparedStatements.put(
                updateLastModifiedSQL, con.prepareStatement(updateLastModifiedSQL));
        preparedStatements.put(
                copyFileSQL, con.prepareStatement(copyFileSQL));
        preparedStatements.put(
                copyFilesSQL, con.prepareStatement(copyFilesSQL));

    }

    /**
     * Verifies that the root file system entry exists. If it doesn't exist yet
     * it will be automatically created.
     *
     * @throws Exception if an error occurs
     */
    protected void verifyRootExists() throws Exception {
        // check if root file system entry exists
        synchronized (selectFolderExistSQL) {
            ResultSet rs = null;
            try {
                Statement stmt = executeStmt(
                        selectFolderExistSQL,
                        new Object[]{FileSystem.SEPARATOR, ""});
                rs = stmt.getResultSet();

                if (rs.next()) {
                    // root entry exists
                    return;
                }
            } catch (SQLException e) {
                String msg = "failed to check existence of file system root entry";
                log.error(msg, e);
                throw new FileSystemException(msg, e);
            } finally {
                closeResultSet(rs);
            }
        }

        // the root entry doesn't exist yet, create it...
        createDeepFolder(FileSystem.SEPARATOR);
    }

    /**
     * Creates the specified files system folder entry, recursively creating
     * any non-existing intermediate folder entries.
     *
     * @param folderPath folder entry to create
     * @throws FileSystemException if an error occurs
     */
    protected void createDeepFolder(String folderPath)
            throws FileSystemException {
        String parentDir = FileSystemPathUtil.getParentDir(folderPath);
        String name = FileSystemPathUtil.getName(folderPath);

        if (!FileSystemPathUtil.denotesRoot(folderPath)) {
            if (!exists(parentDir)) {
                createDeepFolder(parentDir);
            }
        }

        synchronized (insertFolderSQL) {
            try {
                executeStmt(
                        insertFolderSQL,
                        new Object[]{
                                parentDir,
                                name,
                                new Long(System.currentTimeMillis())});
            } catch (SQLException e) {
                String msg = "failed to create folder entry: " + folderPath;
                log.error(msg, e);
                throw new FileSystemException(msg, e);
            }
        }
    }

    /**
     * Recursively copies the given folder to the given destination.
     *
     * @param srcPath folder to be copied
     * @param destPath destination path to which the folder is to be copied
     * @throws FileSystemException if an error occurs
     */
    protected void copyDeepFolder(String srcPath, String destPath)
            throws FileSystemException {

        if (!exists(destPath)) {
            createDeepFolder(destPath);
        }

        String[] names = listFolders(srcPath);

        for (int i = 0; i < names.length; i++) {
            String src = (FileSystemPathUtil.denotesRoot(srcPath)
                    ? srcPath + names[i] : srcPath + FileSystem.SEPARATOR + names[i]);
            String dest = (FileSystemPathUtil.denotesRoot(destPath)
                    ? destPath + names[i] : destPath + FileSystem.SEPARATOR + names[i]);
            copyDeepFolder(src, dest);
        }

        synchronized (copyFilesSQL) {
            try {
                executeStmt(copyFilesSQL, new Object[]{destPath, srcPath});
            } catch (SQLException e) {
                String msg = "failed to copy file entries from " + srcPath + " to " + destPath;
                log.error(msg, e);
                throw new FileSystemException(msg, e);
            }
        }
    }

    /**
     * Copies the given file entry to the given destination path. The parent
     * folder of the destination path will be created if it doesn't exist
     * already. If the destination path refers to an existing file, the file
     * will be overwritten.
     *
     * @param srcPath file to be copied
     * @param destPath destination path to which the file is to be copied
     * @throws FileSystemException if an error occurs
     */
    protected void copyFile(String srcPath, String destPath)
            throws FileSystemException {

        final String srcParentDir = FileSystemPathUtil.getParentDir(srcPath);
        final String srcName = FileSystemPathUtil.getName(srcPath);

        final String destParentDir = FileSystemPathUtil.getParentDir(destPath);
        final String destName = FileSystemPathUtil.getName(destPath);

        if (!exists(destParentDir)) {
            createDeepFolder(destParentDir);
        }
        if (isFile(destPath)) {
            deleteFile(destPath);
        }

        int count = 0;
        synchronized (copyFileSQL) {
            try {
                Statement stmt = executeStmt(
                        copyFileSQL,
                        new Object[]{
                                destParentDir,
                                destName,
                                srcParentDir,
                                srcName});
                count = stmt.getUpdateCount();
            } catch (SQLException e) {
                String msg = "failed to copy file from " + srcPath + " to " + destPath;
                log.error(msg, e);
                throw new FileSystemException(msg, e);
            }
        }

        if (count == 0) {
            throw new FileSystemException("no such file: " + srcPath);
        }
    }

    /**
     * Resets the given <code>PreparedStatement</code> by clearing the parameters
     * and warnings contained.
     * <p/>
     * NOTE: This method MUST be called in a synchronized context as neither
     * this method nor the <code>PreparedStatement</code> instance on which it
     * operates are thread safe.
     *
     * @param stmt The <code>PreparedStatement</code> to reset. If
     *             <code>null</code> this method does nothing.
     */
    protected void resetStatement(PreparedStatement stmt) {
        if (stmt != null) {
            try {
                stmt.clearParameters();
                stmt.clearWarnings();
            } catch (SQLException se) {
                log.error("failed resetting PreparedStatement", se);
            }
        }
    }

    protected void closeResultSet(ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException se) {
                log.error("failed closing ResultSet", se);
            }
        }
    }

    protected void closeStatement(Statement stmt) {
        if (stmt != null) {
            try {
                stmt.close();
            } catch (SQLException se) {
                log.error("failed closing Statement", se);
            }
        }
    }

    //--------------------------------------------------------< inner classes >

    class SizedInputStream extends FilterInputStream {
        private final long size;
        private boolean consumed = false;

        SizedInputStream(InputStream in, long size) {
            super(in);
            this.size = size;
        }

        long getSize() {
            return size;
        }

        boolean isConsumed() {
            return consumed;
        }

        public int read() throws IOException {
            consumed = true;
            return super.read();
        }

        public long skip(long n) throws IOException {
            consumed = true;
            return super.skip(n);
        }

        public int read(byte[] b) throws IOException {
            consumed = true;
            return super.read(b);
        }

        public int read(byte[] b, int off, int len) throws IOException {
            consumed = true;
            return super.read(b, off, len);
        }
    }
}
