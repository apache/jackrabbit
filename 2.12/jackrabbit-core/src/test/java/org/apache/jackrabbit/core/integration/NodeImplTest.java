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
package org.apache.jackrabbit.core.integration;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.version.Version;

import org.apache.jackrabbit.test.AbstractJCRTest;

/**
 * Integration tests for the Node implementation in Jackrabbit core.
 */
public class NodeImplTest extends AbstractJCRTest {

    private Node node;

    protected void setUp() throws Exception {
        super.setUp();
        node = testRootNode.addNode("testNodeImpl", "nt:unstructured");
        testRootNode.save();
    }

    protected void tearDown() throws Exception {
        node.remove();
        testRootNode.save();
        super.tearDown();
    }

    /**
     * Test case for JCR-1389.
     * 
     * @see <a href="https://issues.apache.org/jira/browse/JCR-1389">JCR-1389</a>
     */
    public void testSetEmptyMultiValueProperty() throws RepositoryException {
        Property property =
            node.setProperty("test", new Value[0], PropertyType.LONG);
        assertEquals(
                "JCR-1389: setProperty(name, new Value[0], PropertyType.LONG)"
                + " loses property type",
                PropertyType.LONG, property.getType());
    }

    /**
     * Test case for JCR-1227.
     * 
     * @see <a href="https://issues.apache.org/jira/browse/JCR-1227">JCR-1227</a>
     */
    public void testRestoreEmptyMultiValueProperty() throws Exception {
        node.addMixin("mix:versionable");
        node.setProperty("test", new Value[0], PropertyType.LONG);
        node.save();
        assertEquals(PropertyType.LONG, node.getProperty("test").getType());

        Version version = node.checkin();
        assertEquals(PropertyType.LONG, node.getProperty("test").getType());

        node.restore(version, false);
        assertEquals(
                "JCR-1227: Restore of empty multivalue property always"
                + " changes property type to String",
                PropertyType.LONG, node.getProperty("test").getType());

        node.checkout();
        node.setProperty("test", new Value[0], PropertyType.BOOLEAN);
        node.save();
        assertEquals(PropertyType.BOOLEAN, node.getProperty("test").getType());

        node.restore(version, false);
        assertEquals(
                "JCR-1227: Restore of empty multivalue property always"
                + " changes property type to String",
                PropertyType.LONG, node.getProperty("test").getType());
    }

}
