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
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.PropertyImpl;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.spi.commons.conversion.NameResolver;
import org.apache.jackrabbit.test.NotExecutableException;
import org.apache.jackrabbit.value.StringValue;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RangeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.PropertyType;
import javax.jcr.nodetype.ConstraintViolationException;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.util.HashSet;
import java.util.Set;
import java.security.Principal;
import java.util.UUID;

/**
 * <code>AuthorizableImplTest</code>...
 */
public class AuthorizableImplTest extends AbstractUserTest {

    private List<String> protectedUserProps = new ArrayList<String>();
    private List<String> protectedGroupProps = new ArrayList<String>();

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        if (superuser instanceof SessionImpl) {
            NameResolver resolver = (SessionImpl) superuser;
            protectedUserProps.add(resolver.getJCRName(UserConstants.P_PASSWORD));
            protectedUserProps.add(resolver.getJCRName(UserConstants.P_IMPERSONATORS));
            protectedUserProps.add(resolver.getJCRName(UserConstants.P_PRINCIPAL_NAME));
            protectedUserProps.add(resolver.getJCRName(UserConstants.P_DISABLED));

            protectedGroupProps.add(resolver.getJCRName(UserConstants.P_MEMBERS));
            protectedGroupProps.add(resolver.getJCRName(UserConstants.P_PRINCIPAL_NAME));
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
        for (String pName : protectedUserProps) {
            try {
                u.setProperty(pName, v);
                save(superuser);
                fail("changing the '" + pName + "' property on a User should fail.");
            } catch (RepositoryException e) {
                // success
            }
        }
        
        Group g = getTestGroup(superuser);
        for (String pName : protectedGroupProps) {
            try {
                g.setProperty(pName, v);
                save(superuser);
                fail("changing the '" + pName + "' property on a Group should fail.");
            } catch (RepositoryException e) {
                // success
            }
        }
    }

    public void testRemoveSpecialProperties() throws NotExecutableException, RepositoryException {
        User u = getTestUser(superuser);
        for (String pName : protectedUserProps) {
            try {
                u.removeProperty(pName);
                save(superuser);
                fail("removing the '" + pName + "' property on a User should fail.");
            } catch (RepositoryException e) {
                // success
            }
        }
        Group g = getTestGroup(superuser);
        for (String pName : protectedGroupProps) {
            try {
                g.removeProperty(pName);
                save(superuser);
                fail("removing the '" + pName + "' property on a Group should fail.");
            } catch (RepositoryException e) {
                // success
            }
        }
    }

    public void testProtectedUserProperties() throws NotExecutableException, RepositoryException {
        UserImpl user = (UserImpl) getTestUser(superuser);
        NodeImpl n = user.getNode();

        checkProtected(n.getProperty(UserConstants.P_PASSWORD));
        if (n.hasProperty(UserConstants.P_PRINCIPAL_NAME)) {
            checkProtected(n.getProperty(UserConstants.P_PRINCIPAL_NAME));
        }
        if (n.hasProperty(UserConstants.P_IMPERSONATORS)) {
           checkProtected(n.getProperty(UserConstants.P_IMPERSONATORS));
        }
    }

    public void testProtectedGroupProperties() throws NotExecutableException, RepositoryException {
        GroupImpl gr = (GroupImpl) getTestGroup(superuser);
        NodeImpl n = gr.getNode();

        if (n.hasProperty(UserConstants.P_PRINCIPAL_NAME)) {
            checkProtected(n.getProperty(UserConstants.P_PRINCIPAL_NAME));
        }
        if (n.hasProperty(UserConstants.P_MEMBERS)) {
            checkProtected(n.getProperty(UserConstants.P_MEMBERS));
        }
    }

    public void testMembersPropertyType() throws NotExecutableException, RepositoryException {
        GroupImpl gr = (GroupImpl) getTestGroup(superuser);
        NodeImpl n = gr.getNode();

        if (!n.hasProperty(UserConstants.P_MEMBERS)) {
            gr.addMember(getTestUser(superuser));
        }

        Property p = n.getProperty(UserConstants.P_MEMBERS);
        for (Value v : p.getValues()) {
            assertEquals(PropertyType.WEAKREFERENCE, v.getType());
        }
    }

    public void testMemberOfRangeIterator() throws NotExecutableException, RepositoryException {
        Authorizable auth = null;
        Group group = null;

        try {
            auth = userMgr.createUser(getTestPrincipal().getName(), "pw");
            group = userMgr.createGroup(getTestPrincipal());
            save(superuser);

            Iterator<Group>groups = auth.declaredMemberOf();
            assertTrue(groups instanceof RangeIterator);
            assertEquals(0, ((RangeIterator) groups).getSize());
            groups = auth.memberOf();
            assertTrue(groups instanceof RangeIterator);
            assertEquals(0, ((RangeIterator) groups).getSize());

            group.addMember(auth);
            groups = auth.declaredMemberOf();
            assertTrue(groups instanceof RangeIterator);
            assertEquals(1, ((RangeIterator) groups).getSize());

            groups = auth.memberOf();
            assertTrue(groups instanceof RangeIterator);
            assertEquals(1, ((RangeIterator) groups).getSize());

        } finally {
            if (auth != null) {
                auth.remove();
            }
            if (group != null) {
                group.remove();
            }
            save(superuser);
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
            String imperson = "anyimpersonator";
            n.setProperty(
                    UserConstants.P_IMPERSONATORS,
                    new Value[] {new StringValue(imperson)},
                    PropertyType.STRING);
            fail("Attempt to change protected property rep:impersonators should fail.");
        } catch (ConstraintViolationException e) {
            // ok.
        }
    }

    public void testRemoveSpecialUserPropertiesDirectly() throws RepositoryException, NotExecutableException {
        AuthorizableImpl g = (AuthorizableImpl) getTestUser(superuser);
        NodeImpl n = g.getNode();
        try {
            n.getProperty(UserConstants.P_PASSWORD).remove();
            fail("Attempt to remove protected property rep:password should fail.");
        } catch (ConstraintViolationException e) {
            // ok.
        }
        try {
            if (n.hasProperty(UserConstants.P_PRINCIPAL_NAME)) {
                n.getProperty(UserConstants.P_PRINCIPAL_NAME).remove();
                fail("Attempt to remove protected property rep:principalName should fail.");
            }
        } catch (ConstraintViolationException e) {
            // ok.
        }
    }

    public void testRemoveSpecialGroupPropertiesDirectly() throws RepositoryException, NotExecutableException {
        AuthorizableImpl g = (AuthorizableImpl) getTestGroup(superuser);
        NodeImpl n = g.getNode();
        try {
            if (n.hasProperty(UserConstants.P_PRINCIPAL_NAME)) {
                n.getProperty(UserConstants.P_PRINCIPAL_NAME).remove();
                fail("Attempt to remove protected property rep:principalName should fail.");
            }
        } catch (ConstraintViolationException e) {
            // ok.
        }
        try {
            if (n.hasProperty(UserConstants.P_MEMBERS)) {
                n.getProperty(UserConstants.P_MEMBERS).remove();
                fail("Attempt to remove protected property rep:members should fail.");
            }
        } catch (ConstraintViolationException e) {
            // ok.
        }
    }

    public void testUserGetProperties() throws RepositoryException, NotExecutableException {
        AuthorizableImpl user = (AuthorizableImpl) getTestUser(superuser);
        NodeImpl n = user.getNode();

        for (PropertyIterator it = n.getProperties(); it.hasNext();) {
            PropertyImpl p = (PropertyImpl) it.nextProperty();
            if (p.getDefinition().isProtected()) {
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
            if (p.getDefinition().isProtected()) {
                assertFalse(group.hasProperty(p.getName()));
                assertNull(group.getProperty(p.getName()));
            } else {
                // authorizable defined property
                assertTrue(group.hasProperty(p.getName()));
                assertNotNull(group.getProperty(p.getName()));
            }
        }
    }

    public void testSingleToMultiValued() throws Exception {
        AuthorizableImpl user = (AuthorizableImpl) getTestUser(superuser);
        UserManager uMgr = getUserManager(superuser);
        try {
            Value v = superuser.getValueFactory().createValue("anyValue");
            user.setProperty("someProp", v);
            if (!uMgr.isAutoSave()) {
                superuser.save();
            }
            Value[] vs = new Value[] {v, v};
            user.setProperty("someProp", vs);
            if (!uMgr.isAutoSave()) {
                superuser.save();
            }
        } finally {
            if (user.removeProperty("someProp") && !uMgr.isAutoSave()) {
                superuser.save();
            }
        }
    }

    public void testMultiValuedToSingle() throws Exception {
        AuthorizableImpl user = (AuthorizableImpl) getTestUser(superuser);
        UserManager uMgr = getUserManager(superuser);
        try {
            Value v = superuser.getValueFactory().createValue("anyValue");
            Value[] vs = new Value[] {v, v};
            user.setProperty("someProp", vs);
            if (!uMgr.isAutoSave()) {
                superuser.save();
            }
            user.setProperty("someProp", v);
            if (!uMgr.isAutoSave()) {
                superuser.save();
            }
        } finally {
            if (user.removeProperty("someProp") && !uMgr.isAutoSave()) {
                superuser.save();
            }
        }
    }

    public void testObjectMethods() throws Exception {
        final AuthorizableImpl user = (AuthorizableImpl) getTestUser(superuser);
        AuthorizableImpl user2 = (AuthorizableImpl) getTestUser(superuser);

        assertEquals(user, user2);
        assertEquals(user.hashCode(), user2.hashCode());
        Set<Authorizable> s = new HashSet<Authorizable>();
        s.add(user);
        assertFalse(s.add(user2));

        Authorizable user3 = new Authorizable() {

            public String getID() throws RepositoryException {
                return user.getID();
            }

            public boolean isGroup() {
                return user.isGroup();
            }

            public Principal getPrincipal() throws RepositoryException {
                return user.getPrincipal();
            }

            public Iterator<Group> declaredMemberOf() throws RepositoryException {
                return user.declaredMemberOf();
            }

            public Iterator<Group> memberOf() throws RepositoryException {
                return user.memberOf();
            }

            public void remove() throws RepositoryException {
                user.remove();
            }

            public Iterator<String> getPropertyNames() throws RepositoryException {
                return user.getPropertyNames();
            }

            public Iterator<String> getPropertyNames(String relPath) throws RepositoryException {
                return user.getPropertyNames(relPath);
            }

            public boolean hasProperty(String name) throws RepositoryException {
                return user.hasProperty(name);
            }

            public void setProperty(String name, Value value) throws RepositoryException {
                user.setProperty(name, value);
            }

            public void setProperty(String name, Value[] values) throws RepositoryException {
                user.setProperty(name, values);
            }

            public Value[] getProperty(String name) throws RepositoryException {
                return user.getProperty(name);
            }

            public boolean removeProperty(String name) throws RepositoryException {
                return user.removeProperty(name);
            }

            public String getPath() throws UnsupportedRepositoryOperationException, RepositoryException {
                return user.getPath();
            }
        };

        assertFalse(user.equals(user3));
        assertTrue(s.add(user3));
    }

    public void testGetPath() throws Exception {
        AuthorizableImpl user = (AuthorizableImpl) getTestUser(superuser);
        try {
            assertEquals(user.getNode().getPath(), user.getPath());
        } catch (UnsupportedRepositoryOperationException e) {
            // ok.
        }

    }

    /**
     * this is a very specialized test for JCR-3654
     * @throws Exception
     */
    public void testMembershipCacheTraversal() throws Exception {
        UserManager uMgr = getUserManager(superuser);
        //uMgr.autoSave(true);
        User u = uMgr.createUser("any_principal" + UUID.randomUUID(), "foobar");

        // create group with mixin properties
        GroupImpl gr = (GroupImpl) uMgr.createGroup("any_group" + UUID.randomUUID());
        for (int i=0; i<100; i++) {
            User testUser = uMgr.createUser("any_principal" + UUID.randomUUID(), "foobar");
            gr.addMember(testUser);
        }

        gr.addMember(u);
        NodeImpl n = gr.getNode();
        n.addMixin("mix:title");
        superuser.save();

        // removing the authorizable node forces a traversal to collect the memberships
        //u = (User) uMgr.getAuthorizable(u.getID());
        //superuser.getNode(u.getPath()).remove();
        u.remove();
        Iterator<Group> grp = u.declaredMemberOf();
        assertTrue("User need to be member of group", grp.hasNext());
        Group result = grp.next();
        assertEquals("User needs to be member of group", gr.getID(), result.getID());

        Iterator<Authorizable> auths = gr.getDeclaredMembers();
        int i = 0;
        while (auths.hasNext()) {
            auths.next();
            i++;
        }
        assertEquals("Group needs to have 100 members", 100, i);
    }
}
