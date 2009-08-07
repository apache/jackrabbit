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
package org.apache.jackrabbit.test.api;

import org.apache.jackrabbit.test.AbstractJCRTest;
import org.apache.jackrabbit.test.NotExecutableException;
import org.apache.jackrabbit.test.api.nodetype.NodeTypeUtil;

import javax.jcr.PropertyType;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.RepositoryException;

import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.util.Calendar;

/**
 * <code>SetValueValueFormatExceptionTest</code> tests if Property.setValue() throws
 * a ValueFormatException if a best-effort conversion fails.
 * The ValueFormatException has to be thrown immediately (not on save).
 *
 * @test
 * @sources SetValueValueFormatExceptionTest.java
 * @executeClass org.apache.jackrabbit.test.api.SetValueValueFormatExceptionTest
 * @keywords level2
 */
public class SetValueValueFormatExceptionTest extends AbstractJCRTest {

    /**
     * Tests if setValue(Value) throws a ValueFormatException immediately (not
     * on save) if a conversion fails.
     */
    public void testValue()
        throws NotExecutableException, RepositoryException {

        Property booleanProperty = createProperty(PropertyType.BOOLEAN, false);
        try {
            Value dateValue = NodeTypeUtil.getValueOfType(superuser, PropertyType.DATE);
            booleanProperty.setValue(dateValue);
            fail("Property.setValue(Value) must throw a ValueFormatException " +
                    "immediately if a conversion fails.");
        } catch (ValueFormatException e) {
            // success
        }
    }

    /**
     * Tests if setValue(Value[]) throws a ValueFormatException immediately (not
     * on save) if a conversion fails.
     */
    public void testValueArray()
        throws NotExecutableException, RepositoryException {

        Property booleanProperty = createProperty(PropertyType.BOOLEAN, true);
        try {
            Value dateValues[] =
                    new Value[]{NodeTypeUtil.getValueOfType(superuser, PropertyType.DOUBLE)};
            booleanProperty.setValue(dateValues);
            fail("Property.setValue(Value[]) must throw a ValueFormatException " +
                    "immediately if a conversion fails.");
        } catch (ValueFormatException e) {
            // success
        }
    }

    /**
     * Tests if setValue(String) throws a ValueFormatException immediately (not
     * on save) if a conversion fails.
     */
    public void testString()
        throws NotExecutableException, RepositoryException {

        Property dateProperty = createProperty(PropertyType.DATE, false);
        try {
            dateProperty.setValue("abc");
            fail("Property.setValue(String) must throw a ValueFormatException " +
                    "immediately if a conversion fails.");
        } catch (ValueFormatException e) {
            // success
        }
    }

    /**
     * Tests if setValue(String[]) throws a ValueFormatException immediately (not
     * on save) if a conversion fails.
     */
    public void testStringArray()
        throws NotExecutableException, RepositoryException {

        Property dateProperty = createProperty(PropertyType.DATE, true);
        try {
            String values[] = new String[]{"abc"};
            dateProperty.setValue(values);
            fail("Property.setValue(String[]) must throw a ValueFormatException " +
                    "immediately if a conversion fails.");
        } catch (ValueFormatException e) {
            // success
        }
    }

    /**
     * Tests if setValue(InputStream) throws a ValueFormatException immediately (not
     * on save) if a conversion fails.
     */
    public void testInputStream()
        throws NotExecutableException, RepositoryException {

        Property dateProperty = createProperty(PropertyType.DATE, false);
        try {
            byte[] bytes = {123};
            InputStream value = new ByteArrayInputStream(bytes);
            dateProperty.setValue(value);
            fail("Property.setValue(InputStream) must throw a ValueFormatException " +
                    "immediately if a conversion fails.");
        } catch (ValueFormatException e) {
            // success
        }
    }

    /**
     * Tests if setValue(long) throws a ValueFormatException immediately (not
     * on save) if a conversion fails.
     */
    public void testLong()
        throws NotExecutableException, RepositoryException {

        Property booleanProperty = createProperty(PropertyType.BOOLEAN, false);
        try {
            booleanProperty.setValue(123);
            fail("Property.setValue(long) must throw a ValueFormatException " +
                    "immediately if a conversion fails.");
        } catch (ValueFormatException e) {
            // success
        }
    }

    /**
     * Tests if setValue(double) throws a ValueFormatException immediately (not
     * on save) if a conversion fails.
     */
    public void testDouble()
        throws NotExecutableException, RepositoryException {

        Property booleanProperty = createProperty(PropertyType.BOOLEAN, false);
        try {
            booleanProperty.setValue(1.23);
            fail("Property.setValue(double) must throw a ValueFormatException " +
                    "immediately if a conversion fails.");
        } catch (ValueFormatException e) {
            // success
        }
    }

    /**
     * Tests if setValue(Calendar) throws a ValueFormatException immediately (not
     * on save) if a conversion fails.
     */
    public void testCalendar()
        throws NotExecutableException, RepositoryException {

        Property booleanProperty = createProperty(PropertyType.BOOLEAN, false);
        try {
            booleanProperty.setValue(Calendar.getInstance());
            fail("Property.setValue(Calendar) must throw a ValueFormatException " +
                    "immediately if a conversion fails.");
        } catch (ValueFormatException e) {
            // success
        }
    }

    /**
     * Tests if setValue(boolean) throws a ValueFormatException immediately (not
     * on save) if a conversion fails.
     */
    public void testBoolean()
        throws NotExecutableException, RepositoryException {

        Property dateProperty = createProperty(PropertyType.DATE, false);
        try {
            dateProperty.setValue(true);
            fail("Property.setValue(boolean) must throw a ValueFormatException " +
                    "immediately if a conversion fails.");
        } catch (ValueFormatException e) {
            // success
        }
    }

    /**
     * Tests if setValue(Node) throws a ValueFormatException immediately (not
     * on save) if the property is not of type REFERENCE.
     */
    public void testNode()
        throws NotExecutableException, RepositoryException {

        Property booleanProperty = createProperty(PropertyType.BOOLEAN, false);
        try {
            Node referenceableNode = testRootNode.addNode(nodeName3, testNodeType);
            if (needsMixin(referenceableNode, mixReferenceable)) {
                referenceableNode.addMixin(mixReferenceable);
            }

            // some implementations may require a save after addMixin()
            testRootNode.save();

            // make sure the node is now referenceable
            assertTrue("test node should be mix:referenceable", referenceableNode.isNodeType(mixReferenceable));

            booleanProperty.setValue(referenceableNode);
            fail("Property.setValue(Node) must throw a ValueFormatException " +
                    "immediately if the property is not of type REFERENCE.");
        }
        catch (ValueFormatException e) {
            // success
        }
    }

    /**
     * Tests if setValue(Node) throws a ValueFormatException immediately (not
     * on save) if the specified node is not referencable.
     */
    public void testNodeNotReferenceable()
        throws NotExecutableException, RepositoryException {

        if (testRootNode.isNodeType(mixReferenceable)) {
            throw new NotExecutableException("test requires testRootNode to be non-referenceable");
        }

        Property referenceProperty = createProperty(PropertyType.REFERENCE, false);
        try {
            referenceProperty.setValue(testRootNode);
            fail("Property.setValue(Node) must throw a ValueFormatException " +
                    "immediately if the specified node is not referenceable.");
        } catch (ValueFormatException e) {
            // success
        }
    }

    /**
     * Creates a node under {@link #testRootNode} and sets a property on with
     * <code>propertyType</code> on the newly created node.
     *
     * @param propertyType the type of the property to create.
     * @param multiple     if the property must support multiple values.
     * @return the property
     * @throws RepositoryException    if an error occurs
     * @throws NotExecutableException if there is no such property defined on
     *                                the node type for the new child node.
     */
    private Property createProperty(int propertyType, boolean multiple) throws RepositoryException, NotExecutableException {
        Node n = testRootNode.addNode(nodeName1, testNodeType);
        if (propertyType == PropertyType.REFERENCE && !n.isNodeType(mixReferenceable)) {
            if (!n.canAddMixin(mixReferenceable)) {
                throw new NotExecutableException(testNodeType + " does not support adding of mix:referenceable");
            } else {
                n.addMixin(mixReferenceable);
                // some implementations may require a save after addMixin()
                testRootNode.save();
            }
        }

        Value initValue;
        if (propertyType == PropertyType.REFERENCE) {
            initValue = superuser.getValueFactory().createValue(n);
        } else {
            initValue = NodeTypeUtil.getValueOfType(superuser, propertyType);
        }

        if (multiple) {
            Value[] initValues = new Value[]{initValue};
            if (!n.getPrimaryNodeType().canSetProperty(propertyName1, initValues)) {

                throw new NotExecutableException("Node type: " + testNodeType +
                        " does not support a multi valued " +
                        PropertyType.nameFromValue(propertyType) + " property " +
                        "called: " + propertyName1);
            }
            return n.setProperty(propertyName1, initValues);
        } else {
            if (!n.getPrimaryNodeType().canSetProperty(propertyName1, initValue)) {

                throw new NotExecutableException("Node type: " + testNodeType +
                        " does not support a single valued " +
                        PropertyType.nameFromValue(propertyType) + " property " +
                        "called: " + propertyName1);
            }
            return n.setProperty(propertyName1, initValue);
        }
    }
}
