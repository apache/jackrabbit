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

import javax.jcr.ItemNotFoundException;
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
 * <code>AddNewPropertyTest</code>...
 */
public class AddNewPropertyTest extends AbstractJCRTest {

    private static Logger log = LoggerFactory.getLogger(AddNewPropertyTest.class);

    private String propname;

    @Override
    protected void tearDown() throws Exception {
        testRootNode.refresh(false);
        super.tearDown();
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        String propName = propertyName1;
        while (testRootNode.hasProperty(propName)) {
            propName = propName + "_";
        }
        this.propname = propName;
    }

    public void testPropertyAccessibleAfterSave() throws NotExecutableException, RepositoryException {
        Property p;
        try {
            p = testRootNode.setProperty(propname, "anyValue");
        } catch (RepositoryException e) {
            throw new NotExecutableException();
        }

        // check if p is valid and can be accessed from the parent node.
        String name = p.getName();
        assertEquals("Added property must have the original name", name, propname);
        assertTrue("Accessing the created property again must return the 'same' item.", p.isSame(testRootNode.getProperty(propname)));
    }

    /**
     * Implementation specific test: Node.setProperty for non-existing property
     * with a <code>null</code> value must throw  <code>ItemNotFoundException</code>
     *
     * @throws NotExecutableException
     * @throws RepositoryException
     * @throws LockException
     * @throws ConstraintViolationException
     * @throws VersionException
     */
    public void testAddPropertyWithNullValue() throws NotExecutableException, RepositoryException, LockException, ConstraintViolationException, VersionException {
        try {
            testRootNode.setProperty(propname, (Value) null);
        } catch (ItemNotFoundException e) {
            // OK
        }
    }

    /**
     * Implementation specific test: Node.setProperty for non-existing property
     * with a <code>null</code> value array must throw  <code>ItemNotFoundException</code>
     *
     * @throws NotExecutableException
     * @throws RepositoryException
     * @throws LockException
     * @throws ConstraintViolationException
     * @throws VersionException
     */
    public void testAddPropertyWithNullValues() throws NotExecutableException, RepositoryException, LockException, ConstraintViolationException, VersionException {
        try {
            testRootNode.setProperty(propname, (Value[]) null);
        } catch (ItemNotFoundException e) {
            // OK
        }
    }
}