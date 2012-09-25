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
package org.apache.jackrabbit.core.util.db;

import java.sql.Connection;
import java.util.Properties;

import javax.jcr.RepositoryException;
import javax.sql.DataSource;

import junit.framework.TestCase;

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.derby.iapi.jdbc.EngineConnection;
import org.apache.jackrabbit.core.config.ConfigurationException;
import org.apache.jackrabbit.core.config.DataSourceConfig;

/**
 * 
 */
public class ConnectionFactoryTest extends TestCase {

    private ConnectionFactory connectionFactory;

    private static final String DRIVER = "org.apache.derby.jdbc.EmbeddedDriver";

    private static final String DERBY_URL = "jdbc:derby:target/connection-factory-test/db;create=true";

    private static final String MYSQL_URL = "jdbc:mysql://localhost:3306/dbName?autoReconnect=true";

    private static final String MSSQL_URL_1 = "jdbc:jtds:sqlserver://localhost:2433/dbName";

    private static final String MSSQL_URL_2 = "jdbc:sqlserver://localhost:1433;databaseName=dbName";

    private static final String ORACLE_URL = "jdbc:oracle:thin:@localhost:1521:xe";

    private static final String H2_URL = "jdbc:h2:tcp://localhost/dbName";

     @Override
     public void setUp() {
         System.setProperty("derby.stream.error.file", "target/derby-connectionfactorytest.log");
         connectionFactory = new ConnectionFactory();
     }

    public void testGetDataSource_defaults_Derby() throws Exception {
        DataSource ds1 = connectionFactory.getDataSource(DRIVER, DERBY_URL, "user", "password");
        assertTrue(ds1 instanceof BasicDataSource);
        BasicDataSource ds = (BasicDataSource) ds1;
        assertPoolDefaults(ds, "values(1)", -1);
    }

    public void testGuessValidationQuery_MYSQL() throws Exception {
        DataSource ds1 = connectionFactory.getDataSource(DRIVER, MYSQL_URL, "user", "password");
        assertEquals("select 1", ((BasicDataSource) ds1).getValidationQuery());
    }

    public void testGuessValidationQuery_MSSQL() throws Exception {
        DataSource ds1 = connectionFactory.getDataSource(DRIVER, MSSQL_URL_1, "user", "password");
        assertEquals("select 1", ((BasicDataSource) ds1).getValidationQuery());
        DataSource ds2 = connectionFactory.getDataSource(DRIVER, MSSQL_URL_2, "user", "password");
        assertEquals("select 1", ((BasicDataSource) ds2).getValidationQuery());
    }

    public void testGuessValidationQuery_ORACLE() throws Exception {
        DataSource ds1 = connectionFactory.getDataSource(DRIVER, ORACLE_URL, "user", "password");
        assertEquals("select 'validationQuery' from dual", ((BasicDataSource) ds1).getValidationQuery());
    }

    public void testGuessValidationQuery_H2() throws Exception {
        DataSource ds1 = connectionFactory.getDataSource(DRIVER, H2_URL, "user", "password");
        assertEquals("select 1", ((BasicDataSource) ds1).getValidationQuery());
    }

    public void testRegisterDataSources_defaultValues() throws Exception {
        BasicDataSource ds = registerAndGet(DERBY_URL, "overwrite", -1);
        assertPoolDefaults(ds, "overwrite", -1);
    }

    public void testRegisterDataSources_noValidationQuery() throws Exception {
        BasicDataSource ds = registerAndGet(MYSQL_URL, "", -1);
        assertEquals("select 1", ds.getValidationQuery());
    }

    public void testGetDatabaseType() throws Exception {
        String name = register(MYSQL_URL, "", -1);
        assertEquals("dbType", connectionFactory.getDataBaseType(name));
    }

    public void testGetDataSource_identity() throws Exception {
        DataSource ds1 = connectionFactory.getDataSource(DRIVER, DERBY_URL, "user", "password");
        DataSource ds2 = connectionFactory.getDataSource(DRIVER, DERBY_URL, "user", "password");
        assertSame(ds1, ds2);
    }

    public void testGetDataSource_identity_differentPasswords() throws Exception {
        DataSource ds1 = connectionFactory.getDataSource(DRIVER, DERBY_URL, "user", "password");
        DataSource ds2 = connectionFactory.getDataSource(DRIVER, DERBY_URL, "user", "password2");
        assertSame(ds1, ds2);
    }

    public void testGetDataSource_noIdentity() throws Exception {
        DataSource ds1 = connectionFactory.getDataSource(DRIVER, DERBY_URL, "user", "password");
        DataSource ds2 = connectionFactory.getDataSource(DRIVER, DERBY_URL, "user2", "password");
        assertNotSame(ds1, ds2);
    }

    public void testUnwrap() throws Exception {
        DataSource ds = connectionFactory.getDataSource(DRIVER, DERBY_URL, "user", "password");
        Connection wrappedCon = ds.getConnection();
        assertNotNull(wrappedCon);
        Connection con = ConnectionFactory.unwrap(wrappedCon);
        assertTrue(con instanceof EngineConnection);
    }

    public void testClose() throws Exception {
        connectionFactory.close();
        try {
            connectionFactory.getDataBaseType("logicalName");
            fail("could retrieve after close");
        } catch (IllegalStateException expected) {
        }
        try {
            connectionFactory.getDataSource("logicalName");
            fail("could retrieve after close");
        } catch (IllegalStateException expected) {
        }
        try {
            connectionFactory.getDataSource(DRIVER, DERBY_URL, "user", "password");
            fail("could retrieve after close");
        } catch (IllegalStateException expected) {
        }
    }

    private void assertPoolDefaults(BasicDataSource ds, String validationQuery, int maxCons) {
        assertEquals(maxCons, ds.getMaxActive());
        assertEquals(validationQuery, ds.getValidationQuery());
        assertTrue(ds.getDefaultAutoCommit());
        assertFalse(ds.getTestOnBorrow());
        assertTrue(ds.getTestWhileIdle());
        assertEquals(600000, ds.getTimeBetweenEvictionRunsMillis());
        assertTrue(ds.isPoolPreparedStatements());
        assertEquals(-1, ds.getMaxOpenPreparedStatements());
    }

    private BasicDataSource registerAndGet(String url, String validationQuery, int maxCons) throws Exception {
        final String name = register(url, validationQuery, maxCons);
        DataSource ds = connectionFactory.getDataSource(name);
        assertTrue(ds instanceof BasicDataSource);
        return (BasicDataSource) ds;
    }

    private String register(String url, String validationQuery, int maxCons) throws ConfigurationException,
            RepositoryException {
        final String name = "some random name to not interfere with integration tests...";
        DataSourceConfig dsc = new DataSourceConfig();
        Properties props = new Properties();
        props.put(DataSourceConfig.DRIVER, DRIVER);
        props.put(DataSourceConfig.URL, url);
        props.put(DataSourceConfig.DB_TYPE, "dbType");
        props.put(DataSourceConfig.MAX_POOL_SIZE, Integer.toString(maxCons));
        props.put(DataSourceConfig.VALIDATION_QUERY, validationQuery);
        dsc.addDataSourceDefinition(name, props);
        connectionFactory.registerDataSources(dsc);
        return name;
    }
}
