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
package org.apache.jackrabbit.core.security.authorization.acl;

import java.security.Principal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.Privilege;

import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.api.security.JackrabbitAccessControlList;
import org.apache.jackrabbit.core.security.authorization.AbstractEvaluationTest;
import org.apache.jackrabbit.core.security.authorization.AccessControlConstants;
import org.apache.jackrabbit.test.NotExecutableException;

public class ReadNodeTypeTest extends AbstractEvaluationTest {

    private String path;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // create some nodes below the test root in order to apply ac-stuff
        Node node = testRootNode.addNode(nodeName1, testNodeType);
        node.addMixin(NodeType.MIX_LOCKABLE);
        superuser.save();

        path = node.getPath();
    }

    @Override
    protected boolean isExecutable() {
        return EvaluationUtil.isExecutable(acMgr);
    }

    @Override
    protected JackrabbitAccessControlList getPolicy(AccessControlManager acM, String path, Principal principal) throws RepositoryException, AccessDeniedException, NotExecutableException {
        return EvaluationUtil.getPolicy(acM, path, principal);
    }

    @Override
    protected Map<String, Value> getRestrictions(Session s, String path) {
        return Collections.emptyMap();
    }

    /**
     * @see <a href="https://issues.apache.org/jira/browse/OAK-2441">OAK-2441</a>
     */
    public void testNodeGetPrimaryType() throws Exception {
        Map<String, Value> rest = new HashMap<String, Value>(getRestrictions(superuser, path));
        rest.put(AccessControlConstants.P_GLOB.toString(), vf.createValue("/jcr:*"));

        withdrawPrivileges(path, privilegesFromName(Privilege.JCR_READ), rest);

        Session testSession = getTestSession();
        Node n = testSession.getNode(path);

        assertFalse(testSession.propertyExists(path + '/' + JcrConstants.JCR_PRIMARYTYPE));
        assertFalse(n.hasProperty(JcrConstants.JCR_PRIMARYTYPE));

        NodeType primary = n.getPrimaryNodeType();
    }

    /**
     * @see <a href="https://issues.apache.org/jira/browse/OAK-2441">OAK-2441</a>
     */
    public void testNodeGetMixinTypes() throws Exception {
        Session testSession = getTestSession();
        assertTrue(testSession.propertyExists(path + '/' + JcrConstants.JCR_MIXINTYPES));

        Map<String, Value> rest = new HashMap<String, Value>(getRestrictions(superuser, path));
        rest.put(AccessControlConstants.P_GLOB.toString(), vf.createValue("/jcr:*"));

        withdrawPrivileges(path, privilegesFromName(Privilege.JCR_READ), rest);

        int noMixins = superuser.getNode(path).getMixinNodeTypes().length;

        Node n = testSession.getNode(path);
        assertFalse(testSession.propertyExists(path + '/' + JcrConstants.JCR_MIXINTYPES));
        assertFalse(n.hasProperty(JcrConstants.JCR_MIXINTYPES));

        NodeType[] mixins = n.getMixinNodeTypes();
        assertEquals(noMixins, mixins.length);
    }

    public void testNodeGetMixinTypesWithTransientModifications() throws Exception {
        int noMixins = superuser.getNode(path).getMixinNodeTypes().length;

        Node node = superuser.getNode(path);
        node.addMixin(NodeType.MIX_CREATED);

        NodeType[] mixins = node.getMixinNodeTypes();
        assertEquals(noMixins+1, mixins.length);
    }
}