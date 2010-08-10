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
import org.apache.jackrabbit.core.security.authorization.AccessControlConstants;
import org.apache.jackrabbit.test.NotExecutableException;

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.Privilege;
import java.security.Principal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * <code>ReadTest</code>...
 */
public class ReadTest extends AbstractEvaluationTest {

    private String path;
    private String childNPath;

    protected void setUp() throws Exception {
        super.setUp();

        // create some nodes below the test root in order to apply ac-stuff
        Node node = testRootNode.addNode(nodeName1, testNodeType);
        Node cn1 = node.addNode(nodeName2, testNodeType);
        superuser.save();

        path = node.getPath();
        childNPath = cn1.getPath();
    }

    protected boolean isExecutable() {
        return EvaluationUtil.isExecutable(acMgr);
    }

    protected JackrabbitAccessControlList getPolicy(AccessControlManager acM, String path, Principal principal) throws RepositoryException, AccessDeniedException, NotExecutableException {
        return EvaluationUtil.getPolicy(acM, path, principal);
    }

    protected Map<String, Value> getRestrictions(Session s, String path) {
        return Collections.emptyMap();
    }

    public void testReadDenied() throws Exception {
        Privilege[] privileges = privilegesFromName(Privilege.JCR_READ);

        /* deny READ privilege for testUser at 'path' */
        withdrawPrivileges(path, privileges, getRestrictions(superuser, path));
        /*
         allow READ privilege for testUser at 'childNPath'
         */
        givePrivileges(childNPath, privileges, getRestrictions(superuser, childNPath));


        Session testSession = getTestSession();

        assertFalse(testSession.nodeExists(path));
        assertTrue(testSession.nodeExists(childNPath));
        Node n = testSession.getNode(childNPath);
        n.getDefinition();
    }

    public void testGlobRestriction() throws Exception {
        Session testSession = getTestSession();
        AccessControlManager testAcMgr = getTestACManager();
        ValueFactory vf = superuser.getValueFactory();
        /*
          precondition:
          testuser must have READ-only permission on test-node and below
        */
        checkReadOnly(path);
        checkReadOnly(childNPath);

        Node child = superuser.getNode(childNPath).addNode(nodeName3);
        superuser.save();
        String childchildPath = child.getPath();

        Privilege[] read = privilegesFromName(Privilege.JCR_READ);

        Map<String, Value> restrictions = new HashMap(getRestrictions(superuser, path));
        restrictions.put(AccessControlConstants.P_GLOB.toString(), vf.createValue("*/"+jcrPrimaryType));

        withdrawPrivileges(path, read, restrictions);

        assertTrue(testAcMgr.hasPrivileges(path, read));
        assertTrue(testSession.hasPermission(path, javax.jcr.Session.ACTION_READ));
        testSession.getNode(path);

        assertTrue(testAcMgr.hasPrivileges(childNPath, read));
        assertTrue(testSession.hasPermission(childNPath, javax.jcr.Session.ACTION_READ));
        testSession.getNode(childNPath);

        String propPath = path + "/" + jcrPrimaryType;
        assertFalse(testSession.hasPermission(propPath, javax.jcr.Session.ACTION_READ));
        assertFalse(testSession.propertyExists(propPath));

        propPath = childNPath + "/" + jcrPrimaryType;
        assertFalse(testSession.hasPermission(propPath, javax.jcr.Session.ACTION_READ));
        assertFalse(testSession.propertyExists(propPath));
    }
}