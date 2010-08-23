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
package org.apache.jackrabbit.test.api;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Workspace;
import javax.jcr.NoSuchWorkspaceException;

import org.apache.jackrabbit.test.AbstractJCRTest;

/**
 * <code>WorkspaceManagementTest</code>...
 */
public class WorkspaceManagementTest extends AbstractJCRTest {

    private Workspace workspace;

    protected void setUp() throws Exception {
        super.setUp();

        super.checkSupportedOption(Repository.OPTION_WORKSPACE_MANAGEMENT_SUPPORTED);

        workspace = superuser.getWorkspace();
    }

    /**
     * Tests {@link javax.jcr.Workspace#createWorkspace(String)} and
     * {@link javax.jcr.Workspace#createWorkspace(String, String)}.
     *
     * @throws javax.jcr.RepositoryException
     */
    public void testCreateWorkspace() throws RepositoryException {

        try {
            workspace.createWorkspace(workspaceName);
            fail("creating a new workspace with the name of an already existing one must fail");
        } catch (RepositoryException e) {
            // excepted
        }

        // create empty workspace
        workspace.createWorkspace("tmp" + System.currentTimeMillis());

        // create pre-initialized workspace, specifying unknwon src workspace
        try {
            workspace.createWorkspace("tmp" + System.currentTimeMillis(), "unknownworkspace");
            fail("NoSuchWorkspaceException expected");
        } catch (NoSuchWorkspaceException e) {
            // excepted
        }

        // create pre-initialized workspace, specifying existing src workspace
        workspace.createWorkspace("tmp" + System.currentTimeMillis(), superuser.getWorkspace().getName());
    }


    /**
     * Tests {@link javax.jcr.Workspace#deleteWorkspace(String)}.
     *
     * @throws javax.jcr.RepositoryException
     */
    public void testDeleteWorkspace() throws RepositoryException {

        try {
            workspace.deleteWorkspace("unknownworkspace");
            fail("NoSuchWorkspaceException expected");
        } catch (NoSuchWorkspaceException e) {
            // excepted
        }
    }
}