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
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeType;
import java.io.InputStream;
import java.io.IOException;

/**
 * <code>SetPropertyConstraintViolationExceptionTest</code> tests if
 * setProperty() throws a ConstraintViolationException either immediately (by
 * setValue()) or on save, if the change would violate a value constraint.
 *
 * @test
 * @sources SetPropertyConstraintViolationExceptionTest.java
 * @executeClass org.apache.jackrabbit.test.api.SetPropertyConstraintViolationExceptionTest
 * @keywords level2
 */
public class SetPropertyConstraintViolationExceptionTest extends AbstractJCRTest {

    /**
     * Tests if setProperty(String name, boolean value) and setProperty(String
     * name, Value value) where value is a BooleanValue throw a
     * ConstraintViolationException either immediately (by setProperty()), or on
     * save, if the change would violate a node type constraint
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

        // create a sub node of testRootNode of type propDef.getDeclaringNodeType()
        Node node;
        try {
            String nodeType = propDef.getDeclaringNodeType().getName();
            node = testRootNode.addNode(nodeName2, nodeType);
            testRootNode.save();
        } catch (ConstraintViolationException e) {
            // implementation specific constraints do not allow to set up test environment
            throw new NotExecutableException("Not able to create required test items.");
        }

        // test of signature setProperty(String name, boolean value)
        try {
            node.setProperty(propDef.getName(), valueNotSatisfied.getBoolean());
            node.save();
            fail("setProperty(String name, boolean value) must throw a " +
                    "ConstraintViolationException if the change would violate a " +
                    "node type constraint either immediately or on save");
        } catch (ConstraintViolationException e) {
            // success
        }

        // test of signature setProperty(String name, Value value)
        try {
            node.setProperty(propDef.getName(), valueNotSatisfied);
            node.save();
            fail("setProperty(String name, boolean value) must throw a " +
                    "ConstraintViolationException if the change would violate a " +
                    "node type constraint either immediately or on save");
        } catch (ConstraintViolationException e) {
            // success
        }
    }

    /**
     * Tests if setProperty(String name, Calendar value) and setProperty(String
     * name, Value value) where value is a DateValue throw a
     * ConstraintViolationException either immediately (by setProperty()), or on
     * save, if the change would violate a node type constraint
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

        // create a sub node of testRootNode of type propDef.getDeclaringNodeType()
        Node node;
        try {
            String nodeType = propDef.getDeclaringNodeType().getName();
            node = testRootNode.addNode(nodeName2, nodeType);
            testRootNode.save();
        } catch (ConstraintViolationException e) {
            // implementation specific constraints do not allow to set up test environment
            throw new NotExecutableException("Not able to create required test items.");
        }

        // test of signature setProperty(String name, Calendar value)
        try {
            node.setProperty(propDef.getName(), valueNotSatisfied.getDate());
            node.save();
            fail("setProperty(String name, Calendar value) must throw a " +
                    "ConstraintViolationException if the change would violate a " +
                    "node type constraint either immediately or on save");
        } catch (ConstraintViolationException e) {
            // success
        }

        // test of signature setProperty(String name, Value value)
        try {
            node.setProperty(propDef.getName(), valueNotSatisfied);
            node.save();
            fail("setProperty(String name, Value value) must throw a " +
                    "ConstraintViolationException if the change would violate a " +
                    "node type constraint either immediately or on save");
        } catch (ConstraintViolationException e) {
            // success
        }
    }

    /**
     * Tests if setProperty(String name, double value) and setProperty(String
     * name, Value value) where value is a DoubleValue throw a
     * ConstraintViolationException either immediately (by setProperty()), or on
     * save, if the change would violate a node type constraint
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

        // create a sub node of testRootNode of type propDef.getDeclaringNodeType()
        Node node;
        try {
            String nodeType = propDef.getDeclaringNodeType().getName();
            node = testRootNode.addNode(nodeName2, nodeType);
            testRootNode.save();
        } catch (ConstraintViolationException e) {
            // implementation specific constraints do not allow to set up test environment
            throw new NotExecutableException("Not able to create required test items.");
        }

        // test of signature setProperty(String name, double value)
        try {
            node.setProperty(propDef.getName(), valueNotSatisfied.getDouble());
            node.save();
            fail("setProperty(String name, double value) must throw a " +
                    "ConstraintViolationException if the change would violate a " +
                    "node type constraint either immediately or on save");
        } catch (ConstraintViolationException e) {
            // success
        }

        // test of signature setProperty(String name, Value value)
        try {
            node.setProperty(propDef.getName(), valueNotSatisfied);
            node.save();
            fail("setProperty(String name, Value value) must throw a " +
                    "ConstraintViolationException if the change would violate a " +
                    "node type constraint either immediately or on save");
        } catch (ConstraintViolationException e) {
            // success
        }
    }

    /**
     * Tests if setProperty(String name, InputStream value) and
     * setProperty(String name, Value value) where value is a BinaryValue throw
     * a ConstraintViolationException either immediately (by setProperty()), or
     * on save, if the change would violate a node type constraint
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

        // create a sub node of testRootNode of type propDef.getDeclaringNodeType()
        Node node;
        try {
            String nodeType = propDef.getDeclaringNodeType().getName();
            node = testRootNode.addNode(nodeName2, nodeType);
            testRootNode.save();
        } catch (ConstraintViolationException e) {
            // implementation specific constraints do not allow to set up test environment
            throw new NotExecutableException("Not able to create required test items.");
        }

        // test of signature setProperty(String name, InputStream value)
        InputStream in = valueNotSatisfied1.getStream();
        try {
            node.setProperty(propDef.getName(), in);
            node.save();
            fail("setProperty(String name, InputStream value) must throw a " +
                    "ConstraintViolationException if the change would violate a " +
                    "node type constraint either immediately or on save");
        } catch (ConstraintViolationException e) {
            // success
        } finally {
            try {
                in.close();
            } catch (IOException ignore) {}
        }

        // test of signature setProperty(String name, Value value)
        try {
            node.setProperty(propDef.getName(), valueNotSatisfied2);
            node.save();
            fail("setProperty(String name, Value value) must throw a " +
                    "ConstraintViolationException if the change would violate a " +
                    "node type constraint either immediately or on save");
        } catch (ConstraintViolationException e) {
            // success
        }
    }

    /**
     * Tests if setProperty(String name, long value) and setProperty(String
     * name, Value value) where value is a LongValue throw a
     * ConstraintViolationException either immediately (by setProperty()), or on
     * save, if the change would violate a node type constraint
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

        // create a sub node of testRootNode of type propDef.getDeclaringNodeType()
        Node node;
        try {
            String nodeType = propDef.getDeclaringNodeType().getName();
            node = testRootNode.addNode(nodeName2, nodeType);
            testRootNode.save();
        } catch (ConstraintViolationException e) {
            // implementation specific constraints do not allow to set up test environment
            throw new NotExecutableException("Not able to create required test items.");
        }

        // test of signature setProperty(String name, long value)
        try {
            node.setProperty(propDef.getName(), valueNotSatisfied.getLong());
            node.save();
            fail("setProperty(String name, long value) must throw a " +
                    "ConstraintViolationException if the change would violate a " +
                    "node type constraint either immediately or on save");
        } catch (ConstraintViolationException e) {
            // success
        }

        // test of signature setProperty(String name, Value value)
        try {
            node.setProperty(propDef.getName(), valueNotSatisfied);
            node.save();
            fail("setProperty(String name, Value value) must throw a " +
                    "ConstraintViolationException if the change would violate a " +
                    "node type constraint either immediately or on save");
        } catch (ConstraintViolationException e) {
            // success
        }
    }

    /**
     * Tests if setProperty(String name, Node value) and setProperty(String
     * name, Value value) where value is a ReferenceValue throw a
     * ConstraintViolationException either immediately (by setProperty()), or on
     * save, if the change would violate a node type constraint
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
        Node node;
        Node nodeNotSatisfied;
        try {
            String nodeType = propDef.getDeclaringNodeType().getName();
            node = testRootNode.addNode(nodeName2, nodeType);

            // create a referenceable node not satisfying the constraint
            nodeNotSatisfied = testRootNode.addNode(nodeName4, nodeTypeNotSatisfied);
            nodeNotSatisfied.addMixin(mixReferenceable);

            testRootNode.save();
        } catch (ConstraintViolationException e) {
            // implementation specific constraints do not allow to set up test environment
            throw new NotExecutableException("Not able to create required test items.");
        }

        // test of signature setProperty(String name, Node value)
        try {
            node.setProperty(propDef.getName(), nodeNotSatisfied);
            node.save();
            fail("setProperty(String name, Node value) must throw a " +
                    "ConstraintViolationException if the change would violate a " +
                    "node type constraint either immediately or on save");
        } catch (ConstraintViolationException e) {
            // success
        }

        // test of signature setProperty(String name, Value value)
        try {
            node.setProperty(propDef.getName(), superuser.getValueFactory().createValue(nodeNotSatisfied));
            node.save();
            fail("setProperty(String name, Value value) must throw a " +
                    "ConstraintViolationException if the change would violate a " +
                    "node type constraint either immediately or on save");
        } catch (ConstraintViolationException e) {
            // success
        }
    }
}