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
package org.apache.jackrabbit.core.version;

import org.apache.jackrabbit.core.UserTransactionImpl;
import org.apache.jackrabbit.test.AbstractJCRTest;

import javax.jcr.Node;
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.version.Version;

/**
 * Test case for JCR-1481
 */
public class CheckinRemoveVersionTest extends AbstractJCRTest {

    public void testCheckinRemoveVersionWithXA() throws Exception {
        Node n = testRootNode.addNode(nodeName1);
        n.addMixin(mixVersionable);
        testRootNode.save();
        UserTransactionImpl tx = new UserTransactionImpl(superuser);
        tx.begin();
        try {
            Version v10 = n.checkin();
            assertTrue("Version.getReferences() must return base version", v10.getReferences().hasNext());
            try {
                n.getVersionHistory().removeVersion(v10.getName());
                fail("VersionHistory.removeVersion() must throw ReferentialIntegrityException when" +
                        " version is still referenced.");
            } catch (ReferentialIntegrityException e) {
                // expected
            }
        } finally {
            tx.rollback();
        }
    }
}
