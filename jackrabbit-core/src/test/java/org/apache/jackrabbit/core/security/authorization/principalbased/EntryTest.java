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

import org.apache.jackrabbit.api.jsr283.security.AccessControlException;
import org.apache.jackrabbit.api.jsr283.security.Privilege;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.security.authorization.AbstractEntryTest;
import org.apache.jackrabbit.core.security.authorization.JackrabbitAccessControlEntry;
import org.apache.jackrabbit.value.StringValue;
import org.apache.jackrabbit.value.BooleanValue;
import org.apache.jackrabbit.test.NotExecutableException;
import org.apache.jackrabbit.spi.commons.conversion.NameResolver;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * <code>EntryTest</code>...
 */
public class EntryTest extends AbstractEntryTest {

    private Map restrictions;
    private ACLTemplate acl;

    private String nodePath;
    private String glob;

    protected void setUp() throws Exception {
        super.setUp();

        if (superuser instanceof NameResolver) {
            NameResolver resolver = (NameResolver) superuser;
            nodePath = resolver.getJCRName(ACLTemplate.P_NODE_PATH);
            glob = resolver.getJCRName(ACLTemplate.P_GLOB);
        } else {
            throw new NotExecutableException();
        }

        restrictions = new HashMap(2);
        restrictions.put(nodePath, superuser.getValueFactory().createValue("/a/b/c/d", PropertyType.PATH));
        restrictions.put(glob,  superuser.getValueFactory().createValue("*"));
        acl = new ACLTemplate(testPrincipal, testPath, (SessionImpl) superuser, superuser.getValueFactory());
    }

    protected JackrabbitAccessControlEntry createEntry(Principal principal, Privilege[] privileges, boolean isAllow)
            throws RepositoryException {
        return (JackrabbitAccessControlEntry) acl.createEntry(principal, privileges, isAllow, restrictions);
    }

    private JackrabbitAccessControlEntry createEntry(Principal principal, Privilege[] privileges, boolean isAllow, Map restrictions)
            throws RepositoryException {
        return (JackrabbitAccessControlEntry) acl.createEntry(principal, privileges, isAllow, restrictions);
    }

    public void testNodePathMustNotBeNull() throws RepositoryException, NotExecutableException {
        try {
            Privilege[] privs = privilegesFromName(Privilege.JCR_ALL);
            createEntry(testPrincipal, privs, true, Collections.EMPTY_MAP);
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

        Map restr = new HashMap();
        restr.put(nodePath,  restrictions.get(nodePath));
        pe = createEntry(testPrincipal, privs, true, restr);
        assertNull(pe.getRestriction(glob));

        restr = new HashMap();
        restr.put(nodePath,  restrictions.get(nodePath));
        restr.put(glob,  new StringValue(""));

        pe = createEntry(testPrincipal, privs, true, restr);
        assertEquals("", pe.getRestriction(glob).getString());

        restr = new HashMap();
        restr.put(nodePath,  restrictions.get(nodePath));
        restr.put(glob,  new BooleanValue(true));
        assertEquals(PropertyType.STRING, pe.getRestriction(glob).getType());
    }

    public void testTypeConversion() throws RepositoryException, NotExecutableException {
        // ACLTemplate impl tries to convert the property types if the don't
        // match the required ones.
        Privilege[] privs = privilegesFromName(Privilege.JCR_ALL);

        Map restr = new HashMap();
        restr.put(nodePath, new StringValue("/a/b/c/d"));
        JackrabbitAccessControlEntry pe = createEntry(testPrincipal, privs, true, restr);

        assertEquals("/a/b/c/d", pe.getRestriction(nodePath).getString());
        assertEquals(PropertyType.PATH, pe.getRestriction(nodePath).getType());

        restr = new HashMap();
        restr.put(nodePath,  restrictions.get(nodePath));
        restr.put(glob,  new BooleanValue(true));
        pe = createEntry(testPrincipal, privs, true, restr);

        assertEquals(true, pe.getRestriction(glob).getBoolean());
        assertEquals(PropertyType.STRING, pe.getRestriction(glob).getType());
    }

    public void testMatches() throws RepositoryException {
        Privilege[] privs = new Privilege[] {acMgr.privilegeFromName(Privilege.JCR_ALL)};
        ACLTemplate.Entry ace = (ACLTemplate.Entry) createEntry(testPrincipal, privs, true);

        String nPath = ((Value) restrictions.get(nodePath)).getString();
        List toMatch = new ArrayList();
        toMatch.add(nPath + "/any");
        toMatch.add(nPath + "/anyother");
        toMatch.add(nPath + "/f/g/h");
        toMatch.add(nPath);
        for (Iterator it = toMatch.iterator(); it.hasNext();) {
            String str = it.next().toString();
            assertTrue("Restrictions should match " + str, ace.matches(str));
        }

        List notToMatch = new ArrayList();
        notToMatch.add(null);
        notToMatch.add("");
        notToMatch.add("/");
        notToMatch.add("/a/b/c/");
        for (Iterator it = notToMatch.iterator(); it.hasNext();) {
            Object obj = it.next();
            String str = (obj == null) ? null : obj.toString();
            assertFalse("Restrictions shouldn't match " + str, ace.matches(str));
        }
    }
}