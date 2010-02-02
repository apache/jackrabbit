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

import javax.jcr.InvalidItemStateException;
import javax.jcr.Item;
import javax.jcr.ItemExistsException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.version.VersionException;

import org.apache.jackrabbit.test.AbstractJCRTest;
import org.apache.jackrabbit.test.NotExecutableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>RemoveItemTest</code>...
 */
public abstract class RemoveItemTest extends AbstractJCRTest {

    private static Logger log = LoggerFactory.getLogger(RemoveItemTest.class);

    protected Item removeItem;
    protected String removePath;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        removeItem = createRemoveItem();
        removePath = removeItem.getPath();
    }

    @Override
    protected void tearDown() throws Exception {
        removeItem = null;
        super.tearDown();
    }

    protected abstract Item createRemoveItem() throws NotExecutableException, RepositoryException, LockException, ConstraintViolationException, ItemExistsException, NoSuchNodeTypeException, VersionException;

    /**
     * Transiently removes a persisted item using {@link javax.jcr.Item#remove()}
     * and test, whether that item cannot be access from the session any more.
     */
    public void testRemoveItem() throws RepositoryException {
        removeItem.remove();

        // check if the node has been properly removed
        try {
            superuser.getItem(removePath);
            fail("A transiently removed item should no longer be accessible from the session.");
        } catch (PathNotFoundException e) {
            // ok , works as expected
        }
    }

    /**
     * Same as {@link #testRemoveItem()}, but calls save() (persisting the removal)
     * before executing the test.
     */
    public void testRemoveItem2() throws RepositoryException, NotExecutableException {
        removeItem.remove();
        testRootNode.save();
        try {
            superuser.getItem(removePath);
            fail("Persistently removed node should no longer be accessible from the session.");
        } catch (PathNotFoundException e) {
            // ok , works as expected
        }
    }
    /**
     * Test if a node, that has been transiently removed is not 'New'.
     */
    public void testNotNewRemovedItem() throws RepositoryException {
        removeItem.remove();
        assertFalse("Transiently removed node must not be 'new'.", removeItem.isNew());
    }

    /**
     * Same as {@link #testNotNewRemovedItem()} but calls save() before
     * executing the test.
     */
    public void testNotNewRemovedItem2() throws RepositoryException {
        removeItem.remove();
        testRootNode.save();
        assertFalse("Removed node must not be 'new'.", removeItem.isNew());
    }

    /**
     * Test if a node, that has be transiently remove is not 'Modified'.
     */
    public void testNotModifiedRemovedItem() throws RepositoryException {
        removeItem.remove();
        assertFalse("Transiently removed node must not be 'modified'.", removeItem.isModified());
    }

    /**
     * Same as {@link #testNotModifiedRemovedItem()} but calls save() before
     * executing the test.
     */
    public void testNotModifiedRemovedItem2() throws RepositoryException {
        removeItem.remove();
        testRootNode.save();
        assertFalse("Removed node must not be 'modified'.", removeItem.isModified());
    }

    /**
     * A removed item must throw InvalidItemStateException upon any call to an
     * item specific method.
     */
    public void testInvalidStateRemovedItem() throws RepositoryException {
        removeItem.remove();
        try {
            removeItem.getName();
            fail("Calling getName() on a removed node must throw InvalidItemStateException.");
        } catch (InvalidItemStateException e) {
            //ok
        }

        try {
            removeItem.getPath();
            fail("Calling getPath() on a removed node must throw InvalidItemStateException.");
        } catch (InvalidItemStateException e) {
            //ok
        }

        try {
            removeItem.save();
            fail("Calling save() on a removed node must throw InvalidItemStateException.");
        } catch (InvalidItemStateException e) {
            //ok
        }
    }

    /**
     * Same as {@link #testInvalidStateRemovedItem()} but calls save() before
     * executing the test.
     */
    public void testInvalidStateRemovedItem2() throws RepositoryException {
        removeItem.remove();
        testRootNode.save();
        try {
            removeItem.getName();
            fail("Calling getName() on a removed node must throw InvalidItemStateException.");
        } catch (InvalidItemStateException e) {
            //ok
        }

        try {
            removeItem.getPath();
            fail("Calling getPath() on a removed node must throw InvalidItemStateException.");
        } catch (InvalidItemStateException e) {
            //ok
        }

        try {
            removeItem.save();
            fail("Calling save() on a removed node must throw InvalidItemStateException.");
        } catch (InvalidItemStateException e) {
            //ok
        }
    }
}