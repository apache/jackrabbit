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
package org.apache.jackrabbit.core.security.authentication.token;

import java.lang.Thread.UncaughtExceptionHandler;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.authentication.token.TokenCredentials;
import org.apache.jackrabbit.api.security.principal.ItemBasedPrincipal;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.test.AbstractJCRTest;
import org.apache.jackrabbit.test.NotExecutableException;
import org.apache.jackrabbit.test.RepositoryStub;

/**
 * <code>TokenBasedLoginTest</code>...
 */
public class TokenBasedLoginTest extends AbstractJCRTest {

    private static final String TOKENS_NAME = ".tokens";
    private static final String TOKEN_ATTRIBUTE = TokenBasedAuthentication.TOKEN_ATTRIBUTE;

    private User testuser;
    private String testuserPath;
    private SimpleCredentials creds;

    private boolean doSave;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        if (superuser instanceof JackrabbitSession) {
            UserManager umgr = ((JackrabbitSession) superuser).getUserManager();
            String uid = "test";
            while (umgr.getAuthorizable(uid) != null) {
                uid += "_";
            }

            testuser = umgr.createUser(uid, uid);
            Principal p = testuser.getPrincipal();
            if (p instanceof ItemBasedPrincipal) {
                testuserPath = ((ItemBasedPrincipal) p).getPath();
            } else {
                throw new NotExecutableException();
            }

            creds = new SimpleCredentials(uid, uid.toCharArray());

            if (!umgr.isAutoSave()) {
                doSave = true;
                superuser.save();
            }
        } else {
            throw new NotExecutableException();
        }
    }

    @Override
    protected void tearDown() throws Exception {
        if (testuser != null) {
            testuser.remove();
            if (doSave) {
                superuser.save();
            }
        }
        super.tearDown();
    }

    public void testLogin() throws RepositoryException {
        Repository repo = getHelper().getRepository();

        // make sure regular simple login works.
        Session s = repo.login(creds);
        s.logout();

        // test if token creation works.
        creds.setAttribute(TOKEN_ATTRIBUTE, "");
        // an additional attribute that must match
        creds.setAttribute(TOKEN_ATTRIBUTE + ".any", "any");
        // an attribute just for info purposes
        creds.setAttribute("attr", "attr");

        String token = null;

        s = repo.login(creds);
        try {
            // token credentials must be created
            Set<TokenCredentials> tokenCreds = ((SessionImpl) s).getSubject().getPublicCredentials(TokenCredentials.class);
            assertFalse(tokenCreds.isEmpty());
            assertEquals(1, tokenCreds.size());

            TokenCredentials tc = tokenCreds.iterator().next();          
            token = tc.getToken();

            // original simple credentials: token attribute should be updated
            assertNotNull(creds.getAttribute(TOKEN_ATTRIBUTE));
            assertFalse("".equals(creds.getAttribute(TOKEN_ATTRIBUTE)));

            // simple credentials must also be present on the subject
            Set<SimpleCredentials> scs = ((SessionImpl) s).getSubject().getPublicCredentials(SimpleCredentials.class);
            assertFalse(scs.isEmpty());
            assertEquals(1, scs.size());
            SimpleCredentials sc = scs.iterator().next();
            assertNotNull(sc.getAttribute(TOKEN_ATTRIBUTE));
            assertFalse("".equals(sc.getAttribute(TOKEN_ATTRIBUTE)));

            // test if session attributes only exposed non-mandatory attributes
            assertNull(s.getAttribute(TOKEN_ATTRIBUTE));
            for (String attrName : tc.getAttributeNames()) {
                if (TokenBasedAuthentication.isMandatoryAttribute(attrName)) {
                    assertNull(s.getAttribute(attrName));
                } else {
                    assertEquals(tc.getAttribute(attrName), s.getAttribute(attrName));
                }
            }

            // only test node characteristics if user-node resided within the same
            // workspace as 'superuser' has been created for.
            if (superuser.nodeExists(testuserPath)) {
                Node userNode = superuser.getNode(testuserPath);

                assertTrue(userNode.hasNode(TOKENS_NAME));

                Node tNode = userNode.getNode(TOKENS_NAME);
                assertTrue(tNode.hasNodes());

                Node ttNode = tNode.getNodes().nextNode();
                assertTrue(ttNode.hasProperty("attr"));
                assertEquals("attr", ttNode.getProperty("attr").getString());

                assertTrue(ttNode.hasProperty(TOKEN_ATTRIBUTE + ".any"));
                assertEquals("any", ttNode.getProperty(TOKEN_ATTRIBUTE + ".any").getString());

                String id = ttNode.getIdentifier();
                assertTrue(token.startsWith(id));
            }

        } finally {
            s.logout();
        }

        // login with token only must succeed as well.
        TokenCredentials tokenOnly = new TokenCredentials(token);
        tokenOnly.setAttribute(TOKEN_ATTRIBUTE + ".any", "any");

        s = repo.login(tokenOnly);
        try {
            assertEquals(creds.getUserID(), s.getUserID());

            Set<TokenCredentials> tokenCreds = ((SessionImpl) s).getSubject().getPublicCredentials(TokenCredentials.class);
            assertFalse(tokenCreds.isEmpty());
            assertEquals(1, tokenCreds.size());

            TokenCredentials tc = tokenCreds.iterator().next();
            String tk = tc.getToken();
            assertEquals(token, tk);

            assertNull(s.getAttribute(TOKEN_ATTRIBUTE));
            for (String attrName : tc.getAttributeNames()) {
                if (TokenBasedAuthentication.isMandatoryAttribute(attrName)) {
                    assertNull(s.getAttribute(attrName));
                } else {
                    assertEquals(tc.getAttribute(attrName), s.getAttribute(attrName));
                }
            }

        } finally {
            s.logout();
        }

        // the non-mandatory attribute may have any value if present with the creds.
        tokenOnly.setAttribute("attr", "another");
        s = repo.login(tokenOnly);
        try {
            assertEquals(creds.getUserID(), s.getUserID());
        } finally {
            s.logout();
            tokenOnly.removeAttribute("attr");
        }

        // login with token but wrong mandatory attribute
        tokenOnly.setAttribute(TOKEN_ATTRIBUTE + ".any", "another");
        try {
            s = repo.login(tokenOnly);
            s.logout();
            fail("The additional mandatory attr doesn't match. login must fail.");
        } catch (LoginException e) {
            // success
        }

        // login with token but missing the mandatory attribute
        tokenOnly.removeAttribute(TOKEN_ATTRIBUTE + ".any");
        try {
            s = repo.login(tokenOnly);
            s.logout();
            fail("The additional mandatory attr is missing. login must fail.");
        } catch (LoginException e) {
            // success
        }
    }

    /**
     * Tests concurrent login on the Repository including token creation.
     * Test copied and slightly adjusted from org.apache.jackrabbit.core.ConcurrentLoginTest
     */
    public void testConcurrentLogin() throws Throwable {
        assertParallelExecutionSucceeds(10, new JcrRunnable() {
            public void run() throws RepositoryException {
                for (int i = 0; i < 100; i++) {
                    SimpleCredentials sc = new SimpleCredentials(testuser.getID(), testuser.getID().toCharArray());
                    sc.setAttribute(TokenBasedAuthentication.TOKEN_ATTRIBUTE, "");

                    Session s = getHelper().getRepository().login(sc);
                    try {
                        Set<TokenCredentials> tcs = ((SessionImpl) s).getSubject().getPublicCredentials(TokenCredentials.class);
                        assertFalse(tcs.isEmpty());
                    } finally {
                        s.logout();
                    }
                }
            }
        });

    }

    /**
     * Tests concurrent login of 3 different users on the Repository including
     * token creation.
     * Test copied and slightly adjusted from org.apache.jackrabbit.core.ConcurrentLoginTest
     * @throws InterruptedException 
     */
    public void testConcurrentLoginOfDifferentUsers() throws InterruptedException {
        assertParallelExecutionSucceeds(10, new JcrRunnable() {
            public void run() throws RepositoryException {
                for (int i = 0; i < 100; i++) {
                    SimpleCredentials c;
                    double rand = 3 * Math.random();
                    int index = (int) Math.floor(rand);
                    switch (index) {
                        case 0:
                            c = new SimpleCredentials(testuser.getID(), testuser.getID().toCharArray());
                            break;
                        case 1:
                            c = new SimpleCredentials(getHelper().getProperty(RepositoryStub.PROP_PREFIX + "." + RepositoryStub.PROP_SUPERUSER_NAME), getHelper().getProperty(RepositoryStub.PROP_PREFIX + "." + RepositoryStub.PROP_SUPERUSER_PWD).toCharArray());
                            break;
                        default:
                            c = new SimpleCredentials(getHelper().getProperty(RepositoryStub.PROP_PREFIX + "." + RepositoryStub.PROP_READONLY_NAME), getHelper().getProperty(RepositoryStub.PROP_PREFIX + "." + RepositoryStub.PROP_READONLY_PWD).toCharArray());
                            break;
                    }
                    c.setAttribute(TokenBasedAuthentication.TOKEN_ATTRIBUTE, "");
                    Session s = getHelper().getRepository().login(c);
                    try {
                        Set<TokenCredentials> tcs = ((SessionImpl) s).getSubject().getPublicCredentials(TokenCredentials.class);
                        assertFalse(tcs.isEmpty());
                    } finally {
                        s.logout();
                    }
                }
            }
        });
    }

    /**
     * Tests concurrent login on the Repository including token creation.
     * Test copied and slightly adjusted from org.apache.jackrabbit.core.ConcurrentLoginTest
     * @throws InterruptedException 
     */
    public void testConcurrentLoginDifferentWorkspaces() throws RepositoryException, NotExecutableException, InterruptedException {
        final String testID = testuser.getID();

        // check if test is executable
        // - multiple workspaces must be present
        final List<String> wspNames = Arrays.asList(superuser.getWorkspace().getAccessibleWorkspaceNames());
        if (wspNames.size() <= 1) {
            throw new NotExecutableException();
        }
        // - testuser must be present for all workspaces
        for (String wspName : wspNames) {
            JackrabbitSession s = null;
            try {
                s = (JackrabbitSession) getHelper().getSuperuserSession(wspName);
                if (s.getUserManager().getAuthorizable(testID) == null) {
                    throw new NotExecutableException();
                }
            } finally {
                if (s != null) {
                    s.logout();
                }
            }
        }

        assertParallelExecutionSucceeds(10, new JcrRunnable() {
            public void run() throws RepositoryException {
                for (int i = 0; i < 100; i++) {
                    double rand = wspNames.size() * Math.random();
                    int index = (int) Math.floor(rand);
                    String wspName = wspNames.get(index);

                    SimpleCredentials sc = new SimpleCredentials(testID, testID.toCharArray());
                    sc.setAttribute(TokenBasedAuthentication.TOKEN_ATTRIBUTE, "");

                    Session s = getHelper().getRepository().login(sc, wspName);
                    try {
                        Set<TokenCredentials> tcs = ((SessionImpl) s).getSubject().getPublicCredentials(TokenCredentials.class);
                        assertFalse(tcs.isEmpty());
                    } finally {
                        s.logout();
                    }
                }
            }
        });
    }

    static class UncheckedRepositoryException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public UncheckedRepositoryException(RepositoryException e) {
            super(e);
        }
    }

    /**
     * Similar to {@link Runnable} but allows to throw {@link RepositoryException} in its {@link #run()} method.
     */
    static interface JcrRunnable {
        public void run() throws RepositoryException;
    }

    private static void assertParallelExecutionSucceeds(int numThreads, JcrRunnable runnable) throws InterruptedException {
        List<Thread> testRunner = new ArrayList<Thread>();
        for (int i = 0; i < numThreads; i++) {
            testRunner.add(new Thread(new Runnable() {
                public void run() {
                    try {
                        runnable.run();
                    } catch (RepositoryException e) {
                        throw new UncheckedRepositoryException(e);
                    }
                }
            }));
        }
        assertThreadExecutionSucceeds(testRunner);
    }

    /**
     * Executes all given threads and waits for them to finish. Exception in any of the thread will make the calling thread die with an exception as well
     * @param testRunner the threads to execute
     * @throws InterruptedException
     */
    private static void assertThreadExecutionSucceeds(Collection<Thread> testRunner) throws InterruptedException {
        Map<String, Throwable> threadExceptions = new ConcurrentHashMap<>();
        // start threads
        for (Thread aTestRunner : testRunner) {
            aTestRunner.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
                public void uncaughtException(Thread t, Throwable e) {
                    threadExceptions.put(t.getName() + " (id " + t.getId() +")", e);
                }
            } );
            aTestRunner.start();
        }

        // join threads
        for (Thread aTestRunner : testRunner) {
            aTestRunner.join();
        }

        // only rethrow one exception (put the other ones as suppressed)
        IllegalStateException mainException = threadExceptions.entrySet().stream().findFirst().map(e -> new IllegalStateException("Thread " + e.getKey() + " failed", e.getValue())).orElse(null);
        if (mainException != null) {
            threadExceptions.entrySet().stream().skip(1).forEach(e -> mainException.addSuppressed(new IllegalStateException("Thread " + e.getKey() + " failed", e.getValue())));
            throw mainException;
        }
    }
}