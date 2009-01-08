/*
 * $Id$
 *
 * Copyright 1997-2005 Day Management AG
 * Barfuesserplatz 6, 4001 Basel, Switzerland
 * All Rights Reserved.
 *
 * This software is the confidential and proprietary information of
 * Day Management AG, ("Confidential Information"). You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Day.
 */
package org.apache.jackrabbit.core.security.authorization.principalbased;

import org.apache.jackrabbit.core.security.authorization.AbstractWriteTest;
import org.apache.jackrabbit.core.security.authorization.JackrabbitAccessControlList;
import org.apache.jackrabbit.core.security.JackrabbitAccessControlManager;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.test.NotExecutableException;
import org.apache.jackrabbit.api.jsr283.security.AccessControlManager;
import org.apache.jackrabbit.api.jsr283.security.AccessControlPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.AccessDeniedException;
import javax.jcr.Session;
import java.security.Principal;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * <code>EvaluationTest</code>...
 */
public class WriteTest extends AbstractWriteTest {

    private static Logger log = LoggerFactory.getLogger(WriteTest.class);

    private List toClear = new ArrayList();

    protected void setUp() throws Exception {
        super.setUp();

        // simple test to check if proper provider is present:
        getPolicy(acMgr, path, getTestUser().getPrincipal());
    }

    protected void clearACInfo() {
        for (Iterator it = toClear.iterator(); it.hasNext();) {
            String path = it.next().toString();
            try {
                AccessControlPolicy[] policies = acMgr.getPolicies(path);
                for (int i = 0; i < policies.length; i++) {
                    acMgr.removePolicy(path, policies[i]);
                    superuser.save();
                }
            } catch (RepositoryException e) {
                // log error and ignore
                log.error(e.getMessage());
            }
        }
    }

    protected JackrabbitAccessControlList getPolicy(AccessControlManager acM, String path, Principal principal) throws RepositoryException, AccessDeniedException, NotExecutableException {
        if (acM instanceof JackrabbitAccessControlManager) {
            AccessControlPolicy[] policies = ((JackrabbitAccessControlManager) acM).getApplicablePolicies(principal);
            for (int i = 0; i < policies.length; i++) {
                if (policies[i] instanceof ACLTemplate) {
                    ACLTemplate acl = (ACLTemplate) policies[i];
                    toClear.add(acl.getPath());
                    return acl;
                }
            }
        }
        throw new NotExecutableException();
    }

    protected Map getRestrictions(Session s, String path) throws RepositoryException, NotExecutableException {
        if (s instanceof SessionImpl) {
            Map restr = new HashMap();
            restr.put(((SessionImpl) s).getJCRName(ACLTemplate.P_NODE_PATH), path);
            return restr;
        } else {
            throw new NotExecutableException();
        }
    }

    // TODO: add specific tests with other restrictions
}
