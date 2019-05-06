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
package org.apache.jackrabbit.core.security.principal;

import junit.framework.TestCase;
import org.apache.jackrabbit.api.security.principal.PrincipalIterator;
import org.apache.jackrabbit.test.NotExecutableException;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.security.Principal;
import java.util.Properties;

/**
 * <code>AbstractPrincipalProviderTest</code>...
 */
public class AbstractPrincipalProviderTest extends TestCase {

    public void testNegativeCacheEntries() throws RepositoryException, NotExecutableException {
        String unknownName = "UnknownPrincipal";

        PrincipalProvider caching = new DummyProvider();
        Properties options = new Properties();
        options.setProperty(DefaultPrincipalProvider.NEGATIVE_ENTRY_KEY, "true");
        caching.init(options);

        // accessing from wrapper must not throw! as negative entry is expected
        // to be in the cache (default behavior of the DefaultPrincipalProvider)
        assertNull(caching.getPrincipal(unknownName));
        assertNull(caching.getPrincipal(unknownName));

        PrincipalProvider throwing = new DummyProvider();
        options = new Properties();
        options.setProperty(DefaultPrincipalProvider.NEGATIVE_ENTRY_KEY, "false");
        throwing.init(options);

        // however: the noNegativeCacheProvider configured NOT to cache null-results
        // is expected to call 'providePrincipal' for each call to 'getPrincipal'
        // with a principalName that doesn't exist.
        assertNull(throwing.getPrincipal(unknownName));
        try {
            throwing.getPrincipal(unknownName);
            fail("exception expected");
        } catch (UnsupportedOperationException e) {
            // success
        }
    }

    private class DummyProvider extends AbstractPrincipalProvider {

        private boolean first = true;
        @Override
        protected Principal providePrincipal(String principalName) {
            if (first) {
                first = false;
                return null;
            } else {
                throw new UnsupportedOperationException();
            }
        }
        public PrincipalIterator findPrincipals(String simpleFilter) {
            throw new UnsupportedOperationException();
        }

        public PrincipalIterator findPrincipals(String simpleFilter, int searchType) {
            throw new UnsupportedOperationException();
        }

        public PrincipalIterator getPrincipals(int searchType) {
            throw new UnsupportedOperationException();
        }

        public PrincipalIterator getGroupMembership(Principal principal) {
            throw new UnsupportedOperationException();
        }

        public boolean canReadPrincipal(Session session, Principal principalToRead) {
            return true;
        }
    }
}