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
package org.apache.jackrabbit.core.fs.db;

import org.apache.jackrabbit.core.util.db.CheckSchemaOperation;

/**
 * <code>MSSqlFileSystem</code> is a JDBC-based <code>FileSystem</code>
 * implementation for Jackrabbit that persists file system entries in an
 * MS SQL database.
 * <p>
 * It is configured through the following properties:
 * <ul>
 * <li><code>driver</code>: the FQN name of the JDBC driver class
 * (default: <code>"com.microsoft.sqlserver.jdbc.SQLServerDriver"</code>)</li>
 * <li><code>schema</code>: type of schema to be used
 * (default: <code>"mssql"</code>)</li>
 * <li><code>url</code>: the database url (e.g.
 * <code>"jdbc:sqlserver://[host]:[port];&lt;params&gt;"</code>)</li>
 * <li><code>user</code>: the database user</li>
 * <li><code>password</code>: the user's password</li>
 * <li><code>schemaObjectPrefix</code>: prefix to be prepended to schema objects</li>
 * <li><code>tableSpace</code>: the tablespace to use</li>
 * </ul>
 * See also {@link DbFileSystem}.
 * <p>
 * The following is a fragment from a sample configuration:
 * <pre>
 *   &lt;FileSystem class="org.apache.jackrabbit.core.fs.db.MSSqlFileSystem"&gt;
 *       &lt;param name="url" value="jdbc:sqlserver://localhost:1433"/&gt;
 *       &lt;param name="user" value="padv25"/&gt;
 *       &lt;param name="password" value="padv25"/&gt;
 *       &lt;param name="schemaObjectPrefix" value="rep_"/&gt;
 *       &lt;param name="tableSpace" value="default"/&gt;
 *  &lt;/FileSystem&gt;
 * </pre>
 */
public class MSSqlFileSystem extends DbFileSystem {

    /** the variable for the MS SQL table space */
    public static final String TABLE_SPACE_VARIABLE = "${tableSpace}";

    /** the MS SQL table space to use */
    protected String tableSpace = "";

    /**
     * Returns the configured MS SQL table space.
     * @return the configured MS SQL table space.
     */
    public String getTableSpace() {
        return tableSpace;
    }

    /**
     * Sets the MS SQL table space.
     * @param tableSpace the MS SQL table space.
     */
    public void setTableSpace(String tableSpace) {
        if (tableSpace != null && tableSpace.length() > 0) {
            this.tableSpace = "on " + tableSpace.trim();
        } else {
            this.tableSpace = "";
        }
    }

    /**
     * Creates a new <code>MSSqlFileSystem</code> instance.
     */
    public MSSqlFileSystem() {
        // preset some attributes to reasonable defaults
        schema = "mssql";
        driver = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
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