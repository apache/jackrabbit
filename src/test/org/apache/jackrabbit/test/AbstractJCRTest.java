/*
 * Copyright 2004 The Apache Software Foundation.
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
import java.util.StringTokenizer;

/**
 * Abstract base class for all JCR test classes.
 */
public abstract class AbstractJCRTest extends JUnitTest {

    /**
     * Helper object to access repository transparently
     */
    public static final RepositoryHelper helper = new RepositoryHelper();

    protected static final String JCR_PRIMARY_TYPE = "jcr:primaryType";

    protected static final String NT_UNSTRUCTURED = "nt:unstructured";

    protected static final String MIX_REFERENCABLE = "mix:referencable";

    protected static final String NT_BASE = "nt:base";

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
     * The superuser session
     */
    protected Session superuser;

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

        superuser = helper.getSuperuserSession();
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

    protected void tearDown() throws Exception {
        if (superuser != null) {
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
            superuser.logout();
        }
    }

    protected String getProperty(String name) throws RepositoryException {
        String testCaseName = getName();
        String testClassName = getClass().getName();
        int idx;
        if ((idx = testClassName.lastIndexOf('.')) > -1) {
            testClassName = testClassName.substring(idx + 1);
        }

        // check test case specific property first
        String value = helper.getProperty(RepositoryStub.PROP_PREFIX + "."
                + testClassName + "." + testCaseName + "." + name);
        if (value != null) {
            return value;
        }
        return helper.getProperty(RepositoryStub.PROP_PREFIX + "." + name);
    }

}
