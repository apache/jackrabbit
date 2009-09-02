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

import org.apache.jackrabbit.api.security.JackrabbitAccessControlList;
import org.apache.jackrabbit.test.NotExecutableException;

import javax.jcr.AccessDeniedException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.AccessControlPolicy;
import javax.jcr.security.AccessControlPolicyIterator;
import java.security.Principal;
import java.util.Collections;
import java.util.Map;

/**
 * <code>EvaluationTest</code>...
 */
final class EvaluationUtil {

    static boolean isExecutable(AccessControlManager acMgr) {
        try {
            AccessControlPolicy[] rootPolicies = acMgr.getPolicies("/");
            if (rootPolicies.length > 0 && rootPolicies[0] instanceof ACLTemplate) {
                return true;
            }
        } catch (RepositoryException e) {
            // ignore
        }
        return false;
    }

    static JackrabbitAccessControlList getPolicy(AccessControlManager acM, String path, Principal principal) throws RepositoryException,
            AccessDeniedException, NotExecutableException {
        // try applicable (new) ACLs first
        AccessControlPolicyIterator itr = acM.getApplicablePolicies(path);
        while (itr.hasNext()) {
            AccessControlPolicy policy = itr.nextAccessControlPolicy();
            if (policy instanceof ACLTemplate) {
                return (ACLTemplate) policy;
            }
        }
        // try if there is an acl that has been set before:
        AccessControlPolicy[] pcls = acM.getPolicies(path);
        for (AccessControlPolicy policy : pcls) {
            if (policy instanceof ACLTemplate) {
                return (ACLTemplate) policy;
            }
        }
        // no applicable or existing ACLTemplate to edit -> not executable.
        throw new NotExecutableException();
    }

    static Map<String, Value> getRestrictions(Session s, String path) throws RepositoryException, NotExecutableException {
        return Collections.emptyMap();
    }
}