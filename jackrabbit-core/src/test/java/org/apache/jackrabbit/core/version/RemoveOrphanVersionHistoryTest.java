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
package org.apache.jackrabbit.core.version;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.Workspace;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;

import org.apache.jackrabbit.test.AbstractJCRTest;

/**
 * Test case for JCR-134.
 */
public class RemoveOrphanVersionHistoryTest extends AbstractJCRTest {

    /**
     * Test orphan version history cleaning in a single workspace.
     * @throws RepositoryException if an error occurs.
     */
    public void testRemoveOrphanVersionHistory() throws RepositoryException {
        Node n = testRootNode.addNode(nodeName1);
        n.addMixin(mixVersionable);
        testRootNode.save();
        Session session = n.getSession();
        VersionHistory vh = n.getVersionHistory();
        String vhUuid = vh.getUUID();
        assertExists(session, vhUuid);

        // First version
        Version v10 = n.checkin();
        n.checkout();

        // Second version
        Version v11 = n.checkin();
        n.checkout();

        // Remove node
        n.remove();
        testRootNode.save();
        assertExists(session, vhUuid);

        // Remove the first version
        vh.removeVersion(v10.getName());
        assertExists(session, vhUuid);

        // Remove the second and last version
        vh.removeVersion(v11.getName());

        try {
            session.getNodeByUUID(vhUuid);
            fail("Orphan version history must have been removed");
        } catch (ItemNotFoundException e) {
            // Expected
        }
    }

    /**
     * Test orphan version history cleaning in multiple workspace.
     * @throws RepositoryException if an error occurs.
     */
    public void testWorkspaceRemoveOrphanVersionHistory() throws RepositoryException {
        Node n = testRootNode.addNode(nodeName1);
        n.addMixin(mixVersionable);
        testRootNode.save();
        Session session = n.getSession();
        VersionHistory vh = n.getVersionHistory();
        String vhUuid = vh.getUUID();
        assertExists(session, vhUuid);

        // First version
        Version v10 = n.checkin();
        n.checkout();

        Workspace defaultWorkspace = n.getSession().getWorkspace();
        Session otherWsSession = n.getSession().getRepository().login(new SimpleCredentials("superuser", "".toCharArray()), workspaceName);
        // Clone the node in another workspace
        otherWsSession.getWorkspace().clone(defaultWorkspace.getName(), n.getPath(), n.getPath(), false);
        Node otherWsRootNode = otherWsSession.getRootNode();
        Node clonedNode = otherWsRootNode.getNode(n.getPath().substring(1));
        // Ensure that version histories are the same
        assertEquals(vhUuid, clonedNode.getVersionHistory().getUUID());

        Version v11 = clonedNode.checkin();
        clonedNode.checkout();

        // Remove node
        n.remove();
        testRootNode.save();
        assertExists(session, vhUuid);
        assertExists(otherWsSession, vhUuid);

        // Remove the first version
        vh.removeVersion(v10.getName());
        assertExists(session, vhUuid);
        assertExists(otherWsSession, vhUuid);

        // Remove cloned node
        clonedNode.remove();
        otherWsRootNode.save();
        assertExists(session, vhUuid);
        assertExists(otherWsSession, vhUuid);

        // Remove the last version
        vh.removeVersion(v11.getName());

        try {
            session.getNodeByUUID(vhUuid);
            fail("Orphan version history must have been removed from the default workspace");
        } catch (ItemNotFoundException e) {
            // Expected
        }

        try {
            otherWsSession.getNodeByUUID(vhUuid);
            fail("Orphan version history must have been removed from the other workspace");
        } catch (ItemNotFoundException e) {
            // Expected
        }
    }

    /**
     * Assert that a node exists in a session.
     * @param session the session.
     * @param uuid the node's UUID.
     * @throws RepositoryException if an error occurs.
     */
    protected void assertExists(Session session, String uuid) throws RepositoryException
    {
        try {
            session.getNodeByUUID(uuid);
        } catch (ItemNotFoundException e) {
            fail("Unknown uuid: " + uuid);
        }
    }
}
