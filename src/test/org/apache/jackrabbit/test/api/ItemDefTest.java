/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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
import javax.jcr.RepositoryException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PathNotFoundException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDef;
import javax.jcr.nodetype.NodeDef;

/**
 * This test checks if item definitions with mandatory constraints are
 * respected.
 * <p/>
 * If the default workspace does not contain a node with a node type definition
 * that specifies a mandatory child node a {@link NotExecutableException} is
 * thrown. If the default workspace does not contain a node with a node type
 * definition that specifies a mandatory property a {@link NotExecutableException}
 * is thrown.
 *
 * @test
 * @sources ItemDefTest.java
 * @executeClass org.apache.jackrabbit.test.api.ItemDefTest
 * @keywords level1
 */
public class ItemDefTest extends AbstractJCRTest {

    /** If <code>true</code> indicates that the test found a mandatory node */
    private boolean foundMandatoryNode = false;

    /** If <code>true</code> indicates that the test found a mandatory property */
    private boolean foundMandatoryProperty = false;

    /**
     * Sets up the fixture for this test.
     */
    protected void setUp() throws Exception {
        isReadOnly = true;
        super.setUp();
    }

    /**
     * Tests if a node's mandatory properties and nodes are legally set. The
     * information which properties and nodes are mandatory is taken from the
     * node's primary and mixin node types. This test runs recursively through
     * the entire repository.
     */
    public void testIsMandatory() throws RepositoryException, NotExecutableException {
        //Session session=superuser;
        Session session = helper.getReadOnlySession();
        Node root = session.getRootNode();
        traverse(root);
        if (!foundMandatoryNode) {
            throw new NotExecutableException("Workspace does not contain any node with a mandatory child node definition");
        }
        if (!foundMandatoryProperty) {
            throw new NotExecutableException("Workspace does not contain any node with a mandatory property definition");
        }
    }

    /**
     * Traverses the node hierarchy and applies {@link #checkMandatoryConstraint(javax.jcr.Node, javax.jcr.nodetype.NodeType)}
     * to all descendant nodes of <code>parentNode</code>.
     */
    private void traverse(Node parentNode)
            throws RepositoryException {

        NodeIterator nodes = parentNode.getNodes();
        while (nodes.hasNext()) {
            Node node = nodes.nextNode();

            NodeType primeType = node.getPrimaryNodeType();
            checkMandatoryConstraint(node, primeType);

            NodeType mixins[] = node.getMixinNodeTypes();
            for (int i = 0; i < mixins.length; i++) {
                checkMandatoryConstraint(node, mixins[i]);
            }

            traverse(node);
        }
    }

    /**
     * Checks if mandatory node / property definitions are respected.
     */
    private void checkMandatoryConstraint(Node node, NodeType type)
            throws RepositoryException {

        // test if node contains all mandatory properties of current type
        PropertyDef propDefs[] = type.getPropertyDefs();
        for (int i = 0; i < propDefs.length; i++) {
            PropertyDef propDef = propDefs[i];

            if (propDef.isMandatory()) {
                foundMandatoryProperty = true;
                String name = propDef.getName();

                try {
                    Property p = node.getProperty(name);
                    if (propDef.isMultiple()) {
                        // empty array fails
                        assertFalse("A mandatory and multiple property " +
                                "must not be empty.",
                                p.getValues().length == 0);
                    } else {
                        // empty value fails
                        assertNotNull("A mandatory property must have a value",
                                p.getValue());
                    }
                } catch (PathNotFoundException e) {
                    fail("Mandatory property " + name + " does not exist.");
                }
            }
        }

        // test if node contains all mandatory nodes of current type
        NodeDef nodeDefs[] = type.getChildNodeDefs();
        for (int i = 0; i < nodeDefs.length; i++) {
            NodeDef nodeDef = nodeDefs[i];
            if (nodeDef.isMandatory()) {
                foundMandatoryNode = true;
                try {
                    node.getNode(nodeDef.getName());
                } catch (PathNotFoundException e) {
                    fail("Mandatory child " + nodeDef.getName() + " for " + node.getPath() + " does not exist.");
                }
            }
        }
    }
}