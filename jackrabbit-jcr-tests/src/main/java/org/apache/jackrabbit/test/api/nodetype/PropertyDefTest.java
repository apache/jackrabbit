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
package org.apache.jackrabbit.test.api.nodetype;

import org.apache.jackrabbit.test.AbstractJCRTest;
import org.apache.jackrabbit.test.NotExecutableException;
import org.apache.jackrabbit.test.api.PropertyUtil;

import javax.jcr.Session;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.PropertyType;
import javax.jcr.Value;
import javax.jcr.NodeIterator;
import javax.jcr.NamespaceException;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Tests if property definitions are properly defined.
 *
 * @test
 * @sources PropertyDefTest.java
 * @executeClass org.apache.jackrabbit.test.api.nodetype.PropertyDefTest
 * @keywords level1
 */
public class PropertyDefTest extends AbstractJCRTest {

    // format: '(<min>, <max>)',  '[<min>, <max>]', '(, <max>)' etc.
    private static final Pattern CONSTRAINTSPATTERN_BINARY =
            Pattern.compile("([\\(\\[]) *\\d* *, *\\d* *([\\)\\]])");

    // format: '(<min>, <max>)',  '[<min>, <max>]', '(, <max>)' etc.
    private static final Pattern CONSTRAINTSPATTERN_LONG =
            Pattern.compile("([\\(\\[]) *(\\-?\\d*)? *, *(\\-?\\d*)? *([\\)\\]])");

    // format: '(<min>, <max>)',  '[<min>, <max>]', '(, <max>)' etc.
    private static final Pattern CONSTRAINTSPATTERN_DOUBLE =
            Pattern.compile("([\\(\\[]) *(\\-?\\d+\\.?\\d*)? *, *(\\-?\\d+\\.?\\d*)? *([\\)\\]])");

    // format: '(<min>, <max>)',  '[<min>, <max>]', '(, <max>)' etc.
    private static final Pattern CONSTRAINTSPATTERN_DATE =
            Pattern.compile("([\\(\\[]) *(" + PropertyUtil.PATTERNSTRING_DATE + ")? *, *" +
            "(" + PropertyUtil.PATTERNSTRING_DATE + ")? *([\\)\\]])");

    private static final Pattern CONSTRAINTSPATTERN_PATH =
            Pattern.compile(PropertyUtil.PATTERNSTRING_PATH_WITHOUT_LAST_SLASH +
            "(/|/\\*)?");

    /**
     * The session we use for the tests
     */
    private Session session;

    /**
     * The node type manager of the session
     */
    private NodeTypeManager manager;

    /**
     * If <code>true</code> indicates that the test found a mandatory property
     */
    private boolean foundMandatoryProperty = false;

    /**
     * Sets up the fixture for the test cases.
     */
    protected void setUp() throws Exception {
        isReadOnly = true;
        super.setUp();

        session = helper.getReadOnlySession();
        manager = session.getWorkspace().getNodeTypeManager();
        // re-fetch testRootNode with read-only session
        testRootNode = (Node) session.getItem(testRoot);
    }

    /**
     * Releases the session aquired in {@link #setUp()}.
     */
    protected void tearDown() throws Exception {
        if (session != null) {
            session.logout();
            session = null;
        }
        manager = null;
        super.tearDown();
    }

    /**
     * Test getDeclaringNodeType() returns the node type which is defining the
     * requested property def. Test runs for all existing node types.
     */
    public void testGetDeclaringNodeType() throws RepositoryException {

        NodeTypeIterator types = manager.getAllNodeTypes();
        // loop all node types
        while (types.hasNext()) {
            NodeType currentType = types.nextNodeType();
            PropertyDefinition defsOfCurrentType[] =
                    currentType.getPropertyDefinitions();

            // loop all property defs of each node type
            for (int i = 0; i < defsOfCurrentType.length; i++) {
                PropertyDefinition def = defsOfCurrentType[i];
                NodeType type = def.getDeclaringNodeType();

                // check if def is part of the property defs of the
                // declaring node type
                PropertyDefinition defs[] = type.getPropertyDefinitions();
                boolean hasType = false;
                for (int j = 0; j < defs.length; j++) {
                    if (defs[j].getName().equals(def.getName())) {
                        hasType = true;
                        break;
                    }
                }
                assertTrue("getDeclaringNodeType() must return the node " +
                        "which defines the corresponding property def.",
                        hasType);
            }
        }
    }

    /**
     * Tests if auto create properties are not a residual set definition
     * (getName() does not return "*")
     */
    public void testIsAutoCreate() throws RepositoryException {

        NodeTypeIterator types = manager.getAllNodeTypes();
        // loop all node types
        while (types.hasNext()) {
            NodeType type = types.nextNodeType();
            PropertyDefinition defs[] = type.getPropertyDefinitions();
            for (int i = 0; i < defs.length; i++) {
                if (defs[i].isAutoCreated()) {
                    assertFalse("An auto create property must not be a " +
                            "residual set definition.",
                            defs[i].getName().equals("*"));
                }
            }
        }
    }

    /**
     * This test checks if item definitions with mandatory constraints are
     * respected.
     * <p/>
     * If the default workspace does not contain a node with a node type
     * definition that specifies a mandatory property a {@link
     * org.apache.jackrabbit.test.NotExecutableException} is thrown.
     */
    public void testIsMandatory() throws RepositoryException, NotExecutableException {
        traverse(testRootNode);
        if (!foundMandatoryProperty) {
            throw new NotExecutableException("Workspace does not contain any node with a mandatory property definition");
        }
    }

    /**
     * Tests if isRequiredType() returns a valid PropertyType. </p> The test
     * runs for all available node types.
     */
    public void testIsRequiredType()
            throws RepositoryException {

        NodeTypeIterator types = manager.getAllNodeTypes();
        // loop all node types
        while (types.hasNext()) {
            NodeType type = types.nextNodeType();
            PropertyDefinition defs[] = type.getPropertyDefinitions();
            for (int i = 0; i < defs.length; i++) {
                switch (defs[i].getRequiredType()) {
                    case PropertyType.STRING:
                    case PropertyType.BINARY:
                    case PropertyType.DATE:
                    case PropertyType.LONG:
                    case PropertyType.DOUBLE:
                    case PropertyType.NAME:
                    case PropertyType.PATH:
                    case PropertyType.REFERENCE:
                    case PropertyType.BOOLEAN:
                    case PropertyType.UNDEFINED:
                        // success
                        break;
                    default:
                        fail("getRequiredType() returns an " +
                                "invalid PropertyType.");
                }
            }
        }
    }

    /**
     * Tests if value constraints match the pattern specified by the required
     * property type. </p> The test runs for all value constraints of all
     * properties of all available node types.
     */
    public void testGetValueConstraints() throws RepositoryException {

        NodeTypeIterator types = manager.getAllNodeTypes();
        // loop all node types
        while (types.hasNext()) {
            NodeType type = types.nextNodeType();
            PropertyDefinition defs[] = type.getPropertyDefinitions();
            for (int i = 0; i < defs.length; i++) {
                PropertyDefinition def = defs[i];

                String constraints[] = def.getValueConstraints();
                if (constraints != null) {

                    for (int j = 0; j < constraints.length; j++) {
                        Matcher matcher;

                        switch (defs[i].getRequiredType()) {
                            case PropertyType.STRING:
                            case PropertyType.UNDEFINED:
                                // any value matches
                                break;

                            case PropertyType.BINARY:
                                matcher =
                                        CONSTRAINTSPATTERN_BINARY.matcher(constraints[j]);
                                assertTrue("Value constraint does not match " +
                                        "the pattern of PropertyType.BINARY ",
                                        matcher.matches());
                                break;

                            case PropertyType.DATE:
                                matcher =
                                        CONSTRAINTSPATTERN_DATE.matcher(constraints[j]);
                                assertTrue("Value constraint does not match " +
                                        "the pattern of PropertyType.DATE ",
                                        matcher.matches());
                                break;

                            case PropertyType.LONG:
                                matcher =
                                        CONSTRAINTSPATTERN_LONG.matcher(constraints[j]);
                                assertTrue("Value constraint does not match " +
                                        "the pattern of PropertyType.LONG",
                                        matcher.matches());
                                break;

                            case PropertyType.DOUBLE:
                                matcher =
                                        CONSTRAINTSPATTERN_DOUBLE.matcher(constraints[j]);
                                assertTrue("Value constraint does not match " +
                                        "the pattern of PropertyType.DOUBLE",
                                        matcher.matches());
                                break;

                            case PropertyType.NAME:
                                matcher =
                                        PropertyUtil.PATTERN_NAME.matcher(constraints[j]);
                                assertTrue("Value constraint does not match " +
                                        "the pattern of PropertyType.NAME",
                                        matcher.matches());

                                checkPrefix(constraints[j]);
                                break;

                            case PropertyType.PATH:
                                matcher = CONSTRAINTSPATTERN_PATH.matcher(constraints[j]);
                                assertTrue("Value constraint does not match " +
                                        "the pattern of PropertyType.PATH",
                                        matcher.matches());

                                String elems[] = constraints[j].split("/");
                                for (int k = 0; k < elems.length; k++) {
                                    checkPrefix(elems[k]);
                                }
                                break;

                            case PropertyType.REFERENCE:
                                matcher =
                                        PropertyUtil.PATTERN_NAME.matcher(constraints[j]);
                                assertTrue("Value constraint does not match " +
                                        "the pattern of PropertyType.REFERENCE",
                                        matcher.matches());

                                checkPrefix(constraints[j]);
                                break;

                            case PropertyType.BOOLEAN:
                                assertTrue("Value constraint does not match " +
                                        "the pattern of PropertyType.BOOLEAN",
                                        constraints[j].equals("true") ||
                                        constraints[j].equals("false"));
                                break;
                        }
                    }
                }
            }
        }
    }

    /**
     * Tests if single-valued properties do have not more than one default value
     * </p> The test runs for all default values of all properties of all
     * available node types.
     */
    public void testGetDefaultValues()
            throws RepositoryException {

        NodeTypeIterator types = manager.getAllNodeTypes();
        // loop all node types
        while (types.hasNext()) {
            NodeType type = types.nextNodeType();
            PropertyDefinition defs[] = type.getPropertyDefinitions();
            for (int i = 0; i < defs.length; i++) {
                PropertyDefinition def = defs[i];

                Value values[] = def.getDefaultValues();
                if (values != null) {

                    for (int j = 0; j < values.length; j++) {

                        if (!def.isMultiple()) {
                            assertEquals("Single-valued properties must not " +
                                    "have more than one default value.",
                                    1, values.length);
                        }
                    }
                }
            }
        }
    }

    // ---------------------------------< internal >----------------------------

    /**
     * Traverses the node hierarchy and applies
     * {@link #checkMandatoryConstraint(javax.jcr.Node, javax.jcr.nodetype.NodeType)}
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
     * Checks if mandatory property definitions are respected.
     */
    private void checkMandatoryConstraint(Node node, NodeType type)
            throws RepositoryException {

        // test if node contains all mandatory properties of current type
        PropertyDefinition propDefs[] = type.getPropertyDefinitions();
        for (int i = 0; i < propDefs.length; i++) {
            PropertyDefinition propDef = propDefs[i];

            if (propDef.isMandatory()) {
                foundMandatoryProperty = true;
                String name = propDef.getName();

                assertTrue("Node " + node.getPath() + " does not contain " +
                        "value for mandatory property: " + name, node.hasProperty(name));
                // todo check back with latest spec!
                /*
                try {
                    Property p = node.getProperty(name);
                    if (propDef.isMultiple()) {
                        // empty array fails
                        assertFalse("The mandatory and multiple property " + p.getName() +
                                " must not be empty.",
                                p.getValues().length == 0);
                    } else {
                        // empty value fails
                        assertNotNull("A mandatory property must have a value",
                                p.getValue());
                    }
                } catch (PathNotFoundException e) {
                    fail("Mandatory property " + name + " does not exist.");
                }
                */
            }
        }
    }

    /**
     * Checks for NAME, PATH and REFERENCE constraint values if the constraint
     * is reflecting the namespace mapping in the current <code>Session</code>
     *
     * @throws NamespaceException if the prefix of name is not a registered
     *                            namespace prefix
     */
    private void checkPrefix(String name)
            throws NamespaceException, RepositoryException {

        if (name.indexOf(":") != -1) {
            String prefix = name.split(":")[0];

            // NamespaceException is thrown if fails
            session.getNamespaceURI(prefix);
        }
    }
}
