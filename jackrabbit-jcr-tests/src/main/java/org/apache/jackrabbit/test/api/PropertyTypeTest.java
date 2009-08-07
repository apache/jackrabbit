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

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.PropertyIterator;
import javax.jcr.NodeIterator;
import javax.jcr.Property;

/**
 * Tests if the type of a property is set according to the node type as well
 * as no property is of type UNDEFINED. This test runs recursively through
 * the workspace starting at {@link #testRoot}.
 *
 * @test
 * @sources PropertyTypeTest.java
 * @executeClass org.apache.jackrabbit.test.api.PropertyTypeTest
 * @keywords level1
 */
public class PropertyTypeTest extends AbstractJCRTest {

    /**
     * Sets up the fixture for this test.
     */
    protected void setUp() throws Exception {
        isReadOnly = true;
        super.setUp();
    }

    /**
     * Tests if the type of a property is set according to the node type as well
     * as no property is of type UNDEFINED. This test runs recursively through
     * the workspace starting at {@link #testRoot}.
     */
    public void testType() throws RepositoryException {
        Session session = helper.getReadOnlySession();
        try {
            Node root = session.getRootNode().getNode(testPath);
            typeCheckChildren(root);
        } finally {
            session.logout();
        }
    }

    private void typeCheckChildren(Node parentNode)
            throws RepositoryException {

        NodeIterator nodes = parentNode.getNodes();
        while (nodes.hasNext()) {
            Node node = nodes.nextNode();

            PropertyIterator props = node.getProperties();
            while (props.hasNext()) {
                Property prop = props.nextProperty();
                int reqType = prop.getDefinition().getRequiredType();
                int type = PropertyType.UNDEFINED;
                boolean isEmptyMultipleArray = false;

                if (prop.getDefinition().isMultiple()) {
                    if (prop.getValues().length > 0) {
                        type = prop.getValues()[0].getType();
                    } else {
                        isEmptyMultipleArray = true;
                    }
                } else {
                    type = prop.getValue().getType();
                }

                if (!isEmptyMultipleArray &&
                        reqType != PropertyType.UNDEFINED) {

                    assertFalse("The type of a property must not " +
                            "be UNDEFINED",
                            type == PropertyType.UNDEFINED);

                    assertEquals("The type of a property has to match " +
                            "the type of the property definition.",
                            type,
                            reqType);
                }
            }
            typeCheckChildren(node);
        }
    }


}