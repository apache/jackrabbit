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
package org.apache.jackrabbit.jcr2spi;

import org.apache.jackrabbit.test.AbstractJCRTest;
import org.apache.jackrabbit.test.NotExecutableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Workspace;
import java.util.Arrays;
import java.util.List;

/**
 * <code>WorkspaceTest</code>...
 */
public class WorkspaceTest extends AbstractJCRTest {

    /**
     * logger instance
     */
    private static final Logger log = LoggerFactory.getLogger(WorkspaceTest.class);

    private static String getNewWorkspaceName(Workspace wsp) throws RepositoryException {
        List<String> names = Arrays.asList(wsp.getAccessibleWorkspaceNames());
        int index = 0;
        while (names.contains("testWsp_" + index)) {
            index++;
        }
        return "testWsp_" + index;
    }

    public void testCreateWorkspace() throws Exception {
        Session s = null;
        try {
            Workspace wsp = superuser.getWorkspace();
            String name = getNewWorkspaceName(wsp);
            wsp.createWorkspace(name);

            List<String> wsps = Arrays.asList(wsp.getAccessibleWorkspaceNames());
            assertTrue(wsps.contains(name));

            s = getHelper().getSuperuserSession(name);
            Workspace newW = s.getWorkspace();
            assertEquals(name, newW.getName());
        } catch (UnsupportedRepositoryOperationException e) {
            throw new NotExecutableException();
        } catch (UnsupportedOperationException e) {
            throw new NotExecutableException();
        } finally {
            if (s != null) {
                s.logout();
            }
        }
    }

    public void testCreateWorkspaceFromSource() throws Exception {
        Session s = null;
        try {
            Workspace wsp = superuser.getWorkspace();
            String name = getNewWorkspaceName(wsp);

            wsp.createWorkspace(name, wsp.getName());

            List<String> wsps = Arrays.asList(wsp.getAccessibleWorkspaceNames());
            assertTrue(wsps.contains(name));

            s = getHelper().getSuperuserSession(name);
            Workspace newW = s.getWorkspace();
            assertEquals(name, newW.getName());
        } catch (UnsupportedRepositoryOperationException e) {
            throw new NotExecutableException();
        } catch (UnsupportedOperationException e) {
            throw new NotExecutableException();
        } finally {
            if (s != null) {
                s.logout();
            }
        }
    }

    public void testDeleteWorkspace() throws Exception {
        try {
            Workspace wsp = superuser.getWorkspace();
            String name = getNewWorkspaceName(wsp);

            wsp.createWorkspace(name, wsp.getName());

            List<String> wsps = Arrays.asList(wsp.getAccessibleWorkspaceNames());
            assertTrue(wsps.contains(name));

            wsp.deleteWorkspace(name);

            wsps = Arrays.asList(wsp.getAccessibleWorkspaceNames());
            assertFalse(wsps.contains(name));

            Session s = null;
            try {
                s = getHelper().getSuperuserSession(name);
                fail(name + " has been deleted.");
            } catch (NoSuchWorkspaceException e) {
                // success
            } finally {
                if (s != null) {
                    s.logout();
                }
            }
        } catch (UnsupportedRepositoryOperationException e) {
            throw new NotExecutableException();
        } catch (UnsupportedOperationException e) {
            throw new NotExecutableException();
        }
    }
}