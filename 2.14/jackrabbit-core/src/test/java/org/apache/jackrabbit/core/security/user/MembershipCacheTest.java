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

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.SimpleCredentials;

import org.apache.commons.io.FileUtils;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.apache.jackrabbit.test.JUnitTest;

/**
 * Performance test for JCR-3658.
 */
public class MembershipCacheTest extends JUnitTest {

    private static final String TEST_USER_PREFIX = "MembershipCacheTestUser-";
    private static final String REPO_HOME = new File("target",
            MembershipCacheTest.class.getSimpleName()).getPath();
    private static final int NUM_USERS = 100;
    private static final int NUM_GROUPS = 8;
    private static final int NUM_READERS = 8;
    private RepositoryImpl repo;
    private JackrabbitSession session;
    private UserManager userMgr;
    private MembershipCache cache;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        FileUtils.deleteDirectory(new File(REPO_HOME));
        RepositoryConfig config = RepositoryConfig.create(
                getClass().getResourceAsStream("repository.xml"), REPO_HOME);
        repo = RepositoryImpl.create(config);
        session = createSession();
        userMgr = session.getUserManager();
        cache = ((UserManagerImpl) userMgr).getMembershipCache();
        boolean autoSave = userMgr.isAutoSave();
        userMgr.autoSave(false);
        // create test users and groups
        List<User> users = new ArrayList<User>();
        for (int i = 0; i < NUM_USERS; i++) {
            users.add(userMgr.createUser(TEST_USER_PREFIX + i, "secret"));
        }
        for (int i = 0; i < NUM_GROUPS; i++) {
            Group g = userMgr.createGroup("MembershipCacheTestGroup-" + i);
            for (User u : users) {
                g.addMember(u);
            }
        }
        session.save();
        userMgr.autoSave(autoSave);
        logger.info("Initial cache size: " + cache.getSize());
    }

    @Override
    protected void tearDown() throws Exception {
        boolean autoSave = userMgr.isAutoSave();
        userMgr.autoSave(false);
        for (int i = 0; i < NUM_USERS; i++) {
            userMgr.getAuthorizable(TEST_USER_PREFIX + i).remove();
        }
        for (int i = 0; i < NUM_GROUPS; i++) {
            userMgr.getAuthorizable("MembershipCacheTestGroup-" + i).remove();
        }
        session.save();
        userMgr.autoSave(autoSave);
        userMgr = null;
        cache = null;
        session.logout();
        repo.shutdown();
        repo = null;
        FileUtils.deleteDirectory(new File(REPO_HOME));
        super.tearDown();
    }

    public void testConcurrency() throws Exception {
        Stats stats = new Stats();
        List<Exception> exceptions = Collections.synchronizedList(new ArrayList<Exception>());
        List<Reader> readers = new ArrayList<Reader>();
        for (int i = 0; i < NUM_READERS; i++) {
            Reader r = new Reader(createSession(), stats, exceptions);
            r.addUser(TEST_USER_PREFIX + 0);
            readers.add(r);
        }
        Node test = session.getRootNode().addNode("test", "nt:unstructured");
        session.save();
        for (Reader r : readers) {
            r.start();
        }
        for (int i = 1; i < NUM_USERS; i++) {
            test.addNode("node-" + i);
            session.save();
            for (Reader r : readers) {
                r.addUser(TEST_USER_PREFIX + i);
            }
        }
        for (Reader r : readers) {
            r.join();
        }
        test.remove();
        session.save();
        System.out.println(stats);
        for (Exception e : exceptions) {
            throw e;
        }
    }

    public void testRun75() throws Exception {
        for (int i = 0; i < 75; i++) {
            testConcurrency();
            cache.clear();
        }
    }

    private JackrabbitSession createSession() throws RepositoryException {
        return (JackrabbitSession) repo.login(
                new SimpleCredentials("admin", "admin".toCharArray()));
    }

    private static final class Reader extends Thread {

        private final JackrabbitSession session;
        private final UserManager userMgr;
        private final Stats stats;
        private final List<Object> knownUsers = new ArrayList<Object>();
        private final Random random = new Random();
        private final List<Exception> exceptions;

        public Reader(JackrabbitSession s,
                      Stats stats,
                      List<Exception> exceptions)
                throws RepositoryException {
            this.session = s;
            this.userMgr = s.getUserManager();
            this.stats = stats;
            this.exceptions = exceptions;
        }

        void addUser(String user) {
            synchronized (knownUsers) {
                knownUsers.add(user);
            }
        }

        public void run() {
            try {
                while (knownUsers.size() < NUM_USERS) {
                    Object idOrUser;
                    int idx;
                    synchronized (knownUsers) {
                        idx = random.nextInt(knownUsers.size());
                        idOrUser = knownUsers.get(idx);
                    }
                    User user;
                    if (idOrUser instanceof String) {
                        user = (User) userMgr.getAuthorizable((String) idOrUser);
                        synchronized (knownUsers) {
                            knownUsers.set(idx, user);
                        }
                    } else {
                        user = (User) idOrUser;
                    }
                    long time = System.nanoTime();
                    user.memberOf();
                    stats.logTime(System.nanoTime() - time);
                }
            } catch (RepositoryException e) {
                exceptions.add(e);
            } finally {
                session.logout();
            }
        }
    }

    private static final class Stats {

        private AtomicLong[] buckets = new AtomicLong[20];

        public Stats() {
            for (int i = 0; i < buckets.length; i++) {
                buckets[i] = new AtomicLong();
            }
        }

        void logTime(long nanos) {
            if (nanos == 0) {
                buckets[0].incrementAndGet();
            } else {
                buckets[(int) Math.log10(nanos)].incrementAndGet();
            }
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            String separator = "";
            for (AtomicLong bucket : buckets) {
                sb.append(separator);
                sb.append(bucket.get());
                separator = ",";
            }
            return sb.toString();
        }
    }


}
