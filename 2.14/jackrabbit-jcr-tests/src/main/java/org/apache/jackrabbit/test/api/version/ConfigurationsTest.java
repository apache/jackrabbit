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
package org.apache.jackrabbit.test.api.version;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.version.Version;
import javax.jcr.version.VersionManager;


/**
 * <code>ConfigurationsTest</code> covers methods related to the Configurations
 * feature in Versioning.
 * @since JCR 2.0
 */
public class ConfigurationsTest extends AbstractVersionTest {

    private VersionManager vm;

    private static String PREFIX = "/jcr:system/jcr:configurations/";

    private String ntConfiguration;

    private Node versionableNode2;

    protected void setUp() throws Exception {
        super.setUp();
        checkSupportedOption(Repository.OPTION_BASELINES_SUPPORTED);
        vm = superuser.getWorkspace().getVersionManager();

        versionableNode2 = createVersionableNode(testRootNode, nodeName4, versionableNodeType);

        ntConfiguration = superuser.getNamespacePrefix(NS_NT_URI) + ":configuration";

    }

    protected void tearDown() throws Exception {
        // remove configuration, otherwise
        // subsequent tests will fail when cleaning the test root
        if (versionableNode != null) {
            removeConfiguration(versionableNode);
        }
        removeConfiguration(versionableNode2);
        versionableNode2.remove();
        // save is called in super.tearDown()
        super.tearDown();
    }

    private void removeConfiguration(Node node) throws RepositoryException {
        if (node.hasProperty("jcr:configuration")) {
            node.getProperty("jcr:configuration").getNode().remove();
        }
    }

    public void testCreateConfiguration() throws Exception {
        Node config = vm.createConfiguration(versionableNode.getPath());
        assertNotNull(config);
        NodeType nt = config.getPrimaryNodeType();
        assertTrue("created node must be subtype of nt:configuration", nt.isNodeType(ntConfiguration));

        // check if the configuration points to the versionable
        assertTrue("jcr:root property of the configuration must reference the versionable node",
                config.getProperty("jcr:root").getNode().isSame(versionableNode));

        // check if the versionable points to the configuration
        assertTrue("jcr:configuration property of the versionable node must reference the configuration",
                versionableNode.getProperty("jcr:configuration").getNode().isSame(config));

    }

    public void testCreateConfigurationNotVersionableFails() throws Exception {
        try {
            vm.createConfiguration(nonVersionableNode.getPath());
            fail("Create configuration must fail for non-versionable node");
        } catch (UnsupportedRepositoryOperationException e) {
            // ignore
        }
    }

    public void testCreateConfigurationTwiceFails() throws Exception {
        vm.createConfiguration(versionableNode.getPath());
        try {
            vm.createConfiguration(versionableNode.getPath());
            fail("Create configuration must fail if versionable is already a configuration");
        } catch (UnsupportedRepositoryOperationException e) {
            // ignore
        }
    }

    public void testConfigurationsPath() throws Exception {
        Node config = vm.createConfiguration(versionableNode.getPath());
        assertNotNull(config);
        NodeType nt = config.getPrimaryNodeType();
        assertTrue("created node must be subtype of nt:configuration", nt.isNodeType(ntConfiguration));

        assertTrue("path for configuration must be below " + PREFIX + ", but was " +
                config.getPath(), config.getPath().startsWith(PREFIX));
    }

    public void testCheckinConfigFailsWithUnversionedChild() throws Exception {
        Node config = vm.createConfiguration(versionableNode.getPath());
        try {
            vm.checkin(config.getPath());
            fail("Checkin configuration must fail one of the recorded versions is not versioned.");
        } catch (UnsupportedRepositoryOperationException e) {
            // ignore
        }
    }

    public void testCheckinConfig() throws Exception {
        vm.checkin(versionableNode.getPath());
        Node config = vm.createConfiguration(versionableNode.getPath());
        vm.checkin(config.getPath());
    }

    public void testCreateConfigWithBaseline() throws Exception {
        // create configuration
        String path = versionableNode.getPath();
        Version baseVersion = vm.checkin(path);
        Node config = vm.createConfiguration(path);
        // record baseline
        Version baseline = vm.checkin(config.getPath());

        // remove workspace nodes
        removeConfiguration(versionableNode);
        versionableNode.remove();
        versionableNode = null;
        testRootNode.getSession().save();

        // and try to restore it
        vm.restore(path, baseline, true);

        versionableNode = testRootNode.getSession().getNode(path);
        Version baseVersion2 = vm.getBaseVersion(versionableNode.getPath());
        assertTrue("restored node must have former base version.", baseVersion.isSame(baseVersion2));

        config = versionableNode.getProperty("jcr:configuration").getNode();

        // base version of config must be baseline
        assertTrue("Baseversion of restored config must be given baseline.",
                vm.getBaseVersion(config.getPath()).isSame(baseline));

    }

    public void testCreateConfigWithNonExistentParentFails() throws Exception {
        // create configuration
        String path = versionableNode.getPath();
        vm.checkin(path);
        Node config = vm.createConfiguration(path);
        // record baseline
        Version baseline = vm.checkin(config.getPath());

        // remove workspace nodes
        removeConfiguration(versionableNode);
        versionableNode.remove();
        versionableNode = null;
        testRootNode.getSession().save();

        try {
            vm.restore("/non/existent/parent", baseline, true);
            fail("Create configuration must fail if parent does not exist.");
        } catch (RepositoryException e) {
            // ignore
        }
    }

    public void testCreateConfigWithExistentConfigFromBaselineFails() throws Exception {
        // create configuration
        String path = versionableNode.getPath();
        vm.checkin(path);
        Node config = vm.createConfiguration(path);
        // record baseline
        Version baseline = vm.checkin(config.getPath());

        try {
            vm.restore(testRoot + "/nonExisting", baseline, true);
            fail("Create configuration must fail if config recorded in baseline already exists.");
        } catch (RepositoryException e) {
            // ignore
        }
    }

    public void testRestoreBaseline() throws Exception {
        // create configuration
        String path = versionableNode.getPath();
        Version bv1 = vm.checkpoint(path);
        Node config = vm.createConfiguration(path);
        // record baseline 1 (should contain bv1)
        Version bl1 = vm.checkpoint(config.getPath());
        // create bv2
        Version bv2 = vm.checkpoint(path);
        // record baseline 2 (should contain bv2)
        Version bl2 = vm.checkpoint(config.getPath());

        // restore bl1
        vm.restore(bl1, true);
        Version bv = vm.getBaseVersion(path);
        assertTrue("restored node must have former base version V1.0.", bv.isSame(bv1));

        // restore bl2
        vm.restore(bl2, true);
        bv = vm.getBaseVersion(path);
        assertTrue("restored node must have former base version V1.1.", bv.isSame(bv2));
    }

    public void testRestoreConfig() throws Exception {
        // create configuration
        String path = versionableNode.getPath();
        Version bv1 = vm.checkpoint(path);
        Node config = vm.createConfiguration(path);
        String configPath = config.getPath();

        // record baseline 1 (should contain bv1)
        Version bl1 = vm.checkpoint(config.getPath());
        // create bv2
        Version bv2 = vm.checkpoint(path);
        // record baseline 2 (should contain bv2)
        Version bl2 = vm.checkpoint(config.getPath());

        // restore bl1
        vm.restore(configPath, bl1.getName(), true);
        Version bv = vm.getBaseVersion(path);
        assertTrue("restored node must have former base version V1.0.", bv.isSame(bv1));

        // restore bl2
        vm.restore(configPath, bl2.getName(), true);
        bv = vm.getBaseVersion(path);
        assertTrue("restored node must have former base version V1.1.", bv.isSame(bv2));
    }
}
