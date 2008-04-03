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
package org.apache.jackrabbit.core.security.authorization.combined;

import org.apache.jackrabbit.core.security.authorization.AbstractPolicyEntryTest;
import org.apache.jackrabbit.core.security.authorization.PolicyEntry;
import org.apache.jackrabbit.core.security.authorization.PrivilegeRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.security.Principal;

/**
 * <code>PolicyEntryImplTest</code>...
 */
public class PolicyEntryImplTest extends AbstractPolicyEntryTest {

    private static Logger log = LoggerFactory.getLogger(PolicyEntryImplTest.class);

    private String nodePath;
    private String glob;

    protected void setUp() throws Exception {
        super.setUp();

        nodePath = "/a/b/c/d";
        glob = "*";
    }

    protected PolicyEntry createPolicyEntry(Principal principal, int privileges, boolean isAllow) {
        return new PolicyEntryImpl(principal, privileges, isAllow, nodePath, glob);
    }

    public void testPrincipalMustNotBeNull() {
        try {
            PolicyEntry pe = new PolicyEntryImpl(null, PrivilegeRegistry.ALL, true, nodePath, glob);
            fail("Principal must not be null");
        } catch (IllegalArgumentException e) {
            // success
        }
    }

    public void testNodePathMustNotBeNull() {
        try {
            PolicyEntry pe = new PolicyEntryImpl(testPrincipal, PrivilegeRegistry.ALL, true, null, glob);
            fail("NodePath must not be null");
        } catch (IllegalArgumentException e) {
            // success
        }
    }

    public void testGetNodePath() {
        PolicyEntryImpl pe = new PolicyEntryImpl(testPrincipal, PrivilegeRegistry.ALL, true, nodePath, glob);
        assertEquals(nodePath, pe.getNodePath());
    }

    public void testGetGlob() {
        PolicyEntryImpl pe = new PolicyEntryImpl(testPrincipal, PrivilegeRegistry.ALL, true, nodePath, glob);
        assertEquals(glob, pe.getGlob());

        pe = new PolicyEntryImpl(testPrincipal, PrivilegeRegistry.ALL, true, nodePath, null);
        assertNull(pe.getGlob());

        pe = new PolicyEntryImpl(testPrincipal, PrivilegeRegistry.ALL, true, nodePath, "");
        assertEquals("", pe.getGlob());
    }

    public void testMatches() throws RepositoryException {
        PolicyEntryImpl pe = new PolicyEntryImpl(testPrincipal,
                PrivilegeRegistry.ALL, true, nodePath, glob);

        // TODO: review again
        List toMatch = new ArrayList();
        toMatch.add(nodePath + "/any");
        toMatch.add(nodePath + "/anyother");
        toMatch.add(nodePath + "/f/g/h");
        toMatch.add(nodePath);
        for (Iterator it = toMatch.iterator(); it.hasNext();) {
            String str = it.next().toString();
            assertTrue(pe.getNodePath() + pe.getGlob() + " should match " + str, pe.matches(str));
        }

        List notToMatch = new ArrayList();
        notToMatch.add(null);
        notToMatch.add("");
        notToMatch.add("/");
        notToMatch.add("/a/b/c/");
        for (Iterator it = notToMatch.iterator(); it.hasNext();) {
            Object obj = it.next();
            String str = (obj == null) ? null : obj.toString();
            assertFalse(pe.getNodePath() + pe.getGlob() + " shouldn't match " + str, pe.matches(str));
        }
    }
}