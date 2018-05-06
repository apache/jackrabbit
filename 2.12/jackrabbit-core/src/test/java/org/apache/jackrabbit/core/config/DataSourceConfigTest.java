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
package org.apache.jackrabbit.core.config;

import java.util.Properties;

import org.apache.jackrabbit.core.config.DataSourceConfig.DataSourceDefinition;

import junit.framework.TestCase;

public class DataSourceConfigTest extends TestCase {

    private DataSourceConfig cfg;

    private Properties minimalProps;

    private Properties minimalProps2;

    @Override
    public void setUp() {
        cfg = new DataSourceConfig();
        minimalProps = new Properties();
        minimalProps.put(DataSourceConfig.DRIVER, "org.apache.derby.jdbc.EmbeddedDriver");
        minimalProps.put(DataSourceConfig.URL, "url");
        minimalProps.put(DataSourceConfig.DB_TYPE, "dbType");
        minimalProps2 = new Properties();
        minimalProps2.put(DataSourceConfig.DRIVER, "org.apache.derby.jdbc.EmbeddedDriver");
        minimalProps2.put(DataSourceConfig.URL, "url2");
        minimalProps2.put(DataSourceConfig.DB_TYPE, "dbType2");
    }

    public void testEmptyConfig() {
        assertEquals(0, cfg.getDefinitions().size());
    }

    public void testMinimalRegularConfig() throws ConfigurationException {
        cfg.addDataSourceDefinition("ds", minimalProps);
        DataSourceDefinition def = cfg.getDefinitions().get(0);
        assertEquals("ds", def.getLogicalName());
        assertEquals("org.apache.derby.jdbc.EmbeddedDriver", def.getDriver());
        assertEquals("url", def.getUrl());
        assertEquals("dbType", def.getDbType());
        // check default values:
        assertNull(def.getUser());
        assertNull(def.getPassword());
        assertNull(def.getValidationQuery());
        assertEquals(-1, def.getMaxPoolSize()); // unlimited
    }

    public void testMultipleDefs() throws ConfigurationException {
        cfg.addDataSourceDefinition("ds1", minimalProps);
        cfg.addDataSourceDefinition("ds2", minimalProps2);
        assertEquals(2, cfg.getDefinitions().size());
    }

    public void testTooMinimalConfig() {
        try {
            minimalProps.remove(DataSourceConfig.URL);
            cfg.addDataSourceDefinition("ds", minimalProps);
            fail();
        } catch (ConfigurationException e) {
            // expected
        }
    }

    public void testInvalidProperty() {
        try {
            minimalProps.put("unknown property", "value");
            cfg.addDataSourceDefinition("ds", minimalProps);
            fail();
        } catch (ConfigurationException e) {
            // expected
        }
    }

    public void testUnparseableProperty() {
        try {
            minimalProps.put(DataSourceConfig.MAX_POOL_SIZE, "no int");
            cfg.addDataSourceDefinition("ds", minimalProps);
            fail();
        } catch (ConfigurationException e) {
            // expected
        }
    }

    public void testDuplicateLogicalName() throws ConfigurationException {
        cfg.addDataSourceDefinition("ds", minimalProps);
        try {
            cfg.addDataSourceDefinition("ds", minimalProps2);
            fail();
        } catch (ConfigurationException e) {
            // expected
        }
    }

    public void testEmptyLogicalName() throws ConfigurationException {
        try {
            cfg.addDataSourceDefinition("  ", minimalProps);
            fail();
        } catch (ConfigurationException e) {
            // expected
        }
    }

    public void testNullLogicalName() throws ConfigurationException {
        try {
            cfg.addDataSourceDefinition(null, minimalProps);
            fail();
        } catch (ConfigurationException e) {
            // expected
        }
    }

    /**
     * It only makes sense to configure driver, url, username, password and dbType for
     * a DataSource which is to be obtained from JNDI.
     * 
     * @throws ConfigurationException
     */
    public void testConfiguredJNDIConfig() throws ConfigurationException {
        minimalProps.put(DataSourceConfig.DRIVER, "javax.naming.InitialContext");
        minimalProps.put(DataSourceConfig.MAX_POOL_SIZE, "10");
        try {
            cfg.addDataSourceDefinition("ds", minimalProps);
            fail();
        } catch (ConfigurationException e) {
            // expected
        }
    }
}
