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
package org.apache.jackrabbit.core.security.authorization.acl;

import java.security.Principal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.security.Privilege;

import org.apache.jackrabbit.api.JackrabbitWorkspace;
import org.apache.jackrabbit.api.security.JackrabbitAccessControlEntry;
import org.apache.jackrabbit.api.security.authorization.PrivilegeManager;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.security.authorization.AbstractEntryTest;
import org.apache.jackrabbit.test.NotExecutableException;

/**
 * <code>EntryTest</code>...
 */
public class ACLTemplateEntryTest extends AbstractEntryTest {

    private ACLTemplate acl;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        SessionImpl s = (SessionImpl) superuser;
        PrivilegeManager privMgr = ((JackrabbitWorkspace) superuser.getWorkspace()).getPrivilegeManager();

        acl = new ACLTemplate(testPath, s.getPrincipalManager(), privMgr, s.getValueFactory(), s, false);
    }

    @Override
    protected JackrabbitAccessControlEntry createEntry(Principal principal, Privilege[] privileges, boolean isAllow)
            throws RepositoryException {
        return acl.createEntry(principal, privileges, isAllow, Collections.<String, Value>emptyMap());
    }

    @Override
    protected JackrabbitAccessControlEntry createEntry(Principal principal, Privilege[] privileges, boolean isAllow, Map<String, Value> restrictions) throws RepositoryException {
        return acl.createEntry(principal, privileges, isAllow, restrictions);
    }

    @Override
    protected JackrabbitAccessControlEntry createEntryFromBase(JackrabbitAccessControlEntry base, Privilege[] privileges, boolean isAllow) throws RepositoryException, NotExecutableException {
        if (base instanceof ACLTemplate.Entry) {
            return acl.createEntry((ACLTemplate.Entry) base, privileges, isAllow);
        } else {
            throw new NotExecutableException();
        }
    }

    @Override
    protected Map<String, Value> getTestRestrictions() throws RepositoryException {
        String restrName = ((SessionImpl) superuser).getJCRName(ACLTemplate.P_GLOB);
        return Collections.singletonMap(restrName, superuser.getValueFactory().createValue("/.*"));        
    }

    public void testRestrictions() throws RepositoryException {
        // test if restrictions with expanded name are properly resolved
        Map<String, Value> restrictions = new HashMap<String,Value>();
        restrictions.put(ACLTemplate.P_GLOB.toString(), superuser.getValueFactory().createValue("*/test"));

        Privilege[] privs = new Privilege[] {acMgr.privilegeFromName(Privilege.JCR_ALL)};
        ACLTemplate.Entry ace = acl.createEntry(testPrincipal, privs, true, restrictions);

        Value v = ace.getRestriction(ACLTemplate.P_GLOB.toString());
        Value v2 = ace.getRestriction(((SessionImpl) superuser).getJCRName(ACLTemplate.P_GLOB));
        assertEquals(v, v2);
    }
}