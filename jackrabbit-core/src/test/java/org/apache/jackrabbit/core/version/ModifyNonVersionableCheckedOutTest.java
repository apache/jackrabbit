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

import org.apache.jackrabbit.test.AbstractJCRTest;

public class ModifyNonVersionableCheckedOutTest extends AbstractJCRTest {

    public void testNonVersionableCheckedOut() throws Exception {
        Node node = testRootNode.addNode(nodeName1, "nt:unstructured");
        superuser.save();

        assertTrue(node.isCheckedOut());

        node.setProperty("jcr:isCheckedOut", false);
        superuser.save();

        assertTrue(node.getPath() + " does not have mix:versionable and thus should be reported as checked out",
                node.isCheckedOut());
    }

    public void testModifyNonVersionableNodeWithCheckedOutProperty() throws Exception {
        Node node = testRootNode.addNode(nodeName1, "nt:unstructured");
        superuser.save();

        assertTrue(node.isCheckedOut());

        node.setProperty("jcr:isCheckedOut", false);
        superuser.save();

        node.setProperty("test", true);
        superuser.save();

        assertTrue(node.getProperty("test").getBoolean());

        node.setProperty("test", false);
        superuser.save();

        assertFalse(node.getProperty("test").getBoolean());

        node.getProperty("test").remove();
        superuser.save();
        assertFalse(node.hasProperty("test"));

        node.addNode(nodeName2, "nt:unstructured");
        superuser.save();

        assertTrue(node.hasNode(nodeName2));

        node.getNode(nodeName2).remove();
        superuser.save();

        assertFalse(node.hasNode(nodeName2));
    }

   public void testAddRemoveMixinVersionable() throws Exception {
        Node node = testRootNode.addNode(nodeName1, "nt:unstructured");
        node.addMixin(mixVersionable);
        superuser.save();
        node.checkin();
        superuser.save();
        assertFalse(node.isCheckedOut());
        node.checkout();
        superuser.save();
        assertTrue(node.isCheckedOut());
        node.removeMixin(mixVersionable);
        superuser.save();
        assertTrue(node.isCheckedOut());
        assertFalse(node.hasProperty(jcrIsCheckedOut));
    }
}
