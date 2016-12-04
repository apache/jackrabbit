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
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

import javax.jcr.InvalidItemStateException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.SimpleCredentials;

import org.apache.commons.io.FileUtils;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.apache.jackrabbit.test.JUnitTest;

/**
 * Performance test for JCR-3892.
 */
public class MembershipCachePerfTest extends JUnitTest {

    private static final String TEST_USER_PREFIX = "MembershipCacheTestUser-";
    private static final String TEST_GROUP_PREFIX = "MembershipCacheTestGroup-";
    private static final String REPO_HOME = new File("target",
            MembershipCachePerfTest.class.getSimpleName()).getPath();
    private static final int NUM_USERS = 10000;
    private static final int NUM_USERS_PER_GROUP = 5000;
    private static final int NUM_GROUPS = 300;
    private static final int NUM_READERS = 8;
    private static final int NUM_WRITERS = 8;

    private static final int TIME_TEST = 20000;
    private static final int TIME_RAMP_UP = 1000;


    private RepositoryImpl repo;
    private JackrabbitSession session;
    private UserManager userMgr;
    private MembershipCache cache;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        FileUtils.deleteDirectory(new File(REPO_HOME));
        RepositoryConfig config = RepositoryConfig.create(
                getClass().getResourceAsStream("repository-membersplit.xml"), REPO_HOME);
        repo = RepositoryImpl.create(config);
        session = createSession();
        userMgr = session.getUserManager();
        cache = ((UserManagerImpl) userMgr).getMembershipCache();
        boolean autoSave = userMgr.isAutoSave();
        userMgr.autoSave(false);
        // create test users and groups
        System.out.printf("Creating %d users...\n", NUM_USERS);
        List<User> users = new ArrayList<User>();
        for (int i = 0; i < NUM_USERS; i++) {
            users.add(userMgr.createUser(TEST_USER_PREFIX + i, "secret"));
        }
        System.out.printf("Creating %d groups...\n", NUM_GROUPS);
        for (int i = 0; i < NUM_GROUPS; i++) {
            Group g = userMgr.createGroup(TEST_GROUP_PREFIX + i);
            for (int j=0; j<NUM_USERS_PER_GROUP; j++) {
                g.addMember(users.get(j));
            }
            session.save();
            System.out.printf(".").flush();
        }
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
            userMgr.getAuthorizable(TEST_GROUP_PREFIX + i).remove();
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

    public void testInvalidationPerformance() throws Exception {
        List<Exception> exceptions = Collections.synchronizedList(new ArrayList<Exception>());
        List<Reader> readers = new ArrayList<Reader>();
        Stats readerStats = new Stats();
        for (int i = 0; i < NUM_READERS; i++) {
            Reader r = new Reader(createSession(), readerStats, exceptions);
            readers.add(r);
        }

        List<Writer> writers = new ArrayList<Writer>();
        Stats writerStats = new Stats();
        for (int i = 0; i < NUM_WRITERS; i++) {
            Writer w = new Writer(createSession(), writerStats, exceptions);
            writers.add(w);
        }

        Node test = session.getRootNode().addNode("test", "nt:unstructured");
        session.save();

        for (Reader r : readers) {
            r.start();
        }

        // invalidate stats after ramp-up
        Thread.sleep(TIME_RAMP_UP);
        cache.clear();
        readerStats.clear();

        // start writers
        for (Writer w : writers) {
            w.start();
        }

        long endTime = System.currentTimeMillis() + TIME_TEST;
        while (System.currentTimeMillis() < endTime) {
            Thread.sleep(1000);
            System.out.printf("running...current cache size: %d\n", cache.getSize());
        }

        for (Reader r : readers) {
            r.setRunning(false);
        }
        for (Writer w : writers) {
            w.setRunning(false);
        }

        for (Reader r : readers) {
            r.join();
        }
        for (Writer w : writers) {
            w.join();
        }

        test.remove();
        session.save();

        System.out.printf("-----------------------------------------------\n");
        System.out.printf("Test time: %d, Ramp-up time %d\n", TIME_TEST, TIME_RAMP_UP);
        System.out.printf("Number of users: %d\n", NUM_USERS);
        System.out.printf("Avg number of users/group: %d\n", NUM_USERS_PER_GROUP);
        System.out.printf("Number of groups: %d\n", NUM_GROUPS);
        System.out.printf("Number of readers: %d\n", NUM_READERS);
        System.out.printf("Number of writers: %d\n", NUM_WRITERS);
        System.out.printf("Cache size: %d\n", cache.getSize());
        System.out.printf("Time to get memberships:\n");
        readerStats.printResults(System.out);
        System.out.printf("-----------------------------------------------\n");
        System.out.printf("Time to alter memberships:\n");
        writerStats.printResults(System.out);
        System.out.printf("-----------------------------------------------\n");

        for (Exception e : exceptions) {
            throw e;
        }
        logger.info("cache size: " + cache.getSize());
    }

    private JackrabbitSession createSession() throws RepositoryException {
        return (JackrabbitSession) repo.login(
                new SimpleCredentials("admin", "admin".toCharArray()));
    }

    private static final class Reader extends Thread {

        private final JackrabbitSession session;
        private final UserManager userMgr;
        private final Stats stats;
        private final Random random = new Random();
        private final List<Exception> exceptions;

        private boolean running = true;

        public Reader(JackrabbitSession s,
                      Stats stats,
                      List<Exception> exceptions)
                throws RepositoryException {
            this.session = s;
            this.userMgr = s.getUserManager();
            this.stats = stats;
            this.exceptions = exceptions;
        }

        public void setRunning(boolean running) {
            this.running = running;
        }

        public void run() {
            try {
                while (running) {
                    int idx = random.nextInt(NUM_USERS);
                    Authorizable user = userMgr.getAuthorizable(TEST_USER_PREFIX + idx);
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

    private static final class Writer extends Thread {

        private final JackrabbitSession session;
        private final UserManager userMgr;
        private final Stats stats;
        private final Random random = new Random();
        private final List<Exception> exceptions;

        private boolean running = true;

        public Writer(JackrabbitSession s,
                      Stats stats,
                      List<Exception> exceptions)
                throws RepositoryException {
            this.session = s;
            this.stats = stats;
            this.userMgr = s.getUserManager();
            userMgr.autoSave(false);
            this.exceptions = exceptions;
        }

        public void setRunning(boolean running) {
            this.running = running;
        }

        public void run() {
            try {
                while (running) {
                    int userIdx = random.nextInt(NUM_USERS);
                    int groupIdx = random.nextInt(NUM_GROUPS);
                    User user = (User) userMgr.getAuthorizable(TEST_USER_PREFIX + userIdx);
                    Group group = (Group) userMgr.getAuthorizable(TEST_GROUP_PREFIX + groupIdx);

                    do {
                        long time = System.nanoTime();
                        try {
                            if (group.isDeclaredMember(user)) {
                                group.removeMember(user);
                            } else {
                                group.addMember(user);
                            }
                            session.save();
                            stats.logTime(System.nanoTime() - time);
                            break;
                        } catch (InvalidItemStateException e) {
                            // concurrent writing...try again
                            session.refresh(false);
                        }
                    } while (running);
                    Thread.sleep(10);
                }
            } catch (RepositoryException e) {
                exceptions.add(e);
            } catch (InterruptedException e) {
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

        void clear() {
            for (AtomicLong b: buckets) {
                b.set(0);
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

        public void printResults(PrintStream out) {
            long total = 0;
            long last = 0;
            for (int power = 0; power<buckets.length; power++) {
                long value = buckets[power].get();
                total += value;
                if (value > 0) {
                    last = power;
                }
            }
            if (last == 0) {
                last = buckets.length - 1;
            }

            String[] units = {"ns", "10ns", "100ns", "1us", "10us", "100us", "1ms", "10ms", "100ms"};
            for (int power = 0; power<=last; power++) {
                long value = buckets[power].get();
                String unit = power < units.length ? units[power] : Math.pow(10, power-units.length) + "s";
                out.printf("%-6s: %2.2f%% (%d)\n", unit, 100.0 * (double) value / (double) total, value);
            }
        }

    }


}
