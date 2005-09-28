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

import org.xml.sax.InputSource;

import junit.framework.TestCase;

/**
 * Test cases for repository configuration handling.
 */
public class RepositoryConfigTest extends TestCase {

    private static final String REPOSITORY_XML = "org/apache/jackrabbit/core/config/repository.xml";

    private static final String REPOSITORY_HOME = "target/test-repository";

    private RepositoryConfig config;

    private static void deleteAll(File file) {
        if (file.exists()) {
            if (file.isDirectory()) {
                File[] children = file.listFiles();
                for (int i = 0; i < children.length; i++) {
                    deleteAll(children[i]);
                }
            }
            file.delete();
        }
    }

    /**
     * Sets up the test case by reading the test repository configuration file.
     */
    protected void setUp() throws Exception {
        ClassLoader loader = getClass().getClassLoader();
        InputStream xml = loader.getResourceAsStream(REPOSITORY_XML);
        config = RepositoryConfig.create(new InputSource(xml), REPOSITORY_HOME);
    }

    protected void tearDown() {
        deleteAll(new File(REPOSITORY_HOME));
    }

    /**
     * Test that the repository configuration file is correctly parsed.
     */
    public void testRepositoryConfig() throws Exception {
        assertEquals(REPOSITORY_HOME, config.getHomeDir());
        assertEquals("default", config.getDefaultWorkspaceName());
        assertEquals(
                new File(REPOSITORY_HOME, "workspaces").getPath(),
                config.getWorkspacesConfigRootDir());
        assertEquals("Jackrabbit", config.getAppName());

        AccessManagerConfig amc = config.getAccessManagerConfig();
        assertEquals(
                "org.apache.jackrabbit.core.security.SimpleAccessManager",
                amc.getClassName());
        assertTrue(amc.getParameters().isEmpty());

        VersioningConfig vc = config.getVersioningConfig();
        assertEquals(new File(REPOSITORY_HOME, "version"), vc.getHomeDir());
        assertEquals(
                "org.apache.jackrabbit.core.state.obj.ObjectPersistenceManager",
                vc.getPersistenceManagerConfig().getClassName());
        assertTrue(vc.getPersistenceManagerConfig().getParameters().isEmpty());
    }

    public void testInit() throws Exception {
        File workspaces_dir = new File(REPOSITORY_HOME, "workspaces");
        File workspace_dir = new File(workspaces_dir, "default");
        File workspace_xml = new File(workspace_dir, "workspace.xml");
        assertTrue(workspace_xml.exists());
    }

    public void testCreateWorkspaceConfig() throws Exception {
        config.createWorkspaceConfig("test-workspace");
        File workspaces_dir = new File(REPOSITORY_HOME, "workspaces");
        File workspace_dir = new File(workspaces_dir, "test-workspace");
        File workspace_xml = new File(workspace_dir, "workspace.xml");
        assertTrue(workspace_xml.exists());
    }

    public void testCreateDuplicateWorkspaceConfig() throws Exception {
        try {
            config.createWorkspaceConfig("default");
            fail("No exception thrown when creating a duplicate workspace");
        } catch (ConfigurationException e) {
            // test passed
        }
    }

}
