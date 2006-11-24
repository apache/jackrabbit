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
import java.util.List;
import java.util.LinkedList;

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
    private static Logger log = LoggerFactory.getLogger(DbFileSystem.class);

    protected static final String SCHEMA_OBJECT_PREFIX_VARIABLE =
            "${schemaObjectPrefix}";

    protected boolean initialized;

    protected String schema;
    protected String schemaObjectPrefix;

    // initial size of buffer used to serialize objects
    protected static final int INITIAL_BUFFER_SIZE = 8192;

    // jdbc connection
    protected Connection con;

    // the list of prepared statements, used in close()
    private List preparedStatements;

    // shared prepared statements
    protected PreparedStatement selectExistStmt;
    protected PreparedStatement selectFileExistStmt;
    protected PreparedStatement selectFolderExistStmt;
    protected PreparedStatement selectChildCountStmt;
    protected PreparedStatement selectDataStmt;
    protected PreparedStatement selectLastModifiedStmt;
    protected PreparedStatement selectLengthStmt;
    protected PreparedStatement selectFileNamesStmt;
    protected PreparedStatement selectFolderNamesStmt;
    protected PreparedStatement selectFileAndFolderNamesStmt;
    protected PreparedStatement deleteFileStmt;
    protected PreparedStatement deleteFolderStmt;
    protected PreparedStatement insertFileStmt;
    protected PreparedStatement insertFolderStmt;
    protected PreparedStatement updateDataStmt;
    protected PreparedStatement updateLastModifiedStmt;
    protected PreparedStatement copyFileStmt;
    protected PreparedStatement copyFilesStmt;

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

    //-------------------------------------------< java.lang.Object overrides >
    /**
     * {@inheritDoc}
     */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DatabaseFileSystem) {
            DatabaseFileSystem other = (DatabaseFileSystem) obj;
            if (((schema != null) ? schema.equals(other.schema) : other.schema == null)
                    && ((schemaObjectPrefix != null) ? schemaObjectPrefix.equals(other.schemaObjectPrefix) : other.schemaObjectPrefix == null)) {
                return true;
            }
        }
        return false;
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
            checkSchema();

            // prepare statements
            preparedStatements = initPreparedStatements();

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
            if (preparedStatements != null) {
                while (!preparedStatements.isEmpty()) {
                    closeStatement(
                            (PreparedStatement) preparedStatements.remove(0)
                    );
                }
            }
            preparedStatements = null;

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
        PreparedStatement stmt = deleteFileStmt;
        synchronized (stmt) {
            try {
                stmt.setString(1, parentDir);
                stmt.setString(2, name);
                count = stmt.executeUpdate();
            } catch (SQLException e) {
                String msg = "failed to delete file: " + filePath;
                log.error(msg, e);
                throw new FileSystemException(msg, e);
            } finally {
                resetStatement(stmt);
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
        PreparedStatement stmt = deleteFolderStmt;
        synchronized (stmt) {
            try {
                stmt.setString(1, parentDir);
                stmt.setString(2, name);
                stmt.setString(3, folderPath);
                stmt.setString(4, folderPath + FileSystem.SEPARATOR + "%");
                count = stmt.executeUpdate();
            } catch (SQLException e) {
                String msg = "failed to delete folder: " + folderPath;
                log.error(msg, e);
                throw new FileSystemException(msg, e);
            } finally {
                resetStatement(stmt);
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

        PreparedStatement stmt = selectExistStmt;
        synchronized (stmt) {
            ResultSet rs = null;
            try {
                stmt.setString(1, parentDir);
                stmt.setString(2, name);
                stmt.execute();
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
                resetStatement(stmt);
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

        PreparedStatement stmt = selectFileExistStmt;
        synchronized (stmt) {
            ResultSet rs = null;
            try {
                stmt.setString(1, parentDir);
                stmt.setString(2, name);
                stmt.execute();
                rs = stmt.getResultSet();

                // a file exists if the result set has at least one entry
                return rs.next();
            } catch (SQLException e) {
                String msg = "failed to check existence of file: " + path;
                log.error(msg, e);
                throw new FileSystemException(msg, e);
            } finally {
                closeResultSet(rs);
                resetStatement(stmt);
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

        PreparedStatement stmt = selectFolderExistStmt;
        synchronized (stmt) {
            ResultSet rs = null;
            try {
                stmt.setString(1, parentDir);
                stmt.setString(2, name);
                stmt.execute();
                rs = stmt.getResultSet();

                // a folder exists if the result set has at least one entry
                return rs.next();
            } catch (SQLException e) {
                String msg = "failed to check existence of folder: " + path;
                log.error(msg, e);
                throw new FileSystemException(msg, e);
            } finally {
                closeResultSet(rs);
                resetStatement(stmt);
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

        PreparedStatement stmt = selectLastModifiedStmt;
        synchronized (stmt) {
            ResultSet rs = null;
            try {
                stmt.setString(1, parentDir);
                stmt.setString(2, name);
                stmt.execute();
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
                resetStatement(stmt);
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

        PreparedStatement stmt = selectLengthStmt;
        synchronized (stmt) {
            ResultSet rs = null;
            try {
                stmt.setString(1, parentDir);
                stmt.setString(2, name);
                stmt.execute();
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
                resetStatement(stmt);
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

        PreparedStatement stmt = selectChildCountStmt;
        synchronized (stmt) {
            ResultSet rs = null;
            try {
                stmt.setString(1, path);
                stmt.execute();
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
                resetStatement(stmt);
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

        PreparedStatement stmt = selectFileAndFolderNamesStmt;
        synchronized (stmt) {
            ResultSet rs = null;
            try {
                stmt.setString(1, folderPath);
                stmt.execute();
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
                resetStatement(stmt);
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

        PreparedStatement stmt = selectFileNamesStmt;
        synchronized (stmt) {
            ResultSet rs = null;
            try {
                stmt.setString(1, folderPath);
                stmt.execute();
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
                resetStatement(stmt);
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

        PreparedStatement stmt = selectFolderNamesStmt;
        synchronized (stmt) {
            ResultSet rs = null;
            try {
                stmt.setString(1, folderPath);
                stmt.execute();
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
                resetStatement(stmt);
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
        PreparedStatement stmt = updateLastModifiedStmt;
        synchronized (stmt) {
            try {
                stmt.setLong(1, System.currentTimeMillis());
                stmt.setString(2, parentDir);
                stmt.setString(3, name);
                count = stmt.executeUpdate();
            } catch (SQLException e) {
                String msg = "failed to touch file: " + filePath;
                log.error(msg, e);
                throw new FileSystemException(msg, e);
            } finally {
                resetStatement(stmt);
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

        PreparedStatement stmt = selectDataStmt;
        synchronized (stmt) {
            try {
                stmt.setString(1, parentDir);
                stmt.setString(2, name);
                stmt.execute();
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
            } finally {
                resetStatement(stmt);
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

                public void close() throws IOException {
                    super.close();

                    PreparedStatement stmt = null;
                    InputStream in = null;
                    try {
                        if (isFile(filePath)) {
                            stmt = updateDataStmt;
                            synchronized (stmt) {
                                long length = tmpFile.length();
                                in = new FileInputStream(tmpFile);
                                stmt.setBinaryStream(1, in, (int) length);
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
                            stmt.setBinaryStream(3, in, (int) length);
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
                    try {
                        if (isFile(filePath)) {
                            stmt = updateDataStmt;
                            synchronized (stmt) {
                                long length = tmpFile.length();
                                in = new FileInputStream(tmpFile);
                                stmt.setBinaryStream(1, in, (int) length);
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
                            stmt.setBinaryStream(3, in, (int) length);
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
        con.setAutoCommit(true);
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
            InputStream in = getClass().getResourceAsStream(schema + ".ddl");
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
                        sql = Text.replace(sql, SCHEMA_OBJECT_PREFIX_VARIABLE, schemaObjectPrefix);
                        // execute sql stmt
                        stmt.executeUpdate(sql);
                    }
                    // read next sql stmt
                    sql = reader.readLine();
                }
            } finally {
                closeStream(in);
                closeStatement(stmt);
            }
        }
    }

    /**
     * initializes the prepared statements and returns the list of them. please
     * note, that this list is used to close the statements in the {@link #close()}
     * call.
     *
     * @return the list of prepared statements.
     * @throws SQLException
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
                + "values (?, ?, ?, 0)"));

        stmts.add(updateDataStmt = con.prepareStatement("update "
                + schemaObjectPrefix + "FSENTRY "
                + "set FSENTRY_DATA = ?, FSENTRY_LASTMOD = ?, FSENTRY_LENGTH = ? "
                + "where FSENTRY_PATH = ? and FSENTRY_NAME = ? "
                + "and FSENTRY_DATA is not null"));

        stmts.add(updateLastModifiedStmt = con.prepareStatement("update "
                + schemaObjectPrefix + "FSENTRY set FSENTRY_LASTMOD = ? "
                + "where FSENTRY_PATH = ? and FSENTRY_NAME = ? "
                + "and FSENTRY_DATA is not null"));

        stmts.add(selectExistStmt = con.prepareStatement("select 1 from "
                + schemaObjectPrefix + "FSENTRY where FSENTRY_PATH = ? "
                + "and FSENTRY_NAME = ?"));

        stmts.add(selectFileExistStmt = con.prepareStatement("select 1 from "
                + schemaObjectPrefix + "FSENTRY where FSENTRY_PATH = ? "
                + "and FSENTRY_NAME = ? and FSENTRY_DATA is not null"));

        stmts.add(selectFolderExistStmt = con.prepareStatement("select 1 from "
                + schemaObjectPrefix + "FSENTRY where FSENTRY_PATH = ? "
                + "and FSENTRY_NAME = ? and FSENTRY_DATA is null"));

        stmts.add(selectFileNamesStmt = con.prepareStatement("select FSENTRY_NAME from "
                + schemaObjectPrefix + "FSENTRY where FSENTRY_PATH = ? "
                + "and FSENTRY_DATA is not null"));

        stmts.add(selectFolderNamesStmt = con.prepareStatement("select FSENTRY_NAME from "
                + schemaObjectPrefix + "FSENTRY where FSENTRY_PATH = ? "
                + "and FSENTRY_DATA is null"));

        stmts.add(selectFileAndFolderNamesStmt = con.prepareStatement("select FSENTRY_NAME from "
                + schemaObjectPrefix + "FSENTRY where FSENTRY_PATH = ?"));

        stmts.add(selectChildCountStmt = con.prepareStatement("select count(FSENTRY_NAME) from "
                + schemaObjectPrefix + "FSENTRY where FSENTRY_PATH = ?  "));

        stmts.add(selectDataStmt = con.prepareStatement("select FSENTRY_DATA from "
                + schemaObjectPrefix + "FSENTRY where FSENTRY_PATH = ? "
                + "and FSENTRY_NAME = ? and FSENTRY_DATA is not null"));

        stmts.add(selectLastModifiedStmt = con.prepareStatement("select FSENTRY_LASTMOD from "
                + schemaObjectPrefix + "FSENTRY where FSENTRY_PATH = ? "
                + "and FSENTRY_NAME = ?"));

        stmts.add(selectLengthStmt = con.prepareStatement("select FSENTRY_LENGTH from "
                + schemaObjectPrefix + "FSENTRY where FSENTRY_PATH = ? "
                + "and FSENTRY_NAME = ? and FSENTRY_DATA is not null"));

        stmts.add(deleteFileStmt = con.prepareStatement("delete from "
                + schemaObjectPrefix + "FSENTRY where FSENTRY_PATH = ? "
                + "and FSENTRY_NAME = ? and FSENTRY_DATA is not null"));

        stmts.add(deleteFolderStmt = con.prepareStatement("delete from "
                + schemaObjectPrefix + "FSENTRY where "
                + "(FSENTRY_PATH = ? and FSENTRY_NAME = ? and FSENTRY_DATA is null) "
                + "or (FSENTRY_PATH = ?) "
                + "or (FSENTRY_PATH like ?) "));

        stmts.add(copyFileStmt = con.prepareStatement("insert into "
                + schemaObjectPrefix + "FSENTRY "
                + "(FSENTRY_PATH, FSENTRY_NAME, FSENTRY_DATA, "
                + "FSENTRY_LASTMOD, FSENTRY_LENGTH) "
                + "select ?, ?, FSENTRY_DATA, "
                + "FSENTRY_LASTMOD, FSENTRY_LENGTH from "
                + schemaObjectPrefix + "FSENTRY where FSENTRY_PATH = ? "
                + "and FSENTRY_NAME = ? and FSENTRY_DATA is not null"));

        stmts.add(copyFilesStmt = con.prepareStatement("insert into "
                + schemaObjectPrefix + "FSENTRY "
                + "(FSENTRY_PATH, FSENTRY_NAME, FSENTRY_DATA, "
                + "FSENTRY_LASTMOD, FSENTRY_LENGTH) "
                + "select ?, FSENTRY_NAME, FSENTRY_DATA, "
                + "FSENTRY_LASTMOD, FSENTRY_LENGTH from "
                + schemaObjectPrefix + "FSENTRY where FSENTRY_PATH = ? "
                + "and FSENTRY_DATA is not null"));

        return stmts;
    }

    /**
     * Verifies that the root file system entry exists. If it doesn't exist yet
     * it will be automatically created.
     *
     * @throws Exception if an error occurs
     */
    protected void verifyRootExists() throws Exception {
        // check if root file system entry exists
        PreparedStatement stmt = selectFolderExistStmt;
        synchronized (stmt) {
            ResultSet rs = null;
            try {
                stmt.setString(1, FileSystem.SEPARATOR);
                stmt.setString(2, "");
                stmt.execute();
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
                resetStatement(stmt);
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

        PreparedStatement stmt = insertFolderStmt;
        synchronized (stmt) {
            try {
                stmt.setString(1, parentDir);
                stmt.setString(2, name);
                stmt.setLong(3, System.currentTimeMillis());
                stmt.executeUpdate();
            } catch (SQLException e) {
                String msg = "failed to create folder entry: " + folderPath;
                log.error(msg, e);
                throw new FileSystemException(msg, e);
            } finally {
                resetStatement(stmt);
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

        PreparedStatement stmt = copyFilesStmt;
        synchronized (stmt) {
            try {
                stmt.setString(1, destPath);
                stmt.setString(2, srcPath);
                stmt.executeUpdate();
            } catch (SQLException e) {
                String msg = "failed to copy file entries from " + srcPath + " to " + destPath;
                log.error(msg, e);
                throw new FileSystemException(msg, e);
            } finally {
                resetStatement(stmt);
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
        PreparedStatement stmt = copyFileStmt;
        synchronized (stmt) {
            try {
                stmt.setString(1, destParentDir);
                stmt.setString(2, destName);
                stmt.setString(3, srcParentDir);
                stmt.setString(4, srcName);
                count = stmt.executeUpdate();
            } catch (SQLException e) {
                String msg = "failed to copy file from " + srcPath + " to " + destPath;
                log.error(msg, e);
                throw new FileSystemException(msg, e);
            } finally {
                resetStatement(stmt);
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

    protected void closeStream(InputStream in) {
        if (in != null) {
            try {
                in.close();
            } catch (IOException ignore) {
                // ignore
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
}
