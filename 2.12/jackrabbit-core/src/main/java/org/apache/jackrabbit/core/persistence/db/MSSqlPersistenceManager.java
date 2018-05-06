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
package org.apache.jackrabbit.core.persistence.db;

import org.apache.jackrabbit.util.Text;

/**
 * <code>MSSqlPersistenceManager</code> is a JDBC-based
 * <code>PersistenceManager</code> for Jackrabbit that persists
 * <code>ItemState</code> and <code>NodeReferences</code> objects in MS SQL
 * database using a simple custom serialization format and a
 * very basic non-normalized database schema (in essence tables with one 'key'
 * and one 'data' column).
 * <p>
 * It is configured through the following properties:
 * <ul>
 * <li><code>driver</code>: the FQN name of the JDBC driver class
 * (default: <code>"com.microsoft.sqlserver.jdbc.SQLServerDriver"</code>)</li>
 * <li><code>schema</code>: type of schema to be used
 * (default: <code>"mssql"</code>)</li>
 * <li><code>url</code>: the database url (e.g.
 * <code>"jdbc:microsoft:sqlserver://[host]:[port];databaseName=[dbname]"</code>)</li>
 * <li><code>user</code>: the database user</li>
 * <li><code>password</code>: the user's password</li>
 * <li><code>schemaObjectPrefix</code>: prefix to be prepended to schema objects</li>
 * <li><code>tableSpace</code>: the tablespace to use</li>
 * <li><code>externalBLOBs</code>: if <code>true</code> (the default) BINARY
 * values (BLOBs) are stored in the local file system;
 * if <code>false</code> BLOBs are stored in the database</li>
 * </ul>
 * See also {@link SimpleDbPersistenceManager}.
 * <p>
 * The following is a fragment from a sample configuration:
 * <pre>
 *   &lt;PersistenceManager class="org.apache.jackrabbit.core.persistence.db.MSSqlPersistenceManager"&gt;
 *       &lt;param name="url" value="jdbc:microsoft:sqlserver://localhost:1433;mydb"/&gt;
 *       &lt;param name="user" value="mydba"/&gt;
 *       &lt;param name="password" value="mydba"/&gt;
 *       &lt;param name="schemaObjectPrefix" value="${wsp.name}_"/&gt;
 *       &lt;param name="tableSpace" value=""/&gt;
 *       &lt;param name="externalBLOBs" value="false"/&gt;
 *  &lt;/PersistenceManager&gt;
 * </pre>
 *
 * @deprecated Please migrate to a bundle persistence manager
 *   (<a href="https://issues.apache.org/jira/browse/JCR-2802">JCR-2802</a>)
 */
@Deprecated
public class MSSqlPersistenceManager extends SimpleDbPersistenceManager {

    /** the variable for the MSSql table space */
    public static final String TABLE_SPACE_VARIABLE = "${tableSpace}";
    
    /** the MSSql table space to use */
    protected String tableSpace;

    /**
     * Creates a new <code>MSSqlPersistenceManager</code> instance.
     */
    public MSSqlPersistenceManager() {
        // preset some attributes to reasonable defaults
        schema = "mssql";
        driver = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
        schemaObjectPrefix = "";
        user = "";
        password = "";
        initialized = false;
        tableSpace = "";
    }
        
    /**
     * Returns the configured MSSql table space.
     * @return the configured MSSql table space.
     */
    public String getTableSpace() {
        return tableSpace;
    }

    /**
     * Sets the MSSql table space.
     * @param tableSpace the MSSql table space.
     */
    public void setTableSpace(String tableSpace) {
        if (tableSpace != null && tableSpace.length() > 0) {
            this.tableSpace = "on " + tableSpace.trim();
        } else {
            this.tableSpace = "";
        }
    }

   protected String createSchemaSql(String sql) {
       return Text.replace(
               super.createSchemaSql(sql), TABLE_SPACE_VARIABLE, tableSpace);
   }
}