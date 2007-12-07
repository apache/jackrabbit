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

import javax.jcr.Session;
import javax.jcr.Property;
import javax.jcr.Node;
import javax.jcr.Value;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.version.VersionException;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.util.Calendar;

/**
 * <code>SetValueVersionExceptionTest</code>...
 *
 * @test
 * @sources SetValueVersionExceptionTest.java
 * @executeClass org.apache.jackrabbit.test.api.SetValueVersionExceptionTest
 * @keywords level2 versionable
 */
public class SetValueVersionExceptionTest extends AbstractJCRTest {

    /**
     * The session we use for the tests
     */
    private Session session;

    private Node node;

    private Property property;
    private Property multiProperty;

    private Value value;
    private Value values[];

    /**
     * Sets up the fixture for the test cases.
     */
    protected void setUp() throws Exception {
        super.setUp();
        session = helper.getReadOnlySession();

        value = session.getValueFactory().createValue("abc");
        values = new Value[] {value};

        if (!isSupported(Repository.OPTION_VERSIONING_SUPPORTED)) {
            throw new NotExecutableException("Versioning is not supported.");
        }

        // create a node that is versionable
        node = testRootNode.addNode(nodeName1, testNodeType);
        // or try to make it versionable if it is not
        if (!node.isNodeType(mixVersionable)) {
            if (node.canAddMixin(mixVersionable)) {
                node.addMixin(mixVersionable);
            } else {
                throw new NotExecutableException("Failed to set up required test items");
            }
        }

        property = node.setProperty(propertyName1, value);
        multiProperty = node.setProperty(propertyName2, values);

        testRootNode.save();

        node.checkin();
    }

    /**
     * Releases the session aquired in {@link #setUp()}.
     */
    protected void tearDown() throws Exception {
        try {
            node.checkout();
        } finally {
            if (session != null) {
                session.logout();
                session = null;
            }
            node = null;
            property = null;
            multiProperty = null;
            value = null;
            values = null;
            super.tearDown();
        }
    }

    /**
     * Tests if setValue(Value) throws a VersionException immediately
     * or on save if the parent node of this property is checked-in.
     */
    public void testValue() throws RepositoryException {
        try {
            property.setValue(value);
            node.save();
            fail("Property.setValue(Value) must throw a VersionException " +
                 "immediately or on save if the parent node of this property " +
                 "is checked-in.");
        }
        catch (VersionException e) {
            // success
        }
    }

    /**
     * Tests if setValue(Value[]) throws a VersionException immediately
     * or on save if the parent node of this property is checked-in.
     */
    public void testValueArray() throws RepositoryException {
        try {
            multiProperty.setValue(values);
            node.save();
            fail("Property.setValue(Value[]) must throw a VersionException " +
                 "immediately or on save if the parent node of this property " +
                 "is checked-in.");
        }
        catch (VersionException e) {
            // success
        }
    }

    /**
     * Tests if setValue(String) throws a VersionException immediately
     * or on save if the parent node of this property is checked-in.
     */
    public void testString() throws RepositoryException {
        try {
            property.setValue("abc");
            node.save();
            fail("Property.setValue(String) must throw a VersionException " +
                 "immediately or on save if the parent node of this property " +
                 "is checked-in.");
        }
        catch (VersionException e) {
            // success
        }
    }

    /**
     * Tests if setValue(String[]) throws a VersionException immediately
     * or on save if the parent node of this property is checked-in.
     */
    public void testStringArray() throws RepositoryException {
        try {
            String values[] = new String[] {"abc"};
            multiProperty.setValue(values);
            node.save();
            fail("Property.setValue(String[]) must throw a VersionException " +
                 "immediately or on save if the parent node of this property " +
                 "is checked-in.");
        }
        catch (VersionException e) {
            // success
        }
    }

    /**
     * Tests if setValue(InputStream) throws a VersionException immediately
     * or on save if the parent node of this property is checked-in.
     */
    public void testInputStream() throws RepositoryException {
        try {
            byte[] bytes = {123};
            InputStream value = new ByteArrayInputStream(bytes);
            property.setValue(value);
            node.save();
            fail("Property.setValue(InputStream) must throw a VersionException " +
                 "immediately or on save if the parent node of this property " +
                 "is checked-in.");
        }
        catch (VersionException e) {
            // success
        }
    }

    /**
     * Tests if setValue(long) throws a VersionException immediately
     * or on save if the parent node of this property is checked-in.
     */
    public void testLong() throws RepositoryException {
        try {
            property.setValue(123);
            node.save();
            fail("Property.setValue(long) must throw a VersionException " +
                 "immediately or on save if the parent node of this property " +
                 "is checked-in.");
        }
        catch (VersionException e) {
            // success
        }
    }

    /**
     * Tests if setValue(double) throws a VersionException immediately
     * or on save if the parent node of this property is checked-in.
     */
    public void testDouble() throws RepositoryException {
        try {
            property.setValue(1.23);
            node.save();
            fail("Property.setValue(double) must throw a VersionException " +
                 "immediately or on save if the parent node of this property " +
                 "is checked-in.");
        }
        catch (VersionException e) {
            // success
        }
    }

    /**
     * Tests if setValue(Calendar) throws a VersionException immediately
     * or on save if the parent node of this property is checked-in.
     */
    public void testCalendar() throws RepositoryException {
        try {
            property.setValue(Calendar.getInstance());
            node.save();
            fail("Property.setValue(Calendar) must throw a VersionException " +
                 "immediately or on save if the parent node of this property " +
                 "is checked-in.");
        }
        catch (VersionException e) {
            // success
        }
    }

    /**
     * Tests if setValue(boolean) throws a VersionException immediately
     * or on save if the parent node of this property is checked-in.
     */
    public void testBoolean() throws RepositoryException {
        try {
            property.setValue(true);
            node.save();
            fail("Property.setValue(boolean) must throw a VersionException " +
                 "immediately or on save if the parent node of this property " +
                 "is checked-in.");
        }
        catch (VersionException e) {
            // success
        }
    }

    /**
     * Tests if setValue(Node) throws a VersionException immediately
     * or on save if the parent node of this property is checked-in.
     * @tck.config nodetype2 name of a node type with a reference property
     * @tck.config propertyname3 name of a single value reference property
     *   declared in nodetype2
     */
    public void testNode()
        throws NotExecutableException, RepositoryException {

        String nodeType3 = getProperty("nodetype3");

        // create a referenceable node
        Node referenceableNode = (nodeType3 == null)
            ? testRootNode.addNode(nodeName3)
            : testRootNode.addNode(nodeName3, nodeType3);

        // try to make it referenceable if it is not
        if (!referenceableNode.isNodeType(mixReferenceable)) {
            if (referenceableNode.canAddMixin(mixReferenceable)) {
              referenceableNode.addMixin(mixReferenceable);
            } else {
                throw new NotExecutableException("Failed to set up required test items.");
            }
        }

        // implementation specific if mixin takes effect immediately or upon save
        testRootNode.save();

        String refPropName = getProperty("propertyname3");
        String nodeType = getProperty("nodetype2");

        Node node = testRootNode.addNode(nodeName4, nodeType);

        // try to make it versionable if it is not
        if (!node.isNodeType(mixVersionable)) {
            if (node.canAddMixin(mixVersionable)) {
                node.addMixin(mixVersionable);
            } else {
                throw new NotExecutableException("Failed to set up required test items.");
            }
        }

        // fail early when reference properties are not suppoerted
        ensureCanSetProperty(node, refPropName, node.getSession().getValueFactory().createValue(referenceableNode));

        Property property = node.setProperty(refPropName, referenceableNode);
        testRootNode.save();

        node.checkin();

        try {
            property.setValue(referenceableNode);
            node.save();
            fail("Property.setValue(Node) must throw a VersionException " +
                 "immediately or on save if the parent node of this property " +
                 "is checked-in.");
        }
        catch (VersionException e) {
            // success
        }

        node.checkout();
    }
}
