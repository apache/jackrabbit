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
import java.sql.SQLException;

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
 * <li><code>user</code>: the database user (default: <code>""</code>)</li>
 * <li><code>password</code>: the user's password (default: <code>""</code>)</li>
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
     * Creates a new <code>DerbyFileSystem</code> instance.
     */
    public DerbyFileSystem() {
        // preset some attributes to reasonable defaults
        schema = "derby";
        driver = "org.apache.derby.jdbc.EmbeddedDriver";
        schemaObjectPrefix = "";
        user = "";
        password = "";
        initialized = false;
    }

    //-----------------------------------------------< DbFileSystem overrides >
    /**
     * {@inheritDoc}
     */
    public void close() throws FileSystemException {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        // prepare connection url for issuing shutdown command
        String url;
        try {
            url = con.getMetaData().getURL();
        } catch (SQLException e) {
            String msg = "error closing file system";
            log.error(msg, e);
            throw new FileSystemException(msg, e);
        }

        int pos = url.lastIndexOf(';');
        if (pos != -1) {
            // strip any attributes from connection url
            url = url.substring(0, pos);
        }
        url += ";shutdown=true";

        // call base class implementation
        super.close();

        // now it's safe to shutdown the embedded Derby database
        try {
            DriverManager.getConnection(url);
        } catch (SQLException e) {
            // a shutdown command always raises a SQLException
            log.info(e.getMessage());
        }
    }
}
