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
import org.apache.jackrabbit.test.RepositoryStub;
import org.apache.jackrabbit.test.NotExecutableException;

import javax.jcr.RepositoryException;
import javax.jcr.Node;
import javax.jcr.Session;

/**
 * Encapsulates the various properties that are needed for a serialization test
 * case.
 */
class SerializationContext {

    private AbstractJCRTest baseTest;
    public String testroot;
    public String nodetype;
    public String sourceFolderName;
    public String targetFolderName;
    public String rootNodeName;
    public String nodeName1;
    public String nodeName2;
    public String nodeName3;
    public String testNodeType;
    public String propertyName1;
    public String jcrPrimaryType;
    public String mixReferenceable;

    public String propertyValueMayChange;
    public String propertySkipped;

    public String nodeTypesTestNode;
    public String mixinTypeTestNode;
    public String propertyTypesTestNode;
    public String sameNameChildrenTestNode;
    public String multiValuePropertiesTestNode;
    public String referenceableNodeTestNode;
    public String orderChildrenTestNode;
    public String namespaceTestNode;
    public String sameNameSibsFalseChildNodeDefinition;


    public String stringTestProperty;
    public String binaryTestProperty;
    public String dateTestProperty;
    public String longTestProperty;
    public String doubleTestProperty;
    public String booleanTestProperty;
    public String nameTestProperty;
    public String pathTestProperty;
    public String referenceTestProperty;
    public String multiValueTestProperty;

    public SerializationContext(AbstractJCRTest test, Session session)
            throws RepositoryException {
        // creates a serialization context based on a test class
        baseTest = test;

        testroot = get("testroot");
        nodetype = get("nodetype");
        sourceFolderName = get("sourceFolderName");
        targetFolderName = get("targetFolderName");
        rootNodeName = get("rootNodeName");
        nodeName1 = get(RepositoryStub.PROP_NODE_NAME1);
        nodeName2 = get(RepositoryStub.PROP_NODE_NAME2);
        nodeName3 = get(RepositoryStub.PROP_NODE_NAME3);
        testNodeType = get(RepositoryStub.PROP_NODETYPE);
        propertyName1 = get(RepositoryStub.PROP_PROP_NAME1);
        jcrPrimaryType = session.getNamespacePrefix(AbstractJCRTest.NS_JCR_URI) + ":primaryType";
        mixReferenceable = session.getNamespacePrefix(AbstractJCRTest.NS_MIX_URI) + ":referenceable";

        propertyValueMayChange = " " + get("propertyValueMayChange") + " ";
        propertySkipped = " " + get("propertySkipped") + " ";

        nodeTypesTestNode = get("nodeTypesTestNode");
        mixinTypeTestNode = get("mixinTypeTestNode");
        propertyTypesTestNode = get("propertyTypesTestNode");
        sameNameChildrenTestNode = get("sameNameChildrenTestNode");
        multiValuePropertiesTestNode = get("multiValuePropertiesTestNode");
        referenceableNodeTestNode = get("referenceableNodeTestNode");
        orderChildrenTestNode = get("orderChildrenTestNode");
        namespaceTestNode = get("namespaceTestNode");
        sameNameSibsFalseChildNodeDefinition = get("sameNameSibsFalseChildNodeDefinition");

        stringTestProperty = get("stringTestProperty");
        binaryTestProperty = get("binaryTestProperty");
        dateTestProperty = get("dateTestProperty");
        longTestProperty = get("longTestProperty");
        doubleTestProperty = get("doubleTestProperty");
        booleanTestProperty = get("booleanTestProperty");
        nameTestProperty = get("nameTestProperty");
        pathTestProperty = get("pathTestProperty");
        referenceTestProperty = get("referenceTestProperty");
        multiValueTestProperty = get("multiValueTestProperty");
    }

    private String get(String name) throws RepositoryException {
        String value = baseTest.getProperty(name);
        if (value == null) {
            throw new NullPointerException("Property '" + name + "' is not defined.");
        }
        return value;
    }

    public void log(String message) {
        baseTest.log.println(message);
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
}
