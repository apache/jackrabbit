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
package org.apache.jackrabbit.test.api.retention;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.retention.RetentionPolicy;

import org.apache.jackrabbit.test.NotExecutableException;
import org.apache.jackrabbit.test.RepositoryStub;

/**
 * <code>RetentionPolicyEffectTest</code>...
 */
public class RetentionPolicyEffectTest extends AbstractRetentionTest {

    private Node childN;
    private Property childP;
    private Session otherS;
    
    protected void setUp() throws Exception {
        super.setUp();

        childN = testRootNode.addNode(nodeName2);
        Value v = getJcrValue(superuser, RepositoryStub.PROP_PROP_VALUE1, RepositoryStub.PROP_PROP_TYPE1, "test");
        childP = testRootNode.setProperty(propertyName1, v);
        superuser.save();

        otherS = getHelper().getSuperuserSession();
    }

    protected void tearDown() throws Exception {
        if (otherS != null) {
            otherS.logout();
        }
        superuser.refresh(false);
        RetentionPolicy rp = retentionMgr.getRetentionPolicy(testNodePath);
        if (rp != null) {
            retentionMgr.removeRetentionPolicy(testNodePath);
            superuser.save();
        }
        super.tearDown();
    }

    // TODO: test importXML (session/wsp) / move (session/wsp) / copy ...
    // TODO: test effect on child items
    
    public void testTransientRententionPolicy() throws RepositoryException, NotExecutableException {
        retentionMgr.setRetentionPolicy(testNodePath, getApplicableRetentionPolicy());

        assertNoEffect(testRootNode, nodeName3, propertyName2);
        assertNoEffect(childN, nodeName3, propertyName2);
        assertNoEffect(childP);
    }

    public void testTransientRententionPolicy2() throws RepositoryException, NotExecutableException {
        retentionMgr.setRetentionPolicy(testNodePath, getApplicableRetentionPolicy());

        assertNoEffect((Node) otherS.getItem(testNodePath), nodeName3, propertyName2);
        assertNoEffect((Node) otherS.getItem(childN.getPath()), nodeName3, propertyName2);
        assertNoEffect((Property) otherS.getItem(childP.getPath()));
    }

    public void testRententionPolicy() throws RepositoryException, NotExecutableException {
        retentionMgr.setRetentionPolicy(testNodePath, getApplicableRetentionPolicy());
        superuser.save();

        // check for superuser
        assertEffect(testRootNode, childN.getName(), childP.getName(), nodeName3, propertyName2);
    }

    public void testRententionPolicy2() throws RepositoryException, NotExecutableException {
        retentionMgr.setRetentionPolicy(testNodePath, getApplicableRetentionPolicy());
        superuser.save();

        // check for other session
        assertEffect((Node) otherS.getItem(testNodePath), childN.getName(), childP.getName(), nodeName3, propertyName2);
    }

    private void assertEffect(Node targetNode, String childName,
                                        String propName, String childName2,
                                        String propName2) throws RepositoryException {
        Session s = targetNode.getSession();
        try {
            Node child = targetNode.getNode(childName);
            child.remove();
            s.save();
            fail("Retention policy present must prevent a child node from being removed.");
        } catch (RepositoryException e) {
            // success
            s.refresh(false);
        }
        try {
            Property p = targetNode.getProperty(propName);
            p.remove();
            s.save();
            fail("Retention policy present must prevent a child property from being removed.");
        } catch (RepositoryException e) {
            // success
            s.refresh(false);
        }
        try {
            Property p = targetNode.getProperty(propName);
            p.setValue("test2");
            s.save();
            fail("Retention policy present must prevent the child property from being modified.");
        } catch (RepositoryException e) {
            // success
            s.refresh(false);
        }
        try {
            targetNode.addNode(childName2);
            s.save();
            fail("Retention policy present must prevent the target node from having new nodes added.");
        } catch (RepositoryException e) {
            // success
            s.refresh(false);
        }
        try {
            Value v = getJcrValue(s, RepositoryStub.PROP_PROP_VALUE2, RepositoryStub.PROP_PROP_TYPE2, "test");
            targetNode.setProperty(propName2, v);
            s.save();
            fail("Retention policy present must prevent the target node from having new properties set.");
        } catch (RepositoryException e) {
            // success
            s.refresh(false);
        }
        try {
            targetNode.remove();
            s.save();
            fail("Hold present must prevent the target node from being removed.");
        } catch (RepositoryException e) {
            // success
            s.refresh(false);
        }
    }

    private void assertNoEffect(Node target, String childName, String propName) throws RepositoryException {
        Session s = target.getSession();

        Node n = target.addNode(childName);
        Value v = getJcrValue(s, RepositoryStub.PROP_PROP_VALUE2, RepositoryStub.PROP_PROP_TYPE2, "test");
        Property p = target.setProperty(propName, v);

        n.remove();
        p.remove();
    }

    private void assertNoEffect(Property target) throws RepositoryException {
        target.setValue("test3");
        target.remove();
    }
}