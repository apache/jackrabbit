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

import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.api.security.user.AbstractUserTest;
import org.apache.jackrabbit.test.NotExecutableException;
import org.apache.jackrabbit.value.StringValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.nodetype.ConstraintViolationException;

/**
 * <code>AuthorizableImplTest</code>...
 */
public class AuthorizableImplTest extends AbstractUserTest {

    private static Logger log = LoggerFactory.getLogger(AuthorizableImplTest.class);

    private static void checkProtected(Property prop) throws RepositoryException {
        assertTrue(prop.getDefinition().isProtected());
    }

    public void testSetSpecialProperties() throws NotExecutableException, RepositoryException {
        Value v = superuser.getValueFactory().createValue("any_value");
        User u = getTestUser(superuser);
        try {
            u.setProperty("rep:userId", v);
            fail("changing the user id should fail.");
        } catch (RepositoryException e) {
            // success
        }

        try {
            u.setProperty("rep:principalName", v);
            fail("changing the principalName should fail.");
        } catch (RepositoryException e) {
            // success
        }

        try {
            Value[] vs = new Value[] {v};
            u.setProperty("rep:referees", vs);
            fail("changing the referees property should fail.");
        } catch (RepositoryException e) {
            // success
        }

        Group g = getTestGroup(superuser);
        try {
            g.setProperty("rep:principalName", v);
            fail("changing the principalName should fail.");
        } catch (RepositoryException e) {
            // success
        }
        try {
            Value[] vs = new Value[] {v};
            g.setProperty("rep:members", vs);
            fail("changing the members property should fail.");
        } catch (RepositoryException e) {
            // success
        }
    }

    public void testRemoveSpecialProperties() throws NotExecutableException, RepositoryException {
        User u = getTestUser(superuser);
        try {
            u.removeProperty("rep:userId");
            fail("removing the user id should fail.");
        } catch (RepositoryException e) {
            // success
        }

        try {
            u.removeProperty("rep:principalName");
            fail("removing the principalName should fail.");
        } catch (RepositoryException e) {
            // success
        }

        Group g = getTestGroup(superuser);
        try {
            g.removeProperty("rep:principalName");
            fail("removing the principalName should fail.");
        } catch (RepositoryException e) {
            // success
        }
        try {
            g.removeProperty("rep:members");
            fail("removing the members property should fail.");
        } catch (RepositoryException e) {
            // success
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
        if (n.hasProperty(UserConstants.P_IMPERSONATORS)) {
           checkProtected(n.getProperty(UserConstants.P_IMPERSONATORS));
        }
    }

    public void testProtectedGroupProperties() throws NotExecutableException, RepositoryException {
        GroupImpl gr = (GroupImpl) getTestGroup(superuser);
        NodeImpl n = gr.getNode();

        checkProtected(n.getProperty(UserConstants.P_PRINCIPAL_NAME));
        if (n.hasProperty(UserConstants.P_MEMBERS)) {
           checkProtected(n.getProperty(UserConstants.P_MEMBERS));
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
}