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
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;

import org.apache.jackrabbit.test.AbstractJCRTest;
import org.apache.jackrabbit.test.NotExecutableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>RefreshFalseTest</code>...
 */
public class RefreshFalseTest extends AbstractJCRTest {

    private static Logger log = LoggerFactory.getLogger(RefreshFalseTest.class);

    private Value testValue;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        if (testRootNode.hasProperty(propertyName1)) {
            testRootNode.getProperty(propertyName1).remove();
            testRootNode.save();
        }

        testValue = testRootNode.getSession().getValueFactory().createValue("anyString");
        if (!testRootNode.getPrimaryNodeType().canSetProperty(propertyName1, testValue)) {
            throw new NotExecutableException("");
        }
    }

    @Override
    protected void tearDown() throws Exception {
        testValue = null;
        super.tearDown();
    }

    public void testNewProperty() throws RepositoryException, LockException, ConstraintViolationException, VersionException {
        Property p = testRootNode.setProperty(propertyName1, testValue);
        testRootNode.refresh(false);

        try {
            p.getString();
            fail("Refresh 'false' must invalidate a new child property");
        } catch (InvalidItemStateException e) {
            // ok
        }
        assertFalse("Refresh 'false' must remove a new child property", testRootNode.hasProperty(propertyName1));
    }

    public void testRemovedNewProperty() throws RepositoryException, LockException, ConstraintViolationException, VersionException {
        Property p = testRootNode.setProperty(propertyName1, testValue);
        p.remove();

        testRootNode.refresh(false);

        try {
            p.getString();
            fail("Refresh 'false' must not bring a removed new child property back to life.");
        } catch (InvalidItemStateException e) {
            // ok
        }
        assertFalse("Refresh 'false' must not bring a removed new child property back to life.", testRootNode.hasProperty(propertyName1));
    }

    public void testRemovedProperty() throws RepositoryException, LockException, ConstraintViolationException, VersionException {
        Property p = testRootNode.setProperty(propertyName1, testValue);
        testRootNode.save();

        p.remove();
        testRootNode.refresh(false);

        // Property p must be reverted to 'existing' -> getString must succeed.
        p.getString();
        // similarly accessing the property again must succeed.
        testRootNode.getProperty(propertyName1);
    }

    public void testShadowingProperty() throws RepositoryException, LockException, ConstraintViolationException, VersionException {
        Property p = testRootNode.setProperty(propertyName1, testValue);
        testRootNode.save();

        p.remove();
        Property pNew = testRootNode.setProperty(propertyName1, "SomeOtherTestValue");

        testRootNode.refresh(false);

        try {
            pNew.getString();
            fail("Refresh 'false' must remove a new (shadowing) property and bring 'removed' persistent property back to life.");
        } catch (InvalidItemStateException e) {
            // ok
        }

        // Property p must be reverted to 'existing' -> getString must succeed.
        p.getString();
        // similarly accessing the property again must succeed.
        Property pAgain = testRootNode.getProperty(propertyName1);
        assertTrue("Refresh 'false' must remove a new property and bring 'removed' persistent property back to life.", p.isSame(pAgain));
    }
}