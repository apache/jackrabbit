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
import org.apache.jackrabbit.core.util.db.CheckSchemaOperation;
import org.apache.jackrabbit.core.util.db.ConnectionHelper;
import org.apache.jackrabbit.core.util.db.OracleConnectionHelper;

/**
 * Extends the {@link BundleDbPersistenceManager} by Oracle specific code. <p/> Configuration:<br>
 * <ul>
 * <li>&lt;param name="{@link #setExternalBLOBs(String)} externalBLOBs}" value="false"/>
 * <li>&lt;param name="{@link #setBundleCacheSize(String) bundleCacheSize}" value="8"/>
 * <li>&lt;param name="{@link #setConsistencyCheck(String) consistencyCheck}" value="false"/>
 * <li>&lt;param name="{@link #setMinBlobSize(String) minBlobSize}" value="16384"/>
 * <li>&lt;param name="{@link #setDriver(String) driver}" value="oracle.jdbc.OracleDriverr"/>
 * <li>&lt;param name="{@link #setUrl(String) url}" value="jdbc:oracle:thin:@127.0.0.1:1521:xe"/>
 * <li>&lt;param name="{@link #setUser(String) user}" value=""/>
 * <li>&lt;param name="{@link #setPassword(String) password}" value=""/>
 * <li>&lt;param name="{@link #setSchema(String) schema}" value="oracle"/>
 * <li>&lt;param name="{@link #setSchemaObjectPrefix(String) schemaObjectPrefix}" value="${wsp.name}_"/>
 * <li>&lt;param name="{@link #setTableSpace(String) tableSpace}" value=""/>
 * <li>&lt;param name="{@link #setErrorHandling(String) errorHandling}" value=""/>
 * </ul>
 */
public class OraclePersistenceManager extends BundleDbPersistenceManager {

    /** the Oracle table space to use */
    protected String tableSpace = "";

    /**
     * Creates a new oracle persistence manager
     */
    public OraclePersistenceManager() {
        // enable db blob support
        setExternalBLOBs(false);
    }

    /**
     * Returns the configured Oracle table space.
     * 
     * @return the configured Oracle table space.
     */
    public String getTableSpace() {
        return tableSpace;
    }

    /**
     * Sets the Oracle table space.
     * 
     * @param tableSpace the Oracle table space.
     */
    public void setTableSpace(String tableSpace) {
        if (tableSpace != null && tableSpace.trim().length() > 0) {
            this.tableSpace = "tablespace " + tableSpace.trim();
        } else {
            this.tableSpace = "";
        }
    }

    public void init(PMContext context) throws Exception {
        // init default values
        if (getDriver() == null) {
            setDriver("oracle.jdbc.OracleDriver");
        }
        if (getUrl() == null) {
            setUrl("jdbc:oracle:thin:@127.0.0.1:1521:xe");
        }
        if (getDatabaseType() == null) {
            setDatabaseType("oracle");
        }
        if (getSchemaObjectPrefix() == null) {
            setSchemaObjectPrefix(context.getHomeDir().getName() + "_");
        }
        super.init(context);
    }

    /**
     * Returns a new instance of a NGKDbNameIndex.
     * 
     * @return a new instance of a NGKDbNameIndex.
     * @throws SQLException if an SQL error occurs.
     */
    protected DbNameIndex createDbNameIndex() throws SQLException {
        return new NGKDbNameIndex(conHelper, schemaObjectPrefix);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ConnectionHelper createConnectionHelper(DataSource dataSrc) throws Exception {
        OracleConnectionHelper helper = new OracleConnectionHelper(dataSrc, blockOnConnectionLoss);
        helper.init();
        return helper;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected CheckSchemaOperation createCheckSchemaOperation() {
        return super.createCheckSchemaOperation().addVariableReplacement(
            CheckSchemaOperation.TABLE_SPACE_VARIABLE, tableSpace);
    }
}
