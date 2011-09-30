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

import org.apache.jackrabbit.api.security.JackrabbitAccessControlList;
import org.apache.jackrabbit.api.security.JackrabbitAccessControlManager;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.test.NotExecutableException;

import javax.jcr.AccessDeniedException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.AccessControlPolicy;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

/**
 * <code>EvaluationTest</code>...
 */
class EvaluationUtil {

    static boolean isExecutable(SessionImpl s, AccessControlManager acMgr) {
        if (acMgr instanceof JackrabbitAccessControlManager) {
            for (Principal princ : s.getSubject().getPrincipals()) {
                try {
                    AccessControlPolicy[] policies = ((JackrabbitAccessControlManager) acMgr).getApplicablePolicies(princ);
                    for (AccessControlPolicy policy : policies) {
                        if (policy instanceof ACLTemplate) {
                            return true;
                        }
                    }
                    policies = ((JackrabbitAccessControlManager) acMgr).getPolicies(princ);
                    for (AccessControlPolicy policy : policies) {
                        if (policy instanceof ACLTemplate) {
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

    static JackrabbitAccessControlList getPolicy(AccessControlManager acM,
                                                 String path,
                                                 Principal principal)
            throws RepositoryException, AccessDeniedException, NotExecutableException {
        if (acM instanceof JackrabbitAccessControlManager && path != null) {
            // first try applicable policies
            AccessControlPolicy[] policies = ((JackrabbitAccessControlManager) acM).getApplicablePolicies(principal);
            for (AccessControlPolicy policy : policies) {
                if (policy instanceof ACLTemplate) {
                    return (ACLTemplate) policy;
                }
            }

            // second existing policies
            policies = ((JackrabbitAccessControlManager) acM).getPolicies(principal);
            for (AccessControlPolicy policy : policies) {
                if (policy instanceof ACLTemplate) {
                    return (ACLTemplate) policy;
                }
            }
        }
        throw new NotExecutableException();
    }

    static  Map<String, Value> getRestrictions(Session s, String path) throws RepositoryException, NotExecutableException {
        if (s instanceof SessionImpl && path != null) {
            Map<String, Value> restr = new HashMap<String, Value>();
            restr.put(((SessionImpl) s).getJCRName(ACLTemplate.P_NODE_PATH), s.getValueFactory().createValue(path, PropertyType.PATH));
            return restr;
        } else {
            throw new NotExecutableException();
        }
    }
}