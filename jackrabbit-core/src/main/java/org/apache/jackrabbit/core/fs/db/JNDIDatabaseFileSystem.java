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

import javax.naming.InitialContext;
import javax.sql.DataSource;

/**
 * @deprecated
 * This class should not be used because it is not database vendor specific.
 * Each DatabaseFileSystem now supports getting the connection via JNDI
 * by setting the driver to javax.naming.InitialContext
 * and the URL to the JNDI name.
 * <p>
 * Database file system that uses JNDI to acquire the database connection.
 * The JNDI location of the {@link DataSource} to be used in given as
 * the <code>dataSourceLocation</code> configuration property. See the
 * {@link DbFileSystem} for more configuration details.
 * <p>
 * <strong>WARNING:</strong> The acquired database connection is kept
 * for the entire lifetime of the file system instance. The configured data
 * source should be prepared for this.
 */
public class JNDIDatabaseFileSystem extends DatabaseFileSystem {

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

    //--------------------------------------------------< DatabaseFileSystem >

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataSource getDataSource() throws Exception {
        InitialContext ic = new InitialContext();
        return (DataSource) ic.lookup(dataSourceLocation);
    }
}
