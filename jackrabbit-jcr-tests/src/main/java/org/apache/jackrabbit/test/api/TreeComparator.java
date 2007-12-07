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

import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.NodeIterator;
import javax.jcr.PropertyIterator;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.Value;
import javax.jcr.PathNotFoundException;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeType;
import java.util.Calendar;
import java.io.ByteArrayInputStream;

/**
 * <code>TreeComparator</code> compares two trees. This allows re-use for
 * different tests, and it allows to test a function on any tree, not just a
 * simple example node.
 * <p/>
 * TreeComparator also creates an example tree that contains as many features as
 * possible.
 */
class TreeComparator extends AbstractJCRTest {
    public SerializationContext sc;

    public final boolean WORKSPACE = true;
    public final boolean SESSION = false;

    public final int CHECK_EMPTY = -1;
    public final int IGNORE = 0;
    public final int CHECK_SAME = 1;

    public int check = CHECK_SAME;

    private Session session;
    private Workspace workspace;

    public boolean skipBinary = false, noRecurse = false;
    public String sourceFolder, targetFolder;
    public String root;

    public TreeComparator(SerializationContext sc, Session s) throws Exception {
        this.sc = sc;
        setUp();
        session = s;
        workspace = session.getWorkspace();
        init();
    }

    public void tearDown() throws Exception {
        session = null;
        workspace = null;
        super.tearDown();
    }

    public void setSession(Session session) {
        this.session = session;
    }
    
    /**
     * Makes sure that the source and target folder exist, and are empty
     */
    private void init() throws RepositoryException {
        root = sc.testroot;
        sourceFolder = root + "/" + sc.sourceFolderName;
        targetFolder = root + "/" + sc.targetFolderName;

        // make sure the node does not have a and target sub node.
        try {
            session.getItem(sourceFolder).remove();
            session.save();
        } catch (PathNotFoundException e) {
            // item does not exist
        }
        try {
            Item tgt = session.getItem(targetFolder);
            tgt.remove();
            session.save();
        } catch (PathNotFoundException e) {
            // item does not exist
        }

        // add the source and target nodes
        Node rootNode = (Node) session.getItem(root);
        rootNode.addNode(sc.sourceFolderName);
        rootNode.addNode(sc.targetFolderName);
        session.save();
    }

    /**
     * Creates an example tree in the workspace.
     */
    public void createExampleTree() {
        createExampleTree(true);
    }

    /**
     * Creates a simple example tree. Use this tree for general repository
     * functions, such as serialization, namespaces and versioning.
     * <p/>
     * The beauty of this is that the tree contains exactly the features that
     * are supported by the repository. Any repository exceptions that occur are
     * displayed on "out", but are otherwise ignored.
     *
     * @param save If true, the example tree is saved to the workspace. If
     *             false, it remains in the session.
     */
    public void createExampleTree(boolean save) {
        try {
            Node src = (Node) session.getItem(sourceFolder);
            Node root = src.addNode(sc.rootNodeName);
            root.addNode(nodeName1);
            root.addNode(nodeName2, testNodeType);
            byte[] byteArray = {(byte) 0, (byte) 255, (byte) 167, (byte) 100, (byte) 21, (byte) 6, (byte) 19, (byte) 71, (byte) 221};
            root.setProperty(propertyName1, new ByteArrayInputStream(byteArray));
            root.setProperty(nodeName3, "hello");
        } catch (Exception e) {
            log.println("Error while creating example tree: " + e.getMessage());
        }
        if (save) {
            try {
                session.save();
            } catch (RepositoryException e) {
                fail("Cannot save the example tree to the repository: " + e);
            }
        }
    }

    /**
     * Creates a complex example tree in the workspace
     */
    public void createComplexTree() {
        createComplexTree(true);
    }

    /**
     * Creates a complex example tree that uses as many features of the
     * repository as possible
     *
     * @param save Save the tree to the workspace. If false, the tree remains in
     *             the session.
     */
    public void createComplexTree(boolean save) {
        Node rootNode = null;
        Node nt = null;
        Node pt = null;
        Node mvp = null;
        Node referenceable = null;
        try {
            Node src = (Node) session.getItem(sourceFolder);
            rootNode = src.addNode(sc.rootNodeName);
            nt = rootNode.addNode(sc.nodeTypesTestNode);
            rootNode.addNode(sc.mixinTypeTestNode);
            pt = rootNode.addNode(sc.propertyTypesTestNode);
            rootNode.addNode(sc.sameNameChildrenTestNode);
            mvp = rootNode.addNode(sc.multiValuePropertiesTestNode);
            referenceable = rootNode.addNode(sc.referenceableNodeTestNode);
            rootNode.addNode(sc.orderChildrenTestNode);
            rootNode.addNode(sc.namespaceTestNode);
        } catch (RepositoryException e) {
            log.println("Error while creating example tree: " + e.getMessage());
        }

        // Add nodes with mixin types
        NodeTypeManager ntmgr = null;
        NodeTypeIterator types = null;
        try {
            ntmgr = workspace.getNodeTypeManager();
            types = ntmgr.getMixinNodeTypes();
        } catch (RepositoryException e) {
            fail("Cannot access NodeType iterator: " + e);
        }
        while (types.hasNext()) {
            NodeType t = (NodeType) types.next();
            String name = t.getName();
            name = "Node_" + name.replaceAll(":", "_");

            Node n = null;
            try {
                n = nt.addNode(name);
                n.addMixin(t.getName());
                // try saving, because some exceptions are trown only at save time
                session.save();
            } catch (RepositoryException e) {
                log.println("Cannot create node with mixin node type: " + e);
                // if saving failed for a node, then remove it again (or else the next save will fail on it)
                try {
                    if (n != null) {
                        n.remove();
                    }
                } catch (RepositoryException e1) {
                    log.println("Could not remove node: " + e);
                }
            }
        }

        // Create all property types
        try {
            // String
            pt.setProperty(sc.stringTestProperty, "This is a string.");
            // Binary
            byte[] byteArray = {(byte) 0, (byte) 255, (byte) 167, (byte) 100, (byte) 21, (byte) 6, (byte) 19, (byte) 71, (byte) 221};
            pt.setProperty(sc.binaryTestProperty, new ByteArrayInputStream(byteArray));
            // Date
            Calendar c = Calendar.getInstance();
            c.set(2005, 6, 21, 13, 30, 5);
            pt.setProperty(sc.dateTestProperty, c);
            // Long
            pt.setProperty(sc.longTestProperty, (long) (1 / 3));
            // Double
            pt.setProperty(sc.doubleTestProperty, (double) Math.PI);
            // Boolean
            pt.setProperty(sc.booleanTestProperty, true);
            // Name
            pt.setProperty(sc.nameTestProperty, superuser.getValueFactory().createValue(jcrPrimaryType, PropertyType.NAME));
            // Path
            pt.setProperty(sc.pathTestProperty, superuser.getValueFactory().createValue("paths/dont/have/to/point/anywhere", PropertyType.PATH));
            // Reference: Note that I only check if the node exists. We do not specify what happens with
            // the UUID during serialization.
            if (!referenceable.isNodeType(mixReferenceable)) {
                referenceable.addMixin(mixReferenceable);
                // some implementations may require a save after addMixin()                
                session.save();
            }

            pt.setProperty(sc.referenceTestProperty, referenceable);

            // Create a boolean property on the root node, so I can test noRecurse and skipBinary at the same time
            rootNode.setProperty(sc.binaryTestProperty, new ByteArrayInputStream(byteArray));
            session.save();
        } catch (Exception e) {
            fail("Could not add property: " + e);
        }

        // multi value properties
        String[] s = {"one", "two", "three"};
        try {
            mvp.setProperty(sc.multiValueTestProperty, s);
            session.save();
        } catch (RepositoryException e) {
            log.println("Could not create multi-value property: " + e);
        }

        // Save to the workspace. Note that export is from session anyway.
        if (save) {
            try {
                session.save();
            } catch (RepositoryException e) {
                fail("Cannot save the example tree to the repository: " + e);
            }
        }
    }

    /**
     * Compares the trees in the source and target folder.
     *
     * @param skipBinary True if skipbinary is set, so binary properties are not
     *                   in the taget tree.
     * @param noRecurse  True if noRecurse is used, so only the top node and its
     *                   properties are in the target tree.
     */
    public void compare(boolean skipBinary, boolean noRecurse) {
        this.skipBinary = skipBinary;
        this.noRecurse = noRecurse;
        compare();
    }

    /**
     * Compares the source and target tree.
     */
    public void compare() {
        compare(CHECK_SAME);
    }

    /**
     * Compares the source and target tree.
     *
     * @param check CHECK_SAME checks if the two trees have the same nodes and
     *              properties. CHECK_EMPTY checks that the target tree is
     *              empty.
     */
    public void compare(int check) {
        this.check = check;
        compare(sourceFolder + "/" + sc.rootNodeName, 0);
    }

    /**
     * Compares two nodes in the source and target tree
     *
     * @param sourcePath The path of the node in the source tree
     * @param level      The level of depth in the tree
     */
    public void compare(String sourcePath, int level) {
        Node source = null;
        Node target = null;

        // get the source path
        try {
            source = (Node) session.getItem(sourcePath);
        } catch (RepositoryException e) {
            fail("Could not read source node " + sourcePath + ": " + e.getMessage());
        }

        // get the target path
        String targetPath = getTargetPath(sourcePath);
        try {
            session.getItem(targetFolder);
        } catch (RepositoryException e) {
            fail("Target folder not found: " + e);
        }

        // Check noRecurse: After top level, the target tree must be empty
        if (noRecurse && level == 1) {
            check = CHECK_EMPTY;
        }

        // compare source and target
        if (check == CHECK_SAME) {
            try {
                target = (Node) session.getItem(targetPath);
            } catch (RepositoryException e) {
                showTree();
                fail("Could not read target node " + targetPath + ": " + e);
            }
            compareNodes(source, target);
        } else if (check == CHECK_EMPTY) {
            try {
                session.getItem(targetPath);
                fail("The item " + targetPath + " must not be available.");
            } catch (RepositoryException e) {
            }
        }

        // iterate through all child nodes of the source tree
        try {
            NodeIterator ni = source.getNodes();
            while (ni.hasNext()) {
                Node n = (Node) ni.next();
                compare(n.getPath(), level + 1);
            }
        } catch (RepositoryException e) {
            fail("Error while iterating through child nodes: " + e);
        }
    }

    /**
     * Compares two nodes, a and b
     *
     * @param a The node in the source tree
     * @param b The same node in the target tree
     */
    public void compareNodes(Node a, Node b) {
        try {
            log.println("Comparing " + a.getPath() + " to " + b.getPath());
        } catch (RepositoryException e) {
            fail("Nodes not available: " + e.getMessage());
        }

        // check primary node type
        String primaryTypeA = null, primaryTypeB = null;
        try {
            primaryTypeA = a.getProperty(jcrPrimaryType).getName();
            primaryTypeB = b.getProperty(jcrPrimaryType).getName();
        } catch (RepositoryException e) {
            fail("Primary node type not available: " + e);
        }
        assertEquals("Primary node type has changed.", primaryTypeA, primaryTypeB);

        compareProperties(a, b);
    }

    /**
     * Compares all the properties of the two nodes a and b
     *
     * @param a The node in the source tree.
     * @param b The node in the target tree.
     */
    public void compareProperties(Node a, Node b) {
        PropertyIterator ai = null;
        try {
            ai = a.getProperties();
        } catch (RepositoryException e) {
            fail("Cannot access properties: " + e);
        }
        while (ai.hasNext()) {
            Property pa = (Property) ai.next();
            String pName = null;
            // todo
            String pPath = null;
            try {
                pPath = pa.getPath();
            } catch (RepositoryException e) {

            }

            int pType = 0;

            try {
                pName = pa.getName();
                if (pa.getDefinition().isMultiple()) {
                    pType = -9999;
                } else {
                    pType = pa.getValue().getType();
                }
            } catch (RepositoryException e) {
                fail("Cannot access property information: " + e);
            }

            if (propertyValueMayChange(pName)) {
                continue;
            }

            Property pb = null;
            // avoid skipped properties
            if (!propertySkipped(pName)) {
                try {
                    pb = b.getProperty(pName);
                } catch (RepositoryException e) {
                    //fail if the property is not there but should
                    fail("Property '" + pPath + "' not available: " + e);
                }

                if (!(skipBinary && pType == PropertyType.BINARY)) {
                    // todo
                    // compare source and target value
                    compareProperties(pa, pb);
                }
            }
        }
    }

    /**
     * Compares two properties a and b.
     *
     * @param a The property in the source tree.
     * @param b The property in the target tree.
     */
    public void compareProperties(Property a, Property b) {
        String nodeName = null, propertyName = null;
        boolean isMultiple = false;
        try {
            nodeName = a.getParent().getName();
            propertyName = a.getName();
            isMultiple = a.getDefinition().isMultiple();
        } catch (RepositoryException e) {
            fail("Cannot access property information: " + e);
        }

        if (!propertyValueMayChange(propertyName)) {
            if (isMultiple) {
                try {
                    compareValues(nodeName, propertyName, a.getValues(), b.getValues());
                } catch (RepositoryException e) {
                    fail("Could not access property values: " + e);
                }
            } else {
                try {
                    compareValue(nodeName, propertyName, a.getValue(), b.getValue());
                } catch (RepositoryException e) {
                    fail("Could not access property value: " + e);
                }
            }
        }
    }

    /**
     * Compares a set of multi-value properties.
     *
     * @param n Name of the node.
     * @param p Name of the property.
     * @param a Value (array) of the property in the source tree.
     * @param b Value (array) of the property in the target tree.
     */
    public void compareValues(String n, String p, Value[] a, Value[] b) {
        assertEquals("Multi-value property '" + p + "' of node '" + n + "' has changed length: ", a.length, b.length);
        for (int t = 0; t < a.length; t++) {
            compareValue(n, p, a[t], b[t]);
        }
    }

    /**
     * Compares the value of two properties
     *
     * @param n Name of the node.
     * @param p Name of the property.
     * @param a Value of the property in the source tree.
     * @param b Value of the property in the target tree.
     */
    public void compareValue(String n, String p, Value a, Value b) {
        if (!propertyValueMayChange(p)) {
            try {
                assertEquals("Properties '" + p + "' of node '" + n + "' have different values.", a.getString(), b.getString());
            } catch (RepositoryException e) {
                fail("Cannot access the content of the property value: " + e);
            }
        }
    }

    /**
     * Returns whether the value of the property may change in serialization.
     * For example, the last changed date of a node may change when the node is
     * re-imported. The values that may change are declared in the config file.
     *
     * @param propertyName The property name you want to check
     * @return True or false to indicate whether the value may or may not
     *         change.
     */
    public boolean propertyValueMayChange(String propertyName) {
        if (sc.propertyValueMayChange.indexOf(" " + propertyName + " ") < 0) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Returns the path to the source root.
     *
     * @return The path to the source root.
     */
    public String getSourceRootPath() {
        return sourceFolder + "/" + sc.rootNodeName;
    }

    /**
     * Returns the path to the target root.
     *
     * @return The path to the target root.
     */
    private String getTargetPath(String sourcePath) {
        String targetPath = sourcePath.replaceAll(sourceFolder, targetFolder);
        return targetPath;
    }

    /**
     * Displays the current source and target tree for debug/information
     * purpose
     */
    public void showTree() {
        Node n = null;
        try {
            n = (Node) session.getItem(sc.testroot);
            showTree(n, 0);
        } catch (RepositoryException e) {
            log.println("Cannot display tree diagnostics: " + e);
        }
    }

    /**
     * Recursive display of source and target tree
     */
    public void showTree(Node n, int level) throws RepositoryException {
        for (int t = 0; t < level; t++) {
            log.print("-");
        }
        log.print(n.getName() + " ");
        log.print(n.getPrimaryNodeType().getName() + " [ ");
        PropertyIterator pi = n.getProperties();
        while (pi.hasNext()) {
            Property p = (Property) pi.next();
            log.print(p.getName() + " ");
        }
        log.println("]");

        NodeIterator ni = n.getNodes();
        while (ni.hasNext()) {
            showTree((Node) ni.next(), level + 1);
        }
    }

    /**
     * Checks if a given property should be skipped during xml import.
     *  
     * @param propertyName
     * @return
     */
   public boolean propertySkipped(String propertyName) {
        if (sc.propertySkipped.indexOf(" " + propertyName + " ") < 0) {
            return false;
        } else {
            return true;
        }
    }
}
