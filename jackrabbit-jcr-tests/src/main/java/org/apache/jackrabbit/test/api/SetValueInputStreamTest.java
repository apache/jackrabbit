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
import org.apache.jackrabbit.test.api.util.InputStreamWrapper;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * Tests the {@link javax.jcr.Property#setValue(java.io.InputStream)} method.
 * <p>
 * Configuration requirements:
 * <p>
 * The node at {@link #testRoot} must allow a
 * child node of type {@link #testNodeType} with name {@link #nodeName1}. The
 * node type {@link #testNodeType} must define a single value binary property
 * with name {@link #propertyName1}. As a special case, if the specified node
 * type automatically adds a jcr:content child node of type nt:resource, and
 * <code>propertyName1</code> is specified as "jcr:data", that binary property
 * is used instead.
 *
 */
public class SetValueInputStreamTest extends AbstractJCRTest {

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

        // create a new node under the testRootNode
        node = testRootNode.addNode(nodeName1, testNodeType);
        testRootNode.getSession().save();

        // special case for repositories that do allow binary property
        // values, but only on jcr:content/jcr:data
        if (propertyName1.equals("jcr:data") && node.hasNode("jcr:content")
            && node.getNode("jcr:content").isNodeType("nt:resource") && ! node.hasProperty("jcr:data")) {
            node = node.getNode("jcr:content");
        }

        // create a new single-value property and save it
        property1 = node.setProperty(propertyName1, superuser.getValueFactory().createValue(new ByteArrayInputStream(new byte[0])));
        superuser.save();
    }

    protected void tearDown() throws Exception {
        node = null;
        property1 = null;
        super.tearDown();
    }

    /**
     * Tests whether <code>Property.setValue(InputStream)</code> obeys the
     * stream handling contract.
     */
    public void testInputStreamClosed() throws RepositoryException, IOException {
        InputStreamWrapper in = new InputStreamWrapper(new ByteArrayInputStream(data));
        property1.setValue(in);
        assertTrue("Property.setValue(InputStream) is expected to close the passed input stream", in.isClosed());
    }
}
