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

import javax.jcr.Node;
import javax.jcr.Property;

import java.io.InputStream;
import java.io.ByteArrayInputStream;

/**
 * <code>SetPropertyInputStreamTest</code> tests the <code>Node.setProperty(String,
 * InputStream)</code> method
 *
 * @test
 * @sources SetPropertyInputStreamTest.java
 * @executeClass org.apache.jackrabbit.test.api.SetPropertyInputStreamTest
 * @keywords level2
 */
public class SetPropertyInputStreamTest extends AbstractJCRTest {

    private Node testNode;

    byte[] bytes1 = {73, 26, 32, -36, 40, -43, -124};
    private InputStream is1 = new ByteArrayInputStream(bytes1);
    byte[] bytes2 = {-124, -43, 40, -36, 32, 26, 73};
    private InputStream is2 = new ByteArrayInputStream(bytes2);

    protected void setUp() throws Exception {
        super.setUp();
        testNode = testRootNode.addNode(nodeName1, testNodeType);
        testRootNode.save();
        
        // special case for repositories that do allow binary property
        // values, but only on jcr:content/jcr:data
        if (propertyName1.equals("jcr:data") && testNode.hasNode("jcr:content")
            && testNode.getNode("jcr:content").isNodeType("nt:resource") && ! testNode.hasProperty("jcr:data")) {
          testNode = testNode.getNode("jcr:content");
        }
    }

    protected void tearDown() throws Exception {
        testNode = null;
        super.tearDown();
    }

    /**
     * Tests if adding a property with <code>Node.setProperty(String,
     * InputStream)</code> works with <code>Session.save()</code>
     */
    public void testNewInputStreamPropertySession() throws Exception {
        testNode.setProperty(propertyName1, is1);
        superuser.save();
        is1 = new ByteArrayInputStream(bytes1);
        InputStream in = testNode.getProperty(propertyName1).getStream();
        try {
            assertTrue("Setting property with Node.setProperty(String, InputStream) and Session.save() not working",
                    compareInputStreams(is1, in));
        } finally {
            in.close();
        }
    }

    /**
     * Tests if modifying a property with <code>Node.setProperty(String,
     * InputStream)</code> works with <code>Session.save()</code>
     */
    public void testModifyInputStreamPropertySession() throws Exception {
        testNode.setProperty(propertyName1, is1);
        superuser.save();
        testNode.setProperty(propertyName1, is2);
        superuser.save();
        is2 = new ByteArrayInputStream(bytes2);
        InputStream in = testNode.getProperty(propertyName1).getStream();
        try {
            assertTrue("Modifying property with Node.setProperty(String, InputStream) and Session.save() not working",
                    compareInputStreams(is2, in));
        } finally {
            in.close();
        }
    }

    /**
     * Tests if adding a property with <code>Node.setProperty(String,
     * InputStream)</code> works with <code>parentNode.save()</code>
     */
    public void testNewInputStreamPropertyParent() throws Exception {
        testNode.setProperty(propertyName1, is1);
        testRootNode.save();
        is1 = new ByteArrayInputStream(bytes1);
        InputStream in = testNode.getProperty(propertyName1).getStream();
        try {
            assertTrue("Setting property with Node.setProperty(String, InputStream) and parentNode.save() not working",
                    compareInputStreams(is1, in));
        } finally {
            in.close();
        }
    }

    /**
     * Tests if modifying a property with <code>Node.setProperty(String,
     * InputStream)</code> works with <code>parentNode.save()</code>
     */
    public void testModifyInputStreamPropertyParent() throws Exception {
        testNode.setProperty(propertyName1, is1);
        testRootNode.save();
        testNode.setProperty(propertyName1, is2);
        testRootNode.save();
        is2 = new ByteArrayInputStream(bytes2);
        InputStream in = testNode.getProperty(propertyName1).getStream();
        try {
            assertTrue("Modifying property with Node.setProperty(String, InputStream) and parentNode.save() not working",
                    compareInputStreams(is2, in));
        } finally {
            in.close();
        }
    }

    /**
     * Tests if removing a <code>InputStream</code> property with
     * <code>Node.setProperty(String, null)</code> works with
     * <code>Session.save()</code>
     */
    public void testRemoveInputStreamPropertySession() throws Exception {
        testNode.setProperty(propertyName1, is1);
        superuser.save();

        Property property = testNode.getProperty(propertyName1);
        if (property.getDefinition().isMandatory() || property.getDefinition().isProtected()) {
            throw new NotExecutableException("property " + property.getName() + " can not be removed");
        }

        testNode.setProperty(propertyName1, (InputStream) null);
        superuser.save();
        assertFalse("Removing property with Node.setProperty(String, (InputStream)null) and Session.save() not working",
                testNode.hasProperty(propertyName1));
    }

    /**
     * Tests if removing a <code>InputStream</code> property with
     * <code>Node.setProperty(String, null)</code> works with
     * <code>parentNode.save()</code>
     */
    public void testRemoveInputStreamPropertyParent() throws Exception {
        testNode.setProperty(propertyName1, is1);
        testRootNode.save();
        
        Property property = testNode.getProperty(propertyName1);
        if (property.getDefinition().isMandatory() || property.getDefinition().isProtected()) {
            throw new NotExecutableException("property " + property.getName() + " can not be removed");
        }

        testNode.setProperty(propertyName1, (InputStream) null);
        testRootNode.save();
        assertFalse("Removing property with Node.setProperty(String, (InputStream)null) and parentNode.save() not working",
                testNode.hasProperty(propertyName1));
    }

    /**
     * helper function: InputStream comparison
     */
    private boolean compareInputStreams(InputStream f1, InputStream f2) {
        try {
            boolean equal = false;
            int f1byte, f2byte;

            while ((f1byte = f1.read()) != -1) {
                // byte match -> check next
                if ((f2byte = f2.read()) != -1 && f2byte == f1byte) {
                    equal = true;
                    continue;
                }
                // byte mismatch
                else {
                    equal = false;
                    break;
                }
            }

            // length mismatch
            if ((f2byte = f2.read()) != -1) {
                equal = false;
            }

            return equal;
        } catch (Exception e) {
            return false;
        }
    }
}