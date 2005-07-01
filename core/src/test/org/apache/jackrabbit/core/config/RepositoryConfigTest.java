/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
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
package org.apache.jackrabbit.core.config;

import java.io.File;
import java.io.InputStream;
import java.util.Properties;

import org.xml.sax.InputSource;

import junit.framework.TestCase;

/**
 * Test cases for repository configuration handling.
 */
public class RepositoryConfigTest extends TestCase {

    /**
     * Test that a standard repository configuration file is
     * correctly parsed.
     *
     * @throws Exception on errors
     */
    public void testRepositoryXml() throws Exception {
        InputStream xml = getClass().getClassLoader().getResourceAsStream(
                "org/apache/jackrabbit/core/config/repository.xml");
        RepositoryConfig config =
            RepositoryConfig.create(new InputSource(xml), "target");

        assertEquals("target", config.getHomeDir());
        assertEquals("default", config.getDefaultWorkspaceName());
        assertEquals("target/workspaces", config.getWorkspacesConfigRootDir());
        assertEquals("Jackrabbit", config.getAppName());

        AccessManagerConfig amc = config.getAccessManagerConfig();
        assertEquals(
                "org.apache.jackrabbit.core.security.SimpleAccessManager",
                amc.getClassName());
        assertTrue(amc.getParameters().isEmpty());

        VersioningConfig vc = config.getVersioningConfig();
        assertEquals(new File("target/version"), vc.getHomeDir());
        assertEquals(
                "org.apache.jackrabbit.core.state.obj.ObjectPersistenceManager",
                vc.getPersistenceManagerConfig().getClassName());
        assertTrue(vc.getPersistenceManagerConfig().getParameters().isEmpty());
    }

}
