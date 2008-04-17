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

import org.apache.jackrabbit.core.security.JackrabbitAccessControlManager;
import org.apache.jackrabbit.core.security.authorization.AbstractEvaluationTest;
import org.apache.jackrabbit.core.security.authorization.PolicyEntry;
import org.apache.jackrabbit.core.security.authorization.PolicyTemplate;
import org.apache.jackrabbit.core.security.jsr283.security.AccessControlManager;
import org.apache.jackrabbit.core.security.jsr283.security.AccessControlPolicy;
import org.apache.jackrabbit.test.NotExecutableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.AccessDeniedException;
import javax.jcr.RepositoryException;
import java.security.Principal;

/**
 * <code>EvaluationTest</code>...
 */
public class EvaluationTest extends AbstractEvaluationTest {

    private static Logger log = LoggerFactory.getLogger(EvaluationTest.class);

    private String testPolicyPath;

    protected void setUp() throws Exception {
        super.setUp();

        JackrabbitAccessControlManager jam;
        if (acMgr instanceof JackrabbitAccessControlManager) {
            jam = (JackrabbitAccessControlManager) acMgr;
        } else {
            throw new NotExecutableException();
        }
        try {
            AccessControlPolicy rootPolicy = acMgr.getPolicy("/");
            if (!(rootPolicy instanceof PolicyTemplateImpl)) {
                throw new NotExecutableException();
            }
        } catch (RepositoryException e) {
            throw new NotExecutableException();
        }


        StringBuffer b = new StringBuffer("/rep:accesscontrol");
        Principal principal = testUser.getPrincipal();
        testPolicyPath = jam.editPolicy(principal).getPath();
    }

    protected void clearACInfo() {
        try {
            acMgr.removePolicy(testPolicyPath);
            superuser.save();
        } catch (RepositoryException e) {
            // log error and ignore
            log.error(e.getMessage());
        }
    }

    protected PolicyTemplate getPolicyTemplate(AccessControlManager acM, String path) throws RepositoryException, AccessDeniedException, NotExecutableException {
        if (acM instanceof JackrabbitAccessControlManager) {
            PolicyTemplate pt = ((JackrabbitAccessControlManager) acM).editPolicy(testPolicyPath);
            if (pt instanceof PolicyTemplateImpl) {
                return (PolicyTemplateImpl) pt;
            }
        }
        throw new NotExecutableException();
    }

    protected PolicyEntry createEntry(Principal principal, int privileges, boolean isAllow, String[] restrictions) {
        String nodePath = restrictions[0];
        String glob = restrictions[1];
        return new PolicyEntryImpl(principal, privileges, isAllow, nodePath, glob);
    }

    protected String[] getRestrictions(String path) {
        return new String[] {path, "*"};
    }
}