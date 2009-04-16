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
package org.apache.jackrabbit.api.jsr283.version.simple;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import org.apache.jackrabbit.api.jsr283.version.Version;

/**
 * <code>SVFrozenNodeTest</code> covers tests related to frozen nodes in
 * simple versioning
 *
 * @test
 * @sources SVFrozenNodeTest.java
 * @executeClass org.apache.jackrabbit.test.api.version.SVFrozenNodeTest
 * @keywords simple-versioning
 */
public class FrozenNodeTest extends AbstractVersionTest {

    protected void setUp() throws Exception {
        super.setUp();

        versionableNode.checkout();
    }

    /**
     * @throws RepositoryException
     */
    public void testFrozenNodeUUUID() throws RepositoryException {
        Version v = (Version) versionableNode.checkin();
        Node n = v.getFrozenNode();
        String puuid = n.getProperty(jcrUUID).getValue().getString();
        String nuuid = n.getUUID();
        assertEquals("jcr:uuid needs to be equal to the getUUID() return value.", nuuid, puuid);
    }

    /**
     * @throws RepositoryException
     */
    public void testFrozenChildNodeUUUID() throws RepositoryException {
        versionableNode.addNode("child");
        versionableNode.save();
        Version v = (Version) versionableNode.checkin();
        Node n = v.getFrozenNode().getNode("child");
        String puuid = n.getProperty(jcrUUID).getValue().getString();
        String nuuid = n.getUUID();
        assertEquals("jcr:uuid needs to be equal to the getUUID() return value.", nuuid, puuid);
    }

    /**
     * @throws RepositoryException
     */
    public void testFrozenUUUID() throws RepositoryException {
        // make versionable node referenceable
        versionableNode.addMixin(mixReferenceable);
        versionableNode.save();
        Version v = (Version) versionableNode.checkin();
        Node n = v.getFrozenNode();
        String fuuid = n.getProperty(jcrFrozenUuid).getValue().getString();
        String ruuid = versionableNode.getUUID();
        assertEquals("jcr:frozenUuid needs to be equal to the getUUID() return value.", ruuid, fuuid);
    }

    /**
     * @throws RepositoryException
     */
    public void testFrozenChildUUUID() throws RepositoryException {
        Node n1 = versionableNode.addNode("child");
        n1.addMixin(mixReferenceable);
        versionableNode.save();
        Version v = (Version) versionableNode.checkin();
        Node n = v.getFrozenNode().getNode("child");
        String fuuid = n.getProperty(jcrFrozenUuid).getValue().getString();
        String ruuid = n1.getUUID();
        assertEquals("jcr:frozenUuid needs to be equal to the getUUID() return value.", ruuid, fuuid);
    }


    /**
     * @throws RepositoryException
     */
    public void testFrozenNodeNodeType() throws RepositoryException {
        Version v = (Version) versionableNode.checkin();
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
        versionableNode.save();
        Version v = (Version) versionableNode.checkin();
        Node n = v.getFrozenNode().getNode("child");
        String puuid = n.getProperty(jcrPrimaryType).getValue().getString();
        String nuuid = n.getPrimaryNodeType().getName();
        assertEquals("jcr:primaryType needs to be equal to the getPrimaryNodeType() return value.", nuuid, puuid);
    }

    /**
     * @throws RepositoryException
     */
    public void testFrozenNodeType() throws RepositoryException {
        Version v = (Version) versionableNode.checkin();
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
        versionableNode.save();
        Version v = (Version) versionableNode.checkin();
        Node n = v.getFrozenNode().getNode("child");
        String fuuid = n.getProperty("jcr:frozenPrimaryType").getValue().getString();
        String ruuid = n1.getPrimaryNodeType().getName();
        assertEquals("jcr:frozenPrimaryType needs to be equal to the getPrimaryNodeType() return value.", ruuid, fuuid);
    }
}