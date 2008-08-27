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

import org.apache.jackrabbit.api.security.principal.NoSuchPrincipalException;
import org.apache.jackrabbit.api.security.principal.PrincipalIterator;
import org.apache.jackrabbit.api.security.principal.PrincipalManager;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Impersonation;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.PropertyImpl;
import org.apache.jackrabbit.core.security.SystemPrincipal;
import org.apache.jackrabbit.core.security.principal.AdminPrincipal;
import org.apache.jackrabbit.core.security.principal.PrincipalImpl;
import org.apache.jackrabbit.core.security.principal.PrincipalIteratorAdapter;
import org.apache.jackrabbit.value.StringValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.security.auth.Subject;
import java.security.Principal;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * ImpersonationImpl
 */
class ImpersonationImpl implements Impersonation, UserConstants {

    private static final Logger log = LoggerFactory.getLogger(ImpersonationImpl.class);

    private final UserImpl user;
    private final UserManagerImpl userManager;

    ImpersonationImpl(UserImpl user, UserManagerImpl userManager) throws RepositoryException {
        this.user = user;
        this.userManager = userManager;
    }

    //------------------------------------------------------< Impersonation >---
    /**
     * @see Impersonation#getImpersonators()
     */
    public PrincipalIterator getImpersonators() throws RepositoryException {
        Set impersonators = getImpersonatorNames();
        if (impersonators.isEmpty()) {
            return PrincipalIteratorAdapter.EMPTY;
        } else {
            final PrincipalManager pMgr = user.getSession().getPrincipalManager();

            Set s = new HashSet();
            for (Iterator it = impersonators.iterator(); it.hasNext();) {
                String pName = it.next().toString();
                Principal p = null;
                if (pMgr.hasPrincipal(pName)) {
                    try {
                        p = pMgr.getPrincipal(pName);
                    } catch (NoSuchPrincipalException e) {
                        // should never get here.
                    }
                }
                if (p == null) {
                    log.debug("Impersonator " + pName + " does not correspond to a known Principal.");
                    p = new PrincipalImpl(pName);
                }
                s.add(p);

            }
            return new PrincipalIteratorAdapter(s);
        }
    }

    /**
     * @see Impersonation#grantImpersonation(Principal)
     */
    public synchronized boolean grantImpersonation(Principal principal) throws RepositoryException {
        if (principal instanceof AdminPrincipal || principal instanceof SystemPrincipal) {
            log.debug("Admin and System principal are already granted impersonation.");
            return false;
        }

        // make sure the given principals belong to an existing authorizable
        Authorizable auth = user.userManager.getAuthorizable(principal);
        if (auth == null || auth.isGroup()) {
            log.debug("Cannot grant impersonation to a principal that is a Group " +
                      "or an unknown Authorizable.");
            return false;
        }

        String pName = principal.getName();
        // make sure user does not impersonate himself
        for (PrincipalIterator it = user.getPrincipals(); it.hasNext();) {
            Principal p = it.nextPrincipal();
            if (p.getName().equals(pName)) {
                log.debug("Cannot grant impersonation to oneself.");
                return false;
            }
        }

        boolean granted = false;
        Set impersonators = getImpersonatorNames();
        if (impersonators.add(pName)) {
            updateImpersonatorNames(impersonators);
            granted = true;
        }
        return granted;
    }

    /**
     * @see Impersonation#revokeImpersonation(Principal)
     */
    public synchronized boolean revokeImpersonation(Principal principal) throws RepositoryException {
        if (principal instanceof AdminPrincipal || principal instanceof SystemPrincipal) {
            log.debug("Admin and System principal are always granted impersonation.");
            return false;
        }

        boolean revoked = false;
        String pName = principal.getName();

        Set impersonators = getImpersonatorNames();
        if (impersonators.remove(pName)) {
            updateImpersonatorNames(impersonators);
            revoked = true;
        }
        return revoked;
    }

    /**
     * @see Impersonation#allows(Subject) 
     */
    public boolean allows(Subject subject) throws RepositoryException {
        if (subject == null) {
            return false;
        }
        //shortcut admin/system -> always allowed
        if (!subject.getPrincipals(AdminPrincipal.class).isEmpty()
                || !subject.getPrincipals(SystemPrincipal.class).isEmpty()) {
            return true;
        }

        Set principalNames = new HashSet();
        for (Iterator it = subject.getPrincipals().iterator(); it.hasNext();) {
            principalNames.add(((Principal) it.next()).getName());
        }

        boolean allows = false;
        try {
            Set impersonators = getImpersonatorNames();
            allows = impersonators.removeAll(principalNames);
        } catch (RepositoryException e) {
            // should never get here
            log.debug(e.getMessage());
        }
        return allows;
    }

    //------------------------------------------------------------< private >---

    private Set getImpersonatorNames() throws RepositoryException {
        Set princNames = new HashSet();
        if (user.getNode().hasProperty(P_IMPERSONATORS)) {
            Value[] vs = user.getNode().getProperty(P_IMPERSONATORS).getValues();
            for (int i = 0; i < vs.length; i++) {
                princNames.add(vs[i].getString());
            }
        }
        return princNames;
    }

    private void updateImpersonatorNames(Set principalNames) throws RepositoryException {
        NodeImpl userNode = user.getNode();
        try {
            String[] pNames = (String[]) principalNames.toArray(new String[principalNames.size()]);
            if (pNames.length == 0) {
                PropertyImpl prop = userNode.getProperty(P_IMPERSONATORS);
                userManager.removeProtectedItem(prop, userNode);
            } else {
                Value[] values = new Value[pNames.length];
                for (int i = 0; i < pNames.length; i++) {
                    values[i] = new StringValue(pNames[i]);
                }
                userManager.setProtectedProperty(userNode, P_IMPERSONATORS, values);
            }
        } catch (RepositoryException e) {
            // revert pending changes
            userNode.refresh(false);
            throw e;
        }
    }
}
