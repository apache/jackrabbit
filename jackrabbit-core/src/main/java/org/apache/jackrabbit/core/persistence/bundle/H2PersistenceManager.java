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
package org.apache.jackrabbit.core.persistence.bundle;

import org.apache.jackrabbit.core.persistence.PMContext;

import java.sql.Statement;
import java.sql.SQLException;

import javax.jcr.RepositoryException;

/**
 * Extends the {@link BundleDbPersistenceManager} by H2 specific code.
 * <p/>
 * Configuration:<br>
 * <ul>
 * <li>&lt;param name="{@link #setBundleCacheSize(String) bundleCacheSize}" value="8"/>
 * <li>&lt;param name="{@link #setConsistencyCheck(String) consistencyCheck}" value="false"/>
 * <li>&lt;param name="{@link #setMinBlobSize(String) minBlobSize}" value="16384"/>
 * <li>&lt;param name="{@link #setDriver(String) driver}" value="org.hsqldb.jdbcDriver"/>
 * <li>&lt;param name="{@link #setUrl(String) url}" value="jdbc:hsqldb:file:${wsp.home}/db/itemState"/>
 * <li>&lt;param name="{@link #setUser(String) user}" value="sa"/>
 * <li>&lt;param name="{@link #setPassword(String) password}" value=""/>
 * <li>&lt;param name="{@link #setSchema(String) schema}" value="native"/>
 * <li>&lt;param name="{@link #setSchemaObjectPrefix(String) schemaObjectPrefix}" value=""/>
 * <li>&lt;param name="{@link #setErrorHandling(String) errorHandling}" value=""/>
 * <li>&lt;param name="{@link #setLockTimeout(String)} (String) lockTimeout}" value="10000"/>
 * </ul>
 */
public class H2PersistenceManager extends BundleDbPersistenceManager {

    /** the cvs/svn id */
    static final String CVS_ID = "$URL$ $Rev$ $Date$";

    /** the lock time out. see*/
    private long lockTimeout = 10000;

    /**
     * Returns the lock timeout.
     * @return the lock timeout
     */
    public String getLockTimeout() {
        return String.valueOf(lockTimeout);
    }

    /**
     * Sets the lock timeout in milliseconds.
     * @param lockTimeout the lock timeout.
     */
    public void setLockTimeout(String lockTimeout) {
        this.lockTimeout = Long.parseLong(lockTimeout);
    }

    /**
     * Creates a new h2 persistence manager.
     */
    public H2PersistenceManager() {
    }

    /**
     * {@inheritDoc}
     */
   public void init(PMContext context) throws Exception {
        // init default values
        if (getDriver() == null) {
            setDriver("org.h2.Driver");
        }
        if (getUrl() == null) {
            setUrl("jdbc:h2:file:"+ context.getHomeDir().getPath() +"/db/itemState");
        }
        if (getUser() == null) {
            setUser("sa");
        }
        if (getPassword() == null) {
            setPassword("sa");
        }
        if (getSchema() == null) {
            setSchema("h2");
        }
        if (getSchemaObjectPrefix() == null) {
            setSchemaObjectPrefix("");
        }

        super.init(context);
    }

    /**
     * {@inheritDoc}
     */
    protected void checkSchema() throws SQLException, RepositoryException {
        Statement stmt = connectionManager.getConnection().createStatement();
        try {
            stmt.execute("SET LOCK_TIMEOUT " + lockTimeout);
        } finally {
            stmt.close();
        }
        super.checkSchema();
    }

    /**
     * @see PersistenceManager#close
     */
    public synchronized void close() throws Exception {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }
        if (getUrl().startsWith("jdbc:h2:file:")) {
            // have to explicitly shutdown in-proc h2
            Statement stmt = connectionManager.getConnection().createStatement();
            stmt.execute("shutdown");
            stmt.close();
        }
        // call base class implementation
        super.close();
    }

}
