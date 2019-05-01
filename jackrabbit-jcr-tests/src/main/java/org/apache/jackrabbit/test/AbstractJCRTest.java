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
package org.apache.jackrabbit.test;

import junit.framework.TestResult;

import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.Session;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.Repository;
import javax.jcr.NamespaceException;
import javax.jcr.RangeIterator;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.retention.RetentionManager;

import java.util.StringTokenizer;
import java.util.Random;
import java.util.List;
import java.util.Arrays;

/**
 * Abstract base class for all JCR test classes.
 */
public abstract class AbstractJCRTest extends JUnitTest {

    /**
     * Pool of helper objects to access repository transparently
     */
    private static final RepositoryHelperPool HELPER_POOL = RepositoryHelperPoolImpl.getInstance();

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
     * The repository helper for this test.
     */
    private RepositoryHelper helper;

    /**
     * JCR Name jcr:primaryType using the namespace resolver of the current session.
     */
    protected String jcrPrimaryType;

    /**
     * JCR Name jcr:mixinTypes using the namespace resolver of the current session.
     */
    protected String jcrMixinTypes;

    /**
     * JCR Name jcr:predecessors using the namespace resolver of the current session.
     */
    protected String jcrPredecessors;

    /**
     * JCR Name jcr:successors using the namespace resolver of the current session.
     */
    protected String jcrSuccessors;

    /**
     * JCR Name jcr:created using the namespace resolver of the current session.
     */
    protected String jcrCreated;

    /**
     * JCR Name jcr:created using the namespace resolver of the current session.
     */
    protected String jcrVersionHistory;

    /**
     * JCR Name jcr:copiedFrom using the namespace resolver of the current session.
     */
    protected String jcrCopiedFrom;

    /**
     * JCR Name jcr:frozenNode using the namespace resolver of the current session.
     */
    protected String jcrFrozenNode;

    /**
     * JCR Name jcr:frozenUuid using the namespace resolver of the current session.
     */
    protected String jcrFrozenUuid;

    /**
     * JCR Name jcr:rootVersion using the namespace resolver of the current session.
     */
    protected String jcrRootVersion;

    /**
     * JCR Name jcr:isCheckedOut using the namespace resolver of the current session.
     */
    protected String jcrIsCheckedOut;

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
     * JCR Name jcr:mergeFailed using the namespace resolver of the current session.
     */
    protected String jcrMergeFailed;

    /**
     * JCR Name jcr:system using the namespace resolver of the current session.
     */
    protected String jcrSystem;

    /**
     * JCR Name nt:base using the namespace resolver of the current session.
     */
    protected String ntBase;

    /**
     * JCR Name nt:unstructured using the namespace resolver of the current session.
     */
    protected String ntUnstructured;

    /**
     * JCR Name nt:version using the namespace resolver of the current session.
     */
    protected String ntVersion;

    /**
     * JCR Name nt:versionHistory using the namespace resolver of the current session.
     */
    protected String ntVersionHistory;

    /**
     * JCR Name nt:versionHistory using the namespace resolver of the current session.
     */
    protected String ntVersionLabels;

    /**
     * JCR Name nt:frozenNode using the namespace resolver of the current session.
     */
    protected String ntFrozenNode;

    /**
     * JCR Name mix:referenceable using the namespace resolver of the current session.
     */
    protected String mixReferenceable;

    /**
     * JCR Name mix:versionable using the namespace resolver of the current session.
     */
    protected String mixVersionable;

    /**
     * JCR Name mix:simpleVersionable using the namespace resolver of the current session.
     */
    protected String mixSimpleVersionable;

    /**
     * JCR Name mix:lockable using the namespace resolver of the current session.
     */
    protected String mixLockable;

    /**
     * JCR Name mix:title using the namespace resolver of the current session.
     */
    protected String mixTitle;

    /**
     * JCR Name mix:shareable using the namespace resolver of the current session.
     */
    protected String mixShareable;

    /**
     * JCR Name nt:query using the namespace resolver of the current session.
     */
    protected String ntQuery;

    /**
     * JCR Name nt:activity using the namespace resolver of the current session.
     */
    protected String ntActivity;

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
     * The node type name for the test root node.
     */
    protected String testNodeTypeTestRoot;

    /**
     * A node type that does not allow any child nodes, such as nt:base.
     */
    protected String testNodeTypeNoChildren;

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

    /**
     * The value factory for {@link #superuser}.
     */
    protected ValueFactory vf;

    protected void setUp() throws Exception {
        super.setUp();
        testRoot = getProperty(RepositoryStub.PROP_TESTROOT);
        if (testRoot == null) {
            fail("Property '" + RepositoryStub.PROP_TESTROOT + "' is not defined.");
        }

        // cut off '/' to build testPath
        testPath = testRoot.substring(1);
        testNodeType = getProperty(RepositoryStub.PROP_NODETYPE);
        testNodeTypeTestRoot = getProperty(RepositoryStub.PROP_NODETYPETESTROOT);
        if (testNodeTypeTestRoot == null) {
            testNodeTypeTestRoot = testNodeType; // backwards compatibility
        }
        testNodeTypeNoChildren = getProperty(RepositoryStub.PROP_NODETYPENOCHILDREN);
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

        superuser = getHelper().getSuperuserSession();

        // setup some common names
        jcrPrimaryType = superuser.getNamespacePrefix(NS_JCR_URI) + ":primaryType";
        jcrMixinTypes = superuser.getNamespacePrefix(NS_JCR_URI) + ":mixinTypes";
        jcrPredecessors = superuser.getNamespacePrefix(NS_JCR_URI) + ":predecessors";
        jcrSuccessors = superuser.getNamespacePrefix(NS_JCR_URI) + ":successors";
        jcrCreated = superuser.getNamespacePrefix(NS_JCR_URI) + ":created";
        jcrVersionHistory = superuser.getNamespacePrefix(NS_JCR_URI) + ":versionHistory";
        jcrCopiedFrom = superuser.getNamespacePrefix(NS_JCR_URI) + ":copiedFrom";
        jcrFrozenNode = superuser.getNamespacePrefix(NS_JCR_URI) + ":frozenNode";
        jcrFrozenUuid = superuser.getNamespacePrefix(NS_JCR_URI) + ":frozenUuid";
        jcrRootVersion = superuser.getNamespacePrefix(NS_JCR_URI) + ":rootVersion";
        jcrBaseVersion = superuser.getNamespacePrefix(NS_JCR_URI) + ":baseVersion";
        jcrIsCheckedOut = superuser.getNamespacePrefix(NS_JCR_URI) + ":isCheckedOut";
        jcrUUID = superuser.getNamespacePrefix(NS_JCR_URI) + ":uuid";
        jcrLockOwner = superuser.getNamespacePrefix(NS_JCR_URI) + ":lockOwner";
        jcrlockIsDeep = superuser.getNamespacePrefix(NS_JCR_URI) + ":lockIsDeep";
        jcrMergeFailed = superuser.getNamespacePrefix(NS_JCR_URI) + ":mergeFailed";
        jcrSystem = superuser.getNamespacePrefix(NS_JCR_URI) + ":system";
        ntBase = superuser.getNamespacePrefix(NS_NT_URI) + ":base";
        ntUnstructured = superuser.getNamespacePrefix(NS_NT_URI) + ":unstructured";
        ntVersion = superuser.getNamespacePrefix(NS_NT_URI) + ":version";
        ntVersionHistory = superuser.getNamespacePrefix(NS_NT_URI) + ":versionHistory";
        ntVersionLabels = superuser.getNamespacePrefix(NS_NT_URI) + ":versionLabels";
        ntFrozenNode = superuser.getNamespacePrefix(NS_NT_URI) + ":frozenNode";
        mixReferenceable = superuser.getNamespacePrefix(NS_MIX_URI) + ":referenceable";
        mixVersionable = superuser.getNamespacePrefix(NS_MIX_URI) + ":versionable";
        mixSimpleVersionable = superuser.getNamespacePrefix(NS_MIX_URI) + ":simpleVersionable";
        mixLockable = superuser.getNamespacePrefix(NS_MIX_URI) + ":lockable";
        mixShareable = superuser.getNamespacePrefix(NS_MIX_URI) + ":shareable";
        mixTitle = superuser.getNamespacePrefix(NS_MIX_URI) + ":title";
        ntQuery = superuser.getNamespacePrefix(NS_NT_URI) + ":query";
        ntActivity = superuser.getNamespacePrefix(NS_NT_URI) + ":activity";

        // setup custom namespaces
        if (isSupported(Repository.LEVEL_2_SUPPORTED)) {
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
            vf = superuser.getValueFactory();
        }

        if (isReadOnly) {
            if (testPath.length() == 0) {
                // test root is the root node
                testRootNode = superuser.getRootNode();
            } else if (!superuser.getRootNode().hasNode(testPath)) {
                cleanUp();
                fail("Workspace does not contain test data at: " + testRoot);
            } else {
                testRootNode = superuser.getRootNode().getNode(testPath);
            }
        } else if (isSupported(Repository.LEVEL_2_SUPPORTED)) {
            testRootNode = cleanUpTestRoot(superuser);
            // also clean second workspace
            Session s = getHelper().getSuperuserSession(workspaceName);
            try {
                cleanUpTestRoot(s);
            } finally {
                s.logout();
            }
        } else {
            cleanUp();
            fail("Test case requires level 2 support.");
        }
    }

    protected void cleanUp() throws Exception {
        if (superuser != null) {
            try {
                if (!isReadOnly && isSupported(Repository.LEVEL_2_SUPPORTED)) {
                    cleanUpTestRoot(superuser);
                }
            } catch (Exception e) {
                log.println("Exception in tearDown: " + e.toString());
            } finally {
                superuser.logout();
                superuser = null;
                vf = null;
            }
        }
        testRootNode = null;
    }

    protected void tearDown() throws Exception {
        cleanUp();
        super.tearDown();
    }

    /**
     * Runs the test cases of this test class and reports the results to
     * <code>testResult</code>. In contrast to the default implementation of
     * <code>TestCase.run()</code> this method will suppress tests errors with
     * a {@link NotExecutableException}. That is, test cases that throw this
     * exception will still result as successful.
     * @param testResult the test result.
     */
    public void run(TestResult testResult) {
        try {
            helper = HELPER_POOL.borrowHelper();
            try {
                super.run(new JCRTestResult(testResult, log));
            } finally {
                HELPER_POOL.returnHelper(helper);
                helper = null;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @return the repository helper instance that is associated with this test.
     */
    protected RepositoryHelper getHelper() {
        return helper;
    }

    /**
     * Returns the value of the configuration property with <code>propName</code>.
     * The sequence how configuration properties are read is the follwoing:
     * <ol>
     * <li><code>javax.jcr.tck.&lt;testClassName&gt;.&lt;testCaseName&gt;.&lt;propName&gt;</code></li>
     * <li><code>javax.jcr.tck.&lt;testClassName&gt;.&lt;propName&gt;</code></li>
     * <li><code>javax.jcr.tck.&lt;packageName&gt;.&lt;propName&gt;</code></li>
     * <li><code>javax.jcr.tck.&lt;propName&gt;</code></li>
     * </ol>
     * Where:
     * <ul>
     * <li><code>&lt;testClassName&gt;</code> is the name of the test class without package prefix.</li>
     * <li><code>&lt;testMethodName&gt;</code> is the name of the test method</li>
     * <li><code>&lt;packageName&gt;</code> is the name of the package of the test class.
     * Example: packageName for <code>org.apache.jackrabbit.test.api.BooleanPropertyTest</code>: <code>api</code></li>
     * </ul>
     * @param propName the propName of the configration property.
     * @return the value of the property or <code>null</code> if the property
     *  does not exist.
     * @throws RepositoryException if an error occurs while reading from
     *  the configuration.
     */
    public String getProperty(String propName) throws RepositoryException {
        String testCaseName = getName();
        String testClassName = getClass().getName();
        String testPackName = "";
        int idx;
        if ((idx = testClassName.lastIndexOf('.')) > -1) {
            testPackName = testClassName.substring(testClassName.lastIndexOf('.', idx - 1) + 1, idx);
            testClassName = testClassName.substring(idx + 1);
        }

        // 1) test case specific property first
        String value = getHelper().getProperty(RepositoryStub.PROP_PREFIX + "."
                + testClassName + "." + testCaseName + "." + propName);
        if (value != null) {
            return value;
        }

        // 2) check test class property
        value = getHelper().getProperty(RepositoryStub.PROP_PREFIX + "."
                + testClassName + "." + propName);
        if (value != null) {
            return value;
        }

        // 3) check package property
        value = getHelper().getProperty(RepositoryStub.PROP_PREFIX + "."
                + testPackName + "." + propName);
        if (value != null) {
            return value;
        }

        // finally try global property
        return getHelper().getProperty(RepositoryStub.PROP_PREFIX + "." + propName);
    }

    /**
     * Returns the value of the configuration property with specified
     * <code>name</code>. If the property does not exist <code>defaultValue</code> is
     * returned.
     * <p>
     * Configuration properties are defined in the file:
     * <code>repositoryStubImpl.properties</code>.
     *
     * @param name the name of the property to retrieve.
     * @param defaultValue the default value if the property does not exist.
     * @return the value of the property or <code>defaultValue</code> if non existent.
     * @throws RepositoryException if the configuration file cannot be found.
     */
    public String getProperty(String name, String defaultValue) throws RepositoryException {
        String val = getProperty(name);
        if (val == null) {
            val = defaultValue;
        }
        return val;
    }

    /**
     * Create a JCR value based on the configuration.
     *
     * @param s
     * @param valueProp Name of the config property that contains the property value.
     * @param typeProp Name of the config property that contains the property type.
     * If the config parameter is missing, {@link PropertyType#STRING} is used
     * to create the JCR value.
     * @param defaultValue Default value to be used if the config does not define
     * the value property.
     * @return JCR value to be used for a test.
     * @throws RepositoryException
     */
    public Value getJcrValue(Session s, String valueProp, String typeProp, String defaultValue) throws RepositoryException {
        ValueFactory vf = s.getValueFactory();
        String val = getProperty(valueProp, defaultValue);
        int type = PropertyType.valueFromName(getProperty(typeProp, PropertyType.TYPENAME_STRING));
        return vf.createValue(val, type);
    }

    /**
     * Returns the size of the <code>RangeIterator</code> <code>it</code>.
     * Note, that the <code>RangeIterator</code> might get consumed, because
     * {@link RangeIterator#getSize()} might return -1 (information unavailable).
     * @param it a <code>RangeIterator</code>.
     * @return the size of the iterator (number of elements).
     */
    protected long getSize(RangeIterator it) {
        long size = it.getSize();
        if (size != -1) {
            return size;
        }
        size = 0;
        while (it.hasNext()) {
            it.next();
            size++;
        }
        return size;
    }

    /**
     * Returns the local name for the given <code>jcrName</code>.
     * 
     * @param jcrName
     *            the name.
     * @return the local name part.
     */
    protected static String getLocalName(String jcrName) {
        int idx = jcrName.indexOf(':');
        if (idx != -1) {
            return jcrName.substring(idx + 1);
        } else {
            return jcrName;
        }
    }

    /**
     * Returns the prefix for the given <code>jcrName</code>.
     * 
     * @param jcrName
     *            the name.
     * @return the prefix part (empty string when not prefixed)
     */
    protected static String getPrefix(String jcrName) {
        int idx = jcrName.indexOf(':');
        if (idx != -1) {
            return jcrName.substring(0, idx);
        } else {
            return "";
        }
    }

    /**
     * Returns the expanded name for the given <code>jcrName</code>.
     * 
     * @param jcrName
     *            the name.
     * @return the expanded name representation
     * @throws RepositoryException
     * @throws NamespaceException
     */
    protected static String getQualifiedName(Session session, String jcrName) throws RepositoryException {
        String prefix = getPrefix(jcrName);
        String namespace = session.getNamespaceURI(prefix);
        String localname = getLocalName(jcrName);
        return (namespace.length() > 0 ? "{" + namespace + "}" : "{}") + localname;
    }

    /**
     * Returns the name of a workspace that is not accessible from
     * <code>session</code>.
     * @param session the session.
     * @return name of a non existing workspace.
     * @throws RepositoryException if an error occurs.
     */
    protected String getNonExistingWorkspaceName(Session session) throws RepositoryException {
        List<String> names = Arrays.asList(session.getWorkspace().getAccessibleWorkspaceNames());
        String nonExisting = null;
        while (nonExisting == null) {
            String name = createRandomString(10);
            if (!names.contains(name)) {
                nonExisting = name;
            }
        }
        return nonExisting;
    }

    /**
     * Creates a <code>String</code> with a random sequence of characters
     * using 'a' - 'z'.
     * @param numChars number of characters.
     * @return the generated String.
     */
    protected String createRandomString(int numChars) {
        Random rand = new Random(System.currentTimeMillis());
        StringBuffer tmp = new StringBuffer(numChars);
        for (int i = 0; i < numChars; i++) {
            char c = (char) (rand.nextInt(('z' + 1) - 'a') + 'a');
            tmp.append(c);
        }
        return tmp.toString();
    }

    /**
     * Returns <code>true</code> if this repository support a certain optional
     * feature; otherwise <code>false</code> is returned. If there is no
     * such <code>descriptorKey</code> present in the repository, this method
     * also returns false.
     *
     * @param descriptorKey the descriptor key.
     * @return <code>true</code> if the option is supported.
     * @throws RepositoryException if an error occurs.
     */
    protected boolean isSupported(String descriptorKey) throws RepositoryException {
        return "true".equals(getHelper().getRepository().getDescriptor(descriptorKey));
    }

    /**
     * Throws a <code>NotExecutableException</code> if the repository does
     * not support the feature identified by the given <code>discriptorKey</code>.
     *
     * @param descriptorKey the descriptor key.
     * @throws RepositoryException if an error occurs.
     * @throws NotExecutableException If the feature is not supported.
     */
    protected void checkSupportedOption(String descriptorKey) throws RepositoryException, NotExecutableException {
        String value = getHelper().getRepository().getDescriptor(descriptorKey);
        if (value == null || ! Boolean.valueOf(value).booleanValue()) {
            throw new NotExecutableException (
                    "Repository feature not supported: " + descriptorKey);
        }
    }

    /**
     * Checks that the repository supports multiple workspace, otherwise aborts with
     * {@link NotExecutableException}.
     * @throws NotExecutableException when the repository only supports a single
     * workspace
     */
    protected void ensureMultipleWorkspacesSupported() throws RepositoryException, NotExecutableException {
        String workspacenames[] = superuser.getWorkspace().getAccessibleWorkspaceNames();
        if (workspacenames == null || workspacenames.length < 2) {
            throw new NotExecutableException("This repository does not seem to support multiple workspaces.");
        }
    }

    /**
     * Checks that the repository supports locking, otherwise aborts with
     * {@link NotExecutableException}.
     * @throws NotExecutableException when the repository does not support locking
     */
    protected void ensureLockingSupported() throws RepositoryException, NotExecutableException {
        if (!isSupported(Repository.OPTION_LOCKING_SUPPORTED)) {
            throw new NotExecutableException("This repository does not support locking.");
        }
    }

    private boolean canSetProperty(NodeType nodeType, String propertyName, int propertyType, boolean isMultiple) {
        PropertyDefinition propDefs[] = nodeType.getPropertyDefinitions();

        for (int i = 0; i < propDefs.length; i++) {
            if (propDefs[i].getName().equals(propertyName) || propDefs[i].getName().equals("*")) {
                if ((propDefs[i].getRequiredType() == propertyType || propDefs[i].getRequiredType() == PropertyType.UNDEFINED)
                    && propDefs[i].isMultiple() == isMultiple) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean canSetProperty(Node node, String propertyName, int propertyType, boolean isMultiple) throws RepositoryException {

        if (canSetProperty(node.getPrimaryNodeType(), propertyName, propertyType, isMultiple)) {
            return true;
        }
        else {
            NodeType mixins[] = node.getMixinNodeTypes();
            boolean canSetIt = false;
            for (int i = 0; i < mixins.length && !canSetIt; i++) {
                canSetIt |= canSetProperty(mixins[i], propertyName, propertyType, isMultiple);
            }
            return canSetIt;
        }
    }

    /**
     * Checks that the repository can set the property to the required type, otherwise aborts with
     * {@link NotExecutableException}.
     * @throws NotExecutableException when setting the property to the required
     * type is not supported
     */
    protected void ensureCanSetProperty(Node node, String propertyName, int propertyType, boolean isMultiple) throws NotExecutableException, RepositoryException {

        if (! canSetProperty(node, propertyName, propertyType, isMultiple)) {
            throw new NotExecutableException("configured property name " + propertyName + " can not be set on node " + node.getPath());
        }
    }

    /**
     * Checks that the repository can set the property to the required type, otherwise aborts with
     * {@link NotExecutableException}.
     * @throws NotExecutableException when setting the property to the required
     * type is not supported
     */
    protected void ensureCanSetProperty(Node node, String propertyName, Value value) throws NotExecutableException, RepositoryException {
        ensureCanSetProperty(node, propertyName, value.getType(), false);
    }

    /**
     * Checks that the repository can set the property to the required type, otherwise aborts with
     * {@link NotExecutableException}.
     * @throws NotExecutableException when setting the property to the required
     * type is not supported
     */
    protected void ensureCanSetProperty(Node node, String propertyName, Value[] values) throws NotExecutableException, RepositoryException {

        int propertyType = values.length == 0 ? PropertyType.UNDEFINED : values[0].getType();

        if (! canSetProperty(node, propertyName, propertyType, true)) {
            throw new NotExecutableException("configured property name " + propertyName + " can not be set on node " + node.getPath());
        }
    }

    /**
     * Checks that the repository supports the specified node type, otherwise aborts with
     * {@link NotExecutableException}
     * @throws NotExecutableException when the specified node type is unknown
     */
    protected void ensureKnowsNodeType(Session session, String nodetype) throws NotExecutableException, RepositoryException {
        
        try {
            session.getWorkspace().getNodeTypeManager().getNodeType(nodetype);
        }
        catch (NoSuchNodeTypeException ex) {
            throw new NotExecutableException("Repository does not support node type " + nodetype);
        }
    }

    /**
     * Ensures that the given <code>node</code> is of the given mixin type.
     *
     * @param node  a node.
     * @param mixin the name of a mixin type.
     * @throws NotExecutableException if the node is not of type mixin and the
     *                                mixin cannot be added.
     * @throws RepositoryException    if an error occurs.
     */
    protected void ensureMixinType(Node node, String mixin)
            throws NotExecutableException, RepositoryException {
        if (!node.isNodeType(mixin)) {
            if (node.canAddMixin(mixin)) {
                node.addMixin(mixin);
            } else {
                throw new NotExecutableException(node.getPath() +
                        " does not support adding " + mixin);
            }
        }
    }

    /**
     * Checks whether the node already has the specified mixin node type
     */
    protected boolean needsMixin(Node node, String mixin) throws RepositoryException {
        return ! node.getSession().getWorkspace().getNodeTypeManager().getNodeType(node.getPrimaryNodeType().getName()).isNodeType(mixin);
    }

    /**
     * Reverts any pending changes made by <code>s</code> and deletes any nodes
     * under {@link #testRoot}. If there is no node at {@link #testRoot} then
     * the necessary nodes are created.
     *
     * @param s the session to clean up.
     * @return the {@link javax.jcr.Node} that represents the test root.
     * @throws RepositoryException if an error occurs.
     */
    protected Node cleanUpTestRoot(Session s) throws RepositoryException {
        // do a 'rollback'
        s.refresh(false);
        Node root = s.getRootNode();
        Node testRootNode;

        if (root.hasNode(testPath)) {
            RetentionManager rm;
            try {
                rm = s.getRetentionManager();
            } catch (UnsupportedRepositoryOperationException e) {
                rm = null;
            }

            // clean test root
            testRootNode = root.getNode(testPath);
            NodeIterator children = testRootNode.getNodes();
            while (children.hasNext()) {
                Node child = children.nextNode();

                // Remove retention policy if needed
                String childPath = child.getPath();
                if (rm != null && rm.getRetentionPolicy(childPath) != null) {
                    rm.removeRetentionPolicy(childPath);
                    s.save();
                }

                NodeDefinition nodeDef = child.getDefinition();
                if (!nodeDef.isMandatory() && !nodeDef.isProtected()) {
                    // try to remove child
                    try {
                        child.remove();
                    } catch (ConstraintViolationException e) {
                        log.println("unable to remove node: " + child.getPath());
                    }
                }
            }
        } else {
            // create nodes to testPath
            StringTokenizer names = new StringTokenizer(testPath, "/");
            Node currentNode = root;
            while (names.hasMoreTokens()) {
                String name = names.nextToken();
                if (currentNode.hasNode(name)) {
                    currentNode = currentNode.getNode(name);
                } else {
                    currentNode = currentNode.addNode(name, testNodeTypeTestRoot);
                }
            }
            testRootNode = currentNode;
        }
        s.save();
        return testRootNode;
    }
}
