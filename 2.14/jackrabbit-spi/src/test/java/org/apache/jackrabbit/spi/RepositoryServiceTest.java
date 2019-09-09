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
package org.apache.jackrabbit.spi;

import javax.jcr.RepositoryException;
import java.util.Map;
import java.util.Arrays;

/** <code>RepositoryServiceTest</code>... */
public class RepositoryServiceTest extends AbstractSPITest {

    private RepositoryService service;

    protected void setUp() throws Exception {
        super.setUp();
        service = helper.getRepositoryService();
    }

    public void testGetIdFactory() throws RepositoryException {
        assertNotNull(service.getIdFactory());
    }

    public void testGetQValueFactory() throws RepositoryException {
        assertNotNull(service.getQValueFactory());
    }

    public void testGetNameFactory() throws RepositoryException {
        assertNotNull(service.getNameFactory());
    }

    public void testGetPathFactory() throws RepositoryException {
        assertNotNull(service.getPathFactory());
    }

    public void testGetRepositoryDescriptors() throws RepositoryException {
        Map descriptors = service.getRepositoryDescriptors();
        assertNotNull(descriptors);
        assertTrue(!descriptors.isEmpty());
    }

    public void testGetWorkspaceNames() throws RepositoryException {
        String[] workspaceNames = service.getWorkspaceNames(sessionInfo);
        assertNotNull("Workspace names must not be null", workspaceNames);
        assertTrue("Workspace names must contain at least a single workspace", workspaceNames.length > 0);

        String wspName = getProperty(RepositoryServiceStub.PROP_WORKSPACE);
        if (wspName != null) {
            assertTrue("Workspace name used for retrieving the SessionInfo must be included in the available workspaces.", Arrays.asList(workspaceNames).contains(wspName));
        }
    }

    public void testNullWorkspaceName() throws RepositoryException {
        SessionInfo sInfo = service.obtain(helper.getAdminCredentials(), null);
        try {
            assertNotNull(sInfo.getWorkspaceName());
        } finally {
            service.dispose(sInfo);
        }
    }

    // TODO: add more tests
}