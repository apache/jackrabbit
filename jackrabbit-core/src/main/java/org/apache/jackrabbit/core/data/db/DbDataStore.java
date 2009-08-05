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
package org.apache.jackrabbit.core.data.db;

import org.apache.jackrabbit.core.data.DataIdentifier;
import org.apache.jackrabbit.core.data.DataRecord;
import org.apache.jackrabbit.core.data.DataStore;
import org.apache.jackrabbit.core.data.DataStoreException;
import org.apache.jackrabbit.core.persistence.bundle.util.ConnectionRecoveryManager;
import org.apache.jackrabbit.core.persistence.bundle.util.TrackingInputStream;
import org.apache.jackrabbit.core.persistence.bundle.util.ConnectionRecoveryManager.StreamWrapper;
import org.apache.jackrabbit.util.Text;
import org.apache.jackrabbit.uuid.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.WeakHashMap;

import javax.jcr.RepositoryException;

/**
 * A data store implementation that stores the records in a database using JDBC.
 *
 * Configuration:
 * <pre>
 * &lt;DataStore class="org.apache.jackrabbit.core.data.db.DbDataStore">
 *     &lt;param name="{@link #setUrl(String) url}" value="jdbc:postgresql:test"/>
 *     &lt;param name="{@link #setUser(String) user}" value="sa"/>
 *     &lt;param name="{@link #setPassword(String) password}" value="sa"/>
 *     &lt;param name="{@link #setDatabaseType(String) databaseType}" value="postgresql"/>
 *     &lt;param name="{@link #setDriver(String) driver}" value="org.postgresql.Driver"/>
 *     &lt;param name="{@link #setMinRecordLength(int) minRecordLength}" value="1024"/>
 *     &lt;param name="{@link #setMaxConnections(int) maxConnections}" value="2"/>
 *     &lt;param name="{@link #setCopyWhenReading(boolean) copyWhenReading}" value="true"/>
 *     &lt;param name="{@link #setTablePrefix(String) tablePrefix}" value=""/>
 *     &lt;param name="{@link #setSchemaObjectPrefix(String) schemaObjectPrefix}" value=""/>
 *     &lt;param name="{@link #setSchemaCheckEnabled(String) schemaCheckEnabled}" value="true"/>
 * &lt/DataStore>
 * </pre>
 * <p>
 * Only URL, user name and password usually need to be set.
 * The remaining settings are generated using the database URL sub-protocol from the
 * database type resource file.
 * <p>
 * JNDI can be used to get the connection. In this case, use the javax.naming.InitialContext as the driver,
 * and the JNDI name as the URL. If the user and password are configured in the JNDI resource,
 * they should not be configured here. Example JNDI settings:
 * <pre>
 * &lt;param name="driver" value="javax.naming.InitialContext" />
 * &lt;param name="url" value="java:comp/env/jdbc/Test" />
 * </pre>
 * <p>
 * For Microsoft SQL Server 2005, there is a problem reading large BLOBs. You will need to use
 * the JDBC driver version 1.2 or newer, and append ;responseBuffering=adaptive to the database URL.
 * Don't append ;selectMethod=cursor, otherwise it can still run out of memory.
 * Example database URL: jdbc:sqlserver://localhost:4220;DatabaseName=test;responseBuffering=adaptive
 * <p>
 * By default, the data is copied to a temp file when reading, to avoid problems when reading multiple
 * blobs at the same time.
 * <p>
 * The tablePrefix can be used to specify a schema and / or catalog name:
 * &lt;param name="tablePrefix" value="ds.">
 */
public class DbDataStore implements DataStore {

    /**
     * The default value for the minimum object size.
     */
    public static final int DEFAULT_MIN_RECORD_LENGTH = 100;

    /**
     * The default value for the maximum connections.
     */
    public static final int DEFAULT_MAX_CONNECTIONS = 3;
    
    /**
     * Write to a temporary file to get the length (slow, but always works).
     * This is the default setting.
     */
    public static final String STORE_TEMP_FILE = "tempFile";

    /**
     * Call PreparedStatement.setBinaryStream(..., -1)
     */
    public static final String STORE_SIZE_MINUS_ONE = "-1";

    /**
     * Call PreparedStatement.setBinaryStream(..., Integer.MAX_VALUE)
     */
    public static final String STORE_SIZE_MAX = "max";
    
    /**
     * The digest algorithm used to uniquely identify records.
     */
    protected static final String DIGEST = "SHA-1";
    
    /**
     * The prefix used for temporary objects.
     */
    protected static final String TEMP_PREFIX = "TEMP_";

    /**
     * Logger instance
     */
    private static Logger log = LoggerFactory.getLogger(DbDataStore.class);

    /**
     * The minimum modified date. If a file is accessed (read or write) with a modified date
     * older than this value, the modified date is updated to the current time.
     */
    protected long minModifiedDate;

    /**
     * The database URL used.
     */
    protected String url;

    /**
     * The database driver.
     */
    protected String driver;

    /**
     * The user name.
     */
    protected String user;

    /**
     * The password
     */
    protected String password;

    /**
     * The database type used.
     */
    protected String databaseType;

    /**
     * The minimum size of an object that should be stored in this data store.
     */
    protected int minRecordLength = DEFAULT_MIN_RECORD_LENGTH;

    /**
     * The maximum number of open connections.
     */
    protected int maxConnections = DEFAULT_MAX_CONNECTIONS;

    /**
     * A list of connections
     */
    protected Pool connectionPool;

    /**
     * The prefix for the datastore table, empty by default.
     */
    protected String tablePrefix = "";

    /**
     * The prefix of the table names. By default it is empty.
     */
    protected String schemaObjectPrefix = "";

    /**
     * Whether the schema check must be done during initialization.
     */
    private boolean schemaCheckEnabled = true;

    /**
     * This is the property 'table'
     * in the [databaseType].properties file, initialized with the default value.
     */
    protected String tableSQL = "DATASTORE";
    
    /**
     * This is the property 'createTable'
     * in the [databaseType].properties file, initialized with the default value.
     */
    protected String createTableSQL =
        "CREATE TABLE ${tablePrefix}${table}(ID VARCHAR(255) PRIMARY KEY, LENGTH BIGINT, LAST_MODIFIED BIGINT, DATA BLOB)";

    /**
     * This is the property 'insertTemp'
     * in the [databaseType].properties file, initialized with the default value.
     */
    protected String insertTempSQL =
        "INSERT INTO ${tablePrefix}${table} VALUES(?, 0, ?, NULL)";

    /**
     * This is the property 'updateData'
     * in the [databaseType].properties file, initialized with the default value.
     */
    protected String updateDataSQL =
        "UPDATE ${tablePrefix}${table} SET DATA=? WHERE ID=?";

    /**
     * This is the property 'updateLastModified'
     * in the [databaseType].properties file, initialized with the default value.
     */
    protected String updateLastModifiedSQL =
        "UPDATE ${tablePrefix}${table} SET LAST_MODIFIED=? WHERE ID=? AND LAST_MODIFIED<?";

    /**
     * This is the property 'update'
     * in the [databaseType].properties file, initialized with the default value.
     */
    protected String updateSQL =
        "UPDATE ${tablePrefix}${table} SET ID=?, LENGTH=?, LAST_MODIFIED=?"
        + " WHERE ID=? AND NOT EXISTS(SELECT ID FROM ${tablePrefix}${table} WHERE ID=?)";

    /**
     * This is the property 'delete'
     * in the [databaseType].properties file, initialized with the default value.
     */
    protected String deleteSQL =
        "DELETE FROM ${tablePrefix}${table} WHERE ID=?";

    /**
     * This is the property 'deleteOlder'
     * in the [databaseType].properties file, initialized with the default value.
     */
    protected String deleteOlderSQL =
        "DELETE FROM ${tablePrefix}${table} WHERE LAST_MODIFIED<?";

    /**
     * This is the property 'selectMeta'
     * in the [databaseType].properties file, initialized with the default value.
     */
    protected String selectMetaSQL =
        "SELECT LENGTH, LAST_MODIFIED FROM ${tablePrefix}${table} WHERE ID=?";

    /**
     * This is the property 'selectAll'
     * in the [databaseType].properties file, initialized with the default value.
     */
    protected String selectAllSQL =
        "SELECT ID FROM ${tablePrefix}${table}";

    /**
     * This is the property 'selectData'
     * in the [databaseType].properties file, initialized with the default value.
     */
    protected String selectDataSQL =
        "SELECT ID, DATA FROM ${tablePrefix}${table} WHERE ID=?";

    /**
     * The stream storing mechanism used.
     */
    protected String storeStream = STORE_TEMP_FILE;

    /**
     * Copy the stream to a temp file before returning it.
     * Enabled by default to support concurrent reads.
     */
    protected boolean copyWhenReading = true;

    /**
     * All data identifiers that are currently in use are in this set until they are garbage collected.
     */
    protected Map inUse = Collections.synchronizedMap(new WeakHashMap());
    
    /**
     * The temporary identifiers that are currently in use.
     */
    protected List temporaryInUse = Collections.synchronizedList(new ArrayList());

    /**
     * {@inheritDoc}
     */
    public DataRecord addRecord(InputStream stream) throws DataStoreException {
        ResultSet rs = null;
        TempFileInputStream fileInput = null;
        ConnectionRecoveryManager conn = getConnection();
        String id = null, tempId = null;
        try {
            long now;
            for (int i = 0; i < ConnectionRecoveryManager.TRIALS; i++) {
                try {
                    now = System.currentTimeMillis();
                    id = UUID.randomUUID().toString();
                    tempId = TEMP_PREFIX + id;
                    // SELECT LENGTH, LAST_MODIFIED FROM DATASTORE WHERE ID=?
                    PreparedStatement prep = conn.executeStmt(selectMetaSQL, new Object[]{tempId});
                    rs = prep.getResultSet();
                    if (rs.next()) {
                        // re-try in the very, very unlikely event that the row already exists
                        continue;
                    }
                    // INSERT INTO DATASTORE VALUES(?, 0, ?, NULL)
                    conn.executeStmt(insertTempSQL, new Object[]{tempId, new Long(now)});
                    break;
                } catch (Exception e) {
                    throw convert("Can not insert new record", e);
                } finally {
                    DatabaseHelper.closeSilently(rs);
                }
            }
            if (id == null) {
                String msg = "Can not create new record";
                log.error(msg);
                throw new DataStoreException(msg);
            }
            temporaryInUse.add(tempId);
            MessageDigest digest = getDigest();
            DigestInputStream dIn = new DigestInputStream(stream, digest);
            TrackingInputStream in = new TrackingInputStream(dIn);
            StreamWrapper wrapper;
            if (STORE_SIZE_MINUS_ONE.equals(storeStream)) {
                wrapper = new StreamWrapper(in, -1);
            } else if (STORE_SIZE_MAX.equals(storeStream)) {
                wrapper = new StreamWrapper(in, Integer.MAX_VALUE);
            } else if (STORE_TEMP_FILE.equals(storeStream)) {
                File temp = moveToTempFile(in);
                fileInput = new TempFileInputStream(temp);
                long length = temp.length();
                wrapper = new StreamWrapper(fileInput, length);
            } else {
                throw new DataStoreException("Unsupported stream store algorithm: " + storeStream);
            }
            // UPDATE DATASTORE SET DATA=? WHERE ID=?
            conn.executeStmt(updateDataSQL, new Object[]{wrapper, tempId});
            now = System.currentTimeMillis();
            long length = in.getPosition();
            DataIdentifier identifier = new DataIdentifier(digest.digest());
            usesIdentifier(identifier);
            id = identifier.toString();
            // UPDATE DATASTORE SET ID=?, LENGTH=?, LAST_MODIFIED=?
            // WHERE ID=?
            // AND NOT EXISTS(SELECT ID FROM DATASTORE WHERE ID=?)
            PreparedStatement prep = conn.executeStmt(updateSQL, new Object[]{
                    id, new Long(length), new Long(now),
                    tempId, id});
            int count = prep.getUpdateCount();
            if (count == 0) {
                // update count is 0, meaning such a row already exists
                // DELETE FROM DATASTORE WHERE ID=?
                conn.executeStmt(deleteSQL, new Object[]{tempId});
                // SELECT LENGTH, LAST_MODIFIED FROM DATASTORE WHERE ID=?
                prep = conn.executeStmt(selectMetaSQL, new Object[]{id});
                rs = prep.getResultSet();
                if (rs.next()) {
                    long oldLength = rs.getLong(1);
                    long lastModified = rs.getLong(2);
                    if (oldLength != length) {
                        String msg =
                            DIGEST + " collision: temp=" + tempId
                            + " id=" + id + " length=" + length
                            + " oldLength=" + oldLength;
                        log.error(msg);
                        throw new DataStoreException(msg);
                    }
                    touch(identifier, lastModified);
                }
            }
            usesIdentifier(identifier);
            DbDataRecord record = new DbDataRecord(this, identifier, length, now);
            return record;
        } catch (Exception e) {
            throw convert("Can not insert new record", e);
        } finally {
            if (tempId != null) {
                temporaryInUse.remove(tempId);
            }
            DatabaseHelper.closeSilently(rs);
            putBack(conn);
            if (fileInput != null) {
                try {
                    fileInput.close();
                } catch (IOException e) {
                    throw convert("Can not close temporary file", e);
                }
            }
        }
    }

    /**
     * Creates a temp file and copies the data there.
     * The input stream is closed afterwards.
     *
     * @param in the input stream
     * @return the file
     * @throws IOException
     */
    private File moveToTempFile(InputStream in) throws IOException {
        File temp = File.createTempFile("dbRecord", null);
        TempFileInputStream.writeToFileAndClose(in, temp);
        return temp;
    }

    /**
     * {@inheritDoc}
     */
    public synchronized int deleteAllOlderThan(long min) throws DataStoreException {
        ConnectionRecoveryManager conn = getConnection();
        try {
            ArrayList touch = new ArrayList();
            for (Iterator it = new ArrayList(inUse.keySet()).iterator(); it.hasNext();) {
                DataIdentifier identifier = (DataIdentifier) it.next();
                if (identifier != null) {
                    touch.add(identifier.toString());
                }
            }
            touch.addAll(temporaryInUse);
            Iterator it = touch.iterator();
            while (it.hasNext()) {
                String key = (String) it.next();
                updateLastModifiedDate(key, 0);
            }
            // DELETE FROM DATASTORE WHERE LAST_MODIFIED<?
            PreparedStatement prep = conn.executeStmt(deleteOlderSQL, new Long[]{new Long(min)});
            return prep.getUpdateCount();
        } catch (Exception e) {
            throw convert("Can not delete records", e);
        } finally {
            putBack(conn);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Iterator getAllIdentifiers() throws DataStoreException {
        ConnectionRecoveryManager conn = getConnection();
        ArrayList list = new ArrayList();
        ResultSet rs = null;
        try {
            // SELECT ID FROM DATASTORE
            PreparedStatement prep = conn.executeStmt(selectAllSQL, new Object[0]);
            rs = prep.getResultSet();
            while (rs.next()) {
                String id = rs.getString(1);
                if (!id.startsWith(TEMP_PREFIX)) {
                    DataIdentifier identifier = new DataIdentifier(id);
                    list.add(identifier);
                }
            }
            return list.iterator();
        } catch (Exception e) {
            throw convert("Can not read records", e);
        } finally {
            DatabaseHelper.closeSilently(rs);
            putBack(conn);
        }
    }

    /**
     * {@inheritDoc}
     */
    public int getMinRecordLength() {
        return minRecordLength;
    }

    /**
     * Set the minimum object length.
     * The maximum value is around 32000.
     *
     * @param minRecordLength the length
     */
    public void setMinRecordLength(int minRecordLength) {
        this.minRecordLength = minRecordLength;
    }

    /**
     * {@inheritDoc}
     */
    public DataRecord getRecordIfStored(DataIdentifier identifier) throws DataStoreException {
        ConnectionRecoveryManager conn = getConnection();
        usesIdentifier(identifier);
        ResultSet rs = null;
        try {
            String id = identifier.toString();
            // SELECT LENGTH, LAST_MODIFIED FROM DATASTORE WHERE ID = ?
            PreparedStatement prep = conn.executeStmt(selectMetaSQL, new Object[]{id});
            rs = prep.getResultSet();
            if (!rs.next()) {
                throw new DataStoreException("Record not found: " + identifier);
            }
            long length = rs.getLong(1);
            long lastModified = rs.getLong(2);
            touch(identifier, lastModified);
            return new DbDataRecord(this, identifier, length, lastModified);
        } catch (Exception e) {
            throw convert("Can not read identifier " + identifier, e);
        } finally {
            DatabaseHelper.closeSilently(rs);
            putBack(conn);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public DataRecord getRecord(DataIdentifier identifier) throws DataStoreException {
        DataRecord record = getRecordIfStored(identifier);
        if (record == null) {
            throw new DataStoreException("Record not found: " + identifier);
        }
        return record;
    }
    
    /**
     * Open the input stream. This method sets those fields of the caller
     * that need to be closed once the input stream is read.
     * 
     * @param inputStream the database input stream object
     * @param identifier data identifier
     * @throws DataStoreException if the data store could not be accessed, 
     *          or if the given identifier is invalid
     */    
    InputStream openStream(DbInputStream inputStream, DataIdentifier identifier) throws DataStoreException {
        ConnectionRecoveryManager conn = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            // SELECT ID, DATA FROM DATASTORE WHERE ID = ?
            PreparedStatement prep = conn.executeStmt(selectDataSQL, new Object[]{identifier.toString()});
            rs = prep.getResultSet();
            if (!rs.next()) {
                throw new DataStoreException("Record not found: " + identifier);
            }
            InputStream stream = rs.getBinaryStream(2);
            if (stream == null) {
                stream = new ByteArrayInputStream(new byte[0]);
                DatabaseHelper.closeSilently(rs);
                putBack(conn);
            } else if (copyWhenReading) {
                // If we copy while reading, create a temp file and close the stream
                File temp = moveToTempFile(stream);
                stream = new TempFileInputStream(temp);
                DatabaseHelper.closeSilently(rs);
                putBack(conn);
            } else {
                stream = new BufferedInputStream(stream);
                inputStream.setConnection(conn);
                inputStream.setResultSet(rs);
            }
            return stream;
        } catch (Exception e) {
            DatabaseHelper.closeSilently(rs);
            putBack(conn);
            throw convert("Retrieving database resource ", e);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public synchronized void init(String homeDir) throws DataStoreException {
        try {
            initDatabaseType();
            connectionPool = new Pool(this, maxConnections);
            ConnectionRecoveryManager conn = getConnection();
            DatabaseMetaData meta = conn.getConnection().getMetaData();
            log.info("Using JDBC driver " + meta.getDriverName() + " " + meta.getDriverVersion());
            meta.getDriverVersion();
            ResultSet rs = meta.getTables(null, null, schemaObjectPrefix + tableSQL, null);
            boolean exists = rs.next();
            rs.close();
            if (!exists && isSchemaCheckEnabled()) {
                // CREATE TABLE DATASTORE(ID VARCHAR(255) PRIMARY KEY, 
                // LENGTH BIGINT, LAST_MODIFIED BIGINT, DATA BLOB)
                conn.executeStmt(createTableSQL, null);
            }
            putBack(conn);
        } catch (Exception e) {
            throw convert("Can not init data store, driver=" + driver + " url=" + url + " user=" + user + 
                    " schemaObjectPrefix=" + schemaObjectPrefix + " tableSQL=" + tableSQL + " createTableSQL=" + createTableSQL, e);
        }
    }

    protected void initDatabaseType() throws DataStoreException {
        boolean failIfNotFound;
        if (databaseType == null) {
            if (!url.startsWith("jdbc:")) {
                return;
            }
            failIfNotFound = false;
            int start = "jdbc:".length();
            int end = url.indexOf(':', start);
            databaseType = url.substring(start, end);
        } else {
            failIfNotFound = true;
        }

        InputStream in =
            DbDataStore.class.getResourceAsStream(databaseType + ".properties");
        if (in == null) {
            if (failIfNotFound) {
                String msg =
                    "Configuration error: The resource '" + databaseType
                    + ".properties' could not be found;"
                    + " Please verify the databaseType property";
                log.debug(msg);
                throw new DataStoreException(msg);
            } else {
                return;
            }
        }
        Properties prop = new Properties();
        try {
            try {
                prop.load(in);
            } finally {
            in.close();
            }
        } catch (IOException e) {
            String msg = "Configuration error: Could not read properties '" + databaseType + ".properties'";
            log.debug(msg);
            throw new DataStoreException(msg);
        }
        if (driver == null) {
            driver = getProperty(prop, "driver", driver);
        }
        tableSQL = getProperty(prop, "table", tableSQL);
        createTableSQL = getProperty(prop, "createTable", createTableSQL);
        insertTempSQL = getProperty(prop, "insertTemp", insertTempSQL);
        updateDataSQL = getProperty(prop, "updateData", updateDataSQL);
        updateLastModifiedSQL = getProperty(prop, "updateLastModified", updateLastModifiedSQL);
        updateSQL = getProperty(prop, "update", updateSQL);
        deleteSQL = getProperty(prop, "delete", deleteSQL);
        deleteOlderSQL = getProperty(prop, "deleteOlder", deleteOlderSQL);
        selectMetaSQL = getProperty(prop, "selectMeta", selectMetaSQL);
        selectAllSQL = getProperty(prop, "selectAll", selectAllSQL);
        selectDataSQL = getProperty(prop, "selectData", selectDataSQL);
        storeStream = getProperty(prop, "storeStream", storeStream);
        if (STORE_SIZE_MINUS_ONE.equals(storeStream)) {
        } else if (STORE_TEMP_FILE.equals(storeStream)) {
        } else if (STORE_SIZE_MAX.equals(storeStream)) {
        } else {
            String msg = "Unsupported Stream store mechanism: " + storeStream
                    + " supported are: " + STORE_SIZE_MINUS_ONE + ", "
                    + STORE_TEMP_FILE + ", " + STORE_SIZE_MAX;
            log.debug(msg);
            throw new DataStoreException(msg);
        }
    }

    /**
     * Get the expanded property value. The following placeholders are supported:
     * ${table}: the table name (the default is DATASTORE) and
     * ${tablePrefix}: tablePrefix plus schemaObjectPrefix as set in the configuration 
     *
     * @param prop the properties object
     * @param key the key
     * @param defaultValue the default value
     * @return the property value (placeholders are replaced)
     */
    protected String getProperty(Properties prop, String key, String defaultValue) {
        String sql = prop.getProperty(key, defaultValue);
        sql = Text.replace(sql, "${table}", tableSQL).trim();
        sql = Text.replace(sql, "${tablePrefix}", tablePrefix + schemaObjectPrefix).trim();
        return sql;
    }

    /**
     * Convert an exception to a data store exception.
     *
     * @param cause the message
     * @param e the root cause
     * @return the data store exception
     */
    protected DataStoreException convert(String cause, Exception e) {
        log.warn(cause, e);
        if (e instanceof DataStoreException) {
            return (DataStoreException) e;
        } else {
            return new DataStoreException(cause, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void updateModifiedDateOnAccess(long before) {
        log.debug("Update modifiedDate on access before " + before);
        minModifiedDate = before;
    }

    /**
     * Update the modified date of an entry if required.
     *
     * @param identifier the entry identifier
     * @param lastModified the current last modified date
     * @return the new modified date
     */
    long touch(DataIdentifier identifier, long lastModified) throws DataStoreException {
        usesIdentifier(identifier);
        return updateLastModifiedDate(identifier.toString(), lastModified);
    }

    private long updateLastModifiedDate(String key, long lastModified) throws DataStoreException {
        if (lastModified < minModifiedDate) {
            long now = System.currentTimeMillis();
            Long n = new Long(now);
            ConnectionRecoveryManager conn = getConnection();
            try {
                // UPDATE DATASTORE SET LAST_MODIFIED = ? WHERE ID = ? AND LAST_MODIFIED < ?
                conn.executeStmt(updateLastModifiedSQL, new Object[]{
                        n, key, n
                });
                return now;
            } catch (Exception e) {
                throw convert("Can not update lastModified", e);
            } finally {
                putBack(conn);
            }
        }
        return lastModified;
    }

    /**
     * Get the database type (if set).
     * @return the database type
     */
    public String getDatabaseType() {
        return databaseType;
    }

    /**
     * Set the database type. By default the sub-protocol of the JDBC database URL is used if it is not set.
     * It must match the resource file [databaseType].properties. Example: mysql.
     *
     * @param databaseType
     */
    public void setDatabaseType(String databaseType) {
        this.databaseType = databaseType;
    }

    /**
     * Get the database driver
     *
     * @return the driver
     */
    public String getDriver() {
        return driver;
    }

    /**
     * Set the database driver class name.
     * If not set, the default driver class name for the database type is used,
     * as set in the [databaseType].properties resource; key 'driver'.
     *
     * @param driver
     */
    public void setDriver(String driver) {
        this.driver = driver;
    }

    /**
     * Get the password.
     *
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * Set the password.
     *
     * @param password
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Get the database URL.
     *
     * @return the URL
     */
    public String getUrl() {
        return url;
    }

    /**
     * Set the database URL.
     * Example: jdbc:postgresql:test
     *
     * @param url
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * Get the user name.
     *
     * @return the user name
     */
    public String getUser() {
        return user;
    }

    /**
     * Set the user name.
     *
     * @param user
     */
    public void setUser(String user) {
        this.user = user;
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

    /**
     * {@inheritDoc}
     */
    public synchronized void close() throws DataStoreException {
        ArrayList list = connectionPool.getAll();
        for (int i = 0; i < list.size(); i++) {
            ConnectionRecoveryManager conn = (ConnectionRecoveryManager) list.get(i);
            conn.close();
        }
        list.clear();
    }

    protected void usesIdentifier(DataIdentifier identifier) {
        inUse.put(identifier, new WeakReference(identifier));
    }

    /**
     * {@inheritDoc}
     */
    public void clearInUse() {
        inUse.clear();
    }

    protected synchronized MessageDigest getDigest() throws DataStoreException {
        try {
            return MessageDigest.getInstance(DIGEST);
        } catch (NoSuchAlgorithmException e) {
            throw convert("No such algorithm: " + DIGEST, e);
        }
    }

    protected ConnectionRecoveryManager getConnection() throws DataStoreException {
        try {
            ConnectionRecoveryManager conn = (ConnectionRecoveryManager) connectionPool.get();
            conn.setAutoReconnect(true);
            return conn;
        } catch (InterruptedException e) {
            throw new DataStoreException("Interrupted", e);
        } catch (RepositoryException e) {
            throw new DataStoreException("Can not open a new connection", e);
        }
    }

    protected void putBack(ConnectionRecoveryManager conn) throws DataStoreException {
        try {
            connectionPool.add(conn);
        } catch (InterruptedException e) {
            throw new DataStoreException("Interrupted", e);
        }
    }

    /**
     * Get the maximum number of concurrent connections.
     *
     * @return the maximum number of connections.
     */
    public int getMaxConnections() {
        return maxConnections;
    }

    /**
     * Set the maximum number of concurrent connections in the pool.
     * At least 3 connections are required if the garbage collection process is used.
     *
     * @param maxConnections the new value
     */
    public void setMaxConnections(int maxConnections) {
        this.maxConnections = maxConnections;
    }

    /**
     * Create a new connection.
     *
     * @return the new connection
     */
    public ConnectionRecoveryManager createNewConnection() throws RepositoryException {
        ConnectionRecoveryManager conn = new ConnectionRecoveryManager(false, driver, url, user, password);
        return conn;
    }

    /**
     * Is a stream copied to a temporary file before returning?
     *
     * @return the setting
     */
    public boolean getCopyWhenReading() {
        return copyWhenReading;
    }

    /**
     * The the copy setting. If enabled,
     * a stream is always copied to a temporary file when reading a stream.
     *
     * @param copyWhenReading the new setting
     */
    public void setCopyWhenReading(boolean copyWhenReading) {
        this.copyWhenReading = copyWhenReading;
    }

    /**
     * Get the table prefix. 
     *
     * @return the table prefix.
     */
    public String getTablePrefix() {
        return tablePrefix;
    }

    /**
     * Set the new table prefix. The default is empty.
     * The table name is constructed like this:
     * ${tablePrefix}${schemaObjectPrefix}${tableName}
     *
     * @param tablePrefix the new value
     */
    public void setTablePrefix(String tablePrefix) {
        this.tablePrefix = tablePrefix;
    }
    
    /**
     * Get the schema prefix.
     * 
     * @return the schema object prefix
     */
    public String getSchemaObjectPrefix() {
        return schemaObjectPrefix;
    }

    /**
     * Set the schema object prefix. The default is empty.
     * The table name is constructed like this:
     * ${tablePrefix}${schemaObjectPrefix}${tableName}
     * 
     * @param schemaObjectPrefix the new prefix
     */
    public void setSchemaObjectPrefix(String schemaObjectPrefix) {
        this.schemaObjectPrefix = schemaObjectPrefix;
    }    

}
