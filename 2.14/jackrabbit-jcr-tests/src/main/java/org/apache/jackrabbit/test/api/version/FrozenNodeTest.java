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
import javax.jcr.RepositoryException;
import javax.jcr.version.Version;
import javax.jcr.version.VersionManager;

import org.apache.jackrabbit.test.NotExecutableException;

/**
 * <code>CheckinTest</code> covers tests related to {@link javax.jcr.Node#checkin()}.
 *
 */
public class FrozenNodeTest extends AbstractVersionTest {

    protected void setUp() throws Exception {
        super.setUp();

        VersionManager versionManager = versionableNode.getSession().getWorkspace().getVersionManager();
        String path = versionableNode.getPath();
        versionManager.checkout(path);
    }

    /**
     * @throws RepositoryException
     */
    public void testFrozenNodeUUUID() throws RepositoryException {
        VersionManager versionManager = versionableNode.getSession().getWorkspace().getVersionManager();
        String path = versionableNode.getPath();
        Version v = versionManager.checkin(path);
        Node n = v.getFrozenNode();
        String puuid = n.getProperty(jcrUUID).getValue().getString();
        String nuuid = n.getIdentifier();
        assertEquals("jcr:uuid needs to be equal to the getIdentifier() return value.", nuuid, puuid);
    }

    /**
     * @throws RepositoryException
     */
    public void testFrozenChildNodeUUUID() throws RepositoryException {
        versionableNode.addNode("child");
        versionableNode.getSession().save();
        VersionManager versionManager = versionableNode.getSession().getWorkspace().getVersionManager();
        String path = versionableNode.getPath();
        Version v = versionManager.checkin(path);
        Node n = v.getFrozenNode().getNode("child");
        String puuid = n.getProperty(jcrUUID).getValue().getString();
        String nuuid = n.getIdentifier();
        assertEquals("jcr:uuid needs to be equal to the getIdentifier() return value.", nuuid, puuid);
    }

    /**
     * @throws RepositoryException
     */
    public void testFrozenUUUID() throws RepositoryException,
            NotExecutableException {
        // make versionable node referenceable
        ensureMixinType(versionableNode, mixReferenceable);
        versionableNode.getSession().save();
        VersionManager versionManager = versionableNode.getSession().getWorkspace().getVersionManager();
        String path = versionableNode.getPath();
        Version v = versionManager.checkin(path);
        Node n = v.getFrozenNode();
        String fuuid = n.getProperty(jcrFrozenUuid).getValue().getString();
        String ruuid = versionableNode.getIdentifier();
        assertEquals("jcr:frozenUuid needs to be equal to the getIdentifier() return value.", ruuid, fuuid);
    }

    /**
     * @throws RepositoryException
     */
    public void testFrozenChildUUUID() throws RepositoryException,
            NotExecutableException {
        Node n1 = versionableNode.addNode("child");
        ensureMixinType(n1, mixReferenceable);
        versionableNode.getSession().save();
        VersionManager versionManager = versionableNode.getSession().getWorkspace().getVersionManager();
        String path = versionableNode.getPath();
        Version v = versionManager.checkin(path);
        Node n = v.getFrozenNode().getNode("child");
        String fuuid = n.getProperty(jcrFrozenUuid).getValue().getString();
        String ruuid = n1.getIdentifier();
        assertEquals("jcr:frozenUuid needs to be equal to the getIdentifier() return value.", ruuid, fuuid);
    }


    /**
     * @throws RepositoryException
     */
    public void testFrozenNodeNodeType() throws RepositoryException {
        VersionManager versionManager = versionableNode.getSession().getWorkspace().getVersionManager();
        String path = versionableNode.getPath();
        Version v = versionManager.checkin(path);
        Node n = v.getFrozenNode();
        String puuid = n.getProperty(jcrPrimaryType).getValue().getString();
        String nuuid = n.getPrimaryNodeType().getName();
        assertEquals("jcr:primaryType needs to be equal to the getPrimaryNodeType() return value.", nuuid, puuid);
    }

    /**
     * @throws RepositoryException
     */
    public void testFrozenChildNodeNodeType() throws RepositoryException {
        versionableNode.addNode("child");
        versionableNode.getSession().save();
        VersionManager versionManager = versionableNode.getSession().getWorkspace().getVersionManager();
        String path = versionableNode.getPath();
        Version v = versionManager.checkin(path);
        Node n = v.getFrozenNode().getNode("child");
        String puuid = n.getProperty(jcrPrimaryType).getValue().getString();
        String nuuid = n.getPrimaryNodeType().getName();
        assertEquals("jcr:primaryType needs to be equal to the getPrimaryNodeType() return value.", nuuid, puuid);
    }

    /**
     * @throws RepositoryException
     */
    public void testFrozenNodeType() throws RepositoryException {
        VersionManager versionManager = versionableNode.getSession().getWorkspace().getVersionManager();
        String path = versionableNode.getPath();
        Version v = versionManager.checkin(path);
        Node n = v.getFrozenNode();
        String fuuid = n.getProperty("jcr:frozenPrimaryType").getValue().getString();
        String ruuid = versionableNode.getPrimaryNodeType().getName();
        assertEquals("jcr:frozenPrimaryType needs to be equal to the getPrimaryNodeType() return value.", ruuid, fuuid);
    }

    /**
     * @throws RepositoryException
     */
    public void testFrozenChildNodeType() throws RepositoryException {
        Node n1 = versionableNode.addNode("child");
        versionableNode.getSession().save();
        VersionManager versionManager = versionableNode.getSession().getWorkspace().getVersionManager();
        String path = versionableNode.getPath();
        Version v = versionManager.checkin(path);
        Node n = v.getFrozenNode().getNode("child");
        String fuuid = n.getProperty("jcr:frozenPrimaryType").getValue().getString();
        String ruuid = n1.getPrimaryNodeType().getName();
        assertEquals("jcr:frozenPrimaryType needs to be equal to the getPrimaryNodeType() return value.", ruuid, fuuid);
    }
}
