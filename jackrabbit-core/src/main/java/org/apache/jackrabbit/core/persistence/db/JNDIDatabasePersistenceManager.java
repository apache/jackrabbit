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

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Database persistence manager that uses JNDI to acquire the database
 * connection. The JNDI location of the {@link DataSource} to be used in
 * given as the <code>dataSourceLocation</code> configuration property.
 * See the {@link SimpleDbPersistenceManager} for more configuration
 * details.
 * <p>
 * <strong>WARNING:</strong> The acquired database connection is kept
 * for the entire lifetime of the persistence manager instance. The
 * configured data source should be prepared for this.
 *
 * @deprecated Please migrate to a bundle persistence manager
 *   (<a href="https://issues.apache.org/jira/browse/JCR-2802">JCR-2802</a>)
 */
@Deprecated
public class JNDIDatabasePersistenceManager extends DatabasePersistenceManager {

    /**
     * JNDI location of the data source used to acquire database connections.
     */
    private String dataSourceLocation;

    //----------------------------------------------------< setters & getters >

    /**
     * Returns the JNDI location of the data source.
     *
     * @return data source location
     */
    public String getDataSourceLocation() {
        return dataSourceLocation;
    }

    /**
     * Sets the JNDI location of the data source.
     *
     * @param dataSourceLocation data source location
     */
    public void setDataSourceLocation(String dataSourceLocation) {
        this.dataSourceLocation = dataSourceLocation;
    }

    //-------------------------------------------< DatabasePersistenceManager >

    /**
     * Returns a JDBC connection from a {@link DataSource} acquired from JNDI
     * with the configured data source location.
     *
     * @return new database connection
     * @throws NamingException if the given data source location does not exist
     * @throws SQLException if a database access error occurs
     * @see DatabasePersistenceManager#getConnection()
     */
    protected Connection getConnection() throws NamingException, SQLException {
        InitialContext ic = new InitialContext();
        DataSource dataSource = (DataSource) ic.lookup(dataSourceLocation);
        return dataSource.getConnection();
    }

}
