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

import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import org.apache.jackrabbit.core.persistence.PMContext;
import org.apache.jackrabbit.util.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extends the {@link BundleDbPersistenceManager} by Oracle specific code.
 * <p/>
 * Configuration:<br>
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

    /**
     * the default logger
     */
    private static Logger log = LoggerFactory.getLogger(OraclePersistenceManager.class);

    /** the variable for the Oracle table space */
    public static final String TABLE_SPACE_VARIABLE =
        "${tableSpace}";

    /** the Oracle table space to use */
    protected String tableSpace;

    /**
     * Creates a new oracle persistence manager
     */
    public OraclePersistenceManager() {
        // enable db blob support
        setExternalBLOBs(false);
    }

    /**
     * Returns the configured Oracle table space.
     * @return the configured Oracle table space.
     */
    public String getTableSpace() {
        return tableSpace;
    }

    /**
     * Sets the Oracle table space.
     * @param tableSpace the Oracle table space.
     */
    public void setTableSpace(String tableSpace) {
        if (tableSpace != null) {
            this.tableSpace = tableSpace.trim();
        } else {
            this.tableSpace = null;
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

        // check driver version
        try {
            DatabaseMetaData metaData = connectionManager.getConnection().getMetaData();
            if (metaData.getDriverMajorVersion() < 10) {
                // Oracle drivers prior to version 10 only support
                // writing BLOBs up to 32k in size...
                log.warn("Unsupported driver version detected: "
                        + metaData.getDriverName()
                        + " v" + metaData.getDriverVersion());
            }
        } catch (SQLException e) {
            log.warn("Can not retrieve driver version", e);
        }
    }

    /**
     * Returns a new instance of a NGKDbNameIndex.
     * @return a new instance of a NGKDbNameIndex.
     * @throws SQLException if an SQL error occurs.
     */
    protected DbNameIndex createDbNameIndex() throws SQLException {
        return new NGKDbNameIndex(connectionManager, schemaObjectPrefix);
    }

    /**
     * {@inheritDoc}
     *
     * @return <code>true</code>
     */
    protected boolean checkTablesWithUser() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    protected String createSchemaSQL(String sql) {
        sql = Text.replace(sql, SCHEMA_OBJECT_PREFIX_VARIABLE, schemaObjectPrefix).trim();
        // set the tablespace if it is defined
        String tspace;
        if (tableSpace == null || "".equals(tableSpace)) {
            tspace = "";
        } else {
            tspace = "tablespace " + tableSpace;
        }
        return Text.replace(sql, TABLE_SPACE_VARIABLE, tspace).trim();
    }

    /**
     * Since Oracle only supports table names up to 30 characters in
     * length illegal characters are simply replaced with "_" rather than
     * escaping them with "_x0000_".
     *
     * @inheritDoc
     */
    protected void prepareSchemaObjectPrefix() throws Exception {
        DatabaseMetaData metaData = connectionManager.getConnection().getMetaData();
        String legalChars = metaData.getExtraNameCharacters();
        legalChars += "ABCDEFGHIJKLMNOPQRSTUVWXZY0123456789_";

        String prefix = schemaObjectPrefix.toUpperCase();
        StringBuffer escaped = new StringBuffer();
        for (int i = 0; i < prefix.length(); i++) {
            char c = prefix.charAt(i);
            if (legalChars.indexOf(c) == -1) {
                escaped.append('_');
            } else {
                escaped.append(c);
            }
        }
        schemaObjectPrefix = escaped.toString();
    }
}
