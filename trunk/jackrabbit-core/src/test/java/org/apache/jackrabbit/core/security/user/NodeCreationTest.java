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

import org.apache.commons.collections.map.ListOrderedMap;
import org.apache.jackrabbit.api.security.user.AbstractUserTest;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.AuthorizableExistsException;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.security.TestPrincipal;
import org.apache.jackrabbit.test.NotExecutableException;
import org.apache.jackrabbit.util.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * <code>NodeCreationTest</code>...
 */
public class NodeCreationTest extends AbstractUserTest {

    /**
     * logger instance
     */
    private static final Logger log = LoggerFactory.getLogger(NodeCreationTest.class);

    private SessionImpl s;
    private UserManagerImpl uMgr;
    private final List<NodeImpl> toRemove = new ArrayList();

    private String usersPath;
    private String groupsPath;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        String workspaceName = ((RepositoryImpl) superuser.getRepository()).getConfig().getSecurityConfig().getSecurityManagerConfig().getWorkspaceName();
        s = (SessionImpl) ((SessionImpl) superuser).createSession(workspaceName);

        usersPath = ((UserManagerImpl) userMgr).getUsersPath();
        groupsPath = ((UserManagerImpl) userMgr).getGroupsPath();
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            for (NodeImpl node : toRemove) {
                uMgr.removeProtectedItem(node, (NodeImpl) node.getParent());
                save(s);
            }
        } finally {
            s.logout();
        }
        super.tearDown();
    }

    private void createUserManager(int depth, boolean expandTree, long size) throws RepositoryException {
        Properties props = new Properties();
        props.put(UserManagerImpl.PARAM_DEFAULT_DEPTH, depth);
        props.put(UserManagerImpl.PARAM_AUTO_EXPAND_TREE, expandTree);
        props.put(UserManagerImpl.PARAM_AUTO_EXPAND_SIZE, size);
        props.put(UserManagerImpl.PARAM_GROUPS_PATH, groupsPath);
        props.put(UserManagerImpl.PARAM_USERS_PATH, usersPath);

        uMgr = new UserManagerImpl(s, "admin", props);
    }


    public void testRemoveTree() throws RepositoryException, NotExecutableException {
        UserImpl u = (UserImpl) userMgr.createUser("z", "z");
        save(superuser);
        UserImpl u2 = (UserImpl) userMgr.createUser("zz", "zz");
        save(superuser);

        assertEquals(usersPath + "/z/zz/z", u.getNode().getPath());

        try {
            NodeImpl folder = (NodeImpl) u.getNode().getParent().getParent();
            ((UserManagerImpl) userMgr).removeProtectedItem(folder, (NodeImpl) folder.getParent());
            save(superuser);
        } finally {
            boolean fail = false;
            if (userMgr.getAuthorizable("z") != null) {
                fail = true;
                u.remove();
                save(superuser);
            }
            if (userMgr.getAuthorizable("zz") != null) {
                fail = true;
                u2.remove();
                save(superuser);
            }
            if (fail) {
                fail("Removing the top authorizable folder must remove all users contained.");
            }
        }
    }

    /**
     * If auto-expand is false all users must be created on the second level.
     */
    public void testDefault() throws RepositoryException, NotExecutableException {
        createUserManager(2, false, 1);

        UserImpl u = (UserImpl) uMgr.createUser("z", "z");
        save(s);

        // remember the z-folder for later removal
        toRemove.add((NodeImpl) u.getNode().getParent().getParent());
        assertEquals(usersPath + "/z/zz/z", u.getNode().getPath());

        Map<String, String> m = new ListOrderedMap();
        m.put("zz",     "/z/zz/zz");
        m.put("zzz",    "/z/zz/zzz");
        m.put("zzzz",   "/z/zz/zzzz");
        m.put("zh",     "/z/zh/zh");
        m.put("zHzh",   "/z/zH/zHzh");
        m.put("z_Hz",   "/z/z_/z_Hz");
        m.put("z\u00cfrich", "/z/z\u00cf/z\u00cfrich");

        for (String uid : m.keySet()) {
            u = (UserImpl) uMgr.createUser(uid, uid);
            save(s);
            assertEquals(usersPath + m.get(uid), u.getNode().getPath());
        }
    }

    /**
     * Having 3 default levels -> test uids again.
     *
     * @throws RepositoryException
     */
    public void testChangedDefaultLevel() throws RepositoryException, NotExecutableException {
        createUserManager(3, false, 1);

        UserImpl u = (UserImpl) uMgr.createUser("z", "z");
        save(s);

        // remember the z-folder for later removal
        toRemove.add((NodeImpl) u.getNode().getParent().getParent().getParent());
        assertEquals(usersPath + "/z/zz/zzz/z", u.getNode().getPath());

        Map<String, String> m = new ListOrderedMap();
        m.put("zz",     "/z/zz/zzz/zz");
        m.put("zzz",    "/z/zz/zzz/zzz");
        m.put("zzzz",   "/z/zz/zzz/zzzz");
        m.put("zH",     "/z/zH/zHH/zH");
        m.put("zHzh",   "/z/zH/zHz/zHzh");
        m.put("z_Hz",   "/z/z_/z_H/z_Hz");
        m.put("z\u00cfrich", "/z/z\u00cf/z\u00cfr/z\u00cfrich");

        for (String uid : m.keySet()) {
            u = (UserImpl) uMgr.createUser(uid, uid);
            save(s);

            assertEquals(usersPath + m.get(uid), u.getNode().getPath());

            Authorizable az = uMgr.getAuthorizable(uid);
            assertNotNull(az);
        }
    }

    public void testIllegalChars() throws RepositoryException, NotExecutableException {
        createUserManager(2, true, 2);

        UserImpl u = (UserImpl) uMgr.createUser("z", "z");
        save(s);

        // remember the z-folder for later removal
        toRemove.add((NodeImpl) u.getNode().getParent().getParent());

        String zu = Text.escapeIllegalJcrChars("z*");
        String zur = Text.escapeIllegalJcrChars("z*r");

        Map<String, String> m = new ListOrderedMap();
        // test illegal JCR chars in uid
        // on level 2
        m.put("z*rich", "/z/" + zu + "/" + Text.escapeIllegalJcrChars("z*rich"));
        m.put("z*riq",  "/z/" + zu + "/" + Text.escapeIllegalJcrChars("z*riq"));
        m.put("z*",     "/z/" + zu + "/" + zu);  // still on level 2 (too short for 3)
        // on level 3
        m.put("z*rik",  "/z/" + zu + "/" + zur + "/" + Text.escapeIllegalJcrChars("z*rik"));
        m.put("z*.ri",  "/z/" + zu + "/" + Text.escapeIllegalJcrChars("z*.") + "/" + Text.escapeIllegalJcrChars("z*.ri"));

        for (String uid : m.keySet()) {
            u = (UserImpl) uMgr.createUser(uid, uid);
            save(s);
            assertEquals(usersPath + m.get(uid), u.getNode().getPath());

            Authorizable ath = uMgr.getAuthorizable(uid);
            assertNotNull("User with id " + uid + " must exist.", ath);
            assertFalse("User with id " + uid + " must not be a group.", ath.isGroup());
        }

        // test for groups as well
        GroupImpl gr = (GroupImpl) uMgr.createGroup(new TestPrincipal("z[x]"));
        save(s);
        // remember the z-folder for later removal
        toRemove.add((NodeImpl) gr.getNode().getParent().getParent());

        assertEquals("z[x]", gr.getID());
        String expectedPath = groupsPath + "/z/" + Text.escapeIllegalJcrChars("z[") + "/" + Text.escapeIllegalJcrChars("z[x]");
        assertEquals(expectedPath, gr.getNode().getPath());

        Authorizable ath = uMgr.getAuthorizable(gr.getID());
        assertNotNull(ath);
        assertTrue(ath.isGroup());

        // test if conflicting authorizables are detected.
        try {
            uMgr.createUser("z[x]", "z[x]");
            save(s);
            fail("A group \"z[x]\" already exists.");
        } catch (AuthorizableExistsException e) {
            // success
        }

        try {
            uMgr.createGroup(new TestPrincipal("z*rik"));
            save(s);
            fail("A user \"z*rik\" already exists");
        } catch (AuthorizableExistsException e) {
            // success
        }
    }

    /**
     * If auto-expand is true users must be distributed over more than default-depth
     * levels if max-size is reached.
     * In addition the special cases must be respected (see DefaultIdResolver).
     *
     * @throws RepositoryException
     */
    public void testAutoExpand() throws RepositoryException, NotExecutableException {
        createUserManager(2, true, 5);

        UserImpl u = (UserImpl) uMgr.createUser("z", "z");
        save(s);

        // remember the z-folder for later removal
        toRemove.add((NodeImpl) u.getNode().getParent().getParent());
        assertEquals(usersPath + "/z/zz/z", u.getNode().getPath());

        Map<String, String> m = new ListOrderedMap();
        m.put("zz", "/z/zz/zz");
        // zzz -> potential conflict: must be added to 3rd level
        m.put("zzz", "/z/zz/zzz/zzz");

        // more users -> added to 2nd level until max-size (5) is reached.
        m.put("zzABC", "/z/zz/zzABC");
        m.put("zzzh", "/z/zz/zzzh");

        // max-size on level 2 (zz) is reached -> added to 3rd level.
        m.put("zzzzZ", "/z/zz/zzz/zzzzZ");
        m.put("zzh", "/z/zz/zzh/zzh");
        m.put("zzXyzzz", "/z/zz/zzX/zzXyzzz");

        // zzzz, zzza -> potential conflicts on the 3rd level
        // -> must be added to 4th level
        m.put("zzzz", "/z/zz/zzz/zzzz/zzzz");
        m.put("zzza", "/z/zz/zzz/zzza/zzza");


        // zA -> to short for 3rd -> must be inserted at the 2nd level.
        m.put("zA", "/z/zA/zA");

        for (String uid : m.keySet()) {
            u = (UserImpl) uMgr.createUser(uid, uid);
            save(s);
            assertEquals(usersPath + m.get(uid), u.getNode().getPath());
        }
    }

    /**
     * Test special case of turning autoexpandtree option on having colliding
     * authorizables already present a leve N: In this case auto-expansion must
     * be aborted at that level and the authorizable will be create at level N
     * ignoring that max-size has been reached.
     *
     * @throws RepositoryException
     */
    public void testConflictUponChangingAutoExpandFlag() throws RepositoryException, NotExecutableException {
        createUserManager(2, false, 1);

        UserImpl u = (UserImpl) uMgr.createUser("zzz", "zzz");
        save(s);

        // remember the z-folder for later removal
        toRemove.add((NodeImpl) u.getNode().getParent().getParent());

        assertEquals(usersPath + "/z/zz/zzz", u.getNode().getPath());

        // now create a second user manager that has auto-expand-tree enabled
        createUserManager(2, true, 1);


        Map<String, String> m = new ListOrderedMap();
        // upon creation of any a new user 'zzzA' an additional intermediate
        // folder would be created if there wasn't the colliding authorizable
        // 'zzz' -> autoexpansion is aborted.
        m.put("zzzA", "/z/zz/zzzA");
        // this is also true for 'zzzzz' and zzzBsl
        m.put("zzzzz", "/z/zz/zzzzz");
        m.put("zzzBsl", "/z/zz/zzzBsl");

        // on other levels the expansion must still work as expected.
        // - zzBsl -> zz is completed -> create zzB -> insert zzBsl user
        // - zzBslrich -> zz, zzB are completed -> create zzBs -> insert zzBslrich user
        m.put("zzBsl", "/z/zz/zzB/zzBsl");
        m.put("zzBslrich", "/z/zz/zzB/zzBs/zzBslrich");

        for (String uid : m.keySet()) {
            u = (UserImpl) uMgr.createUser(uid, uid);
            save(s);

            assertEquals(usersPath + m.get(uid), u.getNode().getPath());
            assertNotNull(uMgr.getAuthorizable(uid));
        }
    }

    /**
     * Find by ID must succeed.
     *
     * @throws RepositoryException
     */
    public void testFindById() throws RepositoryException, NotExecutableException {
        createUserManager(2, true, 2);

        UserImpl u = (UserImpl) uMgr.createUser("z", "z");
        save(s);

        // remember the z-folder for later removal
        toRemove.add((NodeImpl) u.getNode().getParent().getParent());
        assertEquals(usersPath + "/z/zz/z", u.getNode().getPath());

        Map<String, String> m = new ListOrderedMap();
        // potential conflicting uid
        m.put("zzz", "/z/zz/zzz/zzz");
        // max-size (2) is reached
        m.put("zzzuerich", "/z/zz/zzz/zzzuerich");
        m.put("zzuerich", "/z/zz/zzu/zzuerich");
        // too short for expanded folders
        m.put("zz", "/z/zz/zz");

        for (String uid : m.keySet()) {
            u = (UserImpl) uMgr.createUser(uid, uid);
            save(s);

            assertEquals(usersPath + m.get(uid), u.getNode().getPath());

            User us = (User) uMgr.getAuthorizable(uid);
            assertNotNull(us);
            assertEquals(uid, us.getID());
        }
    }

    public void testIdIsCaseSensitive() throws RepositoryException, NotExecutableException {
        createUserManager(2, true, 2);

        UserImpl u = (UserImpl) uMgr.createUser("ZuRiCh", "z");
        save(s);

        // remember the z-folder for later removal
        toRemove.add((NodeImpl) u.getNode().getParent().getParent());

        assertEquals("ZuRiCh", u.getID());
    }

    public void testUUIDIsBuildCaseInsensitive() throws RepositoryException, NotExecutableException {
        createUserManager(2, true, 2);

        UserImpl u = (UserImpl) uMgr.createUser("ZuRiCh", "z");
        save(s);

        // remember the z-folder for later removal
        toRemove.add((NodeImpl) u.getNode().getParent().getParent());

        try {
            User u2 = uMgr.createUser("zurich", "z");
            fail("uuid is built from insensitive userID -> must conflict");
        } catch (AuthorizableExistsException e) {
            // success
        }
    }
}