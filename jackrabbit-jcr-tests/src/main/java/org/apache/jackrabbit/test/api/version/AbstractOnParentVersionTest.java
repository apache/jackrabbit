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
package org.apache.jackrabbit.test.api.version;

import javax.jcr.RepositoryException;
import javax.jcr.Property;
import javax.jcr.Node;
import javax.jcr.nodetype.PropertyDefinition;

import javax.jcr.version.OnParentVersionAction;

/**
 * <code>AbstractOnParentVersionTest</code>: the abstract base class for
 * all tests related to OnParentVersion issues.
 */
public abstract class AbstractOnParentVersionTest extends AbstractVersionTest {

    protected int OPVAction;

    protected Property p;
    protected String initialPropValue = "initialValue";
    protected String newPropValue = "anotherValue";   // New string value for the property

    protected String childNodeTypeName;

    protected void setUp() throws Exception {
        super.setUp();

        childNodeTypeName = getProperty("nodetype");

        // set the property
        p = versionableNode.setProperty(propertyName1, initialPropValue);

        // assert that property has the proper opv-behaviour
        PropertyDefinition pd = p.getDefinition();
        if (pd.getOnParentVersion() != OPVAction) {
            fail("JCR Property at '"+p.getPath()+"' does not have the required OnParentVersion "+OnParentVersionAction.nameFromValue(OPVAction)+" definition.");
        }
        testRootNode.save();
    }

    protected void tearDown() throws Exception {
        p = null;
        super.tearDown();
    }

    /**
     * Add a child node to the versionable node created in the setup with the
     * name and nodetype name defined in the corresponding configuration. After
     * creation of the child node, an assertion is made for the proper onParentVersion
     * behaviour.<p/>
     * NOTE: the child node is removed together with the versionable node after
     * each test.
     *
     * @param requiredOpvBehaviour
     * @return
     * @throws RepositoryException
     */
    protected Node addChildNode(int requiredOpvBehaviour) throws RepositoryException {
        if (childNodeTypeName == null) {
            fail("Undefined node type for the child node with OnParentVersion "+OnParentVersionAction.nameFromValue(requiredOpvBehaviour));
        }

        Node childNode = versionableNode.addNode(nodeName4, childNodeTypeName);
        if (childNode.getDefinition().getOnParentVersion() != requiredOpvBehaviour) {
            fail("The childnode "+childNode.getPath()+" does not provide the required OnParentVersion behaviour "+OnParentVersionAction.nameFromValue(requiredOpvBehaviour));
        }
        return childNode;
    }
}