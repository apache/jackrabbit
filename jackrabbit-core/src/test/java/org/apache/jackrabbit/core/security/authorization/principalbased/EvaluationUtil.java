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

import org.apache.jackrabbit.api.jsr283.security.AccessControlManager;
import org.apache.jackrabbit.api.jsr283.security.AccessControlPolicy;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.security.JackrabbitAccessControlManager;
import org.apache.jackrabbit.core.security.authorization.JackrabbitAccessControlList;
import org.apache.jackrabbit.test.NotExecutableException;

import javax.jcr.AccessDeniedException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.PropertyType;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;

/**
 * <code>EvaluationTest</code>...
 */
class EvaluationUtil {

    static boolean isExecutable(SessionImpl s, AccessControlManager acMgr) {
        if (acMgr instanceof JackrabbitAccessControlManager) {
            for (Iterator it = s.getSubject().getPrincipals().iterator(); it.hasNext();) {
                Principal princ = (Principal) it.next();
                try {
                    AccessControlPolicy[] policies = ((JackrabbitAccessControlManager) acMgr).getApplicablePolicies(princ);
                    for (int i = 0; i < policies.length; i++) {
                        if (policies[i] instanceof ACLTemplate) {
                            return true;
                        }
                    }
                } catch (RepositoryException e) {
                    // ignore
                }
            }
        }
        return false;
    }

    static JackrabbitAccessControlList getPolicy(AccessControlManager acM, String path, Principal principal) throws RepositoryException,
            AccessDeniedException, NotExecutableException {
        if (acM instanceof JackrabbitAccessControlManager) {
            AccessControlPolicy[] policies = ((JackrabbitAccessControlManager) acM).getApplicablePolicies(principal);
            for (int i = 0; i < policies.length; i++) {
                if (policies[i] instanceof ACLTemplate) {
                    ACLTemplate acl = (ACLTemplate) policies[i];
                    return acl;
                }
            }
        }
        throw new NotExecutableException();
    }

    static  Map getRestrictions(Session s, String path) throws RepositoryException, NotExecutableException {
        if (s instanceof SessionImpl) {
            Map restr = new HashMap();
            restr.put(((SessionImpl) s).getJCRName(ACLTemplate.P_NODE_PATH), s.getValueFactory().createValue(path, PropertyType.PATH));
            return restr;
        } else {
            throw new NotExecutableException();
        }
    }
}