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

    public SerializationContext(AbstractJCRTest test) throws RepositoryException {
        // creates a serialization context based on a test class
        baseTest = test;

        testroot = get("testroot");
        nodetype = get("nodetype");
        sourceFolderName = get("sourceFolderName");
        targetFolderName = get("targetFolderName");
        rootNodeName = get("rootNodeName");

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

}
