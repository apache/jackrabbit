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

import javax.jcr.Property;
import javax.jcr.Value;
import javax.jcr.RepositoryException;
import javax.jcr.Node;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.IOException;

/**
 * Tests the various {@link Property#setValue(Value)} methods.
 * <p/>
 * Configuration requirements:<br/> The node at {@link #testRoot} must allow a
 * child node of type {@link #testNodeType} with name {@link #nodeName1}. The
 * node type {@link #testNodeType} must define a single value binary property
 * with name {@link #propertyName1}.
 *
 * @test
 * @sources SetValueBinaryTest.java
 * @executeClass org.apache.jackrabbit.test.api.SetValueBinaryTest
 * @keywords level2
 */
public class SetValueBinaryTest extends AbstractJCRTest {

    /**
     * The binary value
     */
    private Value value;

    /**
     * The binary data
     */
    private byte[] data;

    /**
     * The node with the binary property
     */
    private Node node;

    /**
     * The binary property
     */
    private Property property1;

    protected void setUp() throws Exception {
        super.setUp();

        // initialize some binary value
        data = createRandomString(10).getBytes();
        value = superuser.getValueFactory().createValue(new ByteArrayInputStream(data));

        // create a new node under the testRootNode
        node = testRootNode.addNode(nodeName1, testNodeType);

        // create a new single-value property and save it
        property1 = node.setProperty(propertyName1, superuser.getValueFactory().createValue(new ByteArrayInputStream(new byte[0])));
        superuser.save();
    }

    /**
     * Test the persistence of a property modified with an BinaryValue parameter
     * and saved from the Session
     */
    public void testBinarySession() throws RepositoryException, IOException {
        property1.setValue(value);
        superuser.save();
        InputStream in = property1.getValue().getStream();
        try {
            compareStream(data, in);
        } finally {
            in.close();
        }
    }

    /**
     * Test the persistence of a property modified with an input stream
     * parameter and saved from the parent Node
     */
    public void testBooleanParent() throws RepositoryException, IOException {
        InputStream in = value.getStream();
        try {
            property1.setValue(in);
            node.save();
        } finally {
            in.close();
        }
        in = property1.getValue().getStream();
        try {
            compareStream(data, in);
        } finally {
            in.close();
        }
    }

    /**
     * Test the deletion of a property by assigning it a null value, saved from
     * the Session
     */
    public void testRemoveBooleanSession() throws RepositoryException {
        property1.setValue((InputStream) null);
        superuser.save();

        try {
            node.getProperty(propertyName1);
            fail("The property should not exist anymore, as a null Value has been assigned");
        } catch (Exception e) {
            //success : the property has been deleted by assigning it a null value
        }
    }

    /**
     * Test the deletion of a property by assigning it a null value, saved from
     * the parent Node
     */
    public void testRemoveBooleanParent() throws RepositoryException {
        property1.setValue((Value) null);
        node.save();

        try {
            node.getProperty(propertyName1);
            fail("The property should not exist anymore, as a null Value has been assigned");
        } catch (Exception e) {
            //success : the property has been deleted by assigning it a null value
        }
    }

    //--------------------------< internal >------------------------------------

    private void compareStream(byte[] data, InputStream s) throws IOException {
        byte[] read = new byte[1];
        for (int i = 0; i < data.length; i++) {
            assertEquals("Stream data does not match value set.", 1, s.read(read));
            assertEquals("Stream data does not match value set.", data[i], read[0]);
        }
        if (s.available() > 0) {
            fail("InputStream has more data than value set.");
        }
    }
}
