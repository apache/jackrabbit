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
import org.apache.jackrabbit.core.security.authorization.WorkspaceAccessManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Test cases for repository configuration handling.
 */
public class RepositoryConfigTest extends TestCase {

    private static final String REPOSITORY_XML = "target/repository_for_test.xml";
    private static final String REPOSITORY_HOME = "target/repository_for_test";

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
     * Sets up the test case by creating the repository home directory
     * and copying the repository configuration file in place.
     */
    protected void setUp() throws Exception {
        // Create the repository directory
        File home = new File(REPOSITORY_HOME);
        home.mkdirs();

        // Copy the repository configuration file in place
        ClassLoader loader = getClass().getClassLoader();
        InputStream input = loader.getResourceAsStream("org/apache/jackrabbit/core/repository.xml");
        try {
            OutputStream output = new FileOutputStream(REPOSITORY_XML);
            try {
                int n;
                byte[] buffer = new byte[1024];
                while ((n = input.read(buffer)) != -1) {
                    output.write(buffer, 0, n);
                }
            } finally {
                output.close();
            }
        } finally {
            input.close();
        }
    }

    protected void tearDown() {
        File home = new File(REPOSITORY_HOME);
        deleteAll(home);
        File config = new File(REPOSITORY_XML);
        config.delete();
    }

    /**
     * Tests that a file name can be used for the configuration.
     */
    public void testRepositoryConfigCreateWithFileName() {
        try {
            RepositoryConfig.create(REPOSITORY_XML, REPOSITORY_HOME);
        } catch (ConfigurationException e) {
            fail("Valid configuration file name");
        }
        try {
            RepositoryConfig.create("invalid-config-file", REPOSITORY_HOME);
            fail("Invalid configuration file name");
        } catch (ConfigurationException e) {
        }
    }

    /**
     * Tests that a URI can be used for the configuration.
     */
    public void testRepositoryConfigCreateWithURI() throws URISyntaxException {
        try {
            URI uri = new File(REPOSITORY_XML).toURI();
            RepositoryConfig.create(uri, REPOSITORY_HOME);
        } catch (ConfigurationException e) {
            fail("Valid configuration URI");
        }
        try {
            URI uri = new URI("invalid://config/uri");
            RepositoryConfig.create(uri, REPOSITORY_HOME);
            fail("Invalid configuration URI");
        } catch (ConfigurationException e) {
        }
    }

    /**
     * Tests that an input stream can be used for the configuration.
     */
    public void testRepositoryConfigCreateWithInputStream() throws IOException {
        InputStream input = new FileInputStream(REPOSITORY_XML);
        try {
            RepositoryConfig.create(input, REPOSITORY_HOME);
        } catch (ConfigurationException e) {
            fail("Valid configuration input stream");
        } finally {
            input.close();
        }
        input = new InputStream() {
            public int read() throws IOException {
                throw new IOException("invalid input stream");
            }
        };
        try {
            RepositoryConfig.create(input, REPOSITORY_HOME);
            fail("Invalid configuration input stream");
        } catch (ConfigurationException e) {
        } finally {
            input.close();
        }
    }

    /**
     * Tests that an InputSource can be used for the configuration.
     */
    public void testRepositoryConfigCreateWithInputSource() throws IOException {
        try {
            URI uri = new File(REPOSITORY_XML).toURI();
            InputSource source = new InputSource(uri.toString());
            RepositoryConfig.create(source, REPOSITORY_HOME);
        } catch (ConfigurationException e) {
            fail("Valid configuration input source with file URI");
        }
        InputStream stream = new FileInputStream(REPOSITORY_XML);
        try {
            InputSource source = new InputSource(stream);
            RepositoryConfig.create(source, REPOSITORY_HOME);
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
        RepositoryConfig config =
            RepositoryConfig.create(REPOSITORY_XML, REPOSITORY_HOME);
        assertEquals(REPOSITORY_HOME, config.getHomeDir());
        assertEquals("default", config.getDefaultWorkspaceName());
        assertEquals(
                new File(REPOSITORY_HOME, "workspaces").getPath(),
                new File(config.getWorkspacesConfigRootDir()).getPath());
        assertEquals("Jackrabbit", config.getAppName());
        assertEquals("Jackrabbit", config.getSecurityConfig().getAppName());

        // SecurityManagerConfig
        SecurityManagerConfig smc = config.getSecurityConfig().getSecurityManagerConfig();
        assertEquals(
                "org.apache.jackrabbit.core.security.simple.SimpleSecurityManager",
                smc.getClassName());
        assertTrue(smc.getParameters().isEmpty());
        assertNotNull(smc.getWorkspaceName());

        BeanConfig bc = smc.getWorkspaceAccessConfig();
        if (bc != null) {
            WorkspaceAccessManager wac = (WorkspaceAccessManager) smc.getWorkspaceAccessConfig().newInstance();
            assertEquals("org.apache.jackrabbit.core.security.simple.SimpleWorkspaceAccessManager", wac.getClass().getName());
        }

        // AccessManagerConfig
        AccessManagerConfig amc = config.getAccessManagerConfig();
        amc = config.getSecurityConfig().getAccessManagerConfig();
        assertEquals(
                "org.apache.jackrabbit.core.security.simple.SimpleAccessManager",
                amc.getClassName());
        assertTrue(amc.getParameters().isEmpty());

        VersioningConfig vc = config.getVersioningConfig();
        assertEquals(new File(REPOSITORY_HOME, "version"), vc.getHomeDir());
        assertEquals(
                "org.apache.jackrabbit.core.persistence.bundle.DerbyPersistenceManager",
                vc.getPersistenceManagerConfig().getClassName());
    }

    public void testInit() throws Exception {
        RepositoryConfig.create(REPOSITORY_XML, REPOSITORY_HOME);
        File workspaces_dir = new File(REPOSITORY_HOME, "workspaces");
        File workspace_dir = new File(workspaces_dir, "default");
        File workspace_xml = new File(workspace_dir, "workspace.xml");
        assertTrue("Default workspace is created", workspace_xml.exists());
    }

    public void testCreateWorkspaceConfig() throws Exception {
        RepositoryConfig config =
            RepositoryConfig.create(REPOSITORY_XML, REPOSITORY_HOME);
        config.createWorkspaceConfig("test-workspace");
        File workspaces_dir = new File(REPOSITORY_HOME, "workspaces");
        File workspace_dir = new File(workspaces_dir, "test-workspace");
        File workspace_xml = new File(workspace_dir, "workspace.xml");
        assertTrue(workspace_xml.exists());
    }

    public void testCreateDuplicateWorkspaceConfig() throws Exception {
        try {
            RepositoryConfig config =
                RepositoryConfig.create(REPOSITORY_XML, REPOSITORY_HOME);
            config.createWorkspaceConfig("default");
            fail("No exception thrown when creating a duplicate workspace");
        } catch (ConfigurationException e) {
            // test passed
        }
    }

}
