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
import org.apache.jackrabbit.uuid.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;
import java.util.WeakHashMap;

/**
 * A data store implementation that stores the records in a database using JDBC.
 * 
 * Configuration:<br>
 * <ul>
 * <li>&lt;param name="className" value="org.apache.jackrabbit.core.data.FileDataStore"/>
 * <li>&lt;param name="{@link #setUrl(String) url}" value="jdbc:postgresql:test"/>
 * <li>&lt;param name="{@link #setUser(String) user}" value="sa"/>
 * <li>&lt;param name="{@link #setPassword(String) password}" value="sa"/>
 * <li>&lt;param name="{@link #setDatabaseType(String) databaseType}" value="postgresql"/>
 * <li>&lt;param name="{@link #setDriver(String) driver}" value="org.postgresql.Driver"/>
 * <li>&lt;param name="{@link #setMinRecordLength(int) minRecordLength}" value="1024"/>
 * </ul>
 * 
 * <p>
 * Only URL, user name and password usually need to be set. 
 * The remaining settings are generated using the database URL sub-protocol from the
 * database type resource file.
 * <p>
 * A three level directory structure is used to avoid placing too many
 * files in a single directory. The chosen structure is designed to scale
 * up to billions of distinct records.
 */
public class DbDataStore implements DataStore {
    
    /**
     * The digest algorithm used to uniquely identify records.
     */
    private static final String DIGEST = "SHA-1";
    
    /**
     * Logger instance
     */
    private static Logger log = LoggerFactory.getLogger(DbDataStore.class);    
    
    /**
     * The default value for the minimum object size.
     */
    private static final int DEFAULT_MIN_RECORD_LENGTH = 100;
    
    /**
     * The minimum modified date. If a file is accessed (read or write) with a modified date 
     * older than this value, the modified date is updated to the current time.
     */
    private long minModifiedDate;
    
    /**
     * The database URL used.
     */
    private String url;
    
    /**
     * The database driver.
     */
    private String driver;
    
    /**
     * The user name.
     */
    private String user;
    
    /**
     * The password
     */
    private String password;
    
    /**
     * The database type used.
     */
    private String databaseType;
    
    /**
     * The minimum size of an object that should be stored in this data store.
     */
    private int minRecordLength = DEFAULT_MIN_RECORD_LENGTH;
    
    private ConnectionRecoveryManager conn;
    
    private static final String TEMP_PREFIX = "TEMP_";
    
    private String tableSQL = "DATASTORE";
    private String createTableSQL = 
        "CREATE TABLE DATASTORE(ID VARCHAR(255) PRIMARY KEY, LENGTH BIGINT, LAST_MODIFIED BIGINT, DATA BLOB)";
    private String insertTempSQL = 
        "INSERT INTO DATASTORE VALUES(?, 0, ?, NULL)";
    private String updateDataSQL = 
        "UPDATE DATASTORE SET DATA=? WHERE ID=?";
    private String updateLastModifiedSQL = 
        "UPDATE DATASTORE SET LAST_MODIFIED=? WHERE ID=? AND LAST_MODIFIED<?";
    private String updateSQL = 
        "UPDATE DATASTORE SET ID=?, LENGTH=?, LAST_MODIFIED=? WHERE ID=? AND NOT EXISTS(SELECT ID FROM DATASTORE WHERE ID=?)";
    private String deleteSQL = 
        "DELETE FROM DATASTORE WHERE ID=?";
    private String deleteOlderSQL = 
        "DELETE FROM DATASTORE WHERE LAST_MODIFIED<?";
    private String selectMetaSQL = 
        "SELECT LENGTH, LAST_MODIFIED FROM DATASTORE WHERE ID=?";
    private String selectAllSQL = 
        "SELECT ID FROM DATASTORE";
    private String selectDataSQL = 
        "SELECT DATA FROM DATASTORE WHERE ID=?";
    private String storeStream = STORE_TEMP_FILE;
    
    // write to a temporary file to get the length (slow, but always works)
    // this is the default setting
    private static final String STORE_TEMP_FILE = "tempFile";
    
    // call PreparedStatement.setBinaryStream(..., -1)
    private static final String STORE_SIZE_MINUS_ONE = "-1";
    
    // call PreparedStatement.setBinaryStream(..., Integer.MAX_VALUE)
    private static final String STORE_SIZE_MAX = "max";
    
    /**
     * All data identifiers that are currently in use are in this set until they are garbage collected.
     */
    private WeakHashMap inUse = new WeakHashMap();    

    /**
     * {@inheritDoc}
     */
    public synchronized DataRecord addRecord(InputStream stream) throws DataStoreException {
        conn.setAutoReconnect(false);
        String id = null, tempId = null;            
        long now;            
        for (int i = 0; i < ConnectionRecoveryManager.TRIALS; i++) {
            try {
                now = System.currentTimeMillis();
                id = UUID.randomUUID().toString();
                tempId = TEMP_PREFIX + id;
                PreparedStatement prep = conn.executeStmt(selectMetaSQL, new Object[]{tempId});
                ResultSet rs = prep.getResultSet();
                if (rs.next()) {
                    // re-try in the very, very unlikely event that the row already exists
                    continue;
                }
                conn.executeStmt(insertTempSQL, new Object[]{tempId, new Long(now)});
                break;
            } catch (Exception e) {
                throw convert("Can not insert new record", e);
            }
        }
        if (id == null) {
            String msg = "Can not create new record";
            log.error(msg);
            throw new DataStoreException(msg);
        }
        ResultSet rs = null;
        try {
            MessageDigest digest = getDigest();
            DigestInputStream dIn = new DigestInputStream(stream, digest);
            TrackingInputStream in = new TrackingInputStream(dIn);
            File temp = null;
            InputStream fileInput = null;
            StreamWrapper wrapper;
            if (STORE_SIZE_MINUS_ONE.equals(storeStream)) {
                wrapper = new StreamWrapper(in, -1);
            } else if (STORE_SIZE_MAX.equals(storeStream)) {
                    wrapper = new StreamWrapper(in, Integer.MAX_VALUE);
            } else if (STORE_TEMP_FILE.equals(storeStream)) {
                int length = 0;
                temp = File.createTempFile("dbRecord", null);
                OutputStream out = new FileOutputStream(temp);
                byte[] b = new byte[4096];
                while (true) {
                    int n = in.read(b);
                    if (n < 0) {
                        break;
                    }
                    out.write(b, 0, n);
                    length += n;
                }
                out.close();
                fileInput = new BufferedInputStream(new FileInputStream(temp));
                wrapper = new StreamWrapper(fileInput, length);
            } else {
                throw new DataStoreException("Unsupported stream store algorithm: " + storeStream);
            }
            conn.executeStmt(updateDataSQL, new Object[]{wrapper, tempId});
            now = System.currentTimeMillis();
            long length = in.getPosition();
            DataIdentifier identifier = new DataIdentifier(digest.digest());
            id = identifier.toString();
            // UPDATE DATASTORE SET ID=?, LENGTH=?, LAST_MODIFIED=? 
            // WHERE ID=? 
            // AND NOT EXISTS(SELECT ID FROM DATASTORE WHERE ID=?)
            PreparedStatement prep = conn.executeStmt(updateSQL, new Object[]{
                    id, new Long(length), new Long(now), 
                    tempId, id});
            int count = prep.getUpdateCount();
            if (temp != null) {
                fileInput.close();
                temp.delete();
            }
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
                        String msg = DIGEST + " collision: temp=" + tempId + " id=" + id + " length=" + length + " oldLength=" + oldLength;
                        log.error(msg);
                        throw new DataStoreException(msg);
                    }
                    touch(identifier, lastModified);
                }
            }
            usesIdentifier(identifier);
            DbDataRecord record = new DbDataRecord(this, identifier, length, now);
            conn.setAutoReconnect(true);
            return record;
        } catch (Exception e) {
            throw convert("Can not insert new record", e);
        } finally {
            conn.closeSilently(rs);
        }
    }

    /**
     * {@inheritDoc}
     */
    public synchronized int deleteAllOlderThan(long min) throws DataStoreException {
        try {
            Iterator it = inUse.keySet().iterator();
            while (it.hasNext()) {
                DataIdentifier identifier = (DataIdentifier) it.next();
                if (identifier != null) {
                    touch(identifier, 0);
                }
            }
            // DELETE FROM DATASTORE WHERE LAST_MODIFIED<?
            PreparedStatement prep = conn.executeStmt(deleteOlderSQL, new Long[]{new Long(min)});
            return prep.getUpdateCount();
        } catch (Exception e) {
            throw convert("Can not delete records", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Iterator getAllIdentifiers() throws DataStoreException {
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
            conn.closeSilently(rs);
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
     * 
     * @param minRecordLength the length
     */
    public void setMinRecordLength(int minRecordLength) {
        this.minRecordLength = minRecordLength;
    }

    /**
     * {@inheritDoc}
     */
    public synchronized DataRecord getRecord(DataIdentifier identifier) throws DataStoreException {
        usesIdentifier(identifier);
        ResultSet rs = null;
        try {
            // SELECT LENGTH, LAST_MODIFIED FROM DATASTORE WHERE ID = ?
            String id = identifier.toString();
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
            conn.closeSilently(rs);
        }
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void init(String homeDir) throws DataStoreException {
        try {
            initDatabaseType();
            conn = new ConnectionRecoveryManager(false, driver, url, user, password);
            conn.setAutoReconnect(true);
            DatabaseMetaData meta = conn.getConnection().getMetaData();
            ResultSet rs = meta.getTables(null, null, tableSQL, null);
            boolean exists = rs.next();
            rs.close();
            if (!exists) {
                conn.executeStmt(createTableSQL, null);
            }
        } catch (Exception e) {
            throw convert("Can not init data store, driver=" + driver + " url=" + url + " user=" + user, e);
        }
    }
    
    private void initDatabaseType() throws DataStoreException {
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
        InputStream in = DbDataStore.class.getResourceAsStream(databaseType + ".properties");
        if (in == null) {
            if (failIfNotFound) {
                String msg = "Configuration error: The resource '" + databaseType + ".properties' could not be found; Please verify the databaseType property";
                log.debug(msg);
                throw new DataStoreException(msg);
            } else {
                return;
            }
        }
        Properties prop = new Properties();
        try {
            prop.load(new BufferedInputStream(in));
        } catch (IOException e) {
            String msg = "Configuration error: Could not read properties '" + databaseType + ".properties'";
            log.debug(msg);
            throw new DataStoreException(msg);
        }
        if (driver == null) {
            driver = prop.getProperty("driver", driver);
        }
        tableSQL = prop.getProperty("table", tableSQL);
        createTableSQL = prop.getProperty("createTable", createTableSQL);
        insertTempSQL = prop.getProperty("insertTemp", insertTempSQL);
        updateDataSQL = prop.getProperty("updateData", updateDataSQL);
        updateLastModifiedSQL = prop.getProperty("updateLastModified", updateLastModifiedSQL);
        updateSQL = prop.getProperty("update", updateSQL);
        deleteSQL = prop.getProperty("delete", deleteSQL);
        deleteOlderSQL = prop.getProperty("deleteOlder", deleteOlderSQL);
        selectMetaSQL = prop.getProperty("selectMeta", selectMetaSQL);
        selectAllSQL = prop.getProperty("selectAll", selectAllSQL);
        selectDataSQL = prop.getProperty("selectData", selectDataSQL);
        storeStream = prop.getProperty("storeStream", storeStream);
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

    private DataStoreException convert(String cause, Exception e) {
        log.warn(cause, e);
        return new DataStoreException(cause, e);
    }

    /**
     * {@inheritDoc}
     */
    public void updateModifiedDateOnAccess(long before) {
        log.debug("Update modifiedDate on access before " + before);
        minModifiedDate = before;
    }

    synchronized long touch(DataIdentifier identifier, long lastModified) throws DataStoreException {
        usesIdentifier(identifier);
        if (lastModified < minModifiedDate) {
            long now = System.currentTimeMillis();
            Long n = new Long(now);
            // UPDATE DATASTORE SET LAST_MODIFIED = ? WHERE ID = ? AND LAST_MODIFIED < ?
            try {
                conn.executeStmt(updateLastModifiedSQL, new Object[]{
                        n, identifier.toString(), n
                });
                return now;
            } catch (Exception e) {
                throw convert("Can not update lastModified", e);
            }
        }
        return lastModified;
    }

    /**
     * {@inheritDoc}
     */    
    public InputStream getInputStream(DataIdentifier identifier) throws DataStoreException {
        try {
            // SELECT DATA FROM DATASTORE WHERE ID = ?
            String id = identifier.toString();
            PreparedStatement prep = conn.executeStmt(selectDataSQL, new Object[]{id});
            ResultSet rs = prep.getResultSet();
            if (!rs.next()) {
                throw new DataStoreException("Record not found: " + identifier);
            }
            return rs.getBinaryStream(1);
        } catch (Exception e) {
            throw convert("Can not read identifier " + identifier, e);
        }
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
     * {@inheritDoc}
     */
    public void close() {
        conn.close();
    }
    
    private void usesIdentifier(DataIdentifier identifier) {
        inUse.put(identifier, new WeakReference(identifier));
    }
    
    /**
     * {@inheritDoc}
     */
    public void clearInUse() {
        inUse.clear();
    }    
    
    private synchronized MessageDigest getDigest() throws DataStoreException {
        try {
            return MessageDigest.getInstance(DIGEST);
        } catch (NoSuchAlgorithmException e) {
            throw convert("No such algorithm: " + DIGEST, e);
        }
    }

}
