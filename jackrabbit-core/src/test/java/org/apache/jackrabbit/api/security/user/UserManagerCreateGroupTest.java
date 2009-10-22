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
package org.apache.jackrabbit.api.security.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.jackrabbit.test.NotExecutableException;

import javax.jcr.RepositoryException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

/**
 * <code>UserManagerCreateGroupTest</code>...
 */
public class UserManagerCreateGroupTest extends AbstractUserTest {

    private static Logger log = LoggerFactory.getLogger(UserManagerCreateGroupTest.class);

    private List<Authorizable> createdGroups = new ArrayList();

    @Override
    protected void tearDown() throws Exception {
        // remove all created groups again
        for (Authorizable createdGroup : createdGroups) {
            try {
                createdGroup.remove();
                superuser.save();
            } catch (RepositoryException e) {
                log.error("Failed to remove Group " + createdGroup.getID() + " during tearDown.");
            }
        }

        super.tearDown();
    }

    private Group createGroup(Principal p) throws RepositoryException, NotExecutableException {
        Group gr = userMgr.createGroup(p);
        save(superuser);
        return gr;
    }

    private Group createGroup(Principal p, String iPath) throws RepositoryException, NotExecutableException {
        Group gr = userMgr.createGroup(p, iPath);
        save(superuser);
        return gr;
    }

    public void testCreateGroup() throws RepositoryException, NotExecutableException {
        Principal p = getTestPrincipal();
        Group gr = createGroup(p);
        createdGroups.add(gr);

        assertNotNull(gr.getID());
        assertEquals(p.getName(), gr.getPrincipal().getName());
        assertFalse("A new group must not have members.",gr.getMembers().hasNext());
    }

    public void testCreateGroupWithPath() throws RepositoryException, NotExecutableException {
        Principal p = getTestPrincipal();
        Group gr = createGroup(p, "/any/path/to/the/new/group");
        createdGroups.add(gr);

        assertNotNull(gr.getID());
        assertEquals(p.getName(), gr.getPrincipal().getName());
        assertFalse("A new group must not have members.",gr.getMembers().hasNext());
    }

    public void testCreateGroupWithNullPrincipal() throws RepositoryException {
        try {
            Group gr = createGroup(null);
            createdGroups.add(gr);

            fail("A Group cannot be built from 'null' Principal");
        } catch (Exception e) {
            // ok
        }

        try {
            Group gr = createGroup(null, "/any/path/to/the/new/group");
            createdGroups.add(gr);

            fail("A Group cannot be built from 'null' Principal");
        } catch (Exception e) {
            // ok
        }
    }

    public void testCreateDuplicateGroup() throws RepositoryException, NotExecutableException {
        Principal p = getTestPrincipal();
        Group gr = createGroup(p);
        createdGroups.add(gr);

        try {
            Group gr2 = createGroup(p);
            createdGroups.add(gr2);
            fail("Creating 2 groups with the same Principal should throw AuthorizableExistsException.");
        } catch (AuthorizableExistsException e) {
            // success.
        }
    }

    public void testAutoSave() throws RepositoryException {
        boolean autosave = userMgr.isAutoSave();
        if (autosave) {
            try {
                userMgr.autoSave(false);
                autosave = false;
            } catch (RepositoryException e) {
                // cannot change autosave behavior
                // ignore -> test will behave differently.
            }
        }

        Principal p = getTestPrincipal();
        Group gr = userMgr.createGroup(p);
        String id = gr.getID();
        superuser.refresh(false);

        if (!autosave) {
            // transient changes must be gone after the refresh-call.
            assertNull(userMgr.getAuthorizable(id));
            assertNull(userMgr.getAuthorizable(p));
        } else {
            // no transient changes as autosave could not be disabled.
            createdGroups.add(gr);            
            assertNotNull(userMgr.getAuthorizable(id));
            assertNotNull(userMgr.getAuthorizable(p));
        }
    }
}