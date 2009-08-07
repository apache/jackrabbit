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
package org.apache.jackrabbit.test.api.lock;

import org.apache.jackrabbit.test.AbstractJCRTest;
import org.apache.jackrabbit.test.NotExecutableException;

import javax.jcr.lock.LockException;
import javax.jcr.Session;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.PropertyType;
import javax.jcr.PathNotFoundException;
import java.util.Calendar;
import java.io.ByteArrayInputStream;


/**
 * <code>SetValueLockExceptionTest</code> Tests throwing of a LockException for the
 * Property.setValue() methods in case the parentNode of the given property is locked.
 *
 * @test
 * @sources SetValueLockExceptionTest.java
 * @executeClass org.apache.jackrabbit.test.api.lock.SetValueLockExceptionTest
 * @keywords level2
 */
public class SetValueLockExceptionTest extends AbstractJCRTest {

    private Node testNode;

    private static final String binaryProp = "binaryProp";
    private static final String booleanProp = "booleanProp";
    private static final String dateProp = "dateProp";
    private static final String doubleProp = "doubleProp";
    private static final String longProp = "longProp";
    private static final String referenceProp = "referenceProp";
    private static final String stringProp = "stringProp";
    private static final String multiStringProp ="multiStringProp";

    private static final boolean booleanValue = false;
    private Calendar dateValue = null;
    private static final double doubleValue = 3.1414926;
    private static final long longValue = Long.MAX_VALUE;
    private Node referenceNode = null;
    private static final String stringValue = "a string";
    private byte[] binaryValue = null;
    private String[] multiString = {"one", "two", "three"};

    // types for the different method signatures of Property.setValue
    private static final int TYPE_VALUE = 20;
    private static final int TYPE_MULTIVAL = 21;
    private static final int TYPE_MULTSTRING = 22;
    private static int[] types = {PropertyType.DATE, PropertyType.DOUBLE, PropertyType.LONG,
                                  PropertyType.REFERENCE, PropertyType.STRING,
                                  PropertyType.BINARY, PropertyType.BOOLEAN, TYPE_VALUE,
                                  TYPE_MULTIVAL, TYPE_MULTSTRING};


    /**
     * Check if Locking is supported and if yes setup a lockable node with properties
     * each one for the possible values passed to Property.setValue .
     * and
     * @throws Exception
     */
    public void setUp() throws Exception {
        super.setUp();
        if (!isSupported(Repository.OPTION_LOCKING_SUPPORTED)) {
            throw new NotExecutableException("SetValueLockExceptionTest "
                    + "not executable: Locking not supported");
        }
        else {
            // add a lockable node
            testNode = testRootNode.addNode(nodeName1, testNodeType);
            if (needsMixin(testNode, mixLockable)) {
                testNode.addMixin(mixLockable);
            }

            // add properties
            dateValue = Calendar.getInstance();
            referenceNode = createReferenceableNode(nodeName2);
            binaryValue = createRandomString(10).getBytes();

            ByteArrayInputStream in = new ByteArrayInputStream(binaryValue);
            ensureCanSetProperty(testNode, binaryProp, PropertyType.BINARY, false);
            testNode.setProperty(binaryProp, in);
            ensureCanSetProperty(testNode, booleanProp, PropertyType.BOOLEAN, false);
            testNode.setProperty(booleanProp, booleanValue);
            ensureCanSetProperty(testNode, dateProp, PropertyType.DATE, false);
            testNode.setProperty(dateProp, dateValue);
            ensureCanSetProperty(testNode, doubleProp, PropertyType.DOUBLE, false);
            testNode.setProperty(doubleProp, doubleValue);
            ensureCanSetProperty(testNode, longProp, PropertyType.LONG, false);
            testNode.setProperty(longProp, longValue);
            if (referenceNode != null) {
                ensureCanSetProperty(testNode, referenceProp, PropertyType.REFERENCE, false);
                testNode.setProperty(referenceProp, referenceNode);
            }
            ensureCanSetProperty(testNode, stringProp, PropertyType.STRING, false);
            testNode.setProperty(stringProp, stringValue);
            ensureCanSetProperty(testNode, multiStringProp, PropertyType.STRING, true);
            testNode.setProperty(multiStringProp, multiString);
            testRootNode.save();
        }
    }

    public void tearDown() throws Exception {
        if (testNode.holdsLock()) {
            testNode.unlock();
        }
        testNode = null;
        referenceNode = null;
        superuser.save();
        super.tearDown();
    }

    /**
     * Tests if a LockException is thrown if a value is added to a property of a locked node.
     *
     * @param type The possible argument types.
     * @throws RepositoryException
     */
    public void doTestSetValueLockException(int type)
            throws RepositoryException {

        // lock if not yet locked
        if (!testNode.holdsLock()) {
            testNode.lock(false, false);
            superuser.save();
        }

        // another session
        Session session = helper.getReadWriteSession();
        try {
            Node node = (Node) session.getItem(testNode.getPath());
            Property prop = null;
            switch (type) {
                case PropertyType.BINARY:
                    ByteArrayInputStream in = new ByteArrayInputStream(binaryValue);
                    prop = node.getProperty(binaryProp);
                    prop.setValue(in);
                    break;

                case PropertyType.BOOLEAN:
                    prop = node.getProperty(booleanProp);
                    prop.setValue(booleanValue);
                    break;

                case PropertyType.DATE:
                    prop = node.getProperty(dateProp);
                    prop.setValue(dateValue);
                    break;

                case PropertyType.DOUBLE:
                    prop = node.getProperty(doubleProp);
                    prop.setValue(doubleValue);
                    break;

                case PropertyType.LONG:
                    prop = node.getProperty(longProp);
                    prop.setValue(longValue);
                    break;

                case PropertyType.REFERENCE:
                    prop = node.getProperty(referenceProp);
                    if (referenceNode != null) {
                        prop.setValue(referenceNode);
                    }
                    break;

               case PropertyType.STRING:
                    prop = node.getProperty(stringProp);
                    prop.setValue(stringValue);
                    break;

               case TYPE_VALUE:
                    prop = node.getProperty(stringProp);
                    Value value = session.getValueFactory().createValue(stringValue);
                    prop.setValue(value);
                    break;

                case TYPE_MULTIVAL:
                    prop = node.getProperty(multiStringProp);
                    Value[] values = {  session.getValueFactory().createValue(stringValue),
                                        session.getValueFactory().createValue(stringValue),
                                        session.getValueFactory().createValue(stringValue) };
                    prop.setValue(values);
                    break;

               case TYPE_MULTSTRING:
                    prop = node.getProperty(multiStringProp);
                    String[] strVals = {stringValue, stringValue, stringValue};
                    prop.setValue(strVals);
                    break;
            }
            session.save();
            fail("Property.setValue should throw a LockException "
                    + "if the parent node holds a Lock.");
        } catch (LockException le) {
            // ok
        } finally {
            session.logout();
        }
    }

    /**
     * Performs the test for all argument types.
     * @throws RepositoryException
     */
    public void testSetValueLockException() throws RepositoryException {
       for (int i = 0; i < types.length; i++) {
            doTestSetValueLockException(types[i]);
        }
    }

    /**
     * Create a referenceable node under the testRootNode
     * or null if it is not possible to create one.
     * @param name
     * @throws RepositoryException
     */
    public Node createReferenceableNode(String name) throws RepositoryException {
        // remove a yet existing node at the target
        try {
            Node node = testRootNode.getNode(name);
            node.remove();
            superuser.save();
        } catch (PathNotFoundException pnfe) {
            // ok
        }
        // a referenceable node
        Node n1 = testRootNode.addNode(name, testNodeType);
        if (n1.canAddMixin(mixReferenceable)) {
            n1.addMixin(mixReferenceable);
            // make sure jcr:uuid is available
            superuser.save();
            return n1;
        }
        else {
            return null;
        }
    }
}
