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
package org.apache.jackrabbit.core.persistence.pool;

import org.apache.jackrabbit.core.persistence.PMContext;

/**
 * Extends the {@link BundleDbPersistenceManager} by H2 specific code.
 * <p>
 * Configuration:
 * <pre>
 * &lt;PersistenceManager class="org.apache.jackrabbit.core.persistence.pool.H2PersistenceManager"&gt;
 *     &lt;param name="{@link #setBundleCacheSize(String) bundleCacheSize}" value="8"/&gt;
 *     &lt;param name="{@link #setConsistencyCheck(String) consistencyCheck}" value="false"/&gt;
 *     &lt;param name="{@link #setMinBlobSize(String) minBlobSize}" value="16384"/&gt;
 *     &lt;param name="{@link #setDriver(String) driver}" value="org.h2.Driver"/&gt;
 *     &lt;param name="{@link #setUrl(String) url}" value="jdbc:h2:file:${wsp.home}/db/itemState"/&gt;
 *     &lt;param name="{@link #setUser(String) user}" value=""/&gt;
 *     &lt;param name="{@link #setPassword(String) password}" value=""/&gt;
 *     &lt;param name="{@link #setSchema(String) schema}" value="h2"/&gt;
 *     &lt;param name="{@link #setSchemaObjectPrefix(String) schemaObjectPrefix}" value=""/&gt;
 *     &lt;param name="{@link #setErrorHandling(String) errorHandling}" value=""/&gt;
 *     &lt;param name="{@link #setLockTimeout(String) lockTimeout}" value="10000"/&gt;
 * &lt;/PersistenceManager&gt;
 * </pre>
 */
public class H2PersistenceManager extends BundleDbPersistenceManager {

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
     * {@inheritDoc}
     */
   public void init(PMContext context) throws Exception {
        // init default values
        if (getDriver() == null) {
            setDriver("org.h2.Driver");
        }
        if (getUrl() == null) {
            setUrl("jdbc:h2:file:" + context.getHomeDir().getPath() + "/db/itemState");
        }
        if (getDatabaseType() == null) {
            setDatabaseType("h2");
        }
        if (getSchemaObjectPrefix() == null) {
            setSchemaObjectPrefix("");
        }

        super.init(context);
        
        conHelper.exec("SET LOCK_TIMEOUT " + lockTimeout);
    }

}
