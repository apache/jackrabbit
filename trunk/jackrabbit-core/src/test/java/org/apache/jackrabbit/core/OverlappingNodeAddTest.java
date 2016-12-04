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
package org.apache.jackrabbit.core;

import javax.jcr.InvalidItemStateException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.Session;

import org.apache.jackrabbit.test.AbstractJCRTest;

public class OverlappingNodeAddTest extends AbstractJCRTest {

    private Node testfolder;
    private Session s1;
    private Session s2;

    public void setUp() throws Exception {
        super.setUp();
        testfolder = testRootNode.addNode("container", "nt:folder");
        s1 = testfolder.getSession();
        s2 = getHelper().getReadWriteSession();
        s1.save();
    }

    /**
     * Performs add operations on a single node (no SNS) through 2 sessions
     */
    public void testWith2Folders() throws Exception {

        boolean bWasSaved = false;

        String testpath = testfolder.getPath();

        Node f1 = s1.getNode(testpath).addNode("folder", "nt:folder");
        Node c1 = f1.addNode("a", "nt:file");
        Node r1 = c1.addNode("jcr:content", "nt:resource");
        r1.setProperty("jcr:data", "foo");

        Node f2 = s2.getNode(testpath).addNode("folder", "nt:folder");
        Node c2 = f2.addNode("b", "nt:file");
        Node r2 = c2.addNode("jcr:content", "nt:resource");
        r2.setProperty("jcr:data", "bar");

        s1.save();

        String s1FolderId = f1.getIdentifier();

        try {
            s2.save();
            bWasSaved = true;
        } catch (InvalidItemStateException ex) {
            // expected

            // retry; adding refresh doesn't change anything here
            try {
                s2.save();

                bWasSaved = true;

            } catch (InvalidItemStateException ex2) {
                // we would be cool with this
            }
        }

        // we don't have changes in s1, so the keepChanges flag should be
        // irrelevant
        s1.refresh(false);

        // be nice and get a new Node instance
        Node newf1 = s1.getNode(testpath + "/folder");

        // if bWasSaved it should now be visible to Session 1
        assertEquals("'b' was saved, so session 1 should see it", bWasSaved,
                newf1.hasNode("b"));

        // 'a' was saved by Session 1 earlier on
        if (!newf1.hasNode("a")) {
            String message = "child node 'a' not present";

            if (bWasSaved && !s1FolderId.equals(newf1.getIdentifier())) {
                message += ", and also the folder's identifier changed from "
                        + s1FolderId + " to " + newf1.getIdentifier();
            }

            Node oldf1 = null;

            try {
                oldf1 = s1.getNodeByIdentifier(s1FolderId);
            } catch (ItemNotFoundException ex) {
                message += "; node with id "
                        + s1FolderId
                        + " can't be retrieved using getNodeByIdentifier either";
            }

            if (oldf1 != null) {
                try {
                    oldf1.getPath();
                } catch (Exception ex) {
                    message += "; node with id "
                            + s1FolderId
                            + " can be retrieved using getNodeByIdentifier, but getPath() fails with: "
                            + ex.getMessage();
                }
            }

            fail(message);
        }
    }

    protected void tearDown() throws Exception {
        if (s2 != null) {
            s2.logout();
            s2 = null;
        }
        super.tearDown();
    }
}
