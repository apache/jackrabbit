/*
 * $Id$
 *
 * Copyright 1997-2005 Day Management AG
 * Barfuesserplatz 6, 4001 Basel, Switzerland
 * All Rights Reserved.
 *
 * This software is the confidential and proprietary information of
 * Day Management AG, ("Confidential Information"). You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Day.
 */
package org.apache.jackrabbit.jcr2spi;

import org.apache.jackrabbit.test.NotExecutableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Item;
import javax.jcr.Node;

/**
 * <code>RemoveNodeTest</code>...
 */
public class RemoveReferenceableTest extends RemoveNodeTest {

    private static Logger log = LoggerFactory.getLogger(RemoveReferenceableTest.class);

    private String uuid;

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
            fail("Permanently removed node should no longer be accessible from parent node.");
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
            fail("Transiently removed node should no longer be accessible from parent node.");
        } catch (ItemNotFoundException e) {
            // ok , works as expected
        }
    }
}
