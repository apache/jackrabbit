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

import java.io.InputStream;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.persistence.PMContext;
import org.apache.jackrabbit.core.persistence.util.NodePropBundle;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.util.TrackingInputStream;
import org.apache.jackrabbit.core.util.db.DbUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extends the {@link BundleDbPersistenceManager} by PostgreSQL specific code.
 * <p/>
 * Configuration:<br>
 * <ul>
 * <li>&lt;param name="{@link #setBundleCacheSize(String) bundleCacheSize}" value="8"/>
 * <li>&lt;param name="{@link #setConsistencyCheck(String) consistencyCheck}" value="false"/>
 * <li>&lt;param name="{@link #setMinBlobSize(String) minBlobSize}" value="16384"/>
 * <li>&lt;param name="{@link #setDriver(String) driver}" value="org.postgresql.Driver"/>
 * <li>&lt;param name="{@link #setUrl(String) url}" value=""/>
 * <li>&lt;param name="{@link #setUser(String) user}" value=""/>
 * <li>&lt;param name="{@link #setPassword(String) password}" value=""/>
 * <li>&lt;param name="{@link #setSchema(String) schema}" value="postgresql"/>
 * <li>&lt;param name="{@link #setSchemaObjectPrefix(String) schemaObjectPrefix}" value=""/>
 * <li>&lt;param name="{@link #setErrorHandling(String) errorHandling}" value=""/>
 * </ul>
 */
public class PostgreSQLPersistenceManager extends BundleDbPersistenceManager {

    /**
     * Logger instance.
     */
    private static Logger log =
        LoggerFactory.getLogger(PostgreSQLPersistenceManager.class);

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
     * returns the storage model
     * @return the storage model
     */
    public int getStorageModel() {
        return SM_LONGLONG_KEYS;
    }

    /**
     * PostgreSQL needs slightly different handling of the binary value that is received:
     * rs.getBinaryStream vs rs.getBlob in the super class.
     * 
     * {@inheritDoc}
     */
    @Override
    protected NodePropBundle loadBundle(NodeId id) throws ItemStateException {
        ResultSet rs = null;
        try {
            rs = conHelper.exec(bundleSelectSQL, getKey(id), false, 0);
            if (rs.next()) {
                InputStream input = rs.getBinaryStream(1);
                try {
                    TrackingInputStream cin = new TrackingInputStream(input);
                    NodePropBundle bundle = binding.readBundle(cin, id);
                    bundle.setSize(cin.getPosition());
                    return bundle;
                } finally {
                    input.close();
                }
            } else {
                return null;
            }
        } catch (Exception e) {
            String msg = "failed to read bundle: " + id + ": " + e;
            log.error(msg);
            throw new ItemStateException(msg, e);
        } finally {
           DbUtility.close(rs);
        }
    }
}
