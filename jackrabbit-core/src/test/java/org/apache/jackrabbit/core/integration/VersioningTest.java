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
package org.apache.jackrabbit.core.integration;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.version.Version;

import org.apache.jackrabbit.test.AbstractJCRTest;

/**
 * Versioning tests.
 */
public class VersioningTest extends AbstractJCRTest {

    private Node n1;
    private Node n2;

    protected void setUp() throws Exception {
        super.setUp();

        Session s1 = getHelper().getSuperuserSession();
        n1 = s1.getRootNode().addNode("VersioningTest");
        n1.addMixin(mixVersionable);
        n1.getSession().save();

        Session s2 = getHelper().getSuperuserSession(workspaceName);
        s2.getWorkspace().clone(
                s1.getWorkspace().getName(), n1.getPath(),
                "/VersioningTest", true);
        n2 = s2.getRootNode().getNode("VersioningTest");
    }

    protected void tearDown() throws Exception {
        super.tearDown();

        Session s1 = n1.getSession();
        n1.remove();
        s1.save();
        s1.logout();

        Session s2 = n2.getSession();
        n2.remove();
        s2.save();
        s2.logout();
    }

    /**
     * Tests that the version tree documented in
     * AbstractVersionManager.calculateCheckinVersionName() can be
     * constructed and has the expected version names.
     * <p>
     * Note that this test case needs to be modified if the version naming
     * algorithm ever gets changed.
     */
    public void testVersionGraph() throws Exception {
        Version vR = n1.getBaseVersion();

        Version v10 = n1.checkin();
        n1.checkout();
        Version v11 = n1.checkin();
        n1.checkout();
        Version v12 = n1.checkin();
        n1.checkout();
        Version v13 = n1.checkin();
        n1.checkout();
        Version v14 = n1.checkin();
        n1.checkout();
        Version v15 = n1.checkin();
        n1.checkout();
        Version v16 = n1.checkin();

        n1.restore(v12, true);
        n1.checkout();
        Version v120 = n1.checkin();
        n1.checkout();
        Version v121 = n1.checkin();
        n1.checkout();
        Version v122 = n1.checkin();

        n1.restore(v12, true);
        n1.checkout();
        Version v1200 = n1.checkin();

        n1.restore(v121, true);
        n1.checkout();
        Version v1210 = n1.checkin();
        n1.checkout();
        Version v1211 = n1.checkin();

        // x.0 versions can be created if the newly created versionable
        // node is cloned to another workspace before the first checkin
        Version v20 = n2.checkin();

        // Multiple branches can be merged using multiple workspaces
        n2.restore(v122, true);
        n1.restore(v16, true);
        n1.checkout();
        n1.merge(n2.getSession().getWorkspace().getName(), true);
        n1.doneMerge(v122);
        Version v17 = n1.checkin();

        assertEquals("jcr:rootVersion", vR.getName());
        assertPredecessors("", vR);
        assertSuccessors("1.0 2.0", vR);
        assertEquals("1.0", v10.getName());
        assertPredecessors("jcr:rootVersion", v10);
        assertSuccessors("1.1", v10);
        assertEquals("1.1", v11.getName());
        assertPredecessors("1.0", v11);
        assertSuccessors("1.2", v11);
        assertEquals("1.2", v12.getName());
        assertPredecessors("1.1", v12);
        assertSuccessors("1.3 1.2.0 1.2.0.0", v12);
        assertEquals("1.3", v13.getName());
        assertPredecessors("1.2", v13);
        assertSuccessors("1.4", v13);
        assertEquals("1.4", v14.getName());
        assertPredecessors("1.3", v14);
        assertSuccessors("1.5", v14);
        assertEquals("1.5", v15.getName());
        assertPredecessors("1.4", v15);
        assertSuccessors("1.6", v15);
        assertEquals("1.6", v16.getName());
        assertPredecessors("1.5", v16);
        assertSuccessors("1.7", v16);
        assertEquals("1.7", v17.getName());
        assertPredecessors("1.6 1.2.2", v17);
        assertSuccessors("", v17);

        assertEquals("1.2.0", v120.getName());
        assertPredecessors("1.2", v120);
        assertSuccessors("1.2.1", v120);
        assertEquals("1.2.1", v121.getName());
        assertPredecessors("1.2.0", v121);
        assertSuccessors("1.2.2 1.2.1.0", v121);
        assertEquals("1.2.2", v122.getName());
        assertPredecessors("1.2.1", v122);
        assertSuccessors("1.7", v122);

        assertEquals("1.2.0.0", v1200.getName());
        assertPredecessors("1.2", v1200);
        assertSuccessors("", v1200);

        assertEquals("1.2.1.0", v1210.getName());
        assertPredecessors("1.2.1", v1210);
        assertSuccessors("1.2.1.1", v1210);
        assertEquals("1.2.1.1", v1211.getName());
        assertPredecessors("1.2.1.0", v1211);
        assertSuccessors("", v1211);

        assertEquals("2.0", v20.getName());
        assertPredecessors("jcr:rootVersion", v20);
        assertSuccessors("", v20);
    }

    private void assertPredecessors(String expected, Version version)
            throws Exception {
        Set predecessors = new HashSet();
        if (expected.length() > 0) {
            predecessors.addAll(Arrays.asList(expected.split(" ")));
        }
        Version[] versions = version.getPredecessors();
        for (int i = 0; i < versions.length; i++) {
            if (!predecessors.remove(versions[i].getName())) {
                fail("Version " + version.getName()
                        + " has an unexpected predessor "
                        + versions[i].getName());
            }
        }
        if (!predecessors.isEmpty()) {
            fail("Version " + version.getName()
                    + " does not have all expected predecessors");
        }
    }

    private void assertSuccessors(String expected, Version version)
            throws Exception {
        Set successors = new HashSet();
        if (expected.length() > 0) {
            successors.addAll(Arrays.asList(expected.split(" ")));
        }
        Version[] versions = version.getSuccessors();
        for (int i = 0; i < versions.length; i++) {
            if (!successors.remove(versions[i].getName())) {
                fail("Version " + version.getName()
                        + " has an unexpected successor "
                        + versions[i].getName());
            }
        }
        if (!successors.isEmpty()) {
            fail("Version " + version.getName()
                    + " does not have all expected successors");
        }
    }

}
