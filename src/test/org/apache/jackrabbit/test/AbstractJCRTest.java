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
package org.apache.jackrabbit.test;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.Repository;
import javax.jcr.NamespaceException;
import java.util.StringTokenizer;

/**
 * Abstract base class for all JCR test classes.
 */
public abstract class AbstractJCRTest extends JUnitTest {

    /**
     * Helper object to access repository transparently
     */
    public static RepositoryHelper helper = new RepositoryHelper();

    /**
     * Namespace URI for jcr prefix.
     */
    public static final String NS_JCR_URI = "http://www.jcp.org/jcr/1.0";

    /**
     * Namespace URI for nt prefix.
     */
    public static final String NS_NT_URI = "http://www.jcp.org/jcr/nt/1.0";

    /**
     * Namespace URI for mix prefix.
     */
    public static final String NS_MIX_URI = "http://www.jcp.org/jcr/mix/1.0";

    /**
     * Namespace URI for sv prefix
     */
    public static final String NS_SV_URI = "http://www.jcp.org/jcr/sv/1.0";

    /**
     * JCR Name jcr:primaryType using the namespace resolver of the current session.
     */
    protected String jcrPrimaryType;

    /**
     * JCR Name jcr:predecessors using the namespace resolver of the current session.
     */
    protected String jcrPredecessors;

    /**
     * JCR Name jcr:baseVersion using the namespace resolver of the current session.
     */
    protected String jcrBaseVersion;

    /**
     * JCR Name jcr:uuid using the namespace resolver of the current session.
     */
    protected String jcrUUID;

    /**
     * JCR Name jcr:lockOwner using the namespace resolver of the current session.
     */
    protected String jcrLockOwner;

    /**
     * JCR Name jcr:lockIsDeep using the namespace resolver of the current session.
     */
    protected String jcrlockIsDeep;

    /**
     * JCR Name nt:base using the namespace resolver of the current session.
     */
    protected String ntBase;

    /**
     * JCR Name mix:referenceable using the namespace resolver of the current session.
     */
    protected String mixReferenceable;

    /**
     * JCR Name mix:versionable using the namespace resolver of the current session.
     */
    protected String mixVersionable;

    /**
     * JCR Name mix:lockable using the namespace resolver of the current session.
     */
    protected String mixLockable;

    /**
     * JCR Name nt:query using the namespace resolver of the current session.
     */
    protected String ntQuery;

    /**
     * Relative path to the test root node.
     */
    protected String testPath;

    /**
     * Absolute path to the test root node.
     */
    protected String testRoot;

    /**
     * The node type name for newly created nodes.
     */
    protected String testNodeType;

    /**
     * Name of a node that will be created during a test case.
     */
    protected String nodeName1;

    /**
     * Name of a node that will be created during a test case.
     */
    protected String nodeName2;

    /**
     * Name of a node that will be created during a test case.
     */
    protected String nodeName3;

    /**
     * Name of a node that will be created during a test case.
     */
    protected String nodeName4;

    /**
     * Name of a property that will be used during a test case.
     */
    protected String propertyName1;

    /**
     * Name of a property that will be used during a test case.
     */
    protected String propertyName2;

    /**
     * Name of a workspace to use instead of the default workspace.
     */
    protected String workspaceName;

    /**
     * The superuser session for the default workspace
     */
    protected Session superuser;

    /**
     * Flag that indicates if the current test is a read-only test, that is
     * no content is written to the workspace by the test.
     */
    protected boolean isReadOnly = false;

    /**
     * The root <code>Node</code> for testing
     */
    protected Node testRootNode;

    protected void setUp() throws Exception {
        testRoot = getProperty(RepositoryStub.PROP_TESTROOT);
        if (testRoot == null) {
            fail("Property '" + RepositoryStub.PROP_TESTROOT + "' is not defined.");
        }

        // cut off '/' to build testPath
        testPath = testRoot.substring(1);
        testNodeType = getProperty(RepositoryStub.PROP_NODETYPE);
        // setup node names
        nodeName1 = getProperty(RepositoryStub.PROP_NODE_NAME1);
        if (nodeName1 == null) {
            fail("Property '" + RepositoryStub.PROP_NODE_NAME1 + "' is not defined.");
        }
        nodeName2 = getProperty(RepositoryStub.PROP_NODE_NAME2);
        if (nodeName2 == null) {
            fail("Property '" + RepositoryStub.PROP_NODE_NAME2 + "' is not defined.");
        }
        nodeName3 = getProperty(RepositoryStub.PROP_NODE_NAME3);
        if (nodeName3 == null) {
            fail("Property '" + RepositoryStub.PROP_NODE_NAME3 + "' is not defined.");
        }
        nodeName4 = getProperty(RepositoryStub.PROP_NODE_NAME4);
        if (nodeName4 == null) {
            fail("Property '" + RepositoryStub.PROP_NODE_NAME4 + "' is not defined.");
        }
        propertyName1 = getProperty(RepositoryStub.PROP_PROP_NAME1);
        if (propertyName1 == null) {
            fail("Property '" + RepositoryStub.PROP_PROP_NAME1 + "' is not defined.");
        }
        propertyName2 = getProperty(RepositoryStub.PROP_PROP_NAME2);
        if (propertyName2 == null) {
            fail("Property '" + RepositoryStub.PROP_PROP_NAME2 + "' is not defined.");
        }
        workspaceName = getProperty(RepositoryStub.PROP_WORKSPACE_NAME);
        if (workspaceName == null) {
            fail("Property '" + RepositoryStub.PROP_WORKSPACE_NAME + "' is not defined.");
        }

        superuser = helper.getSuperuserSession();

        // setup some common names
        jcrPrimaryType = superuser.getNamespacePrefix(NS_JCR_URI) + ":primaryType";
        jcrPredecessors = superuser.getNamespacePrefix(NS_JCR_URI) + ":predecessors";
        jcrBaseVersion = superuser.getNamespacePrefix(NS_JCR_URI) + ":baseVersion";
        jcrUUID = superuser.getNamespacePrefix(NS_JCR_URI) + ":uuid";
        jcrLockOwner = superuser.getNamespacePrefix(NS_JCR_URI) + ":lockOwner";
        jcrlockIsDeep = superuser.getNamespacePrefix(NS_JCR_URI) + ":lockIsDeep";
        ntBase = superuser.getNamespacePrefix(NS_NT_URI) + ":base";
        mixReferenceable = superuser.getNamespacePrefix(NS_MIX_URI) + ":referenceable";
        mixVersionable = superuser.getNamespacePrefix(NS_MIX_URI) + ":versionable";
        mixLockable = superuser.getNamespacePrefix(NS_MIX_URI) + ":lockable";
        ntQuery = superuser.getNamespacePrefix(NS_NT_URI) + ":query";

        // setup custom namespaces
        if (helper.getRepository().getDescriptor(Repository.LEVEL_2_SUPPORTED) != null) {
            NamespaceRegistry nsReg = superuser.getWorkspace().getNamespaceRegistry();
            String namespaces = getProperty(RepositoryStub.PROP_NAMESPACES);
            if (namespaces != null) {
                String[] prefixes = namespaces.split(" ");
                for (int i = 0; i < prefixes.length; i++) {
                    String uri = getProperty(RepositoryStub.PROP_NAMESPACES + "." + prefixes[i]);
                    if (uri != null) {
                        try {
                            nsReg.getPrefix(uri);
                        } catch (NamespaceException e) {
                            // not yet registered
                            nsReg.registerNamespace(prefixes[i], uri);
                        }
                    }
                }
            }
        }

        if (isReadOnly) {
            if (testPath.length() == 0) {
                // test root is the root node
                testRootNode = superuser.getRootNode();
            } else if (!superuser.getRootNode().hasNode(testPath)) {
                fail("Workspace does not contain test data at: " + testRoot);
            } else {
                testRootNode = superuser.getRootNode().getNode(testPath);
            }
        } else {
            Node root = superuser.getRootNode();
            if (root.hasNode(testPath)) {
                // clean test root
                testRootNode = root.getNode(testPath);
                for (NodeIterator children = testRootNode.getNodes(); children.hasNext();) {
                    children.nextNode().remove();
                }
            } else {
                // create nodes to testPath
                StringTokenizer names = new StringTokenizer(testPath, "/");
                Node currentNode = root;
                while (names.hasMoreTokens()) {
                    currentNode = currentNode.addNode(names.nextToken(), testNodeType);
                }
                testRootNode = currentNode;
            }
            root.save();
        }
    }

    protected void tearDown() throws Exception {
        if (superuser != null) {
            try {
                if (!isReadOnly) {
                    // do a 'rollback'
                    superuser.refresh(false);
                    Node root = superuser.getRootNode();
                    if (root.hasNode(testPath)) {
                        // clean test root
                        testRootNode = root.getNode(testPath);
                        for (NodeIterator children = testRootNode.getNodes(); children.hasNext();) {
                            children.nextNode().remove();
                        }
                        root.save();
                    }
                }
            } catch (Exception e) {
                log.println("Exception in tearDown: " + e.toString());
            } finally {
                superuser.logout();
            }
        }
    }

    protected String getProperty(String name) throws RepositoryException {
        String testCaseName = getName();
        String testClassName = getClass().getName();
        String testPackName = "";
        int idx;
        if ((idx = testClassName.lastIndexOf('.')) > -1) {
            testPackName = testClassName.substring(testClassName.lastIndexOf('.', idx - 1) + 1, idx);
            testClassName = testClassName.substring(idx + 1);
        }

        // 1) test case specific property first
        String value = helper.getProperty(RepositoryStub.PROP_PREFIX + "."
                + testClassName + "." + testCaseName + "." + name);
        if (value != null) {
            return value;
        }

        // 2) check test class property
        value = helper.getProperty(RepositoryStub.PROP_PREFIX + "."
                + testClassName + "." + name);
        if (value != null) {
            return value;
        }

        // 3) check package property
        value = helper.getProperty(RepositoryStub.PROP_PREFIX + "."
                + testPackName + "." + name);
        if (value != null) {
            return value;
        }

        // finally try global property
        return helper.getProperty(RepositoryStub.PROP_PREFIX + "." + name);
    }

}
