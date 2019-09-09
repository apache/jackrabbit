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
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.security.authorization.AbstractVersionManagementTest;
import org.apache.jackrabbit.core.security.authorization.PrivilegeRegistry;
import org.apache.jackrabbit.test.NotExecutableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.Privilege;
import java.security.Principal;
import java.util.Map;

/**
 * <code>VersionTest</code>...
 */
public class VersionTest extends AbstractVersionManagementTest {

    private static Logger log = LoggerFactory.getLogger(VersionTest.class);

    private static String VERSION_STORAGE_PATH = "/jcr:system/jcr:versionStorage";

    protected boolean isExecutable() {
        return EvaluationUtil.isExecutable((SessionImpl) superuser, acMgr);
    }
    
    protected JackrabbitAccessControlList getPolicy(AccessControlManager acMgr, String path, Principal princ) throws
            RepositoryException, NotExecutableException {
        return EvaluationUtil.getPolicy(acMgr, path, princ);
    }
    protected Map<String, Value> getRestrictions(Session s, String path) throws RepositoryException, NotExecutableException {
        return EvaluationUtil.getRestrictions(s, path);
    }

    public void testReadVersionInfo() throws RepositoryException, NotExecutableException {
        Node n = createVersionableNode(testRootNode);
        modifyPrivileges(VERSION_STORAGE_PATH, Privilege.JCR_READ, false);

        Node n2 = (Node) getTestSession().getItem(n.getPath());
        try {
            n2.getVersionHistory();
            fail();
        } catch (AccessDeniedException e) {
            // success
        } catch (ItemNotFoundException e) {
            // success as well
        }
        try {
            n2.getBaseVersion();
            fail();
        } catch (AccessDeniedException e) {
            // success
        } catch (ItemNotFoundException e) {
            // success as well
        }
    }

    public void testReadVersionInfo2() throws RepositoryException, NotExecutableException {
        Node n = createVersionableNode(testRootNode);
        modifyPrivileges(VERSION_STORAGE_PATH, Privilege.JCR_READ, true);

        Node n2 = (Node) getTestSession().getItem(n.getPath());
        n2.getVersionHistory();
        n2.getBaseVersion();
    }

    public void testReadVersionInfo3() throws RepositoryException, NotExecutableException {
        Node trn = getTestNode();
        modifyPrivileges(trn.getPath(), PrivilegeRegistry.REP_WRITE, true);
        modifyPrivileges(trn.getPath(), Privilege.JCR_NODE_TYPE_MANAGEMENT, true);
        modifyPrivileges(trn.getPath(), Privilege.JCR_VERSION_MANAGEMENT, true);
        modifyPrivileges(VERSION_STORAGE_PATH, Privilege.JCR_READ, false);

        Node n = createVersionableNode(trn);
        assertTrue(n.isNodeType(mixVersionable));
        assertFalse(n.isModified());

        try {
            n.getVersionHistory();
            n.getBaseVersion();
            fail("No READ permission in the version storage");
        } catch (AccessDeniedException e) {
            // success
            log.debug(e.getMessage());
        }  catch (ItemNotFoundException e) {
            // success
            log.debug(e.getMessage());
        }
    }
}