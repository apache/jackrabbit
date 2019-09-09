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

import org.apache.jackrabbit.core.persistence.util.Serializer;
import org.apache.jackrabbit.core.util.db.ConnectionFactory;
import org.apache.jackrabbit.core.util.db.DatabaseAware;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import javax.jcr.RepositoryException;

/**
 * <code>SimpleDbPersistenceManager</code> is a generic JDBC-based
 * <code>PersistenceManager</code> for Jackrabbit that persists
 * <code>ItemState</code> and <code>NodeReferences</code> objects using a
 * simple custom binary serialization format (see {@link Serializer}) and a
 * very basic non-normalized database schema (in essence tables with one 'key'
 * and one 'data' column).
 * <p>
 * It is configured through the following properties:
 * <ul>
 * <li><code>driver</code>: the FQN name of the JDBC driver class</li>
 * <li><code>url</code>: the database url of the form <code>jdbc:subprotocol:subname</code></li>
 * <li><code>user</code>: the database user</li>
 * <li><code>password</code>: the user's password</li>
 * <li><code>schema</code>: type of schema to be used
 * (e.g. <code>mysql</code>, <code>mssql</code>, etc.); </li>
 * <li><code>schemaObjectPrefix</code>: prefix to be prepended to schema objects</li>
 * <li><code>externalBLOBs</code>: if <code>true</code> (the default) BINARY
 * values (BLOBs) are stored in the local file system;
 * if <code>false</code> BLOBs are stored in the database</li>
 * </ul>
 * The required schema objects are automatically created by executing the DDL
 * statements read from the [schema].ddl file. The .ddl file is read from the
 * resources by calling <code>getClass().getResourceAsStream(schema + ".ddl")</code>.
 * Every line in the specified .ddl file is executed separately by calling
 * <code>java.sql.Statement.execute(String)</code> where every occurrence of the
 * the string <code>"${schemaObjectPrefix}"</code> has been replaced with the
 * value of the property <code>schemaObjectPrefix</code>.
 * <p>
 * The following is a fragment from a sample configuration using MySQL:
 * <pre>
 *   &lt;PersistenceManager class="org.apache.jackrabbit.core.persistence.db.SimpleDbPersistenceManager"&gt;
 *       &lt;param name="driver" value="com.mysql.jdbc.Driver"/&gt;
 *       &lt;param name="url" value="jdbc:mysql:///test?autoReconnect=true"/&gt;
 *       &lt;param name="schema" value="mysql"/&gt;
 *       &lt;param name="schemaObjectPrefix" value="${wsp.name}_"/&gt;
 *       &lt;param name="externalBLOBs" value="false"/&gt;
 *   &lt;/PersistenceManager&gt;
 * </pre>
 * The following is a fragment from a sample configuration using Daffodil One$DB Embedded:
 * <pre>
 *   &lt;PersistenceManager class="org.apache.jackrabbit.core.persistence.db.SimpleDbPersistenceManager"&gt;
 *       &lt;param name="driver" value="in.co.daffodil.db.jdbc.DaffodilDBDriver"/&gt;
 *       &lt;param name="url" value="jdbc:daffodilDB_embedded:${wsp.name};path=${wsp.home}/../../databases;create=true"/&gt;
 *       &lt;param name="user" value="daffodil"/&gt;
 *       &lt;param name="password" value="daffodil"/&gt;
 *       &lt;param name="schema" value="daffodil"/&gt;
 *       &lt;param name="schemaObjectPrefix" value="${wsp.name}_"/&gt;
 *       &lt;param name="externalBLOBs" value="false"/&gt;
 *   &lt;/PersistenceManager&gt;
 * </pre>
 * The following is a fragment from a sample configuration using DB2:
 * <pre>
 *   &lt;PersistenceManager class="org.apache.jackrabbit.core.persistence.db.SimpleDbPersistenceManager"&gt;
 *       &lt;param name="driver" value="com.ibm.db2.jcc.DB2Driver"/&gt;
 *       &lt;param name="url" value="jdbc:db2:test"/&gt;
 *       &lt;param name="schema" value="db2"/&gt;
 *       &lt;param name="schemaObjectPrefix" value="${wsp.name}_"/&gt;
 *       &lt;param name="externalBLOBs" value="false"/&gt;
 *   &lt;/PersistenceManager&gt;
 * </pre>
 * The following is a fragment from a sample configuration using MSSQL:
 * <pre>
 *   &lt;PersistenceManager class="org.apache.jackrabbit.core.persistence.db.SimpleDbPersistenceManager"&gt;
 *       &lt;param name="driver" value="com.microsoft.jdbc.sqlserver.SQLServerDriver"/&gt;
 *       &lt;param name="url" value="jdbc:microsoft:sqlserver://localhost:1433;;DatabaseName=test;SelectMethod=Cursor;"/&gt;
 *       &lt;param name="schema" value="mssql"/&gt;
 *       &lt;param name="user" value="sa"/&gt;
 *       &lt;param name="password" value=""/&gt;
 *       &lt;param name="schemaObjectPrefix" value="${wsp.name}_"/&gt;
 *       &lt;param name="externalBLOBs" value="false"/&gt;
 *   &lt;/PersistenceManager&gt;
 * </pre>
 * The following is a fragment from a sample configuration using Ingres:
 * <pre>
 *   &lt;PersistenceManager class="org.apache.jackrabbit.core.persistence.db.SimpleDbPersistenceManager"&gt;
 *       &lt;param name="driver" value="com.ingres.jdbc.IngresDriver"/&gt;
 *       &lt;param name="url" value="jdbc:ingres://localhost:II7/test"/&gt;
 *       &lt;param name="schema" value="ingres"/&gt;
 *       &lt;param name="user" value="ingres"/&gt;
 *       &lt;param name="password" value="ingres"/&gt;
 *       &lt;param name="schemaObjectPrefix" value="${wsp.name}_"/&gt;
 *       &lt;param name="externalBLOBs" value="false"/&gt;
 *   &lt;/PersistenceManager&gt;
 * </pre>
 * The following is a fragment from a sample configuration using PostgreSQL:
 * <pre>
 *   &lt;PersistenceManager class="org.apache.jackrabbit.core.persistence.db.SimpleDbPersistenceManager"&gt;
 *       &lt;param name="driver" value="org.postgresql.Driver"/&gt;
 *       &lt;param name="url" value="jdbc:postgresql://localhost/test"/&gt;
 *       &lt;param name="schema" value="postgresql"/&gt;
 *       &lt;param name="user" value="postgres"/&gt;
 *       &lt;param name="password" value="postgres"/&gt;
 *       &lt;param name="schemaObjectPrefix" value="${wsp.name}_"/&gt;
 *       &lt;param name="externalBLOBs" value="false"/&gt;
 *   &lt;/PersistenceManager&gt;
 * </pre>
 * JNDI can be used to get the connection. In this case, use the javax.naming.InitialContext as the driver,
 * and the JNDI name as the URL. If the user and password are configured in the JNDI resource,
 * they should not be configured here. Example JNDI settings:
 * <pre>
 * &lt;param name="driver" value="javax.naming.InitialContext" /&gt;
 * &lt;param name="url" value="java:comp/env/jdbc/Test" /&gt;
 * </pre>
 * See also {@link DerbyPersistenceManager}, {@link OraclePersistenceManager}.
 *
 * @deprecated Please migrate to a bundle persistence manager
 *   (<a href="https://issues.apache.org/jira/browse/JCR-2802">JCR-2802</a>)
 */
@Deprecated
public class SimpleDbPersistenceManager extends DatabasePersistenceManager implements DatabaseAware {

    protected String driver;
    protected String url;
    protected String user;
    protected String password;

    /**
     * The repositories {@link ConnectionFactory}.
     */
    private ConnectionFactory connectionFactory;

    /**
     * {@inheritDoc}
     */
    public void setConnectionFactory(ConnectionFactory connnectionFactory) {
        this.connectionFactory = connnectionFactory;
    }

    //----------------------------------------------------< setters & getters >
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDriver() {
        return driver;
    }

    public void setDriver(String driver) {
        this.driver = driver;
    }

    /**
     * Returns a JDBC connection acquired using the JDBC {@link DriverManager}.
     * @throws SQLException
     *
     * @throws RepositoryException if the driver could not be loaded
     * @throws SQLException if the connection could not be established
     * @see DatabasePersistenceManager#getConnection()
     */
    protected Connection getConnection() throws RepositoryException, SQLException {
        return connectionFactory.getDataSource(driver, url, user, password).getConnection();
    }

}
