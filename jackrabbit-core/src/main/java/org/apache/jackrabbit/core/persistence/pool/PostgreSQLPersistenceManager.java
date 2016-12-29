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

import java.sql.SQLException;

import javax.sql.DataSource;

import org.apache.jackrabbit.core.persistence.PMContext;
import org.apache.jackrabbit.core.util.db.ConnectionHelper;
import org.apache.jackrabbit.core.util.db.PostgreSQLConnectionHelper;

/**
 * Extends the {@link BundleDbPersistenceManager} by PostgreSQL specific code.
 * <p>
 * Configuration:<br>
 * <ul>
 * <li>&lt;param name="{@link #setBundleCacheSize(String) bundleCacheSize}" value="8"/&gt;
 * <li>&lt;param name="{@link #setConsistencyCheck(String) consistencyCheck}" value="false"/&gt;
 * <li>&lt;param name="{@link #setMinBlobSize(String) minBlobSize}" value="16384"/&gt;
 * <li>&lt;param name="{@link #setDriver(String) driver}" value="org.postgresql.Driver"/&gt;
 * <li>&lt;param name="{@link #setUrl(String) url}" value=""/&gt;
 * <li>&lt;param name="{@link #setUser(String) user}" value=""/&gt;
 * <li>&lt;param name="{@link #setPassword(String) password}" value=""/&gt;
 * <li>&lt;param name="{@link #setSchema(String) schema}" value="postgresql"/&gt;
 * <li>&lt;param name="{@link #setSchemaObjectPrefix(String) schemaObjectPrefix}" value=""/&gt;
 * <li>&lt;param name="{@link #setErrorHandling(String) errorHandling}" value=""/&gt;
 * </ul>
 */
public class PostgreSQLPersistenceManager extends BundleDbPersistenceManager {

    /**
     * {@inheritDoc}
     */
    public void init(PMContext context) throws Exception {
        // init default values
        if (getDriver() == null) {
            setDriver("org.postgresql.Driver");
        }
        if (getDatabaseType() == null) {
            setDatabaseType("postgresql");
        }
        super.init(context);
    }

    /**
     * Returns a new instance of a DbNameIndex.
     * @return a new instance of a DbNameIndex.
     * @throws java.sql.SQLException if an SQL error occurs.
     */
    protected DbNameIndex createDbNameIndex() throws SQLException {
        return new PostgreSQLNameIndex(conHelper, schemaObjectPrefix);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ConnectionHelper createConnectionHelper(DataSource dataSrc) throws Exception {
    	return new PostgreSQLConnectionHelper(dataSrc, blockOnConnectionLoss);
    }

    /**
     * returns the storage model
     * @return the storage model
     */
    public int getStorageModel() {
        return SM_LONGLONG_KEYS;
    }

}
