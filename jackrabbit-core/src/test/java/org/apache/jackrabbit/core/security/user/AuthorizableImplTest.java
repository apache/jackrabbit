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
package org.apache.jackrabbit.core.security.user;

import org.apache.jackrabbit.api.security.user.AbstractUserTest;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.PropertyImpl;
import org.apache.jackrabbit.spi.commons.conversion.NameResolver;
import org.apache.jackrabbit.test.NotExecutableException;
import org.apache.jackrabbit.value.StringValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.PropertyIterator;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NodeType;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * <code>AuthorizableImplTest</code>...
 */
public class AuthorizableImplTest extends AbstractUserTest {

    private static Logger log = LoggerFactory.getLogger(AuthorizableImplTest.class);

    private List protectedUserProps = new ArrayList();
    private List protectedGroupProps = new ArrayList();

    protected void setUp() throws Exception {
        super.setUp();

        if (superuser instanceof SessionImpl) {
            NameResolver resolver = (SessionImpl) superuser;
            protectedUserProps.add(resolver.getJCRName(UserConstants.P_USERID));
            protectedUserProps.add(resolver.getJCRName(UserConstants.P_PASSWORD));
            protectedUserProps.add(resolver.getJCRName(UserConstants.P_GROUPS));
            protectedUserProps.add(resolver.getJCRName(UserConstants.P_IMPERSONATORS));
            protectedUserProps.add(resolver.getJCRName(UserConstants.P_PRINCIPAL_NAME));
            protectedUserProps.add(resolver.getJCRName(UserConstants.P_REFEREES));

            protectedUserProps.add(resolver.getJCRName(UserConstants.P_GROUPS));
            protectedGroupProps.add(resolver.getJCRName(UserConstants.P_PRINCIPAL_NAME));
            protectedGroupProps.add(resolver.getJCRName(UserConstants.P_REFEREES));
        } else {
            throw new NotExecutableException();
        }
    }

    private static void checkProtected(Property prop) throws RepositoryException {
        assertTrue(prop.getDefinition().isProtected());
    }

    public void testRemoveAdmin() {
        String adminID = superuser.getUserID();
        try {
            Authorizable admin = userMgr.getAuthorizable(adminID);
            admin.remove();
            fail("The admin user cannot be removed.");
        } catch (RepositoryException e) {
            // OK superuser cannot be removed. not even by the superuser itself.
        }
    }

    public void testSetSpecialProperties() throws NotExecutableException, RepositoryException {
        Value v = superuser.getValueFactory().createValue("any_value");
        User u = getTestUser(superuser);

        for (Iterator it = protectedUserProps.iterator(); it.hasNext();) {
            String pName = it.next().toString();
            try {
                u.setProperty(pName, v);
                fail("changing the '" +pName+ "' property on a User should fail.");
            } catch (RepositoryException e) {
                // success
            }
        }
        Group g = getTestGroup(superuser);
        for (Iterator it = protectedGroupProps.iterator(); it.hasNext();) {
            String pName = it.next().toString();
            try {
                u.setProperty(pName, v);
                fail("changing the '" +pName+ "' property on a Group should fail.");
            } catch (RepositoryException e) {
                // success
            }
        }
    }

    public void testRemoveSpecialProperties() throws NotExecutableException, RepositoryException {
        User u = getTestUser(superuser);
        for (Iterator it = protectedUserProps.iterator(); it.hasNext();) {
            String pName = it.next().toString();
            try {
                u.removeProperty(pName);
                fail("removing the '" +pName+ "' property on a User should fail.");
            } catch (RepositoryException e) {
                // success
            }
        }
        Group g = getTestGroup(superuser);
        for (Iterator it = protectedGroupProps.iterator(); it.hasNext();) {
            String pName = it.next().toString();
            try {
                u.removeProperty(pName);
                fail("removing the '" +pName+ "' property on a Group should fail.");
            } catch (RepositoryException e) {
                // success
            }
        }
    }

    public void testProtectedUserProperties() throws NotExecutableException, RepositoryException {
        UserImpl user = (UserImpl) getTestUser(superuser);
        NodeImpl n = user.getNode();

        checkProtected(n.getProperty(UserConstants.P_USERID));
        checkProtected(n.getProperty(UserConstants.P_PRINCIPAL_NAME));
        if (n.hasProperty(UserConstants.P_REFEREES)) {
           checkProtected(n.getProperty(UserConstants.P_REFEREES));
        }
        if (n.hasProperty(UserConstants.P_GROUPS)) {
            checkProtected(n.getProperty(UserConstants.P_GROUPS));
        }
        if (n.hasProperty(UserConstants.P_IMPERSONATORS)) {
           checkProtected(n.getProperty(UserConstants.P_IMPERSONATORS));
        }
    }

    public void testProtectedGroupProperties() throws NotExecutableException, RepositoryException {
        GroupImpl gr = (GroupImpl) getTestGroup(superuser);
        NodeImpl n = gr.getNode();

        checkProtected(n.getProperty(UserConstants.P_PRINCIPAL_NAME));
        if (n.hasProperty(UserConstants.P_GROUPS)) {
            checkProtected(n.getProperty(UserConstants.P_GROUPS));
        }
        if (n.hasProperty(UserConstants.P_REFEREES)) {
           checkProtected(n.getProperty(UserConstants.P_REFEREES));
        }
    }

    public void testSetSpecialPropertiesDirectly() throws NotExecutableException, RepositoryException {
        AuthorizableImpl user = (AuthorizableImpl) getTestUser(superuser);
        NodeImpl n = user.getNode();
        try {
            String pName = user.getPrincipalName();
            n.setProperty(UserConstants.P_PRINCIPAL_NAME, new StringValue("any-value"));

            // should have failed => change value back.
            n.setProperty(UserConstants.P_PRINCIPAL_NAME, new StringValue(pName));
            fail("Attempt to change protected property rep:principalName should fail.");
        } catch (ConstraintViolationException e) {
            // ok.
        }

        try {
            String refereeName = "anyreferee";
            n.setProperty(UserConstants.P_REFEREES, new Value[] {new StringValue(refereeName)});
            fail("Attempt to change protected property rep:referees should fail.");
        } catch (ConstraintViolationException e) {
            // ok.
        }
        try {
            String imperson = "anyimpersonator";
            n.setProperty(UserConstants.P_IMPERSONATORS, new Value[] {new StringValue(imperson)});
            fail("Attempt to change protected property rep:impersonators should fail.");
        } catch (ConstraintViolationException e) {
            // ok.
        }
    }

    public void testRemoveSpecialPropertiesDirectly() throws RepositoryException, NotExecutableException {
        AuthorizableImpl g = (AuthorizableImpl) getTestGroup(superuser);
        NodeImpl n = g.getNode();
        try {
            n.getProperty(UserConstants.P_PRINCIPAL_NAME).remove();
            fail("Attempt to remove protected property rep:principalName should fail.");
        } catch (ConstraintViolationException e) {
            // ok.
        }
    }

    public void testUserGetProperties() throws RepositoryException, NotExecutableException {
        AuthorizableImpl user = (AuthorizableImpl) getTestUser(superuser);
        NodeImpl n = user.getNode();

        for (PropertyIterator it = n.getProperties(); it.hasNext();) {
            PropertyImpl p = (PropertyImpl) it.nextProperty();
            NodeType declaringNt = p.getDefinition().getDeclaringNodeType();
            if (!declaringNt.isNodeType("rep:Authorizable") ||
                    protectedUserProps.contains(p.getName())) {
                assertFalse(user.hasProperty(p.getName()));
                assertNull(user.getProperty(p.getName()));
            } else {
                // authorizable defined property
                assertTrue(user.hasProperty(p.getName()));
                assertNotNull(user.getProperty(p.getName()));
            }
        }
    }

    public void testGroupGetProperties() throws RepositoryException, NotExecutableException {
        AuthorizableImpl group = (AuthorizableImpl) getTestGroup(superuser);
        NodeImpl n = group.getNode();

        for (PropertyIterator it = n.getProperties(); it.hasNext();) {
            PropertyImpl p = (PropertyImpl) it.nextProperty();
            NodeType declaringNt = p.getDefinition().getDeclaringNodeType();
            if (!declaringNt.isNodeType("rep:Authorizable") ||
                    protectedGroupProps.contains(p.getName())) {
                assertFalse(group.hasProperty(p.getName()));
                assertNull(group.getProperty(p.getName()));
            } else {
                // authorizable defined property
                assertTrue(group.hasProperty(p.getName()));
                assertNotNull(group.getProperty(p.getName()));
            }
        }
    }
}