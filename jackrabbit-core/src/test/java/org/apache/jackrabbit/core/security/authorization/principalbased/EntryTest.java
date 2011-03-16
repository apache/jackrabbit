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
package org.apache.jackrabbit.core.security.authorization.principalbased;

import org.apache.jackrabbit.api.security.JackrabbitAccessControlEntry;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.security.authorization.AbstractEntryTest;
import org.apache.jackrabbit.spi.commons.conversion.NameResolver;
import org.apache.jackrabbit.test.NotExecutableException;
import org.apache.jackrabbit.value.BooleanValue;
import org.apache.jackrabbit.value.StringValue;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.security.AccessControlException;
import javax.jcr.security.Privilege;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <code>EntryTest</code>...
 */
public class EntryTest extends AbstractEntryTest {

    private Map<String, Value> restrictions;
    private ACLTemplate acl;

    private String nodePath;
    private String glob;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        if (superuser instanceof NameResolver) {
            NameResolver resolver = (NameResolver) superuser;
            nodePath = resolver.getJCRName(ACLTemplate.P_NODE_PATH);
            glob = resolver.getJCRName(ACLTemplate.P_GLOB);
        } else {
            throw new NotExecutableException();
        }

        restrictions = new HashMap<String, Value>(2);
        restrictions.put(nodePath, superuser.getValueFactory().createValue("/a/b/c/d", PropertyType.PATH));
        restrictions.put(glob,  superuser.getValueFactory().createValue("*"));
        acl = new ACLTemplate(testPrincipal, testPath, (SessionImpl) superuser, superuser.getValueFactory());
    }

    @Override
    protected JackrabbitAccessControlEntry createEntry(Principal principal, Privilege[] privileges, boolean isAllow)
            throws RepositoryException {
        return (JackrabbitAccessControlEntry) acl.createEntry(principal, privileges, isAllow, restrictions);
    }

    @Override
    protected JackrabbitAccessControlEntry createEntry(Principal principal, Privilege[] privileges, boolean isAllow, Map<String, Value> restrictions)
            throws RepositoryException {
        return (JackrabbitAccessControlEntry) acl.createEntry(principal, privileges, isAllow, restrictions);
    }

    @Override
    protected JackrabbitAccessControlEntry createEntryFromBase(JackrabbitAccessControlEntry base, Privilege[] privileges, boolean isAllow) throws RepositoryException, NotExecutableException {
        Map<String, Value> restr = new HashMap<String, Value>();
        for (String name : base.getRestrictionNames()) {
            restr.put(name, base.getRestriction(name));
        }
        return (JackrabbitAccessControlEntry) acl.createEntry(base.getPrincipal(), privileges, isAllow, restr);
    }

    @Override
    protected Map<String, Value> getTestRestrictions() throws RepositoryException {
        return restrictions;
    }

    public void testNodePathMustNotBeNull() throws RepositoryException, NotExecutableException {
        try {
            Privilege[] privs = privilegesFromName(Privilege.JCR_ALL);
            createEntry(testPrincipal, privs, true, Collections.<String, Value>emptyMap());
            fail("NodePath cannot not be null");
        } catch (AccessControlException e) {
            // success
        }
    }

    public void testGetNodePath() throws RepositoryException, NotExecutableException {
        Privilege[] privs = privilegesFromName(Privilege.JCR_ALL);
        JackrabbitAccessControlEntry pe = createEntry(testPrincipal, privs, true);

        assertEquals(restrictions.get(nodePath), pe.getRestriction(nodePath));
        assertEquals(PropertyType.PATH, pe.getRestriction(nodePath).getType());
    }

    public void testGetGlob() throws RepositoryException, NotExecutableException {
        Privilege[] privs = privilegesFromName(Privilege.JCR_ALL);

        JackrabbitAccessControlEntry pe = createEntry(testPrincipal, privs, true);

        assertEquals(restrictions.get(glob), pe.getRestriction(glob));
        assertEquals(PropertyType.STRING, pe.getRestriction(glob).getType());

        Map<String, Value> restr = new HashMap<String, Value>();
        restr.put(nodePath,  restrictions.get(nodePath));
        pe = createEntry(testPrincipal, privs, true, restr);
        assertNull(pe.getRestriction(glob));

        restr = new HashMap<String, Value>();
        restr.put(nodePath,  restrictions.get(nodePath));
        restr.put(glob,  new StringValue(""));

        pe = createEntry(testPrincipal, privs, true, restr);
        assertEquals("", pe.getRestriction(glob).getString());

        restr = new HashMap<String, Value>();
        restr.put(nodePath,  restrictions.get(nodePath));
        restr.put(glob,  new BooleanValue(true));
        assertEquals(PropertyType.STRING, pe.getRestriction(glob).getType());
    }

    public void testTypeConversion() throws RepositoryException, NotExecutableException {
        // ACLTemplate impl tries to convert the property types if the don't
        // match the required ones.
        Privilege[] privs = privilegesFromName(Privilege.JCR_ALL);

        Map<String, Value> restr = new HashMap<String, Value>();
        restr.put(nodePath, new StringValue("/a/b/c/d"));
        JackrabbitAccessControlEntry pe = createEntry(testPrincipal, privs, true, restr);

        assertEquals("/a/b/c/d", pe.getRestriction(nodePath).getString());
        assertEquals(PropertyType.PATH, pe.getRestriction(nodePath).getType());

        restr = new HashMap<String, Value>();
        restr.put(nodePath,  restrictions.get(nodePath));
        restr.put(glob,  new BooleanValue(true));
        pe = createEntry(testPrincipal, privs, true, restr);

        assertEquals(true, pe.getRestriction(glob).getBoolean());
        assertEquals(PropertyType.STRING, pe.getRestriction(glob).getType());
    }

    public void testMatches() throws RepositoryException {
        Privilege[] privs = new Privilege[] {acMgr.privilegeFromName(Privilege.JCR_ALL)};
        ACLTemplate.Entry ace = (ACLTemplate.Entry) createEntry(testPrincipal, privs, true);

        String nPath = restrictions.get(nodePath).getString();
        List<String> toMatch = new ArrayList<String>();
        toMatch.add(nPath + "/any");
        toMatch.add(nPath + "/anyother");
        toMatch.add(nPath + "/f/g/h");
        toMatch.add(nPath);
        for (String str : toMatch) {
            assertTrue("Restrictions should match " + str, ace.matches(str));
        }

        List<String> notToMatch = new ArrayList<String>();
        notToMatch.add(null);
        notToMatch.add("");
        notToMatch.add("/");
        notToMatch.add("/a/b/c/");
        for (String str : notToMatch) {
            assertFalse("Restrictions shouldn't match " + str, ace.matches(str));
        }
    }

    public void testRestrictions() throws RepositoryException {
        // test if restrictions with expanded name are properly resolved
        Map<String, Value> restrictions = new HashMap<String,Value>();
        restrictions.put(ACLTemplate.P_GLOB.toString(), superuser.getValueFactory().createValue("*/test"));
        restrictions.put(ACLTemplate.P_NODE_PATH.toString(), superuser.getValueFactory().createValue("/a/b/c"));

        Privilege[] privs = new Privilege[] {acMgr.privilegeFromName(Privilege.JCR_ALL)};
        ACLTemplate.Entry ace = (ACLTemplate.Entry) createEntry(testPrincipal, privs, true, restrictions);

        Value v = ace.getRestriction(ACLTemplate.P_GLOB.toString());
        Value v2 = ace.getRestriction(glob);
        assertEquals(v, v2);

        v = ace.getRestriction(ACLTemplate.P_NODE_PATH.toString());
        v2 = ace.getRestriction(nodePath);
        assertEquals(v, v2);

        Map<String, Boolean> toMatch = new HashMap<String, Boolean>();
        toMatch.put("/a/b/c", false);
        toMatch.put("/a/b/ctest", false);
        toMatch.put("/a/b/c/test", true);
        toMatch.put("/a/b/c/something/test", true);
        toMatch.put("/a/b/cde/test", true);

        for (String str : toMatch.keySet()) {
            assertEquals("Path to match : " + str, toMatch.get(str).booleanValue(), ace.matches(str));
        }
    }
}