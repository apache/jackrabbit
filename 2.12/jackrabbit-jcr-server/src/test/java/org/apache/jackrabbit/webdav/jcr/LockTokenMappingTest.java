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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;

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
     * Minimal Lock impl for tests above
     */
    private static class TestLock implements Lock {

        private final String token;

        public TestLock(String token) {
            this.token = token;
        }

        public String getLockOwner() {
            return null;
        }

        public boolean isDeep() {
            return false;
        }

        public Node getNode() {
            return null;
        }

        public String getLockToken() {
            return token;
        }

        public long getSecondsRemaining() throws RepositoryException {
            return 0;
        }

        public boolean isLive() throws RepositoryException {
            return false;
        }

        public boolean isSessionScoped() {
            return false;
        }

        public boolean isLockOwningSession() {
            return false;
        }

        public void refresh() throws LockException, RepositoryException {
        }
    }
}
