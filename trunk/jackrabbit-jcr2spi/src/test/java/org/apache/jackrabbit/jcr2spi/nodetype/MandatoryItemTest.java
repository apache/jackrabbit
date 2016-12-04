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
package org.apache.jackrabbit.jcr2spi.nodetype;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;

import org.apache.jackrabbit.test.AbstractJCRTest;
import org.apache.jackrabbit.test.NotExecutableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** <code>MandatoryItemTest</code>... */
public class MandatoryItemTest extends AbstractJCRTest {

    private static Logger log = LoggerFactory.getLogger(MandatoryItemTest.class);

    private NodeDefinition childNodeDef;
    private PropertyDefinition childPropDef;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        NodeType nt = superuser.getWorkspace().getNodeTypeManager().getNodeType(testNodeType);
        NodeDefinition[] ndefs = nt.getChildNodeDefinitions();
        for (int i = 0; i < ndefs.length; i++) {
            if (ndefs[i].isMandatory() && !ndefs[i].isProtected() && !ndefs[i].isAutoCreated()) {
                childNodeDef = ndefs[i];
                break;
            }
        }
        PropertyDefinition[] pdefs = nt.getPropertyDefinitions();
        for (int i = 0; i < pdefs.length; i++) {
            if (pdefs[i].isMandatory() && !pdefs[i].isProtected() && !pdefs[i].isAutoCreated()) {
                childPropDef = pdefs[i];
                break;
            }
        }
        if (childPropDef == null && childNodeDef == null) {
            cleanUp();
            throw new NotExecutableException();
        }
    }

    public void testCreation() throws NotExecutableException, RepositoryException {
        Node n;
        try {
            n = testRootNode.addNode(nodeName1, testNodeType);
        } catch (RepositoryException e) {
            throw new NotExecutableException();
        }

        try {
            testRootNode.save();
            fail("Saving without having added the mandatory child items must fail.");
        } catch (ConstraintViolationException e) {
            // success
        }

        if (childNodeDef != null) {
            n.addNode(childNodeDef.getName(), childNodeDef.getDefaultPrimaryType().getName());
        }
        if (childPropDef != null) {
            // TODO: check if definition defines default values
            n.setProperty(childPropDef.getName(), "any value");
        }
        // now save must succeed.
        testRootNode.save();
    }

    public void testRemoval() throws NotExecutableException, RepositoryException {
        Node n;
        Node childN = null;
        Property childP = null;
        try {
            n = testRootNode.addNode(nodeName1, testNodeType);
            if (childNodeDef != null) {
                childN = n.addNode(childNodeDef.getName(), childNodeDef.getDefaultPrimaryType().getName());
            }
            if (childPropDef != null) {
                // TODO: check if definition defines default values
                childP = n.setProperty(childPropDef.getName(), "any value");
            }
            testRootNode.save();
        } catch (RepositoryException e) {
            throw new NotExecutableException();
        }

        // remove the mandatory items ((must succeed))
        if (childN != null) {
            childN.remove();
        }
        if (childP != null) {
            childP.remove();
        }
        // ... however, saving must not be allowed.
        try {
            testRootNode.save();
            fail("removing mandatory child items without re-adding them must fail.");
        } catch (ConstraintViolationException e) {
            // success.
        }

        // re-add the mandatory items
        if (childNodeDef != null) {
            childN = n.addNode(childNodeDef.getName(), childNodeDef.getDefaultPrimaryType().getName());
        }
        if (childPropDef != null) {
            // TODO: check if definition defines default values
            childP = n.setProperty(childPropDef.getName(), "any value");
        }
        // save must succeed now.
        testRootNode.save();
    }
}