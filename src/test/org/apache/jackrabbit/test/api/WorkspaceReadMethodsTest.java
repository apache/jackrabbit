/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.test.api;

import org.apache.jackrabbit.test.AbstractJCRTest;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;

/**
 * <code>WorkspaceReadMethodsTest</code>...
 *
 * @test
 * @sources WorkspaceReadMethodsTest.java
 * @executeClass org.apache.jackrabbit.test.api.WorkspaceReadMethodsTest
 * @keywords level1
 */
public class WorkspaceReadMethodsTest extends AbstractJCRTest {

    /**
     * Sets up the fixture for the test.
     */
    protected void setUp() throws Exception {
        isReadOnly = true;
        super.setUp();
    }

    /**
     * Tests the getSession() method which returns the same session object as
     * this workspace was requested from.
     */
    public void testGetSession() throws RepositoryException {
        Session session = helper.getReadOnlySession();
        Session otherSession = session.getWorkspace().getSession();
        assertSame("Workspace.getSession() returns not the same session object.",
                session, otherSession);
        session.logout();
    }

    /**
     * Tests that the name returned by Workspace.getName() is equal to the one
     * used for login.
     */
    public void testGetName() throws RepositoryException {
        Session session = helper.getReadOnlySession(workspaceName);
        String name = session.getWorkspace().getName();
        if (workspaceName != null) {
            assertEquals("Workspace.getName() returns wrong name.",
                    workspaceName, name);
        }
        session.logout();
    }

    /**
     * Tests Workspace.getQueryManager. This should just return correctly a
     * QueryManager object.
     */
    public void testGetQueryManager() throws RepositoryException {
        Workspace ws = helper.getReadOnlySession().getWorkspace();
        assertNotNull("Workspace does not return a QueryManager object.", ws.getQueryManager());
        ws.getSession().logout();
    }

    /**
     * Tests Workspace.getAccessibleWorkspaceNames() by logging into the
     * Workspaces given by the returned names. The credentials are the same as
     * used for accessing the current workspace.
     */
    public void testGetAccessibleWorkspaceNames() throws RepositoryException {
        Session session = helper.getReadOnlySession();
        String[] wsNames = session.getWorkspace().getAccessibleWorkspaceNames();
        for (int i = 0; i < wsNames.length; i++) {
            // login
            Session s = helper.getReadOnlySession(wsNames[i]);
            s.logout();
        }
        session.logout();
    }
}