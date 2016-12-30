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
import org.apache.jackrabbit.core.util.db.CheckSchemaOperation;
import org.apache.jackrabbit.core.util.db.ConnectionHelper;
import org.apache.jackrabbit.core.util.db.DbUtility;
import org.apache.jackrabbit.core.util.db.StreamWrapper;
import org.apache.jackrabbit.util.TransientFileFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileInputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 * Base class for database file systems. This class contains common
 * functionality for database file system subclasses that normally differ only
 * in the way the database connection is acquired. <!-- FIXME: Subclasses should override
 * the {@link #getConnection()} method to return the configured database
 * connection. -->
 * <p>
 * See the {@link DbFileSystem} for a detailed description of the available
 * configuration options and database behaviour.
 */
public abstract class DatabaseFileSystem implements FileSystem {

    /**
     * Logger instance
     */
    private static Logger log = LoggerFactory.getLogger(DatabaseFileSystem.class);

    protected boolean initialized;

    protected String schema;
    protected String schemaObjectPrefix;

    // initial size of buffer used to serialize objects
    protected static final int INITIAL_BUFFER_SIZE = 8192;

    /**
     * Whether the schema check must be done during initialization.
     */
    private boolean schemaCheckEnabled = true;

    /** the {@link ConnectionHelper} set in the {@link #init()} method */
    protected ConnectionHelper conHelper;

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
            conHelper = createConnectionHelper(getDataSource());

            // make sure schemaObjectPrefix consists of legal name characters only
            schemaObjectPrefix = conHelper.prepareDbIdentifier(schemaObjectPrefix);

            // check if schema objects exist and create them if necessary
            if (isSchemaCheckEnabled()) {
                createCheckSchemaOperation().run();
            }

            // build sql statements
            buildSQLStatements();

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
     * @return
     * @throws Exception
     */
    protected abstract DataSource getDataSource() throws Exception;

    /**
     * This method is called from the {@link #init()} method of this class and returns a
     * {@link ConnectionHelper} instance which is assigned to the {@code conHelper} field. Subclasses may
     * override it to return a specialized connection helper.
     * 
     * @param dataSrc the {@link DataSource} of this persistence manager
     * @return a {@link ConnectionHelper}
     * @throws Exception on error
     */
    protected ConnectionHelper createConnectionHelper(DataSource dataSrc) throws Exception {
        return new ConnectionHelper(dataSrc, false);
    }

    /**
     * This method is called from {@link #init()} after the
     * {@link #createConnectionHelper(DataSource)} method, and returns a default {@link CheckSchemaOperation}.
     * Subclasses can overrride this implementation to get a customized implementation.
     * 
     * @return a new {@link CheckSchemaOperation} instance
     */
    protected CheckSchemaOperation createCheckSchemaOperation() {
        InputStream in = DatabaseFileSystem.class.getResourceAsStream(getSchema() + ".ddl");
        return new CheckSchemaOperation(conHelper, in, schemaObjectPrefix + "FSENTRY").addVariableReplacement(
            CheckSchemaOperation.SCHEMA_OBJECT_PREFIX_VARIABLE, schemaObjectPrefix);
    }

    /**
     * {@inheritDoc}
     */
    public void close() throws FileSystemException {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
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
                count = conHelper.update(
                        deleteFileSQL, new Object[]{parentDir, name});
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
                count = conHelper.update(deleteFolderSQL, new Object[]{
                        parentDir,
                        name,
                        folderPath,
                        folderPath + FileSystem.SEPARATOR + "%"});
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
                rs = conHelper.exec(
                        selectExistSQL, new Object[]{parentDir, name}, false, 0);

                // a file system entry exists if the result set
                // has at least one entry
                return rs.next();
            } catch (SQLException e) {
                String msg = "failed to check existence of file system entry: " + path;
                log.error(msg, e);
                throw new FileSystemException(msg, e);
            } finally {
                DbUtility.close(rs);
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
                rs = conHelper.exec(
                        selectFileExistSQL, new Object[]{parentDir, name}, false, 0);

                // a file exists if the result set has at least one entry
                return rs.next();
            } catch (SQLException e) {
                String msg = "failed to check existence of file: " + path;
                log.error(msg, e);
                throw new FileSystemException(msg, e);
            } finally {
                DbUtility.close(rs);
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
                rs = conHelper.exec(
                        selectFolderExistSQL, new Object[]{parentDir, name}, false, 0);

                // a folder exists if the result set has at least one entry
                return rs.next();
            } catch (SQLException e) {
                String msg = "failed to check existence of folder: " + path;
                log.error(msg, e);
                throw new FileSystemException(msg, e);
            } finally {
                DbUtility.close(rs);
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
                rs = conHelper.exec(
                        selectLastModifiedSQL, new Object[]{parentDir, name}, false, 0);
                if (!rs.next()) {
                    throw new FileSystemException("no such file system entry: " + path);
                }
                return rs.getLong(1);
            } catch (SQLException e) {
                String msg = "failed to determine lastModified of file system entry: " + path;
                log.error(msg, e);
                throw new FileSystemException(msg, e);
            } finally {
                DbUtility.close(rs);
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
                rs = conHelper.exec(
                        selectLengthSQL, new Object[]{parentDir, name}, false, 0);
                if (!rs.next()) {
                    throw new FileSystemException("no such file: " + filePath);
                }
                return rs.getLong(1);
            } catch (SQLException e) {
                String msg = "failed to determine length of file: " + filePath;
                log.error(msg, e);
                throw new FileSystemException(msg, e);
            } finally {
                DbUtility.close(rs);
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
                rs = conHelper.exec(selectChildCountSQL, new Object[]{path}, false, 0);
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
                DbUtility.close(rs);
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
                rs = conHelper.exec(
                        selectFileAndFolderNamesSQL, new Object[]{folderPath}, false, 0);
                ArrayList<String> names = new ArrayList<String>();
                while (rs.next()) {
                    String name = rs.getString(1);
                    if (name.length() == 0
                            && FileSystemPathUtil.denotesRoot(folderPath)) {
                        // this is the file system root entry, skip...
                        continue;
                    }
                    names.add(name);
                }
                return names.toArray(new String[names.size()]);
            } catch (SQLException e) {
                String msg = "failed to list child entries of folder: " + folderPath;
                log.error(msg, e);
                throw new FileSystemException(msg, e);
            } finally {
                DbUtility.close(rs);
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
                rs = conHelper.exec(
                        selectFileNamesSQL, new Object[]{folderPath}, false, 0);
                ArrayList<String> names = new ArrayList<String>();
                while (rs.next()) {
                    names.add(rs.getString(1));
                }
                return names.toArray(new String[names.size()]);
            } catch (SQLException e) {
                String msg = "failed to list file entries of folder: " + folderPath;
                log.error(msg, e);
                throw new FileSystemException(msg, e);
            } finally {
                DbUtility.close(rs);
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
                rs = conHelper.exec(
                        selectFolderNamesSQL, new Object[]{folderPath}, false, 0);
                ArrayList<String> names = new ArrayList<String>();
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
                DbUtility.close(rs);
            }
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
                final ResultSet rs = conHelper.exec(
                        selectDataSQL, new Object[]{parentDir, name}, false, 0);

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
                       DbUtility.close(rs);
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
                                conHelper.exec(updateDataSQL,
                                        new Object[]{
                                            new StreamWrapper(in, length),
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
                                conHelper.exec(insertFileSQL,
                                        new Object[]{
                                            parentDir,
                                            name,
                                            new StreamWrapper(in, length),
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

    //----------------------------------< misc. helper methods & overridables >

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
                rs = conHelper.exec(
                        selectFolderExistSQL,
                        new Object[]{FileSystem.SEPARATOR, ""}, false, 0);

                if (rs.next()) {
                    // root entry exists
                    return;
                }
            } catch (SQLException e) {
                String msg = "failed to check existence of file system root entry";
                log.error(msg, e);
                throw new FileSystemException(msg, e);
            } finally {
                DbUtility.close(rs);
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
                conHelper.exec(
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

}
