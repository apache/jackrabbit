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
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.version.VersionException;

import org.apache.jackrabbit.test.NotExecutableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>RemoveNodeTest</code>...
 */
public class RemovePropertyTest extends RemoveItemTest {

    private static Logger log = LoggerFactory.getLogger(RemovePropertyTest.class);

    @Override
    protected Item createRemoveItem() throws NotExecutableException, RepositoryException, LockException, ConstraintViolationException, ItemExistsException, NoSuchNodeTypeException, VersionException {
        Property removeProperty;
        if (testRootNode.hasProperty(propertyName1)) {
            removeProperty = testRootNode.getProperty(propertyName1);
            if (removeProperty.getDefinition().isProtected() || removeProperty.getDefinition().isMandatory()) {
                throw new NotExecutableException("Property to be remove must be mandatory nor protected '" + propertyName1 + "'.");
            }
        } else {
            removeProperty = testRootNode.setProperty(propertyName1, "anyString");
        }
        // make sure the new node is persisted.
        testRootNode.save();
        return removeProperty;
    }

    /**
     * Transiently removes a persisted property using {@link Property#remove()}
     * and test, whether that property cannot be access from its parent.
     */
    public void testRemoveProperty() throws RepositoryException {
        removeItem.remove();
        // check if the property has been properly removed
        try {
            testRootNode.getProperty(propertyName1);
            fail("Transiently removed property should no longer be accessible from parent node.");
        } catch (PathNotFoundException e) {
            // ok , works as expected
        }
    }

    /**
     * Same as {@link #testRemoveProperty()}, but calls save() (persisting the removal)
     * before executing the test.
     */
    public void testRemoveProperty2() throws RepositoryException, NotExecutableException {
        removeItem.remove();
        testRootNode.save();
        try {
            testRootNode.getProperty(propertyName1);
            fail("Permanently removed property should no longer be accessible from parent node.");
        } catch (PathNotFoundException e) {
            // ok , works as expected
        }
    }

    /**
     * A removed property must throw InvalidItemStateException upon any call to a
     * property specific method.
     */
    public void testInvalidStateRemovedProperty() throws RepositoryException {
        removeItem.remove();

        try {
            ((Property)removeItem).getType();
            fail("Calling getType() on a removed property must throw InvalidItemStateException.");
        } catch (InvalidItemStateException e) {
            //ok
        }

        try {
            ((Property)removeItem).getValue();
            fail("Calling getValue() on a removed property must throw InvalidItemStateException.");
        } catch (InvalidItemStateException e) {
            //ok
        }
    }

    /**
     * Same as {@link #testInvalidStateRemovedProperty()} but calls save() before
     * executing the test.
     */
    public void testInvalidStateRemovedProperty2() throws RepositoryException {
        removeItem.remove();
        testRootNode.save();
                try {
            ((Property)removeItem).getType();
            fail("Calling getType() on a removed property must throw InvalidItemStateException.");
        } catch (InvalidItemStateException e) {
            //ok
        }

        try {
            ((Property)removeItem).getValue();
            fail("Calling getValue() on a removed property must throw InvalidItemStateException.");
        } catch (InvalidItemStateException e) {
            //ok
        }
    }
}
