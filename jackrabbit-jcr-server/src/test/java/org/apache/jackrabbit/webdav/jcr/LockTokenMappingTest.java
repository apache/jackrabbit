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
package org.apache.jackrabbit.webdav.jcr;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.lock.Lock;

import org.apache.jackrabbit.webdav.jcr.lock.LockTokenMapper;

import junit.framework.TestCase;

/**
 * <code>LockTokenMappingTest</code>...
 */
public class LockTokenMappingTest extends TestCase {

    // test lock with a lock token similar to the ones assigned by Jackrabbit
    public void testOpenScopedJcr() throws RepositoryException, URISyntaxException {
        testRoundtrip(UUID.randomUUID().toString() + "-X");
    }

    // test a fancy lock string
    public void testOpenScopedFancy() throws RepositoryException, URISyntaxException {
        testRoundtrip("\n\u00c4 \u20ac");
    }

    // the DAV token of a session-scoped lock must not be derivable from
    // the (world-readable) node identifier
    public void testSessionScopedTokenIsNotDerivedFromNodeId()
            throws RepositoryException, URISyntaxException {
        String nodeId = UUID.randomUUID().toString();
        Lock l = new TestLock(nodeWithIdentifier(nodeId), true);
        String davtoken = LockTokenMapper.getDavLocktoken(l);

        assertNotNull(davtoken);
        assertTrue(LockTokenMapper.isForSessionScopedLock(davtoken));
        assertTrue("token must not contain the node identifier",
                davtoken.indexOf(nodeId) < 0);

        // valid URI?
        URI u = new URI(davtoken);
        assertTrue("lock token must be absolute URI", u.isAbsolute());
        assertEquals("lock token URI must be all-ASCII", u.toASCIIString(), u.toString());

        // stable for the same lock...
        assertEquals(davtoken, LockTokenMapper.getDavLocktoken(l));

        // ...but not reused once the lock is gone
        LockTokenMapper.releaseSessionScopedToken(nodeId);
        String next = LockTokenMapper.getDavLocktoken(l);
        assertFalse("a new lock on the same node must get a fresh token",
                davtoken.equals(next));
    }

    // two session-scoped locks on different nodes must get distinct tokens
    public void testSessionScopedTokensAreDistinct() throws RepositoryException {
        Lock l1 = new TestLock(nodeWithIdentifier(UUID.randomUUID().toString()), true);
        Lock l2 = new TestLock(nodeWithIdentifier(UUID.randomUUID().toString()), true);
        assertFalse(LockTokenMapper.getDavLocktoken(l1).equals(
                LockTokenMapper.getDavLocktoken(l2)));
    }

    private void testRoundtrip(String token) throws RepositoryException, URISyntaxException {

        Lock l = new TestLock(token);
        String davtoken = LockTokenMapper.getDavLocktoken(l);

        // valid URI?
        URI u = new URI(davtoken);
        assertTrue("lock token must be absolute URI", u.isAbsolute());
        assertEquals("lock token URI must be all-ASCII", u.toASCIIString(), u.toString());

        String jcrtoken = LockTokenMapper.getJcrLockToken(davtoken);
        assertEquals(jcrtoken, l.getLockToken());
    }

    /**
     * Returns a minimal {@link Node} that only supports
     * {@link Node#getIdentifier()}.
     */
    private static Node nodeWithIdentifier(final String id) {
        return (Node) Proxy.newProxyInstance(
                LockTokenMappingTest.class.getClassLoader(),
                new Class<?>[] {Node.class},
                new InvocationHandler() {
                    public Object invoke(Object proxy, Method method, Object[] args) {
                        if ("getIdentifier".equals(method.getName())) {
                            return id;
                        }
                        throw new UnsupportedOperationException(method.getName());
                    }
                });
    }

    /**
     * Minimal Lock impl for tests above
     */
    private static class TestLock implements Lock {

        private final String token;
        private final Node node;
        private final boolean sessionScoped;
        private final boolean lockOwningSession;

        public TestLock(String token) {
            this.token = token;
            this.node = null;
            this.sessionScoped = false;
            this.lockOwningSession = false;
        }

        public TestLock(Node node, boolean lockOwningSession) {
            this.token = null;
            this.node = node;
            this.sessionScoped = true;
            this.lockOwningSession = lockOwningSession;
        }

        public String getLockOwner() {
            return null;
        }

        public boolean isDeep() {
            return false;
        }

        public Node getNode() {
            return node;
        }

        public String getLockToken() {
            return token;
        }

        public long getSecondsRemaining() {
            return 0;
        }

        public boolean isLive() {
            return false;
        }

        public boolean isSessionScoped() {
            return sessionScoped;
        }

        public boolean isLockOwningSession() {
            return lockOwningSession;
        }

        public void refresh() {
        }
    }
}