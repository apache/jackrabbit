/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

import javax.jcr.Session;
import javax.jcr.PropertyType;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.nodetype.NodeType;
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
     * The session we use for the tests
     */
    private Session session;

    private Property booleanProperty;
    private Property dateProperty;

    /**
     * Sets up the fixture for the test cases.
     */
    protected void setUp() throws Exception {
        super.setUp();
        session = helper.getReadOnlySession();

        // create a node with a boolean property
        PropertyDefinition booleanPropDef =
                NodeTypeUtil.locatePropertyDef(session, PropertyType.BOOLEAN, false, false, false, false);
        if (booleanPropDef != null) {
            NodeType nodeType = booleanPropDef.getDeclaringNodeType();

            Node n = testRootNode.addNode(nodeName1, nodeType.getName());

            Value initValue = NodeTypeUtil.getValueOfType(session, PropertyType.BOOLEAN);
            booleanProperty = n.setProperty(booleanPropDef.getName(), initValue);
        }

        // create a node with a date property
        PropertyDefinition datePropDef =
                NodeTypeUtil.locatePropertyDef(session, PropertyType.DATE, false, false, false, false);
        if (datePropDef != null) {
            NodeType nodeType = datePropDef.getDeclaringNodeType();

            Node n = testRootNode.addNode(nodeName2, nodeType.getName());

            Value initValue = NodeTypeUtil.getValueOfType(session, PropertyType.DATE);
            dateProperty = n.setProperty(datePropDef.getName(), initValue);
        }
    }

    /**
     * Releases the session aquired in {@link #setUp()}.
     */
    protected void tearDown() throws Exception {
        if (session != null) {
            session.logout();
        }
        super.tearDown();
    }

    /**
     * Tests if setValue(Value) throws a ValueFormatException immediately (not
     * on save) if a conversion fails.
     */
    public void testValue()
        throws NotExecutableException, RepositoryException {

        if (booleanProperty == null) {
            throw new NotExecutableException("Failed to set up required test items.");
        }

        try {
            Value dateValue = NodeTypeUtil.getValueOfType(session, PropertyType.DATE);
            booleanProperty.setValue(dateValue);
            fail("Property.setValue(Value) must throw a ValueFormatException " +
                    "immediately if a conversion fails.");
        }
        catch (ValueFormatException e) {
            // success
        }
    }

    /**
     * Tests if setValue(Value[]) throws a ValueFormatException immediately (not
     * on save) if a conversion fails.
     */
    public void testValueArray()
        throws NotExecutableException, RepositoryException {

        // create a node with a multiple boolean property
        PropertyDefinition propDef =
                NodeTypeUtil.locatePropertyDef(session, PropertyType.BOOLEAN, true, false, false, false);
        if (propDef == null) {
            throw new NotExecutableException("Failed to set up required test items.");
        }

        NodeType nodeType = propDef.getDeclaringNodeType();

        Node n = testRootNode.addNode(nodeName3, nodeType.getName());

        Value initValues[] = new Value[] {NodeTypeUtil.getValueOfType(session, PropertyType.BOOLEAN)};
        Property property = n.setProperty(propDef.getName(), initValues);

        try {
            Value dateValues[] =
                new Value[] {NodeTypeUtil.getValueOfType(session, PropertyType.DOUBLE)};
            property.setValue(dateValues);
            fail("Property.setValue(Value[]) must throw a ValueFormatException " +
                    "immediately if a conversion fails.");
        }
        catch (ValueFormatException e) {
            // success
        }
    }

    /**
     * Tests if setValue(String) throws a ValueFormatException immediately (not
     * on save) if a conversion fails.
     */
    public void testString()
        throws NotExecutableException, RepositoryException {

        if (dateProperty == null) {
            throw new NotExecutableException("Failed to set up required test items.");
        }

        try {
            dateProperty.setValue("abc");
            fail("Property.setValue(String) must throw a ValueFormatException " +
                    "immediately if a conversion fails.");
        }
        catch (ValueFormatException e) {
            // success
        }
    }

    /**
     * Tests if setValue(String[]) throws a ValueFormatException immediately (not
     * on save) if a conversion fails.
     */
    public void testStringArray()
        throws NotExecutableException, RepositoryException {

        // create a node with a multiple date property
        PropertyDefinition propDef =
                NodeTypeUtil.locatePropertyDef(session, PropertyType.DATE, true, false, false, false);
        if (propDef == null) {
            throw new NotExecutableException("Failed to set up required test items.");
        }

        NodeType nodeType = propDef.getDeclaringNodeType();

        Node n = testRootNode.addNode(nodeName3, nodeType.getName());

        Value initValues[] = new Value[] {NodeTypeUtil.getValueOfType(session, PropertyType.DATE)};
        Property property = n.setProperty(propDef.getName(), initValues);

        try {
            String values[] = new String[] {"abc"};
            property.setValue(values);
            fail("Property.setValue(String[]) must throw a ValueFormatException " +
                    "immediately if a conversion fails.");
        }
        catch (ValueFormatException e) {
            // success
        }
    }

    /**
     * Tests if setValue(InputStream) throws a ValueFormatException immediately (not
     * on save) if a conversion fails.
     */
    public void testInputStream()
        throws NotExecutableException, RepositoryException {

        if (dateProperty == null) {
            throw new NotExecutableException("Failed to set up required test items.");
        }

        try {
            byte[] bytes = {123};
            InputStream value = new ByteArrayInputStream(bytes);
            dateProperty.setValue(value);
            fail("Property.setValue(InputStream) must throw a ValueFormatException " +
                    "immediately if a conversion fails.");
        }
        catch (ValueFormatException e) {
            // success
        }
    }

    /**
     * Tests if setValue(long) throws a ValueFormatException immediately (not
     * on save) if a conversion fails.
     */
    public void testLong()
        throws NotExecutableException, RepositoryException {

        if (booleanProperty == null) {
            throw new NotExecutableException("Failed to set up required test items.");
        }

        try {
            booleanProperty.setValue(123);
            fail("Property.setValue(long) must throw a ValueFormatException " +
                    "immediately if a conversion fails.");
        }
        catch (ValueFormatException e) {
            // success
        }
    }

    /**
     * Tests if setValue(double) throws a ValueFormatException immediately (not
     * on save) if a conversion fails.
     */
    public void testDouble()
        throws NotExecutableException, RepositoryException {

        if (booleanProperty == null) {
            throw new NotExecutableException("Failed to set up required test items.");
        }

        try {
            booleanProperty.setValue(1.23);
            fail("Property.setValue(double) must throw a ValueFormatException " +
                    "immediately if a conversion fails.");
        }
        catch (ValueFormatException e) {
            // success
        }
    }

    /**
     * Tests if setValue(Calendar) throws a ValueFormatException immediately (not
     * on save) if a conversion fails.
     */
    public void testCalendar()
        throws NotExecutableException, RepositoryException {

        if (booleanProperty == null) {
            throw new NotExecutableException("Failed to set up required test items.");
        }

        try {
            booleanProperty.setValue(Calendar.getInstance());
            fail("Property.setValue(Calendar) must throw a ValueFormatException " +
                    "immediately if a conversion fails.");
        }
        catch (ValueFormatException e) {
            // success
        }
    }

    /**
     * Tests if setValue(boolean) throws a ValueFormatException immediately (not
     * on save) if a conversion fails.
     */
    public void testBoolean()
        throws NotExecutableException, RepositoryException {

        if (dateProperty == null) {
            throw new NotExecutableException("Failed to set up required test items.");
        }

        try {
            dateProperty.setValue(true);
            fail("Property.setValue(boolean) must throw a ValueFormatException " +
                    "immediately if a conversion fails.");
        }
        catch (ValueFormatException e) {
            // success
        }
    }

    /**
     * Tests if setValue(Node) throws a ValueFormatException immediately (not
     * on save) if the property is not of type REFERENCE.
     */
    public void testNode()
        throws NotExecutableException, RepositoryException {

        if (booleanProperty == null) {
            throw new NotExecutableException("Failed to set up required test items.");
        }

        try {
            Node referenceableNode = testRootNode.addNode(nodeName3);
            referenceableNode.addMixin(mixReferenceable);

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

        // create a node with a reference property
        PropertyDefinition propDef =
                NodeTypeUtil.locatePropertyDef(session, PropertyType.REFERENCE, false, false, false, false);
        if (propDef == null) {
            throw new NotExecutableException("Failed to set up required test items.");
        }
        NodeType nodeType = propDef.getDeclaringNodeType();
        Node n = testRootNode.addNode(nodeName3, nodeType.getName());

        // create a referenceable node (to init property)
        Node referenceableNode = testRootNode.addNode(nodeName4 + "Ref");
        referenceableNode.addMixin(mixReferenceable);

        // create a not referenceable node (to throw the exception)
        Node notReferenceableNode = testRootNode.addNode(nodeName4 + "NotRef");

        Property property = n.setProperty(propDef.getName(), referenceableNode);

        try {
            property.setValue(notReferenceableNode);
            fail("Property.setValue(Node) must throw a ValueFormatException " +
                    "immediately if the specified node is not referenceable.");
        }
        catch (ValueFormatException e) {
            // success
        }
    }
}
