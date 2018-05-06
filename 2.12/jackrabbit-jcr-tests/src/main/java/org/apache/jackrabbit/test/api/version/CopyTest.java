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
package org.apache.jackrabbit.test.api.version;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Workspace;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionManager;
import javax.jcr.version.Version;

/**
 * <code>CopyTest</code> checks if full versionable nodes are copied correctly:
 *
 * 15.1.4 Copying Versionable Nodes and Version Lineage
 * Under both simple and full versioning, when an existing versionable node N is
 * copied to a new location either in the same workspace or another, and the
 * repository preserves the versionable mixin (see 10.7.4 Dropping Mixins on
 * Copy):
 * - A copy of N, call it M, is created, as usual.
 * - A new, empty, version history for M, call it HM, is also created.
 *
 * Under full versioning:
 * - The properties jcr:versionHistory, jcr:baseVersion and
 *   jcr:predecessors of M are not copied from N but are initialized as usual.
 * - The jcr:copiedFrom property of HM is set to point to the base version of N.
 *
 */
public class CopyTest extends AbstractVersionTest {

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        // remove copied node
        try {
            String dstPath = getProperty("destination");
            superuser.getNode(dstPath).remove();
            testRootNode.getSession().save();
        } catch (Exception e) {
            log.println("Exception in tearDown: " + e.toString());
        } finally {
            super.tearDown();
        }
    }

    public void testCopy() throws RepositoryException {
        Workspace wsp = superuser.getWorkspace();
        VersionManager vMgr = wsp.getVersionManager();
        String srcPath = versionableNode.getPath();
        String dstPath = getProperty("destination");
        wsp.copy(srcPath, dstPath);

        // check versionable
        Node v = superuser.getNode(dstPath);
        assertTrue("Copied Node.isNodeType(mix:cersionable) must return true.",
                v.isNodeType(mixVersionable));

        // check different version history
        VersionHistory vh1 = vMgr.getVersionHistory(srcPath);
        VersionHistory vh2 = vMgr.getVersionHistory(dstPath);
        assertFalse("Copied node needs a new version history.", vh1.isSame(vh2));

        // check if 1 version
        assertEquals("Copied node must have 1 version.", 1, getNumberOfVersions(vh2));

        // check if jcr:copiedFrom is set correctly
        assertTrue("Version history of desination must have a jcr:copiedFrom property", vh2.hasProperty(jcrCopiedFrom));

        Node ref = vh2.getProperty(jcrCopiedFrom).getNode();
        Version base = vMgr.getBaseVersion(srcPath);
        assertTrue("jcr:copiedFrom must point to the base version of the original.", ref.isSame(base));
    }
}
