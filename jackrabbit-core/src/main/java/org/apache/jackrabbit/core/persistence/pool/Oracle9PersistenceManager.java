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

import javax.sql.DataSource;

import org.apache.jackrabbit.core.util.db.ConnectionHelper;
import org.apache.jackrabbit.core.util.db.Oracle10R1ConnectionHelper;

/**
 * <code>OracleLegacyPersistenceManager</code> provides support for Oracle jdbc drivers prior to version 10
 * which require special handling of BLOB data.
 * <p>Configuration:<br>
 * <ul>
 * <li>&lt;param name="{@link #setBundleCacheSize(String) bundleCacheSize}" value="8"/&gt;
 * <li>&lt;param name="{@link #setConsistencyCheck(String) consistencyCheck}" value="false"/&gt;
 * <li>&lt;param name="{@link #setMinBlobSize(String) minBlobSize}" value="16384"/&gt;
 * <li>&lt;param name="{@link #setDriver(String) driver}" value="oracle.jdbc.OracleDriverr"/&gt;
 * <li>&lt;param name="{@link #setUrl(String) url}" value="jdbc:oracle:thin:@127.0.0.1:1521:xe"/&gt;
 * <li>&lt;param name="{@link #setUser(String) user}" value=""/&gt;
 * <li>&lt;param name="{@link #setPassword(String) password}" value=""/&gt;
 * <li>&lt;param name="{@link #setSchema(String) schema}" value="oracle"/&gt;
 * <li>&lt;param name="{@link #setSchemaObjectPrefix(String) schemaObjectPrefix}" value="${wsp.name}_"/&gt;
 * <li>&lt;param name="{@link #setErrorHandling(String) errorHandling}" value=""/&gt;
 * </ul>
 */
public class Oracle9PersistenceManager extends OraclePersistenceManager {

    /**
     * {@inheritDoc}
     */
    @Override
    protected ConnectionHelper createConnectionHelper(DataSource dataSrc) throws Exception {
        Oracle10R1ConnectionHelper helper = new Oracle10R1ConnectionHelper(dataSrc, blockOnConnectionLoss);
        helper.init();
        return helper;
    }
}
