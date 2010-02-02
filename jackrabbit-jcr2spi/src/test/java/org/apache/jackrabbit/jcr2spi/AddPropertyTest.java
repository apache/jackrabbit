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
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.jcr2spi.state.Status;
import org.apache.jackrabbit.test.AbstractJCRTest;
import org.apache.jackrabbit.test.NotExecutableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** <code>AddPropertyTest</code>... */
public class AddPropertyTest extends AbstractJCRTest {

    private static Logger log = LoggerFactory.getLogger(AddPropertyTest.class);

    private Node testNode;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        testNode = testRootNode.addNode(nodeName1);
        testNode.setProperty(propertyName1, "existingProp");
        testRootNode.save();
    }

    @Override
    protected void tearDown() throws Exception {
        testNode = null;
        super.tearDown();
    }

    private static void assertItemStatus(Item item, int status) throws NotExecutableException {
        if (!(item instanceof ItemImpl)) {
            throw new NotExecutableException("org.apache.jackrabbit.jcr2spi.ItemImpl expected");
        }
        int st = ((ItemImpl) item).getItemState().getStatus();
        assertEquals("Expected status to be " + Status.getName(status) + ", was " + Status.getName(st), status, st);
    }

    public void testReplacingProperty() throws RepositoryException,
            NotExecutableException {
        Property p1 = testNode.setProperty(propertyName1, "value1");
        p1.remove();

        Property p2 = testNode.setProperty(propertyName1, "value2");
        p2.remove();

        Property p3 = testNode.setProperty(propertyName1, "value3");
        testNode.save();

        assertTrue(testNode.hasProperty(propertyName1));
        assertEquals("value3", testNode.getProperty(propertyName1).getString());

        assertItemStatus(p1, Status.REMOVED);
        assertItemStatus(p2, Status.REMOVED);
        assertItemStatus(p3, Status.EXISTING);
    }

    public void testReplacingProperty2() throws RepositoryException,
            NotExecutableException {
        Property p1 = testNode.setProperty(propertyName2, "value1");
        p1.remove();

        Property p2 = testNode.setProperty(propertyName2, "value2");
        p2.remove();

        Property p3 = testNode.setProperty(propertyName2, "value3");
        p3.remove();
        testNode.save();

        assertFalse(testNode.hasProperty(propertyName2));

        assertItemStatus(p1, Status.REMOVED);
        assertItemStatus(p2, Status.REMOVED);
        assertItemStatus(p3, Status.REMOVED);
    }

    public void testRevertReplacingProperty() throws RepositoryException,
            NotExecutableException {
        String val = testNode.getProperty(propertyName1).getString();
        Property p1 = testNode.setProperty(propertyName1, "value1");
        p1.remove();

        Property p2 = testNode.setProperty(propertyName1, "value2");
        p2.remove();

        Property p3 = testNode.setProperty(propertyName1, "value3");
        testNode.refresh(false);

        assertTrue(testNode.hasProperty(propertyName1));
        assertEquals(val, p1.getString());

        assertItemStatus(p1, Status.EXISTING);
        assertItemStatus(p2, Status.REMOVED);
        assertItemStatus(p3, Status.REMOVED);
    }

    public void testAddingProperty() throws RepositoryException,
            NotExecutableException {
        Property p1 = testNode.setProperty(propertyName2, "value1");
        p1.remove();

        Property p2 = testNode.setProperty(propertyName2, "value2");
        p2.remove();

        Property p3 = testNode.setProperty(propertyName2, "value3");
        testNode.save();

        assertTrue(testNode.hasProperty(propertyName2));

        assertItemStatus(p1, Status.REMOVED);
        assertItemStatus(p2, Status.REMOVED);
        assertItemStatus(p3, Status.EXISTING);
    }

    public void testAddingProperty2() throws RepositoryException,
            NotExecutableException {
        Property p1 = testNode.setProperty(propertyName2, "value1");
        p1.remove();

        Property p2 = testNode.setProperty(propertyName2, "value2");
        p2.remove();

        Property p3 = testNode.setProperty(propertyName2, "value3");
        p3.remove();
        testNode.save();

        assertFalse(testNode.hasProperty(propertyName2));

        assertItemStatus(p1, Status.REMOVED);
        assertItemStatus(p2, Status.REMOVED);
        assertItemStatus(p3, Status.REMOVED);
    }

    public void testRevertAddingProperty() throws RepositoryException,
            NotExecutableException {
        Property p1 = testNode.setProperty(propertyName2, "value1");
        p1.remove();

        Property p2 = testNode.setProperty(propertyName2, "value2");
        p2.remove();

        Property p3 = testNode.setProperty(propertyName2, "value3");
        testNode.refresh(false);

        assertFalse(testNode.hasProperty(propertyName2));

        assertItemStatus(p1, Status.REMOVED);
        assertItemStatus(p2, Status.REMOVED);
        assertItemStatus(p3, Status.REMOVED);
    }
}