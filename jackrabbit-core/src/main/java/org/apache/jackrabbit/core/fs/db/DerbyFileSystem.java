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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Connection;

/**
 * <code>DerbyFileSystem</code> is a JDBC-based <code>FileSystem</code>
 * implementation for Jackrabbit that persists file system entries in an
 * embedded Derby database.
 * <p/>
 * It is configured through the following properties:
 * <ul>
 * <li><code>url</code>: the database url of the form
 * <code>"jdbc:derby:[db];[attributes]"</code></li>
 * <li><code>schemaObjectPrefix</code>: prefix to be prepended to schema objects</li>
 * <li><code>driver</code>: the FQN name of the JDBC driver class
 * (default: <code>"org.apache.derby.jdbc.EmbeddedDriver"</code>)</li>
 * <li><code>schema</code>: type of schema to be used
 * (default: <code>"derby"</code>)</li>
 * <li><code>user</code>: the database user (default: <code>null</code>)</li>
 * <li><code>password</code>: the user's password (default: <code>null</code>)</li>
 * <li><code>shutdownOnClose</code>: if <code>true</code> (the default) the
 * database is shutdown when the last connection is closed;
 * set this to <code>false</code> when using a standalone database</li>
 * </ul>
 * See also {@link DbFileSystem}.
 * <p/>
 * The following is a fragment from a sample configuration:
 * <pre>
 *   &lt;FileSystem class="org.apache.jackrabbit.core.fs.db.DerbyFileSystem"&gt;
 *       &lt;param name="url" value="jdbc:derby:${rep.home}/db;create=true"/&gt;
 *       &lt;param name="schemaObjectPrefix" value="rep_"/&gt;
 *  &lt;/FileSystem&gt;
 * </pre>
 */
public class DerbyFileSystem extends DbFileSystem {

    /**
     * Logger instance
     */
    private static Logger log = LoggerFactory.getLogger(DerbyFileSystem.class);

    /**
     * Flag indicating whether this derby database should be shutdown on close.
     */
    protected boolean shutdownOnClose;

    /**
     * Creates a new <code>DerbyFileSystem</code> instance.
     */
    public DerbyFileSystem() {
        // preset some attributes to reasonable defaults
        schema = "derby";
        driver = "org.apache.derby.jdbc.EmbeddedDriver";
        shutdownOnClose = true;
        initialized = false;
    }

    //----------------------------------------------------< setters & getters >

    public boolean getShutdownOnClose() {
        return shutdownOnClose;
    }

    public void setShutdownOnClose(boolean shutdownOnClose) {
        this.shutdownOnClose = shutdownOnClose;
    }

    //-----------------------------------------------< DbFileSystem overrides >

    /**
     * Closes the given connection and shuts down the embedded Derby
     * database if <code>shutdownOnClose</code> is set to true.
     *
     * @param connection database connection
     * @throws SQLException if an error occurs
     * @see DatabaseFileSystem#closeConnection(Connection)
     */
    protected void closeConnection(Connection connection) throws SQLException {
        // prepare connection url for issuing shutdown command
        String url;
        try {
            url = connection.getMetaData().getURL();
        } catch (SQLException e) {
            // JCR-1557: embedded derby db probably already shut down;
            // this happens when configuring multiple FS/PM instances
            // to use the same embedded derby db instance.
            log.debug("failed to retrieve connection url: embedded db probably already shut down", e);
            return;
        }
        int pos = url.lastIndexOf(';');
        if (pos != -1) {
            // strip any attributes from connection url
            url = url.substring(0, pos);
        }
        url += ";shutdown=true";

        // we have to reset the connection to 'autoCommit=true' before closing it;
        // otherwise Derby would mysteriously complain about some pending uncommitted
        // changes which can't possibly be true.
        // @todo further investigate
        connection.setAutoCommit(true);
        connection.close();

        if (shutdownOnClose) {
            // now it's safe to shutdown the embedded Derby database
            try {
                DriverManager.getConnection(url);
            } catch (SQLException e) {
                // a shutdown command always raises a SQLException
                log.info(e.getMessage());
            }
        }
    }
}
