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
package org.apache.jackrabbit.core.journal;

import java.sql.Connection;
import java.sql.SQLException;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

/**
 * @deprecated
 * This class should not be used because it is not database vendor specific.
 * Each DatabaseJournal now supports getting the connection via JNDI
 * by setting the driver to javax.naming.InitialContext
 * and the URL to the JNDI name.
 * <p>
 * Database journal that uses JNDI to acquire the database connection.
 * The JNDI location of the {@link DataSource} to be used in given as
 * the <code>dataSourceLocation</code> configuration property.
 * <p>
 * <strong>WARNING:</strong> The acquired database connection is kept
 * for the entire lifetime of the journal instance. The configured data
 * source should be prepared for this.
 */
public class JNDIDatabaseJournal extends DatabaseJournal {

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

    //-----------------------------------------------------< DatabaseJournal >

    /**
     * Returns a JDBC connection from a {@link DataSource} acquired from JNDI
     * with the configured data source location.
     *
     * @return new database connection
     * @throws SQLException if a database access error occurs
     * @see DataSource#getConnection()
     */
    protected Connection getConnection() throws SQLException {
        try {
            InitialContext ic = new InitialContext();
            DataSource dataSource = (DataSource) ic.lookup(dataSourceLocation);
            return dataSource.getConnection();
        } catch (NamingException e) {
            SQLException exception = new SQLException(
                    "DataSource not found: " + dataSourceLocation);
            exception.initCause(e);
            throw exception;
        }
    }

    /**
     * Overridden to avoid the driver and url checks in DatabaseJournal.
     */
    protected void init() throws JournalException {
    }

}
