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

import javax.jcr.RepositoryException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * <code>UserManagerCreateGroupTest</code>...
 */
public class UserManagerCreateGroupTest extends AbstractUserTest {

    private static Logger log = LoggerFactory.getLogger(UserManagerCreateGroupTest.class);

    private List createdGroups = new ArrayList();

    protected void tearDown() throws Exception {
        // remove all created groups again
        for (Iterator it = createdGroups.iterator(); it.hasNext();) {
            Authorizable gr = (Authorizable) it.next();
            try {
                gr.remove();
            } catch (RepositoryException e) {
                log.error("Failed to remove Group " + gr.getID() + " during tearDown.");
            }
        }

        super.tearDown();
    }

    public void testCreateGroup() throws RepositoryException {
        Principal p = getTestPrincipal();
        Group gr = userMgr.createGroup(p);
        createdGroups.add(gr);

        assertNotNull(gr.getID());
        assertEquals(p.getName(), gr.getPrincipal().getName());
        assertFalse("A new group must not have members.",gr.getMembers().hasNext());
    }

    public void testCreateGroupWithPath() throws RepositoryException {
        Principal p = getTestPrincipal();
        Group gr = userMgr.createGroup(p, "/any/path/to/the/new/group");
        createdGroups.add(gr);

        assertNotNull(gr.getID());
        assertEquals(p.getName(), gr.getPrincipal().getName());
        assertFalse("A new group must not have members.",gr.getMembers().hasNext());
    }

    public void testCreateGroupWithNullPrincipal() throws RepositoryException {
        try {
            Group gr = userMgr.createGroup(null);
            createdGroups.add(gr);

            fail("A Group cannot be built from 'null' Principal");
        } catch (Exception e) {
            // ok
        }

        try {
            Group gr = userMgr.createGroup(null, "/any/path/to/the/new/group");
            createdGroups.add(gr);

            fail("A Group cannot be built from 'null' Principal");
        } catch (Exception e) {
            // ok
        }
    }

    public void testCreateDuplicateGroup() throws RepositoryException {
        Principal p = getTestPrincipal();
        Group gr = userMgr.createGroup(p);
        createdGroups.add(gr);

        try {
            Group gr2 = userMgr.createGroup(p);
            createdGroups.add(gr2);
            fail("Creating 2 groups with the same Principal should throw AuthorizableExistsException.");
        } catch (AuthorizableExistsException e) {
            // success.
        }
    }
}