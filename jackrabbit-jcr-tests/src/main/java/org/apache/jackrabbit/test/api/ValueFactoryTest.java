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
import javax.jcr.ValueFactory;
import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.RepositoryException;
import javax.jcr.PathNotFoundException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import java.util.Calendar;
import java.util.ArrayList;
import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * <code>ValueFactoryTest</code> tests the different ValueFactory.createValue methods.
 *
 * @test
 * @sources ValueFactoryTest.java
 * @executeClass org.apache.jackrabbit.test.api.ValueFactoryTest
 * @keywords level2
 */
public class ValueFactoryTest extends AbstractJCRTest {

    private Session session;
    private ValueFactory valueFactory;

    private static final boolean booleanValue = false;
    private Calendar dateValue = null;
    private static final double doubleValue = 3.1414926;
    private static final long  longValue = Long.MAX_VALUE;
    private Node referenceNode = null;
    private Node notReferenceableNode = null;
    private static final String stringValue = "a string";
    private static String nameValue = "aName";
    private static String pathValue = "/a/Path[1]";
    private byte[] binaryValue = null;

    private  ArrayList values = new ArrayList();

    private String dateValueFail = nameValue;
    private static final String doubleValueFail = nameValue;
    private static final String  longValueFail = nameValue;
    private static String nameValueFail = ";pre fix::name;";
    private static String pathValueFail =nameValueFail;

    private static int[] types = {PropertyType.DATE, PropertyType.DOUBLE, PropertyType.LONG,
                           PropertyType.NAME, PropertyType.PATH, PropertyType.REFERENCE,
                           PropertyType.STRING, PropertyType.BINARY, PropertyType.BOOLEAN};


    public void setUp() throws Exception {
        super.setUp();
        session = helper.getReadWriteSession();
        try {
            valueFactory = session.getValueFactory();
        } catch (UnsupportedRepositoryOperationException e) {
            String message = "ValueFactory Test not executable: " + e.getMessage();
            throw new NotExecutableException(message);
        }
        //notReferenceableNode = getProperty(not_ReferenceableNode);
        nameValue = testRootNode.getName();
        pathValue = testRootNode.getPath();
        dateValue = Calendar.getInstance();
        binaryValue = createRandomString(10).getBytes();
        referenceNode = createReferenceableNode(nodeName1);
    }


    protected void tearDown() throws Exception {
        if (session != null) {
            session.logout();
            session = null;
        }
        valueFactory = null;
        referenceNode = null;
        super.tearDown();
    }

    /**
     * Create a referenceable node under the testRootNode
     * or null if it is not possible to create one.
     * @param name
     * @return
     * @throws RepositoryException
     */
    public Node createReferenceableNode(String name) throws RepositoryException {
        // remove a yet existing node at the target
        try {
            Node node = testRootNode.getNode(name);
            node.remove();
            session.save();
        } catch (PathNotFoundException pnfe) {
            // ok
        }
        // a referenceable node
        Node n1 = testRootNode.addNode(name, testNodeType);
        if (n1.canAddMixin(mixReferenceable)) {
            n1.addMixin(mixReferenceable);
            // make sure jcr:uuid is available
            testRootNode.save();
            return n1;
        }
        else {
            return null;
        }
    }

    /**
     * Tests if the type of a created value is set correctly.
     *
     * @throws RepositoryException
     */
    public void testValueType() throws RepositoryException {
        Value value = null;
        int type = -1;
        for (int i = 0; i < types.length; i++) {

            switch (types[i]) {

                case PropertyType.BINARY:
                    try {
                        ByteArrayInputStream in = new ByteArrayInputStream(binaryValue);
                        value = valueFactory.createValue(in);
                        session.save();
                        type = value.getType();
                        in.close();
                    } catch (IOException ioe) {

                    }
                    assertTrue("Type of created value not correct: Expected: "
                        + PropertyType.nameFromValue(PropertyType.BINARY)
                        + " but was: " + PropertyType.nameFromValue(type),
                        PropertyType.BINARY == type);
                    break;

                case PropertyType.BOOLEAN:
                    value = valueFactory.createValue(booleanValue);
                    session.save();
                    type = value.getType();
                    assertTrue("Type of created value not correct: Expected: "
                        + PropertyType.nameFromValue(PropertyType.BOOLEAN)
                        + " but was: " + PropertyType.nameFromValue(type),
                        PropertyType.BOOLEAN == type);
                    break;

                case PropertyType.DATE:
                    value = valueFactory.createValue(dateValue);
                    session.save();
                    type = value.getType();
                    assertTrue("Type of created value not correct: Expected: "
                        + PropertyType.nameFromValue(PropertyType.DATE)
                        + " but was: " + PropertyType.nameFromValue(type),
                        PropertyType.DATE == type);
                    break;

                case PropertyType.DOUBLE:
                    value = valueFactory.createValue(doubleValue);
                    session.save();
                    type = value.getType();
                    assertTrue("Type of created value not correct: Expected: "
                        + PropertyType.nameFromValue(PropertyType.DOUBLE)
                        + " but was: " + PropertyType.nameFromValue(type),
                        PropertyType.DOUBLE == type);
                    break;

                case PropertyType.LONG:
                    value = valueFactory.createValue(longValue);
                    session.save();
                    type = value.getType();
                    assertTrue("Type of created value not correct: Expected: "
                        + PropertyType.nameFromValue(PropertyType.LONG)
                        + " but was: " + PropertyType.nameFromValue(type),
                        PropertyType.LONG == type);
                    break;

                case PropertyType.NAME:
                    value = valueFactory.createValue(nameValue, PropertyType.NAME);
                    session.save();
                    type = value.getType();
                    assertTrue("Type of created value not correct: Expected: "
                        + PropertyType.nameFromValue(PropertyType.NAME)
                        + " but was: " + PropertyType.nameFromValue(type),
                        PropertyType.NAME == type);
                    break;

                case PropertyType.PATH:
                    value = valueFactory.createValue(pathValue, PropertyType.PATH);
                    session.save();
                    type = value.getType();
                    assertTrue("Type of created value not correct: Expected: "
                        + PropertyType.nameFromValue(PropertyType.PATH)
                        + " but was: " + PropertyType.nameFromValue(type),
                        PropertyType.PATH == type);

                    break;

                case PropertyType.REFERENCE:
                    if (referenceNode != null) {
                        value = valueFactory.createValue(referenceNode);
                        session.save();
                        type = value.getType();
                        assertTrue("Type of created value not correct: Expected: "
                            + PropertyType.nameFromValue(PropertyType.REFERENCE)
                            + " but was: " + PropertyType.nameFromValue(type),
                            PropertyType.REFERENCE == type);
                        // correct value?
                        assertEquals("Reference value does not contain the UUID of the " +
                                "referenced node.", referenceNode.getUUID(), value.getString());
                    }
                    break;

               case PropertyType.STRING:
                    value = valueFactory.createValue(stringValue);
                    session.save();
                    type = value.getType();
                    assertTrue("Type of created value not correct: Expected: "
                        + PropertyType.nameFromValue(PropertyType.STRING)
                        + " but was: " + PropertyType.nameFromValue(type),
                        PropertyType.STRING == type);
                    break;

            }
        }

    }

    /**
     * Tests if a ValueFormatexception is thrown in case the passed string
     * cannot be converted to the required value type.
     * value creation.
     * @throws RepositoryException
     */
    public void testValueFormatException() throws RepositoryException {
        Value value = null;
        for (int i = 0; i < types.length; i++) {

            switch (types[i]) {

                case PropertyType.DATE:
                    try {
                        value = valueFactory.createValue(dateValueFail,PropertyType.DATE);
                        fail("Conversion from String " + dateValueFail
                                + " to a " + PropertyType.nameFromValue(types[i])
                                + " value should throw ValueFormatException.");
                    } catch (ValueFormatException vfe) {
                        //ok
                    }
                    break;

                case PropertyType.DOUBLE:
                    try {
                        value = valueFactory.createValue(doubleValueFail,PropertyType.DOUBLE);
                        fail("Conversion from String " + doubleValueFail
                                + " to a " + PropertyType.nameFromValue(types[i])
                                + " value should throw ValueFormatException.");
                    } catch (ValueFormatException vfe) {
                        //ok
                    }
                    break;

                case PropertyType.LONG:
                    try {
                        value = valueFactory.createValue(longValueFail,PropertyType.LONG);
                        fail("Conversion from String " + longValueFail
                                + " to a " + PropertyType.nameFromValue(types[i])
                                + " value should throw ValueFormatException.");
                    } catch (ValueFormatException vfe) {
                        //ok
                    }
                    break;

                case PropertyType.NAME:
                    try {
                        value = valueFactory.createValue(nameValueFail,PropertyType.NAME);
                        fail("Conversion from String " + nameValueFail
                                + " to a " + PropertyType.nameFromValue(types[i])
                                + " value should throw ValueFormatException.");
                    } catch (ValueFormatException vfe) {
                        //ok
                    }
                    break;

                case PropertyType.PATH:
                    try {
                        value = valueFactory.createValue(pathValueFail,PropertyType.PATH);
                        fail("Conversion from String " + pathValueFail
                                + " to a " + PropertyType.nameFromValue(types[i])
                                + " value should throw ValueFormatException.");
                    } catch (ValueFormatException vfe) {
                        //ok
                    }
                    break;

                default:
                    break;
            }
        }

    }
}
