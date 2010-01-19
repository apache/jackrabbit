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
package org.apache.jackrabbit.core.persistence.bundle;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import javax.jcr.RepositoryException;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.sql.DataSource;

/**
 * A factory for new database connections.
 * Supported are regular JDBC drivers, as well as
 * JNDI resources.
 */
public class ConnectionFactory {

    /**
     * Utility classes should not have a public or default constructor.
     */
    private ConnectionFactory() {
    }

    /**
     * Open a connection using the specified properties.
     * The connection can be created using a JNDI Data Source as well. To do that,
     * the driver class name must reference a javax.naming.Context class
     * (for example javax.naming.InitialContext), and the URL must be the JNDI URL
     * (for example java:comp/env/jdbc/Test).
     *
     * @param driver the JDBC driver or the Context class
     * @param url the database URL
     * @param user the user name
     * @param password the password
     * @return the connection
     * @throws RepositoryException if the driver could not be loaded
     * @throws SQLException if the connection could not be established
     */
    public static Connection getConnection(String driver, String url,
            String user, String password) throws RepositoryException,
            SQLException {
        if (driver != null && driver.length() > 0) {
            try {
                Class< ? > d = Class.forName(driver);
                if (javax.naming.Context.class.isAssignableFrom(d)) {
                    // JNDI context
                    Context context = (Context) d.newInstance();
                    DataSource ds = (DataSource) context.lookup(url);
                    if (user == null && password == null) {
                        return ds.getConnection();
                    } else {
                        return ds.getConnection(user, password);
                    }
                } else {
                    try {
                        // Workaround for Apache Derby:
                        // The JDBC specification recommends the Class.forName method without the .newInstance() method call,
                        // but it is required after a Derby 'shutdown'.
                        d.newInstance();
                    } catch (Throwable e) {
                        // Ignore exceptions
                        // There's no requirement that a JDBC driver class has a public default constructor
                    }
                }
            } catch (ClassNotFoundException e) {
                throw new RepositoryException("Could not load class " + driver, e);
            } catch (InstantiationException e) {
                throw new RepositoryException("Could not instantiate context " + driver, e);
            } catch (IllegalAccessException e) {
                throw new RepositoryException("Could not instantiate context " + driver, e);
            } catch (NamingException e) {
                throw new RepositoryException("Naming exception using " + driver + " url: " + url, e);
            }
        }
        return DriverManager.getConnection(url, user, password);
    }

}
