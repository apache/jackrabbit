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

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Node;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.Version;

/**
 * <code>ReadVersionsWhileModified</code> tests if version history can be read
 * consistently while it is modified by another thread.
 * <p>
 * This is a test case for: <a href="http://issues.apache.org/jira/browse/JCR-18">
 * JCR-18</a>
 */
public class ReadVersionsWhileModified extends AbstractConcurrencyTest {

    private static final int RUN_NUM_SECONDS = 20;

    public void testVersionHistory() throws RepositoryException {
        final Node n = testRootNode.addNode(nodeName1);
        n.addMixin(mixVersionable);
        testRootNode.save();
        final Session s = getHelper().getSuperuserSession();
        Thread t = new Thread(new Runnable() {
            public void run() {
                long end = System.currentTimeMillis() + RUN_NUM_SECONDS * 1000;
                try {
                    Node vn = (Node) s.getItem(n.getPath());
                    while (end > System.currentTimeMillis()) {
                        vn.checkout();
                        vn.checkin();
                    }
                } catch (RepositoryException e) {
                    e.printStackTrace();
                } finally  {
                    s.logout();
                }
            }
        });
        t.start();
        VersionHistory vh = n.getVersionHistory();
        while (t.isAlive()) {
            // walk version history
            Version v = vh.getRootVersion();
            while (v.getSuccessors().length > 0) {
                v = v.getSuccessors()[0];
            }
        }
    }
}
