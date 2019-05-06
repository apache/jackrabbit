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
 * Extends the {@link BundleDbPersistenceManager} by mysql specific code.
 * <p>
 * Configuration:<br>
 * <ul>
 * <li>&lt;param name="{@link #setBundleCacheSize(String) bundleCacheSize}" value="8"/&gt;
 * <li>&lt;param name="{@link #setConsistencyCheck(String) consistencyCheck}" value="false"/&gt;
 * <li>&lt;param name="{@link #setMinBlobSize(String) minBlobSize}" value="16384"/&gt;
 * <li>&lt;param name="{@link #setDriver(String) driver}" value="org.gjt.mm.mysql.Driver"/&gt;
 * <li>&lt;param name="{@link #setUrl(String) url}" value=""/&gt;
 * <li>&lt;param name="{@link #setUser(String) user}" value=""/&gt;
 * <li>&lt;param name="{@link #setPassword(String) password}" value=""/&gt;
 * <li>&lt;param name="{@link #setSchema(String) schema}" value="mysql"/&gt;
 * <li>&lt;param name="{@link #setSchemaObjectPrefix(String) schemaObjectPrefix}" value=""/&gt;
 * <li>&lt;param name="{@link #setErrorHandling(String) errorHandling}" value=""/&gt;
 * </ul>
 */
public class MySqlPersistenceManager extends BundleDbPersistenceManager {

    /**
     * {@inheritDoc}
     */
    public void init(PMContext context) throws Exception {
        // init default values
        if (getDriver() == null) {
            setDriver("org.gjt.mm.mysql.Driver");
        }
        if (getDatabaseType() == null) {
            setDatabaseType("mysql");
        }
        super.init(context);
    }

}
