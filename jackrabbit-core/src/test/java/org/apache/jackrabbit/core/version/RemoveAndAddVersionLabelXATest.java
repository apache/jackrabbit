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

import org.apache.jackrabbit.test.AbstractJCRTest;
import org.apache.jackrabbit.core.UserTransactionImpl;

import javax.jcr.Node;
import javax.transaction.UserTransaction;

/**
 * <code>RemoveAndAddVersionLabelXATest</code> implements a test case for
 * JCR-1587.
 */
public class RemoveAndAddVersionLabelXATest extends AbstractJCRTest {

    public void testVersionLabel() throws Exception {
        UserTransaction tx = new UserTransactionImpl(superuser);
        tx.begin();
        Node n = testRootNode.addNode(nodeName1);
        n.addMixin(mixVersionable);
        testRootNode.save();
        String v1 = n.checkin().getName();
        n.checkout();
        String v2 = n.checkin().getName();
        n.getVersionHistory().addVersionLabel(v2, "label", false);
        tx.commit();

        tx = new UserTransactionImpl(superuser);
        tx.begin();
        n.restore(v1, false);
        n.getVersionHistory().removeVersion(v2);
        n.checkout();
        v2 = n.checkin().getName();
        n.getVersionHistory().addVersionLabel(v2, "label", false);
        tx.commit();
    }
}
