/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

import org.apache.jackrabbit.core.fs.FileSystemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.DriverManager;

/**
 * <code>DB2FileSystem</code> is a JDBC-based <code>FileSystem</code>
 * implementation for Jackrabbit that persists file system entries in a
 * DB2 database.
 * <p/>
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
 * <p/>
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
     * Logger instance
     */
    private static Logger log = LoggerFactory.getLogger(DB2FileSystem.class);

    /**
     * Creates a new <code>DB2FileSystem</code> instance.
     */
    public DB2FileSystem() {
        // preset some attributes to reasonable defaults
        schema = "db2";
        driver = "com.ibm.db2.jcc.DB2Driver";
        schemaObjectPrefix = "";
        user = "";
        password = "";
        initialized = false;
    }

    //-----------------------------------------------< DbFileSystem overrides >
    /**
     * {@inheritDoc}
     * <p/>
     * Since DB2 requires parameter markers within the select clause to be
     * explicitly typed using <code>cast(? as type_name)</code> some statements
     * had to be changed accordingly.
     */
    public void init() throws FileSystemException {
        if (initialized) {
            throw new IllegalStateException("already initialized");
        }

        try {
            // setup jdbc connection
            Class.forName(driver);
            con = DriverManager.getConnection(url, user, password);
            con.setAutoCommit(true);

            // make sure schemaObjectPrefix consists of legal name characters only
            prepareSchemaObjectPrefix();

            // check if schema objects exist and create them if necessary
            checkSchema();

            // prepare statements
            insertFileStmt = con.prepareStatement("insert into "
                    + schemaObjectPrefix + "FSENTRY "
                    + "(FSENTRY_PATH, FSENTRY_NAME, FSENTRY_DATA, "
                    + "FSENTRY_LASTMOD, FSENTRY_LENGTH) "
                    + "values (?, ?, ?, ?, ?)");

            insertFolderStmt = con.prepareStatement("insert into "
                    + schemaObjectPrefix + "FSENTRY "
                    + "(FSENTRY_PATH, FSENTRY_NAME, FSENTRY_LASTMOD, FSENTRY_LENGTH) "
                    + "values (?, ?, ?, 0)");

            updateDataStmt = con.prepareStatement("update "
                    + schemaObjectPrefix + "FSENTRY "
                    + "set FSENTRY_DATA = ?, FSENTRY_LASTMOD = ?, FSENTRY_LENGTH = ? "
                    + "where FSENTRY_PATH = ? and FSENTRY_NAME = ? "
                    + "and FSENTRY_DATA is not null");

            updateLastModifiedStmt = con.prepareStatement("update "
                    + schemaObjectPrefix + "FSENTRY set FSENTRY_LASTMOD = ? "
                    + "where FSENTRY_PATH = ? and FSENTRY_NAME = ? "
                    + "and FSENTRY_DATA is not null");

            selectExistStmt = con.prepareStatement("select 1 from "
                    + schemaObjectPrefix + "FSENTRY where FSENTRY_PATH = ? "
                    + "and FSENTRY_NAME = ?");

            selectFileExistStmt = con.prepareStatement("select 1 from "
                    + schemaObjectPrefix + "FSENTRY where FSENTRY_PATH = ? "
                    + "and FSENTRY_NAME = ? and FSENTRY_DATA is not null");

            selectFolderExistStmt = con.prepareStatement("select 1 from "
                    + schemaObjectPrefix + "FSENTRY where FSENTRY_PATH = ? "
                    + "and FSENTRY_NAME = ? and FSENTRY_DATA is null");

            selectFileNamesStmt = con.prepareStatement("select FSENTRY_NAME from "
                    + schemaObjectPrefix + "FSENTRY where FSENTRY_PATH = ? "
                    + "and FSENTRY_DATA is not null");

            selectFolderNamesStmt = con.prepareStatement("select FSENTRY_NAME from "
                    + schemaObjectPrefix + "FSENTRY where FSENTRY_PATH = ? "
                    + "and FSENTRY_DATA is null");

            selectFileAndFolderNamesStmt = con.prepareStatement("select FSENTRY_NAME from "
                    + schemaObjectPrefix + "FSENTRY where FSENTRY_PATH = ?");

            selectChildCountStmt = con.prepareStatement("select count(FSENTRY_NAME) from "
                    + schemaObjectPrefix + "FSENTRY where FSENTRY_PATH = ?  ");

            selectDataStmt = con.prepareStatement("select FSENTRY_DATA from "
                    + schemaObjectPrefix + "FSENTRY where FSENTRY_PATH = ? "
                    + "and FSENTRY_NAME = ? and FSENTRY_DATA is not null");

            selectLastModifiedStmt = con.prepareStatement("select FSENTRY_LASTMOD from "
                    + schemaObjectPrefix + "FSENTRY where FSENTRY_PATH = ? "
                    + "and FSENTRY_NAME = ?");

            selectLengthStmt = con.prepareStatement("select FSENTRY_LENGTH from "
                    + schemaObjectPrefix + "FSENTRY where FSENTRY_PATH = ? "
                    + "and FSENTRY_NAME = ? and FSENTRY_DATA is not null");

            deleteFileStmt = con.prepareStatement("delete from "
                    + schemaObjectPrefix + "FSENTRY where FSENTRY_PATH = ? "
                    + "and FSENTRY_NAME = ? and FSENTRY_DATA is not null");

            deleteFolderStmt = con.prepareStatement("delete from "
                    + schemaObjectPrefix + "FSENTRY where "
                    + "(FSENTRY_PATH = ? and FSENTRY_NAME = ? and FSENTRY_DATA is null) "
                    + "or (FSENTRY_PATH = ?) "
                    + "or (FSENTRY_PATH like ?) ");

            copyFileStmt = con.prepareStatement("insert into "
                    + schemaObjectPrefix + "FSENTRY "
                    + "(FSENTRY_PATH, FSENTRY_NAME, FSENTRY_DATA, "
                    + "FSENTRY_LASTMOD, FSENTRY_LENGTH) "
                    + "select cast(? as varchar(745)), cast(? as varchar(255)), FSENTRY_DATA, "
                    + "FSENTRY_LASTMOD, FSENTRY_LENGTH from "
                    + schemaObjectPrefix + "FSENTRY where FSENTRY_PATH = ? "
                    + "and FSENTRY_NAME = ? and FSENTRY_DATA is not null");

            copyFilesStmt = con.prepareStatement("insert into "
                    + schemaObjectPrefix + "FSENTRY "
                    + "(FSENTRY_PATH, FSENTRY_NAME, FSENTRY_DATA, "
                    + "FSENTRY_LASTMOD, FSENTRY_LENGTH) "
                    + "select cast(? as varchar(745)), FSENTRY_NAME, FSENTRY_DATA, "
                    + "FSENTRY_LASTMOD, FSENTRY_LENGTH from "
                    + schemaObjectPrefix + "FSENTRY where FSENTRY_PATH = ? "
                    + "and FSENTRY_DATA is not null");

            // finally verify that there's a file system root entry
            verifyRoodExists();

            initialized = true;
        } catch (Exception e) {
            String msg = "failed to initialize file system";
            log.error(msg, e);
            throw new FileSystemException(msg, e);
        }
    }
}
