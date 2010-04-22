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

import javax.jcr.Node;
import javax.jcr.Session;
import javax.transaction.UserTransaction;

import org.apache.jackrabbit.core.UserTransactionImpl;
import org.apache.jackrabbit.test.AbstractJCRTest;

public class RemoveAndCheckinXATest extends AbstractJCRTest {

    public void testRemoveVersion() throws Exception {
        UserTransaction tx = new UserTransactionImpl(superuser);
        tx.begin();
        Node n = testRootNode.addNode(nodeName1);
        n.addMixin(mixVersionable);
        n.addMixin(mixReferenceable);
        testRootNode.save();
        String uuid = n.getUUID();
        // create two versions
        String v1 = n.checkin().getName();
        n.checkout();
        n.checkin();
        n.checkout();
        tx.commit();

        tx = new UserTransactionImpl(superuser);
        tx.begin();
        // remove on version
        n = superuser.getNodeByUUID(uuid);
        n.getVersionHistory().removeVersion(v1);
        n.save();
        tx.commit();

        // new session
        Session session = helper.getSuperuserSession();
        // for jackrabbit 2.x
        // Session session = getHelper().getSuperuserSession();
        tx = new UserTransactionImpl(session);
        tx.begin();
        n = session.getNodeByUUID(uuid);
        n.checkin();
        tx.commit();
    }

}
