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

import javax.jcr.RepositoryException;
import javax.jcr.PropertyType;
import javax.jcr.Value;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeType;
import java.io.InputStream;
import java.io.IOException;

/**
 * <code>SetValueConstraintViolationExceptionTest</code> tests if setValue()
 * throws a ConstraintViolationException either immediately (by setValue()) or
 * on save, if the change would violate a value constraint.
 *
 * @test
 * @sources SetValueConstraintViolationExceptionTest.java
 * @executeClass org.apache.jackrabbit.test.api.SetValueConstraintViolationExceptionTest
 * @keywords level2
 */
public class SetValueConstraintViolationExceptionTest extends AbstractJCRTest {

    /**
     * Tests if setValue(InputStream value) and setValue(Value value) where
     * value is a BinaryValue throw a ConstraintViolationException if the change
     * would violate a value constraint
     */
    public void testBinaryProperty()
            throws NotExecutableException, RepositoryException {

        // locate a PropertyDefinition with ValueConstraints
        PropertyDefinition propDef =
                NodeTypeUtil.locatePropertyDef(superuser, PropertyType.BINARY, false, false, true, false);

        if (propDef == null) {
            throw new NotExecutableException("No binary property def with " +
                    "testable value constraints has been found");
        }

        // find a Value that does not satisfy the ValueConstraints of propDef
        Value valueNotSatisfied1 = NodeTypeUtil.getValueAccordingToValueConstraints(superuser, propDef, false);
        Value valueNotSatisfied2 = NodeTypeUtil.getValueAccordingToValueConstraints(superuser, propDef, false);
        if (valueNotSatisfied1 == null || valueNotSatisfied2 == null) {
            throw new NotExecutableException("No binary property def with " +
                    "testable value constraints has been found");
        }

        // find a Value that does satisfy the ValueConstraints of propDef
        Value valueSatisfied = NodeTypeUtil.getValueAccordingToValueConstraints(superuser, propDef, true);
        if (valueSatisfied == null) {
            throw new NotExecutableException("The value constraints do not allow any value.");
        }

        // create a sub node of testRootNode of type propDef.getDeclaringNodeType()
        // and add a property with constraints to this node
        Node node;
        Property prop;
        try {
            String nodeType = propDef.getDeclaringNodeType().getName();
            node = testRootNode.addNode(nodeName2, nodeType);
            prop = node.setProperty(propDef.getName(), valueSatisfied);
            testRootNode.save();
        } catch (ConstraintViolationException e) {
            // implementation specific constraints do not allow to set up test environment
            throw new NotExecutableException("Not able to create required test items.");
        }

        // test of signature setValue(InputStream value)
        InputStream in = valueNotSatisfied1.getStream();
        try {
            prop.setValue(in);
            node.save();
            fail("setValue(InputStream value) must throw a ConstraintViolationException " +
                    "if the change would violate a node type constraint " +
                    "either immediately or on save");
        } catch (ConstraintViolationException e) {
            // success
        } finally {
            try {
                in.close();
            } catch (IOException ignore) {}
        }

        // test of signature setValue(Value value)
        try {
            prop.setValue(valueNotSatisfied2);
            node.save();
            fail("setValue(Value value) must throw a ConstraintViolationException " +
                    "if the change would violate a node type constraint " +
                    "either immediately or on save");
        } catch (ConstraintViolationException e) {
            // success
        }
    }

    /**
     * Tests if setValue(boolean value) and setValue(Value value) where value is
     * a BooleanValue throw a ConstraintViolationException if the change would
     * violate a value constraint
     */
    public void testBooleanProperty()
            throws NotExecutableException, RepositoryException {

        // locate a PropertyDefinition with ValueConstraints
        PropertyDefinition propDef =
                NodeTypeUtil.locatePropertyDef(superuser, PropertyType.BOOLEAN, false, false, true, false);

        if (propDef == null) {
            throw new NotExecutableException("No boolean property def with " +
                    "testable value constraints has been found");
        }

        // find a Value that does not satisfy the ValueConstraints of propDef
        Value valueNotSatisfied = NodeTypeUtil.getValueAccordingToValueConstraints(superuser, propDef, false);
        if (valueNotSatisfied == null) {
            throw new NotExecutableException("No boolean property def with " +
                    "testable value constraints has been found");
        }

        // find a Value that does satisfy the ValueConstraints of propDef
        Value valueSatisfied = NodeTypeUtil.getValueAccordingToValueConstraints(superuser, propDef, true);
        if (valueSatisfied == null) {
            throw new NotExecutableException("The value constraints do not allow any value.");
        }

        // create a sub node of testRootNode of type propDef.getDeclaringNodeType()
        // and add a property with constraints to this node
        Node node;
        Property prop;
        try {
            String nodeType = propDef.getDeclaringNodeType().getName();
            node = testRootNode.addNode(nodeName2, nodeType);
            prop = node.setProperty(propDef.getName(), valueSatisfied);
            testRootNode.save();
        } catch (ConstraintViolationException e) {
            // implementation specific constraints do not allow to set up test environment
            throw new NotExecutableException("Not able to create required test items.");
        }

        // test of signature setValue(boolean value)
        try {
            prop.setValue(valueNotSatisfied.getBoolean());
            node.save();
            fail("setValue(boolean value) must throw a ConstraintViolationException " +
                    "if the change would violate a node type constraint " +
                    "either immediately or on save");
        } catch (ConstraintViolationException e) {
            // success
        }

        // test of signature setValue(Value value)
        try {
            prop.setValue(valueNotSatisfied);
            node.save();
            fail("setValue(Value value) must throw a ConstraintViolationException " +
                    "if the change would violate a node type constraint " +
                    "either immediately or on save");
        } catch (ConstraintViolationException e) {
            // success
        }
    }

    /**
     * Tests if setValue(Calendar value) and setValue(Value value) where value
     * is a DateValue throw a ConstraintViolationException if the change would
     * violate a value constraint
     */
    public void testDateProperty()
            throws NotExecutableException, RepositoryException {

        // locate a PropertyDefinition with ValueConstraints
        PropertyDefinition propDef =
                NodeTypeUtil.locatePropertyDef(superuser, PropertyType.DATE, false, false, true, false);

        if (propDef == null) {
            throw new NotExecutableException("No date property def with " +
                    "testable value constraints has been found");
        }

        // find a Value that does not satisfy the ValueConstraints of propDef
        Value valueNotSatisfied = NodeTypeUtil.getValueAccordingToValueConstraints(superuser, propDef, false);
        if (valueNotSatisfied == null) {
            throw new NotExecutableException("No date property def with " +
                    "testable value constraints has been found");
        }

        // find a Value that does satisfy the ValueConstraints of propDef
        Value valueSatisfied = NodeTypeUtil.getValueAccordingToValueConstraints(superuser, propDef, true);
        if (valueSatisfied == null) {
            throw new NotExecutableException("The value constraints do not allow any value.");
        }

        // create a sub node of testRootNode of type propDef.getDeclaringNodeType()
        // and add a property with constraints to this node
        Node node;
        Property prop;
        try {
            String nodeType = propDef.getDeclaringNodeType().getName();
            node = testRootNode.addNode(nodeName2, nodeType);
            prop = node.setProperty(propDef.getName(), valueSatisfied);
            testRootNode.save();
        } catch (ConstraintViolationException e) {
            // implementation specific constraints do not allow to set up test environment
            throw new NotExecutableException("Not able to create required test items.");
        }

        // test of signature setValue(Calendar value)
        try {
            prop.setValue(valueNotSatisfied.getDate());
            node.save();
            fail("setValue(Date value) must throw a ConstraintViolationException " +
                    "if the change would violate a node type constraint " +
                    "either immediately or on save");
        } catch (ConstraintViolationException e) {
            // success
        }

        // test of signature setValue(Value value)
        try {
            prop.setValue(valueNotSatisfied);
            node.save();
            fail("setValue(Value value) must throw a ConstraintViolationException " +
                    "if the change would violate a node type constraint " +
                    "either immediately or on save");
        } catch (ConstraintViolationException e) {
            // success
        }
    }

    /**
     * Tests if setValue(Double value) and setValue(Value value) where value is
     * a DoubleValue throw a ConstraintViolationException if the change would
     * violate a value constraint
     */
    public void testDoubleProperty()
            throws NotExecutableException, RepositoryException {

        // locate a PropertyDefinition with ValueConstraints
        PropertyDefinition propDef =
                NodeTypeUtil.locatePropertyDef(superuser, PropertyType.DOUBLE, false, false, true, false);

        if (propDef == null) {
            throw new NotExecutableException("No double property def with " +
                    "testable value constraints has been found");
        }

        // find a Value that does not satisfy the ValueConstraints of propDef
        Value valueNotSatisfied = NodeTypeUtil.getValueAccordingToValueConstraints(superuser, propDef, false);
        if (valueNotSatisfied == null) {
            throw new NotExecutableException("No double property def with " +
                    "testable value constraints has been found");
        }

        // find a Value that does satisfy the ValueConstraints of propDef
        Value valueSatisfied = NodeTypeUtil.getValueAccordingToValueConstraints(superuser, propDef, true);
        if (valueSatisfied == null) {
            throw new NotExecutableException("The value constraints do not allow any value.");
        }

        // create a sub node of testRootNode of type propDef.getDeclaringNodeType()
        // and add a property with constraints to this node
        Node node;
        Property prop;
        try {
            String nodeType = propDef.getDeclaringNodeType().getName();
            node = testRootNode.addNode(nodeName2, nodeType);
            prop = node.setProperty(propDef.getName(), valueSatisfied);
            testRootNode.save();
        } catch (ConstraintViolationException e) {
            // implementation specific constraints do not allow to set up test environment
            throw new NotExecutableException("Not able to create required test items.");
        }

        // test of signature setValue(double value)
        try {
            prop.setValue(valueNotSatisfied.getDouble());
            node.save();
            fail("setValue(double value) must throw a ConstraintViolationException " +
                    "if the change would violate a node type constraint " +
                    "either immediately or on save");
        } catch (ConstraintViolationException e) {
            // success
        }

        // test of signature setValue(Value value)
        try {
            prop.setValue(valueNotSatisfied);
            node.save();
            fail("setValue(Value value) must throw a ConstraintViolationException " +
                    "if the change would violate a node type constraint " +
                    "either immediately or on save");
        } catch (ConstraintViolationException e) {
            // success
        }
    }

    /**
     * Tests if setValue(Long value) and setValue(Value value) where value is a
     * LongValue throw a ConstraintViolationException if the change would
     * violate a value constraint
     */
    public void testLongProperty()
            throws NotExecutableException, RepositoryException {

        // locate a PropertyDefinition with ValueConstraints
        PropertyDefinition propDef =
                NodeTypeUtil.locatePropertyDef(superuser, PropertyType.LONG, false, false, true, false);

        if (propDef == null) {
            throw new NotExecutableException("No long property def with " +
                    "testable value constraints has been found");
        }

        // find a Value that does not satisfy the ValueConstraints of propDef
        Value valueNotSatisfied = NodeTypeUtil.getValueAccordingToValueConstraints(superuser, propDef, false);
        if (valueNotSatisfied == null) {
            throw new NotExecutableException("No long property def with " +
                    "testable value constraints has been found");
        }

        // find a Value that does satisfy the ValueConstraints of propDef
        Value valueSatisfied = NodeTypeUtil.getValueAccordingToValueConstraints(superuser, propDef, true);
        if (valueSatisfied == null) {
            throw new NotExecutableException("The value constraints do not allow any value.");
        }

        // create a sub node of testRootNode of type propDef.getDeclaringNodeType()
        // and add a property with constraints to this node
        Node node;
        Property prop;
        try {
            String nodeType = propDef.getDeclaringNodeType().getName();
            node = testRootNode.addNode(nodeName2, nodeType);
            prop = node.setProperty(propDef.getName(), valueSatisfied);
            testRootNode.save();
        } catch (ConstraintViolationException e) {
            // implementation specific constraints do not allow to set up test environment
            throw new NotExecutableException("Not able to create required test items.");
        }

        // test of signature setValue(long value)
        try {
            prop.setValue(valueNotSatisfied.getLong());
            node.save();
            fail("setValue(long value) must throw a ConstraintViolationException " +
                    "if the change would violate a node type constraint " +
                    "either immediately or on save");
        } catch (ConstraintViolationException e) {
            // success
        }

        // test of signature setValue(Value value)
        try {
            prop.setValue(valueNotSatisfied);
            node.save();
            fail("setValue(Value value) must throw a ConstraintViolationException " +
                    "if the change would violate a node type constraint " +
                    "either immediately or on save");
        } catch (ConstraintViolationException e) {
            // success
        }
    }


    /**
     * Tests if setValue(Node value) and setValue(Value value) where value is a
     * ReferenceValue throw a ConstraintViolationException if the change would
     * violate a value constraint
     */
    public void testReferenceProperty()
            throws NotExecutableException, RepositoryException {

        // locate a PropertyDefinition with ValueConstraints
        PropertyDefinition propDef =
                NodeTypeUtil.locatePropertyDef(superuser, PropertyType.REFERENCE, false, false, true, false);

        if (propDef == null) {
            throw new NotExecutableException("No reference property def with " +
                    "testable value constraints has been found");
        }

        String constraints[] = propDef.getValueConstraints();
        String nodeTypeSatisfied = constraints[0];
        String nodeTypeNotSatisfied = null;

        NodeTypeManager manager = superuser.getWorkspace().getNodeTypeManager();
        NodeTypeIterator types = manager.getAllNodeTypes();

        // find a NodeType which is not satisfying the constraints
        findNodeTypeNotSatisfied:
            while (types.hasNext()) {
                NodeType type = types.nextNodeType();
                String name = type.getName();
                for (int i = 0; i < constraints.length; i++) {
                    if (name.equals(constraints[i])) {
                        continue findNodeTypeNotSatisfied;
                    }
                    nodeTypeNotSatisfied = name;
                    break findNodeTypeNotSatisfied;
                }
            }

        if (nodeTypeNotSatisfied == null) {
            throw new NotExecutableException("No reference property def with " +
                    "testable value constraints has been found");
        }

        // create a sub node of testRootNode of type propDef.getDeclaringNodeType()
        // and add a property with constraints to this node
        Node node;
        Property prop;
        Node nodeSatisfied;
        Node nodeNotSatisfied;
        try {
            String nodeType = propDef.getDeclaringNodeType().getName();
            node = testRootNode.addNode(nodeName2, nodeType);

            // create a referenceable node satisfying the constraint
            nodeSatisfied = testRootNode.addNode(nodeName3, nodeTypeSatisfied);
            nodeSatisfied.addMixin(mixReferenceable);

            // create a referenceable node not satisfying the constraint
            nodeNotSatisfied = testRootNode.addNode(nodeName4, nodeTypeNotSatisfied);
            nodeNotSatisfied.addMixin(mixReferenceable);

            // some implementations may require a save after addMixin()
            testRootNode.save();

            prop = node.setProperty(propDef.getName(), nodeSatisfied);
            testRootNode.save();
        } catch (ConstraintViolationException e) {
            // implementation specific constraints do not allow to set up test environment
            throw new NotExecutableException("Not able to create required test items.");
        }

        // test of signature setValue(Node value)
        try {
            prop.setValue(nodeNotSatisfied);
            node.save();
            fail("setValue(Node value) must throw a ConstraintViolationException " +
                    "if the change would violate a node type constraint " +
                    "either immediately or on save");
        } catch (ConstraintViolationException e) {
            // success
        }

        // test of signature setValue(Value value)
        try {
            prop.setValue(superuser.getValueFactory().createValue(nodeNotSatisfied));
            node.save();
            fail("setValue(Value value) must throw a ConstraintViolationException " +
                    "if the change would violate a node type constraint " +
                    "either immediately or on save");
        } catch (ConstraintViolationException e) {
            // success
        }
    }

    /**
     * Tests if setValue(Value[] values) where values are of type BinaryValue
     * throw a ConstraintViolationException if the change would violate a value
     * constraint
     */
    public void testMultipleBinaryProperty()
            throws NotExecutableException, RepositoryException {

        // locate a PropertyDefinition with ValueConstraints
        PropertyDefinition propDef =
                NodeTypeUtil.locatePropertyDef(superuser, PropertyType.BINARY, true, false, true, false);

        if (propDef == null) {
            throw new NotExecutableException("No multiple binary property def with " +
                    "testable value constraints has been found");
        }

        // find a Value that does not satisfy the ValueConstraints of propDef
        Value valueNotSatisfied = NodeTypeUtil.getValueAccordingToValueConstraints(superuser, propDef, false);
        if (valueNotSatisfied == null) {
            throw new NotExecutableException("No multiple binary property def with " +
                    "testable value constraints has been found");
        }

        // find a Value that does satisfy the ValueConstraints of propDef
        Value valueSatisfied = NodeTypeUtil.getValueAccordingToValueConstraints(superuser, propDef, true);
        if (valueSatisfied == null) {
            throw new NotExecutableException("The value constraints do not allow any value.");
        }

        // create a sub node of testRootNode of type propDef.getDeclaringNodeType()
        // and add a property with constraints to this node
        Node node;
        Property prop;
        try {
            String nodeType = propDef.getDeclaringNodeType().getName();
            node = testRootNode.addNode(nodeName2, nodeType);
            prop = node.setProperty(propDef.getName(), new Value[]{valueSatisfied});
            testRootNode.save();
        } catch (ConstraintViolationException e) {
            // implementation specific constraints do not allow to set up test environment
            throw new NotExecutableException("Not able to create required test items.");
        }

        try {
            prop.setValue(new Value[]{valueNotSatisfied});
            node.save();
            fail("setValue(Value[] values) must throw a ConstraintViolationException " +
                    "if the change would violate a node type constraint " +
                    "either immediately or on save");
        } catch (ConstraintViolationException e) {
            // success
        }
    }

    /**
     * Tests if setValue(Value[] values) where values are of type BooleanValue
     * throw a ConstraintViolationException if the change would violate a value
     * constraint
     */
    public void testMultipleBooleanProperty()
            throws NotExecutableException, RepositoryException {

        // locate a PropertyDefinition with ValueConstraints
        PropertyDefinition propDef =
                NodeTypeUtil.locatePropertyDef(superuser, PropertyType.BOOLEAN, true, false, true, false);

        if (propDef == null) {
            throw new NotExecutableException("No multiple boolean property def with " +
                    "testable value constraints has been found");
        }

        // find a Value that does not satisfy the ValueConstraints of propDef
        Value valueNotSatisfied = NodeTypeUtil.getValueAccordingToValueConstraints(superuser, propDef, false);
        if (valueNotSatisfied == null) {
            throw new NotExecutableException("No multiple boolean property def with " +
                    "testable value constraints has been found");
        }

        // find a Value that does satisfy the ValueConstraints of propDef
        Value valueSatisfied = NodeTypeUtil.getValueAccordingToValueConstraints(superuser, propDef, true);
        if (valueSatisfied == null) {
            throw new NotExecutableException("The value constraints do not allow any value.");
        }

        // create a sub node of testRootNode of type propDef.getDeclaringNodeType()
        // and add a property with constraints to this node
        Node node;
        Property prop;
        try {
            String nodeType = propDef.getDeclaringNodeType().getName();
            node = testRootNode.addNode(nodeName2, nodeType);
            prop = node.setProperty(propDef.getName(), new Value[]{valueSatisfied});
            testRootNode.save();
        } catch (ConstraintViolationException e) {
            // implementation specific constraints do not allow to set up test environment
            throw new NotExecutableException("Not able to create required test items.");
        }

        try {
            prop.setValue(new Value[]{valueNotSatisfied});
            node.save();
            fail("setValue(Value[] values) must throw a ConstraintViolationException " +
                    "if the change would violate a node type constraint " +
                    "either immediately or on save");
        } catch (ConstraintViolationException e) {
            // success
        }
    }


    /**
     * Tests if setValue(Value[] values) where values are of type DateValue
     * throw a ConstraintViolationException if the change would violate a value
     * constraint
     */
    public void testMultipleDateProperty()
            throws NotExecutableException, RepositoryException {

        // locate a PropertyDefinition with ValueConstraints
        PropertyDefinition propDef =
                NodeTypeUtil.locatePropertyDef(superuser, PropertyType.DATE, true, false, true, false);

        if (propDef == null) {
            throw new NotExecutableException("No multiple date property def with " +
                    "testable value constraints has been found");
        }

        // find a Value that does not satisfy the ValueConstraints of propDef
        Value valueNotSatisfied = NodeTypeUtil.getValueAccordingToValueConstraints(superuser, propDef, false);
        if (valueNotSatisfied == null) {
            throw new NotExecutableException("No multiple date property def with " +
                    "testable value constraints has been found");
        }

        // find a Value that does satisfy the ValueConstraints of propDef
        Value valueSatisfied = NodeTypeUtil.getValueAccordingToValueConstraints(superuser, propDef, true);
        if (valueSatisfied == null) {
            throw new NotExecutableException("The value constraints do not allow any value.");
        }

        // create a sub node of testRootNode of type propDef.getDeclaringNodeType()
        // and add a property with constraints to this node
        Node node;
        Property prop;
        try {
            String nodeType = propDef.getDeclaringNodeType().getName();
            node = testRootNode.addNode(nodeName2, nodeType);
            prop = node.setProperty(propDef.getName(), new Value[]{valueSatisfied});
            testRootNode.save();
        } catch (ConstraintViolationException e) {
            // implementation specific constraints do not allow to set up test environment
            throw new NotExecutableException("Not able to create required test items.");
        }

        try {
            prop.setValue(new Value[]{valueNotSatisfied});
            node.save();
            fail("setValue(Value[] values) must throw a ConstraintViolationException " +
                    "if the change would violate a node type constraint " +
                    "either immediately or on save");
        } catch (ConstraintViolationException e) {
            // success
        }
    }

    /**
     * Tests if setValue(Value[] values) where values are of type DoubleValue
     * throw a ConstraintViolationException if the change would violate a value
     * constraint
     */
    public void testMultipleDoubleProperty()
            throws NotExecutableException, RepositoryException {

        // locate a PropertyDefinition with ValueConstraints
        PropertyDefinition propDef =
                NodeTypeUtil.locatePropertyDef(superuser, PropertyType.DOUBLE, true, false, true, false);

        if (propDef == null) {
            throw new NotExecutableException("No multiple double property def with " +
                    "testable value constraints has been found");
        }

        // find a Value that does not satisfy the ValueConstraints of propDef
        Value valueNotSatisfied = NodeTypeUtil.getValueAccordingToValueConstraints(superuser, propDef, false);
        if (valueNotSatisfied == null) {
            throw new NotExecutableException("No multiple double property def with " +
                    "testable value constraints has been found");
        }

        // find a Value that does satisfy the ValueConstraints of propDef
        Value valueSatisfied = NodeTypeUtil.getValueAccordingToValueConstraints(superuser, propDef, true);
        if (valueSatisfied == null) {
            throw new NotExecutableException("The value constraints do not allow any value.");
        }

        // create a sub node of testRootNode of type propDef.getDeclaringNodeType()
        // and add a property with constraints to this node
        Node node;
        Property prop;
        try {
            String nodeType = propDef.getDeclaringNodeType().getName();
            node = testRootNode.addNode(nodeName2, nodeType);
            prop = node.setProperty(propDef.getName(), new Value[]{valueSatisfied});
            testRootNode.save();
        } catch (ConstraintViolationException e) {
            // implementation specific constraints do not allow to set up test environment
            throw new NotExecutableException("Not able to create required test items.");
        }

        // test of signature setValue(Value value)
        try {
            prop.setValue(new Value[]{valueNotSatisfied});
            node.save();
            fail("setValue(Value[] values) must throw a ConstraintViolationException " +
                    "if the change would violate a node type constraint " +
                    "either immediately or on save");
        } catch (ConstraintViolationException e) {
            // success
        }
    }

    /**
     * Tests if setValue(Value[] values) where values are of type LongValue
     * throw a ConstraintViolationException if the change would violate a value
     * constraint
     */
    public void testMultipleLongProperty()
            throws NotExecutableException, RepositoryException {

        // locate a PropertyDefinition with ValueConstraints
        PropertyDefinition propDef =
                NodeTypeUtil.locatePropertyDef(superuser, PropertyType.LONG, true, false, true, false);

        if (propDef == null) {
            throw new NotExecutableException("No multiple long property def with " +
                    "testable value constraints has been found");
        }

        // find a Value that does not satisfy the ValueConstraints of propDef
        Value valueNotSatisfied = NodeTypeUtil.getValueAccordingToValueConstraints(superuser, propDef, false);
        if (valueNotSatisfied == null) {
            throw new NotExecutableException("No multiple long property def with " +
                    "testable value constraints has been found");
        }

        // find a Value that does satisfy the ValueConstraints of propDef
        Value valueSatisfied = NodeTypeUtil.getValueAccordingToValueConstraints(superuser, propDef, true);
        if (valueSatisfied == null) {
            throw new NotExecutableException("The value constraints do not allow any value.");
        }

        // create a sub node of testRootNode of type propDef.getDeclaringNodeType()
        // and add a property with constraints to this node
        Node node;
        Property prop;
        try {
            String nodeType = propDef.getDeclaringNodeType().getName();
            node = testRootNode.addNode(nodeName2, nodeType);
            prop = node.setProperty(propDef.getName(), new Value[]{valueSatisfied});
            testRootNode.save();
        } catch (ConstraintViolationException e) {
            // implementation specific constraints do not allow to set up test environment
            throw new NotExecutableException("Not able to create required test items.");
        }

        // test of signature setValue(Value value)
        try {
            prop.setValue(new Value[]{valueNotSatisfied});
            node.save();
            fail("setValue(Value value) must throw a ConstraintViolationException " +
                    "if the change would violate a node type constraint " +
                    "either immediately or on save");
        } catch (ConstraintViolationException e) {
            // success
        }
    }

    /**
     * Tests if setValue(Value[] values) where values are of type ReferenceValue
     * throw a ConstraintViolationException if the change would violate a value
     * constraint
     */
    public void testMultipleReferenceProperty()
            throws NotExecutableException, RepositoryException {

        // locate a PropertyDefinition with ValueConstraints
        PropertyDefinition propDef =
                NodeTypeUtil.locatePropertyDef(superuser, PropertyType.REFERENCE, true, false, true, false);

        if (propDef == null) {
            throw new NotExecutableException("No multiple reference property def with " +
                    "testable value constraints has been found");
        }

        String constraints[] = propDef.getValueConstraints();
        String nodeTypeSatisfied = constraints[0];
        String nodeTypeNotSatisfied = null;

        NodeTypeManager manager = superuser.getWorkspace().getNodeTypeManager();
        NodeTypeIterator types = manager.getAllNodeTypes();

        // find a NodeType which is not satisfying the constraints
        findNodeTypeNotSatisfied:
            while (types.hasNext()) {
                NodeType type = types.nextNodeType();
                String name = type.getName();
                for (int i = 0; i < constraints.length; i++) {
                    if (name.equals(constraints[i])) {
                        continue findNodeTypeNotSatisfied;
                    }
                    nodeTypeNotSatisfied = name;
                    break findNodeTypeNotSatisfied;
                }
            }

        if (nodeTypeNotSatisfied == null) {
            throw new NotExecutableException("No multiple reference property def with " +
                    "testable value constraints has been found");
        }

        // create a sub node of testRootNode of type propDef.getDeclaringNodeType()
        // and add a property with constraints to this node
        Node node;
        Property prop;
        Node nodeSatisfied;
        Node nodeNotSatisfied;
        try {
            String nodeType = propDef.getDeclaringNodeType().getName();
            node = testRootNode.addNode(nodeName2, nodeType);

            // create a referenceable node satisfying the constraint
            nodeSatisfied = testRootNode.addNode(nodeName3, nodeTypeSatisfied);
            nodeSatisfied.addMixin(mixReferenceable);

            // create a referenceable node not satisfying the constraint
            nodeNotSatisfied = testRootNode.addNode(nodeName4, nodeTypeNotSatisfied);
            nodeNotSatisfied.addMixin(mixReferenceable);
            
            // some implementations may require a save after addMixin()
            testRootNode.save();

            Value valueSatisfied = superuser.getValueFactory().createValue(nodeSatisfied);
            prop = node.setProperty(propDef.getName(), new Value[]{valueSatisfied});
            testRootNode.save();
        } catch (ConstraintViolationException e) {
            // implementation specific constraints do not allow to set up test environment
            throw new NotExecutableException("Not able to create required test items.");
        }

        // test of signature setValue(Value value)
        try {
            Value valueNotSatisfied = superuser.getValueFactory().createValue(nodeNotSatisfied);
            prop.setValue(new Value[]{valueNotSatisfied});
            node.save();
            fail("setValue(Value[] values) must throw a ConstraintViolationException " +
                    "if the change would violate a node type constraint " +
                    "either immediately or on save");
        } catch (ConstraintViolationException e) {
            // success
        }
    }

}
