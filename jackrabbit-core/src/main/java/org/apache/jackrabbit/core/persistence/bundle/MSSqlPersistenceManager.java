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

import org.apache.jackrabbit.util.Text;

/**
 * Extends the {@link BundleDbPersistenceManager} by MS-SQL specific code.
 * <p/>
 * Configuration:<br>
 * <ul>
 * <li>&lt;param name="{@link #setBundleCacheSize(String) bundleCacheSize}" value="8"/>
 * <li>&lt;param name="{@link #setConsistencyCheck(String) consistencyCheck}" value="false"/>
 * <li>&lt;param name="{@link #setMinBlobSize(String) minBlobSize}" value="16384"/>
 * <li>&lt;param name="{@link #setDriver(String) driver}" value="com.microsoft.sqlserver.jdbc.SQLServerDriver"/>
 * <li>&lt;param name="{@link #setUrl(String) url}" value=""/>
 * <li>&lt;param name="{@link #setUser(String) user}" value=""/>
 * <li>&lt;param name="{@link #setPassword(String) password}" value=""/>
 * <li>&lt;param name="{@link #setSchema(String) schema}" value="mssql"/>
 * <li>&lt;param name="{@link #setSchemaObjectPrefix(String) schemaObjectPrefix}" value=""/>
 * <li>&lt;param name="{@link #setErrorHandling(String) errorHandling}" value=""/>
 * <li>&lt;param name="{@link #setTableSpace(String) tableSpace}" value=""/>
 * </ul>
 */
public class MSSqlPersistenceManager extends BundleDbPersistenceManager {

    /** the MS SQL table space to use */
    protected String tableSpace = "";

    public MSSqlPersistenceManager() {
        setDriver("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        setDatabaseType("mssql");
    }

    protected String createSchemaSQL(String sql) {
        return Text.replace(
                super.createSchemaSQL(sql), "${tableSpace}", tableSpace);
    }

    /**
     * Returns the configured MS SQL table space.
     *
     * @return the configured MS SQL table space.
     */
    public String getTableSpace() {
        return tableSpace;
    }

    /**
     * Sets the MS SQL table space.
     *
     * @param tableSpace the MS SQL table space.
     */
    public void setTableSpace(String tableSpace) {
        if (tableSpace != null && tableSpace.length() > 0) {
            this.tableSpace = "on " + tableSpace.trim();
        } else {
            this.tableSpace = "";
        }
    }

}
