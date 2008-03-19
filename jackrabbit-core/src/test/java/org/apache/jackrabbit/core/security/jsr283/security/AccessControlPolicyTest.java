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
package org.apache.jackrabbit.core.security.jsr283.security;

import org.apache.jackrabbit.test.NotExecutableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.AccessDeniedException;
import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * <code>AccessControlPolicyTest</code>...
 */
public class AccessControlPolicyTest extends AbstractAccessControlTest {

    private static Logger log = LoggerFactory.getLogger(AccessControlPolicyTest.class);

    private String path;
    private List addedPolicies = new ArrayList();

    protected void setUp() throws Exception {
        super.setUp();

        // policy-option is cover the by the 'OPTION_SIMPLE_ACCESS_CONTROL_SUPPORTED' -> see super-class

        Node n = testRootNode.addNode(nodeName1, testNodeType);
        superuser.save();
        path = n.getPath();
    }

    protected void tearDown() throws Exception {
        try {
            for (Iterator it = addedPolicies.iterator(); it.hasNext();) {
                String path = it.next().toString();
                acMgr.removePolicy(path);
            }
            superuser.save();
        } catch (Exception e) {
            log.error("Unexpected error while removing test policies.", e);
        }
        super.tearDown();
    }

    private AccessControlPolicy buildInvalidPolicy(String path) throws RepositoryException, AccessDeniedException {
        List applicable = new ArrayList();
        AccessControlPolicyIterator it = acMgr.getApplicablePolicies(path);
        while (it.hasNext()) {
            applicable.add(it.nextAccessControlPolicy().getName());
        }

        String name = "invalidPolicy";
        int index = 0;
        while (applicable.contains(name)) {
            name = "invalidPolicy" + index;
            index++;
        }
        final String policyName = name;
        return new AccessControlPolicy() {

            public String getName() throws RepositoryException {
                return policyName;
            }
            public String getDescription() throws RepositoryException {
                return null;
            }
        };
    }

    public void testGetEffectivePolicy() throws RepositoryException, AccessDeniedException, NotExecutableException {
        checkCanReadAc(path);
        // call must succeed without exception
        AccessControlPolicy policy = acMgr.getEffectivePolicy(path);
        if (policy != null) {
            assertTrue("The name of an AccessControlPolicy must not be null.", policy.getName() != null);
        } else {
            // no policy present on that node. // TODO: check if possible.
        }
    }

    public void testGetEffectivePolicyForNonExistingNode() throws RepositoryException, AccessDeniedException, NotExecutableException {
        String path = getPathToNonExistingNode();
        try {
            acMgr.getEffectivePolicy(path);
            fail("AccessControlManager.getEffectivePolicy for an invalid absPath must throw PathNotFoundException.");
        } catch (PathNotFoundException e) {
            // ok
        }
    }

    public void testGetEffectivePolicyForProperty() throws RepositoryException, AccessDeniedException, NotExecutableException {
        String path = getPathToProperty();
        try {
            acMgr.getEffectivePolicy(path);
            fail("AccessControlManager.getEffectivePolicy for property must throw PathNotFoundException.");
        } catch (PathNotFoundException e) {
            // ok
        }
    }

    public void testGetPolicy() throws RepositoryException, AccessDeniedException, NotExecutableException {
        checkCanReadAc(path);
        // call must succeed without exception
        AccessControlPolicy policy = acMgr.getPolicy(path);
        if (policy != null) {
            assertTrue("The name of an AccessControlPolicy must not be null.", policy.getName() != null);
        } else {
            // no policy present on that node.
        }
    }

    public void testGetApplicablePolicies() throws RepositoryException, AccessDeniedException, NotExecutableException {
        checkCanReadAc(path);
        // call must succeed without exception
        AccessControlPolicyIterator it = acMgr.getApplicablePolicies(path);
        assertNotNull("The iterator of applicable policies must not be null", it);
    }

    public void testApplicablePoliciesAreDistinct() throws RepositoryException, AccessDeniedException, NotExecutableException {
        checkCanReadAc(path);
        // call must succeed without exception
        AccessControlPolicyIterator it = acMgr.getApplicablePolicies(path);
        Set names = new HashSet();

        while (it.hasNext()) {
            AccessControlPolicy policy = it.nextAccessControlPolicy();
            if (!names.add(policy.getName())) {
                fail("The names of the policies present should be unique among the choices presented for a specific node. Name " + policy.getName() + " occured multiple times.");
            }
        }
    }

    public void testSetPolicy() throws RepositoryException, AccessDeniedException, NotExecutableException {
        checkCanModifyAc(path);
        AccessControlPolicyIterator it = acMgr.getApplicablePolicies(path);
        if (it.hasNext()) {
            AccessControlPolicy policy = it.nextAccessControlPolicy();
            acMgr.setPolicy(path, policy);
        } else {
            throw new NotExecutableException();
        }
    }

    public void testSetIllegalPolicy() throws RepositoryException, AccessDeniedException, NotExecutableException {
        checkCanModifyAc(path);
        try {
            acMgr.setPolicy(path, buildInvalidPolicy(path));
            fail("SetPolicy with an unknown policy should throw AccessControlException.");
        } catch (AccessControlException e) {
            // success.
        }
    }

    public void testGetPolicyAfterSet() throws RepositoryException, AccessDeniedException, NotExecutableException {
        checkCanReadAc(path);
        checkCanModifyAc(path);

        AccessControlPolicyIterator it = acMgr.getApplicablePolicies(path);
        if (it.hasNext()) {
            AccessControlPolicy policy = it.nextAccessControlPolicy();
            acMgr.setPolicy(path, policy);

            AccessControlPolicy p = acMgr.getPolicy(path);
            assertEquals("GetPolicy must return the policy that has been set before.", policy, p);
        } else {
            throw new NotExecutableException();
        }
    }

    public void testSetPolicyIsTransient() throws RepositoryException, AccessDeniedException, NotExecutableException {
        checkCanModifyAc(path);

        AccessControlPolicy current = acMgr.getPolicy(path);
        AccessControlPolicyIterator it = acMgr.getApplicablePolicies(path);
        if (it.hasNext()) {
            AccessControlPolicy policy = it.nextAccessControlPolicy();
            acMgr.setPolicy(path, policy);
            superuser.refresh(false);

            String mgs = "Reverting 'setPolicy' must change back the return value of getPolicy.";
            if (current == null) {
                assertEquals(mgs, acMgr.getPolicy(path), current);
            } else {
                assertTrue(mgs, acMgr.getPolicy(path).equals(current));
            }
        } else {
            throw new NotExecutableException();
        }
    }

    public void testGetPolicyAfterSave() throws RepositoryException, AccessDeniedException, NotExecutableException {
        checkCanReadAc(path);
        checkCanModifyAc(path);

        AccessControlPolicy policy;
        AccessControlPolicyIterator it = acMgr.getApplicablePolicies(path);
        if (it.hasNext()) {
            policy = it.nextAccessControlPolicy();
            acMgr.setPolicy(path, policy);
            superuser.save();

            // remember for tearDown
            addedPolicies.add(path);
        } else {
            throw new NotExecutableException();
        }

        Session s2 = null;
        try {
            s2 = helper.getSuperuserSession();
            AccessControlPolicy p = getAccessControlManager(s2).getPolicy(path);
            assertEquals("Policy must be visible to another superuser session.", policy, p);
        } finally {
            if (s2 != null) {
                s2.logout();
            }
        }
    }


    public void testNodeIsModifiedAfterSecondSetPolicy() throws RepositoryException, AccessDeniedException, NotExecutableException {
        checkCanModifyAc(path);
        // make sure an policy has been explicitely set.
        AccessControlPolicyIterator it = acMgr.getApplicablePolicies(path);
        if (it.hasNext()) {
            AccessControlPolicy policy = it.nextAccessControlPolicy();
            acMgr.setPolicy(path, policy);
            superuser.save();
            // remember for tearDown
            addedPolicies.add(path);
        } else {
            throw new NotExecutableException();
        }

        // call 'setPolicy' a second time -> Node must be modified.
        it = acMgr.getApplicablePolicies(path);
        try {
            if (it.hasNext()) {
                Item item = superuser.getItem(path);
                AccessControlPolicy policy = it.nextAccessControlPolicy();
                acMgr.setPolicy(path, policy);

                assertTrue("After setting a policy the node must be marked modified.", item.isModified());
            } else {
                throw new NotExecutableException();
            }
        } finally {
            // revert changes
            superuser.refresh(false);
        }
    }

    public void testNodeIsModifiedAfterSetPolicy() throws RepositoryException, AccessDeniedException, NotExecutableException {
        checkCanModifyAc(path);
        AccessControlPolicyIterator it = acMgr.getApplicablePolicies(path);
        if (it.hasNext()) {
            Item item = superuser.getItem(path);

            AccessControlPolicy policy = it.nextAccessControlPolicy();
            acMgr.setPolicy(path, policy);

            assertTrue("After setting a policy the node must be marked modified.", item.isModified());
        } else {
            throw new NotExecutableException();
        }
    }

    public void testRemovePolicy() throws RepositoryException, AccessDeniedException, NotExecutableException {
        checkCanModifyAc(path);

        AccessControlPolicyIterator it = acMgr.getApplicablePolicies(path);
        if (it.hasNext()) {
            AccessControlPolicy policy = it.nextAccessControlPolicy();
            acMgr.setPolicy(path, policy);
            AccessControlPolicy removed = acMgr.removePolicy(path);
            assertEquals("RemovePolicy must return the policy that has been set before.", policy, removed);
        } else {
            throw new NotExecutableException();
        }
    }

    public void testRemovePolicyIsTransient() throws RepositoryException, AccessDeniedException, NotExecutableException {
        checkCanReadAc(path);
        checkCanModifyAc(path);

        AccessControlPolicy current = acMgr.getPolicy(path);
        if (acMgr.getPolicy(path) == null) {
            // no policy to remove ->> apply one
            AccessControlPolicyIterator it = acMgr.getApplicablePolicies(path);
            if (it.hasNext()) {
                AccessControlPolicy policy = it.nextAccessControlPolicy();
                acMgr.setPolicy(path, policy);
                superuser.save();

                // remember for teardown
                addedPolicies.add(path);

                current = policy;
            } else {
                throw new NotExecutableException();
            }
        }

        // test transient behaviour of the removal
        acMgr.removePolicy(path);
        AccessControlPolicy p = acMgr.getPolicy(path);
        assertTrue("After transient remove AccessControlManager.getPolicy must return null.", p == null);

        // revert changes
        superuser.refresh(false);
        p = acMgr.getPolicy(path);
        assertEquals("Reverting a Policy removal must restore the original state.", p, current);
    }

    public void testNodeIsModifiedAfterRemovePolicy() throws RepositoryException, AccessDeniedException, NotExecutableException {
        checkCanReadAc(path);
        checkCanModifyAc(path);

        Item item = superuser.getItem(path);
        if (acMgr.getPolicy(path) == null) {
            // no policy to remove ->> apply one
            AccessControlPolicyIterator it = acMgr.getApplicablePolicies(path);
            if (it.hasNext()) {
                AccessControlPolicy policy = it.nextAccessControlPolicy();
                acMgr.setPolicy(path, policy);
                superuser.save();

                // remember for teardown
                addedPolicies.add(path);
            } else {
                throw new NotExecutableException();
            }
        }

        // test transient behaviour of the removal
        try {acMgr.removePolicy(path);
            assertTrue("After removing a policy the node must be marked modified.", item.isModified());
        } finally {
            item.refresh(false);
        }
    }

    public void testNullPolicyOnNewNode() throws NotExecutableException, RepositoryException, AccessDeniedException {
        Node n;
        try {
            n = ((Node) superuser.getItem(path)).addNode(nodeName2, testNodeType);
        } catch (RepositoryException e) {
            throw new NotExecutableException();
        }

        assertNull("A new Node must not have an access control policy set.", acMgr.getPolicy(n.getPath()));
    }

    public void testSetPolicyOnNewNode() throws NotExecutableException, RepositoryException, AccessDeniedException {
        Node n;
        try {
            n = ((Node) superuser.getItem(path)).addNode(nodeName2, testNodeType);
        } catch (RepositoryException e) {
            throw new NotExecutableException();
        }

        AccessControlPolicyIterator it = acMgr.getApplicablePolicies(n.getPath());
        while (it.hasNext()) {
            AccessControlPolicy policy = it.nextAccessControlPolicy();
            acMgr.setPolicy(n.getPath(), policy);

            AccessControlPolicy p = acMgr.getPolicy(n.getPath());
            assertNotNull("After calling setPolicy the manager must return a non-null policy for the new Node.", p);
            assertEquals("The name of applicable policy must be equal to the name of the set policy", policy.getName(), p.getName());
        }
    }

    public void testRemoveTransientlyAddedPolicy() throws RepositoryException, AccessDeniedException {
        AccessControlPolicy ex = acMgr.getPolicy(path);

        AccessControlPolicyIterator it = acMgr.getApplicablePolicies(path);
        while (it.hasNext()) {
            AccessControlPolicy policy = it.nextAccessControlPolicy();
            acMgr.setPolicy(path, policy);
            acMgr.removePolicy(path);

            String msg = "transiently added AND removing a policy must revert " +
                    "the changes made. " +
                    "ACMgr.getPolicy must then return the original value.";
            if (ex == null) {
                assertNull(msg, acMgr.getPolicy(path));
            } else {
                assertEquals(msg, ex.getName(), acMgr.getPolicy(path).getName());
            }
        }
    }
}