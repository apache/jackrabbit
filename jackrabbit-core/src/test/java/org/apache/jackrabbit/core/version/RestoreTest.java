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
import javax.jcr.version.Version;
import javax.jcr.version.VersionManager;

/**
 * Test case for JCR-1476.
 */
public class RestoreTest extends AbstractJCRTest {

    public void testRestoreWithXA() throws Exception {
        Node n = testRootNode.addNode(nodeName1);
        n.addMixin(mixVersionable);
        testRootNode.save();
        UserTransactionImpl tx = new UserTransactionImpl(superuser);
        tx.begin();
        Version v10 = n.checkin();
        String versionName = v10.getName();
        n.restore(v10, true);
        assertEquals("Wrong version restored", versionName, n.getBaseVersion().getName());
        tx.commit();
    }

    public void testRestoreToNodeWithAnUnknownPropertyType() throws Exception {
        // create n from a type without jcr:title and no matching residual properties, unlike default nt:unstructured
        Node n = testRootNode.addNode(nodeName1, "nt:query");
        n.addMixin(mixVersionable);
        superuser.save();
        VersionManager versionManager = superuser.getWorkspace().getVersionManager();
        Version v10 = versionManager.checkin(n.getPath());
        versionManager.checkout(n.getPath());
        // use mix:title to allow adding property jcr:title
        n.addMixin("mix:title");
        // add jcr:title property which is *not* allowed by the frozen node its type alone (nt:query)
        n.setProperty("jcr:title", "title");
        superuser.save();
        // restore base version without mix:title mixin, removing jcr:title property
        versionManager.restore(v10, true);
        assertFalse(n.hasProperty("jcr:title"));
    }
}
