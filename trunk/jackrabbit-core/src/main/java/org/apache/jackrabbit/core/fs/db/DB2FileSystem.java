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

/**
 * <code>DB2FileSystem</code> is a JDBC-based <code>FileSystem</code>
 * implementation for Jackrabbit that persists file system entries in a
 * DB2 database.
 * <p>
 * It is configured through the following properties:
 * <ul>
 * <li><code>driver</code>: the FQN name of the JDBC driver class
 * (default: <code>"com.ibm.db2.jcc.DB2Driver"</code>)</li>
 * <li><code>schema</code>: type of schema to be used
 * (default: <code>"db2"</code>)</li>
 * <li><code>url</code>: the database url (e.g.
 * <code>"jdbc:db2:[database]"</code>)</li>
 * <li><code>user</code>: the database user</li>
 * <li><code>password</code>: the user's password</li>
 * <li><code>schemaObjectPrefix</code>: prefix to be prepended to schema objects</li>
 * </ul>
 * See also {@link DbFileSystem}.
 * <p>
 * The following is a fragment from a sample configuration:
 * <pre>
 *   &lt;FileSystem class="org.apache.jackrabbit.core.fs.db.DB2FileSystem"&gt;
 *       &lt;param name="url" value="jdbc:db2:test"/&gt;
 *       &lt;param name="schemaObjectPrefix" value="rep_"/&gt;
 *   &lt;/FileSystem&gt;
 * </pre>
 */
public class DB2FileSystem extends DbFileSystem {

    /**
     * Creates a new <code>DB2FileSystem</code> instance.
     */
    public DB2FileSystem() {
        // preset some attributes to reasonable defaults
        schema = "db2";
        driver = "com.ibm.db2.jcc.DB2Driver";
    }

    //-----------------------------------------< DatabaseFileSystem overrides >
    /**
     * {@inheritDoc}
     * <p>
     * Since DB2 requires parameter markers within the select clause to be
     * explicitly typed using <code>cast(? as type_name)</code> some statements
     * had to be changed accordingly.
     */
    protected void buildSQLStatements() {
        super.buildSQLStatements();

        copyFileSQL = "insert into "
                + schemaObjectPrefix + "FSENTRY "
                + "(FSENTRY_PATH, FSENTRY_NAME, FSENTRY_DATA, "
                + "FSENTRY_LASTMOD, FSENTRY_LENGTH) "
                + "select cast(? as varchar(745)), cast(? as varchar(255)), FSENTRY_DATA, "
                + "FSENTRY_LASTMOD, FSENTRY_LENGTH from "
                + schemaObjectPrefix + "FSENTRY where FSENTRY_PATH = ? "
                + "and FSENTRY_NAME = ? and FSENTRY_DATA is not null";

        copyFilesSQL = "insert into "
                + schemaObjectPrefix + "FSENTRY "
                + "(FSENTRY_PATH, FSENTRY_NAME, FSENTRY_DATA, "
                + "FSENTRY_LASTMOD, FSENTRY_LENGTH) "
                + "select cast(? as varchar(745)), FSENTRY_NAME, FSENTRY_DATA, "
                + "FSENTRY_LASTMOD, FSENTRY_LENGTH from "
                + schemaObjectPrefix + "FSENTRY where FSENTRY_PATH = ? "
                + "and FSENTRY_DATA is not null";
    }
}
