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

import org.apache.jackrabbit.api.security.JackrabbitAccessControlList;
import org.apache.jackrabbit.core.security.authorization.AbstractEvaluationTest;
import org.apache.jackrabbit.test.NotExecutableException;

import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.AccessControlPolicy;
import javax.jcr.security.AccessControlPolicyIterator;
import javax.jcr.version.VersionException;
import java.security.Principal;
import java.util.Map;

/**
 * <code>ACLEditorTest</code>...
 */
public class ACLEditorTest extends AbstractEvaluationTest {

    private String testPath;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        Node node = testRootNode.addNode(nodeName1, "nt:unstructured");
        Node pseudoPolicy = node.addNode("rep:policy", "rep:ACL");
        superuser.save();

        testPath = node.getPath();
    }

    @Override
    protected boolean isExecutable() {
        return EvaluationUtil.isExecutable(acMgr);
    }

    @Override
    protected JackrabbitAccessControlList getPolicy(AccessControlManager acMgr, String path, Principal princ) throws RepositoryException, NotExecutableException {
        return EvaluationUtil.getPolicy(acMgr, path, princ);
    }

    @Override
    protected Map<String, Value> getRestrictions(Session session, String path) throws RepositoryException, NotExecutableException {
        return EvaluationUtil.getRestrictions(session, path);
    }

    public void testNodeNotRepAccessControllable() throws RepositoryException, LockException, ConstraintViolationException, NoSuchNodeTypeException, ItemExistsException, VersionException {
        AccessControlPolicy[] plcs = acMgr.getPolicies(testPath);
        assertNotNull(plcs);
        assertEquals(0, plcs.length);

        AccessControlPolicyIterator it = acMgr.getApplicablePolicies(testPath);
        assertNotNull(it);
        assertEquals(0, it.getSize());
    }

    public void testNodeNotRepAccessControllableAddMixin() throws RepositoryException, LockException, ConstraintViolationException, NoSuchNodeTypeException, ItemExistsException, VersionException {
        superuser.getNode(testPath).addMixin("rep:AccessControllable");
        superuser.save();

        AccessControlPolicy[] plcs = acMgr.getPolicies(testPath);
        assertNotNull(plcs);
        assertEquals(1, plcs.length);

        AccessControlPolicyIterator it = acMgr.getApplicablePolicies(testPath);
        assertNotNull(it);
        assertEquals(0, it.getSize());
    }
}
