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
import org.apache.jackrabbit.core.persistence.bundle.util.NGKDbNameIndex;
import org.apache.jackrabbit.core.persistence.bundle.util.DbNameIndex;

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
 * <li>&lt;param name="{@link #setUser(String) user}" value="crx"/>
 * <li>&lt;param name="{@link #setPassword(String) password}" value="crx"/>
 * <li>&lt;param name="{@link #setSchema(String) schema}" value="oracle"/>
 * <li>&lt;param name="{@link #setSchemaObjectPrefix(String) schemaObjectPrefix}" value="${wsp.name}_"/>
 * <li>&lt;param name="{@link #setErrorHandling(String) errorHandling}" value=""/>
 * </ul>
 */
public class OraclePersistenceManager extends BundleDbPersistenceManager {

    /** the cvs/svn id */
    static final String CVS_ID = "$URL$ $Rev$ $Date$";

    /**
     * Creates a new oracle persistence manager
     */
    public OraclePersistenceManager() {
        // enable db blob support
        setExternalBLOBs("false");
    }

    public void init(PMContext context) throws Exception {
        // init default values
        if (getDriver() == null) {
            setDriver("oracle.jdbc.OracleDriver");
        }
        if (getUrl() == null) {
            setUrl("jdbc:oracle:thin:@127.0.0.1:1521:xe");
        }
        if (getUser() == null) {
            setUser("crx");
        }
        if (getPassword() == null) {
            setPassword("crx");
        }
        if (getSchema() == null) {
            setSchema("oracle");
        }
        if (getSchemaObjectPrefix() == null) {
            setSchemaObjectPrefix(context.getHomeDir().getName() + "_");
        }
        super.init(context);
/*
        // check driver version
        DatabaseMetaData metaData = con.getMetaData();
        if (metaData.getDriverMajorVersion() < 10) {
            // oracle drivers prior to version 10 only support
            // writing BLOBs up to 32k in size...
            log.warn("unsupported driver version detected: "
                    + metaData.getDriverName()
                    + " v" + metaData.getDriverVersion());
        }
*/
    }

    /**
     * Retruns a new instance of a NGKDbNameIndex.
     * @return a new instance of a NGKDbNameIndex.
     * @throws SQLException if an SQL error occurs.
     */
    protected DbNameIndex createDbNameIndex() throws SQLException {
        return new NGKDbNameIndex(con, schemaObjectPrefix);
    }
    
    /**
     * Since Oracle only supports table names up to 30 characters in
     * length illegal characters are simply replaced with "_" rather than
     * escaping them with "_x0000_".
     *
     * @inheritDoc
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
                escaped.append('_');
            } else {
                escaped.append(c);
            }
        }
        schemaObjectPrefix = escaped.toString();
    }
}
