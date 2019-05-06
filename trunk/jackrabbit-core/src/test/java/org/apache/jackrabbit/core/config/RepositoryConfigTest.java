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

 import junit.framework.TestCase;
import org.xml.sax.InputSource;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.input.ClosedInputStream;
import org.apache.jackrabbit.core.cluster.ClusterNode;
import org.apache.jackrabbit.core.security.authorization.WorkspaceAccessManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Test cases for repository configuration handling.
 */
public class RepositoryConfigTest extends TestCase {

    private static final File DIR =
        new File("target", "RepositoryConfigTest");

    private static final File XML =
        new File(DIR, "repository.xml");

    private RepositoryConfig config;

    /**
     * Sets up the test case by creating the repository home directory
     * and copying the repository configuration file in place.
     */
    protected void setUp() throws Exception {
        config = RepositoryConfig.install(DIR);
    }

    protected void tearDown() {
        FileUtils.deleteQuietly(DIR);
    }

    public void testCreateWithRepositoryDirectory() {
        try {
            RepositoryConfig.create(DIR);
        } catch (ConfigurationException e) {
            fail("Valid repository directory");
        }

        try {
            RepositoryConfig.create(new File(DIR, "invalid-repo-dir"));
            fail("Invalid repository directory");
        } catch (ConfigurationException e) {
        }
    }

    public void testCreateWithRepositoryConfigAndDirectory() {
        try {
            RepositoryConfig.create(XML, DIR);
        } catch (ConfigurationException e) {
            fail("Valid repository configuration and directory");
        }

        try {
            RepositoryConfig.create(XML, new File(DIR, "invalid-repo-dir"));
            fail("Invalid repository directory");
        } catch (ConfigurationException e) {
        }

        try {
            RepositoryConfig.create(new File(DIR, "invalid.xml"), DIR);
            fail("Invalid repository configuration");
        } catch (ConfigurationException e) {
        }
    }

    /**
     * Tests that a file name can be used for the configuration.
     */
    public void testRepositoryConfigCreateWithFileName() {
        try {
            RepositoryConfig.create(XML.getPath(), DIR.getPath());
        } catch (ConfigurationException e) {
            fail("Valid configuration file name");
        }

        try {
            RepositoryConfig.create(
                    new File(DIR, "invalid-config-file.xml").getPath(),
                    DIR.getPath());
            fail("Invalid configuration file name");
        } catch (ConfigurationException e) {
        }
    }

    /**
     * Tests that a URI can be used for the configuration.
     */
    public void testRepositoryConfigCreateWithURI() throws URISyntaxException {
        try {
            RepositoryConfig.create(XML.toURI(), DIR.getPath());
        } catch (ConfigurationException e) {
            fail("Valid configuration URI");
        }

        try {
            RepositoryConfig.create(
                    new File(DIR, "invalid-config-file.xml").toURI(),
                    DIR.getPath());
            fail("Invalid configuration URI");
        } catch (ConfigurationException e) {
        }

        try {
            RepositoryConfig.create(
                    new URI("invalid://config/uri"),
                    DIR.getPath());
            fail("Invalid configuration URI");
        } catch (ConfigurationException e) {
        }
    }

    /**
     * Tests that an input stream can be used for the configuration.
     */
    public void testRepositoryConfigCreateWithInputStream() throws IOException {
        InputStream input = new FileInputStream(XML);
        try {
            RepositoryConfig.create(input, DIR.getPath());
        } catch (ConfigurationException e) {
            fail("Valid configuration input stream");
        } finally {
            input.close();
        }

        try {
            RepositoryConfig.create(
                    new InputStream() {
                        public int read() throws IOException {
                            throw new IOException("invalid input stream");
                        }
                    },
                    DIR.getPath());
            fail("Invalid configuration input stream");
        } catch (ConfigurationException e) {
        }

        try {
            RepositoryConfig.create(
                    new ClosedInputStream(),
                    DIR.getPath());
            fail("Invalid configuration input stream");
        } catch (ConfigurationException e) {
        }
    }

    /**
     * Tests that an InputSource can be used for the configuration.
     */
    public void testRepositoryConfigCreateWithInputSource() throws IOException {
        try {
            InputSource source = new InputSource(XML.toURI().toString());
            RepositoryConfig.create(source, DIR.getPath());
        } catch (ConfigurationException e) {
            fail("Valid configuration input source with file URI");
        }

        InputStream stream = new FileInputStream(XML);
        try {
            InputSource source = new InputSource(stream);
            RepositoryConfig.create(source, DIR.getPath());
        } catch (ConfigurationException e) {
            fail("Valid configuration input source with input stream");
        } finally {
            stream.close();
        }
    }

    /**
     * Test that the repository configuration file is correctly parsed.
     */
    public void testRepositoryConfig() throws Exception {
        assertRepositoryConfiguration(config);
    }

    public void testInit() throws Exception {
        File workspaces_dir = new File(DIR, "workspaces");
        File workspace_dir = new File(workspaces_dir, "default");
        File workspace_xml = new File(workspace_dir, "workspace.xml");
        assertTrue("Default workspace is created", workspace_xml.exists());
    }

    public void testCreateWorkspaceConfig() throws Exception {
        config.createWorkspaceConfig("test-workspace", (StringBuffer) null);
        File workspaces_dir = new File(DIR, "workspaces");
        File workspace_dir = new File(workspaces_dir, "test-workspace");
        File workspace_xml = new File(workspace_dir, "workspace.xml");
        assertTrue(workspace_xml.exists());
    }

    public void testCreateDuplicateWorkspaceConfig() throws Exception {
        try {
            config.createWorkspaceConfig("default", (StringBuffer) null);
            fail("No exception thrown when creating a duplicate workspace");
        } catch (ConfigurationException e) {
            // test passed
        }
    }

    public void testRepositoryConfigWithSystemVariables() throws Exception {
        final String id = "testvalue";
        final long syncDelay = 11;

        System.setProperty(ClusterNode.SYSTEM_PROPERTY_NODE_ID, id);
        System.setProperty("cluster.syncDelay", Long.toString(syncDelay));
        try {
            InputStream in = getClass().getResourceAsStream(
                    "/org/apache/jackrabbit/core/cluster/repository.xml");
            RepositoryConfig config = RepositoryConfig.create(in, DIR.getPath());

            ClusterConfig clusterConfig = config.getClusterConfig();
            assertEquals(id, clusterConfig.getId());
            assertEquals(syncDelay, clusterConfig.getSyncDelay());
        } finally {
            System.clearProperty(ClusterNode.SYSTEM_PROPERTY_NODE_ID);
            System.clearProperty("cluster.syncDelay");
        }
    }

    public void testAutomaticClusterNodeIdCreation() throws Exception {
        final long syncDelay = 12;

        assertNull(
                "This test requires the system property " + ClusterNode.SYSTEM_PROPERTY_NODE_ID + " not to be set; found value: "
                        + System.getProperty(ClusterNode.SYSTEM_PROPERTY_NODE_ID) + " (leftover from broken unit test?)",
                System.getProperty(ClusterNode.SYSTEM_PROPERTY_NODE_ID));
        System.setProperty("cluster.syncDelay", Long.toString(syncDelay));
        try {
            File file = new File(DIR, "cluster_node.id");
            assertFalse(file.exists());

            // Check that a new cluster node id is automatically persisted
            InputStream in = getClass().getResourceAsStream(
                    "/org/apache/jackrabbit/core/cluster/repository.xml");
            RepositoryConfig config = RepositoryConfig.create(in, DIR.getPath());

            assertTrue(file.exists());
            String id = FileUtils.readFileToString(file);

            ClusterConfig clusterConfig = config.getClusterConfig();
            assertEquals(id, clusterConfig.getId());
            assertEquals(syncDelay, clusterConfig.getSyncDelay());

            // Check that the persisted cluster node id is used when it exists
            in = getClass().getResourceAsStream(
                    "/org/apache/jackrabbit/core/cluster/repository.xml");
            config = RepositoryConfig.create(in, DIR.getPath());

            assertTrue(file.exists());
            assertEquals(id, FileUtils.readFileToString(file));

            clusterConfig = config.getClusterConfig();
            assertEquals(id, clusterConfig.getId());
            assertEquals(syncDelay, clusterConfig.getSyncDelay());
        } finally {
            System.clearProperty("cluster.syncDelay");
        }
    }

    /**
     * Test that a RepositoryConfig can be copied into a new instance.
     *
     * @throws Exception if an unexpected error occurs during the test
     */
    public void testCopyConfig() throws Exception
    {
        RepositoryConfig copyConfig = RepositoryConfig.create(config);

        assertNotNull("Configuration not created properly", copyConfig);
        assertRepositoryConfiguration(copyConfig);
    }

    private void assertRepositoryConfiguration(RepositoryConfig config)
            throws ConfigurationException {
        assertEquals(DIR.getPath(), config.getHomeDir());
        assertEquals("default", config.getDefaultWorkspaceName());
        assertEquals(
                new File(DIR, "workspaces").getPath(),
                new File(config.getWorkspacesConfigRootDir()).getPath());
        assertEquals("Jackrabbit", config.getSecurityConfig().getAppName());

        // SecurityManagerConfig
        SecurityManagerConfig smc =
            config.getSecurityConfig().getSecurityManagerConfig();
        assertEquals(
                "org.apache.jackrabbit.core.DefaultSecurityManager",
                smc.getClassName());
        assertTrue(smc.getParameters().isEmpty());
        assertNotNull(smc.getWorkspaceName());

        BeanConfig bc = smc.getWorkspaceAccessConfig();
        if (bc != null) {
            WorkspaceAccessManager wac =
                smc.getWorkspaceAccessConfig().newInstance(WorkspaceAccessManager.class);
            assertEquals("org.apache.jackrabbit.core.security.simple.SimpleWorkspaceAccessManager", wac.getClass().getName());
        }

        // AccessManagerConfig
        AccessManagerConfig amc =
            config.getSecurityConfig().getAccessManagerConfig();
        assertEquals(
                "org.apache.jackrabbit.core.security.DefaultAccessManager",
                amc.getClassName());
        assertTrue(amc.getParameters().isEmpty());

        VersioningConfig vc = config.getVersioningConfig();
        assertEquals(new File(DIR, "version"), vc.getHomeDir());
        assertEquals(
                "org.apache.jackrabbit.core.persistence.pool.DerbyPersistenceManager",
                vc.getPersistenceManagerConfig().getClassName());
    }

}
