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

import java.io.InputStream;
import java.util.Properties;

import junit.framework.TestCase;

import org.xml.sax.InputSource;
import org.apache.jackrabbit.core.security.authorization.AccessControlProvider;

/**
 * Test cases for workspace configuration handling.
 */
public class WorkspaceConfigTest extends TestCase {

    private RepositoryConfigurationParser parser;

    protected void setUp() {
        Properties variables = new Properties();
        variables.setProperty("wsp.home", "target");
        parser = new RepositoryConfigurationParser(variables);
    }

    /**
     * Test that a standard workspace configuration file is
     * correctly parsed.
     *
     * @throws Exception on errors
     */
    public void testWorkspaceXml() throws Exception {
        InputStream xml = getClass().getClassLoader().getResourceAsStream(
                "org/apache/jackrabbit/core/config/workspace.xml");
        WorkspaceConfig config =
            parser.parseWorkspaceConfig(new InputSource(xml));

        assertEquals("target", config.getHomeDir());
        assertEquals("default", config.getName());

        PersistenceManagerConfig pmc = config.getPersistenceManagerConfig();
        assertEquals(
                "org.apache.jackrabbit.core.persistence.obj.ObjectPersistenceManager",
                pmc.getClassName());
        assertTrue(pmc.getParameters().isEmpty());

        SearchConfig sc = config.getSearchConfig();
        assertEquals(
                "org.apache.jackrabbit.core.query.lucene.SearchIndex",
                sc.getClassName());
        assertEquals(4, sc.getParameters().size());
        assertEquals("true", sc.getParameters().getProperty("useCompoundFile"));
        assertEquals("1000", sc.getParameters().getProperty("minMergeDocs"));
        assertEquals("10000", sc.getParameters().getProperty("maxMergeDocs"));
        assertEquals("10", sc.getParameters().getProperty("mergeFactor"));

        WorkspaceSecurityConfig ws = config.getSecurityConfig();
        if (ws != null) {
            BeanConfig ppfConfig =  ws.getAccessControlProviderConfig();
            if (ppfConfig != null) {
                AccessControlProvider prov = (AccessControlProvider) ppfConfig.newInstance();
            }
        }
    }
}
