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

import org.apache.jackrabbit.core.util.db.CheckSchemaOperation;

/**
 * Extends the {@link BundleDbPersistenceManager} by MS-SQL specific code.
 * <p>
 * Configuration:<br>
 * <ul>
 * <li>&lt;param name="{@link #setBundleCacheSize(String) bundleCacheSize}" value="8"/&gt;
 * <li>&lt;param name="{@link #setConsistencyCheck(String) consistencyCheck}" value="false"/&gt;
 * <li>&lt;param name="{@link #setMinBlobSize(String) minBlobSize}" value="16384"/&gt;
 * <li>&lt;param name="{@link #setDriver(String) driver}" value="com.microsoft.sqlserver.jdbc.SQLServerDriver"/&gt;
 * <li>&lt;param name="{@link #setUrl(String) url}" value=""/&gt;
 * <li>&lt;param name="{@link #setUser(String) user}" value=""/&gt;
 * <li>&lt;param name="{@link #setPassword(String) password}" value=""/&gt;
 * <li>&lt;param name="{@link #setSchema(String) schema}" value="mssql"/&gt;
 * <li>&lt;param name="{@link #setSchemaObjectPrefix(String) schemaObjectPrefix}" value=""/&gt;
 * <li>&lt;param name="{@link #setErrorHandling(String) errorHandling}" value=""/&gt;
 * <li>&lt;param name="{@link #setTableSpace(String) tableSpace}" value=""/&gt;
 * </ul>
 */
public class MSSqlPersistenceManager extends BundleDbPersistenceManager {

    /** the MS SQL table space to use */
    protected String tableSpace = "";

    public MSSqlPersistenceManager() {
        setDriver("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        setDatabaseType("mssql");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected CheckSchemaOperation createCheckSchemaOperation() {
        return super.createCheckSchemaOperation().addVariableReplacement(
            CheckSchemaOperation.TABLE_SPACE_VARIABLE, tableSpace);
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
        if (tableSpace != null && tableSpace.trim().length() > 0) {
            this.tableSpace = "on " + tableSpace.trim();
        } else {
            this.tableSpace = "";
        }
    }

}
