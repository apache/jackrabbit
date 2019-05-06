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

import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.test.NotExecutableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>RemoveNodeTest</code>...
 */
public class RemoveReferenceableNodeTest extends RemoveNodeTest {

    private static Logger log = LoggerFactory.getLogger(RemoveReferenceableNodeTest.class);

    private String uuid;

    @Override
    protected Item createRemoveItem() throws NotExecutableException, RepositoryException {
        Node removeItem = (Node) super.createRemoveItem();
        // assert removeNode is referenceable
        if (!removeItem.isNodeType(mixReferenceable)) {
            if (!removeItem.canAddMixin(mixReferenceable)) {
                throw new NotExecutableException("Cannot make remove-node '" + nodeName1 + "' mix:referenceable.");
            }
            removeItem.addMixin(mixReferenceable);
        }

        // make sure the new node is persisted.
        testRootNode.save();
        uuid = removeItem.getUUID();
        return removeItem;
    }

    /**
     * Transiently removes a persisted node using {@link javax.jcr.Node#remove()}
     * and test, whether that node cannot be access by the UUID any more.
     */
    public void testAccessByUUID() throws RepositoryException {
        removeItem.remove();
        // check if the node has been properly removed
        try {
            superuser.getNodeByUUID(uuid);
            fail("Transiently removed node should no longer be accessible from parent node.");
        } catch (ItemNotFoundException e) {
            // ok , works as expected
        }
    }

    /**
     * Same as {@link #testRemoveNode()}, but calls save() before executing the
     * test.
     */
    public void testAccessByUUID2() throws RepositoryException, NotExecutableException {
        removeItem.remove();
        testRootNode.save();
        try {
            superuser.getNodeByUUID(uuid);
            fail("Permanently removed node should no longer be accessible from parent node.");
        } catch (ItemNotFoundException e) {
            // ok , works as expected
        }
    }
}
