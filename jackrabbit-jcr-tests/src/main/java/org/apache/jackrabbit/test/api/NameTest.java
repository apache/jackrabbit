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

import java.util.UUID;

import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.Workspace;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;

import org.apache.jackrabbit.test.AbstractJCRTest;
import org.apache.jackrabbit.test.RepositoryStub;

/**
 * <code>NameTest</code>...
 */
public class NameTest extends AbstractJCRTest {

    private String getExpandedName(String jcrName) throws RepositoryException {
        if (jcrName.startsWith("{")) {
            return jcrName;
        } else {
            int pos = jcrName.indexOf(":");
            String prefix = (pos > -1) ? jcrName.substring(0, pos) : "";
            String uri = superuser.getNamespaceURI(prefix);
            return  "{" + uri + "}" + jcrName.substring(pos+1);
        }
    }

    /**
     * Expanded names must always be resolved.
     * Test NAME-value creation.
     * 
     * @throws RepositoryException
     */
    public void testExpandedNameValue() throws RepositoryException {
        ValueFactory vf = superuser.getValueFactory();
        Value nameValue = vf.createValue(Workspace.NAME_VERSION_STORAGE_NODE, PropertyType.NAME);

        assertEquals(PropertyType.NAME, nameValue.getType());
        assertEquals(nameValue.getString(), vf.createValue("jcr:versionStorage", PropertyType.NAME).getString());
        assertEquals(nameValue, vf.createValue("jcr:versionStorage", PropertyType.NAME));
        assertEquals("jcr:versionStorage", nameValue.getString());
    }
    
    /**
     * Expanded names must always be resolved.
     * Test setting a NAME-value property.
     *
     * @throws RepositoryException
     */
    public void testExpandedNameValueProperty() throws RepositoryException {
        ValueFactory vf = superuser.getValueFactory();
        Value nameValue = vf.createValue(Workspace.NAME_VERSION_STORAGE_NODE, PropertyType.NAME);

        Property p = testRootNode.setProperty(propertyName1, nameValue);
        assertEquals(PropertyType.NAME, p.getType());
        assertEquals(nameValue.getString(), p.getValue().getString());
        assertEquals(nameValue, p.getValue());
        assertEquals("jcr:versionStorage", p.getString());
    }

    /**
     * Test if the name of property created with an expanded name is properly
     * return as standard JCR name.
     * 
     * @throws RepositoryException
     */
    public void testExpandedNameItem() throws RepositoryException {
        String propName = getExpandedName(propertyName1);
        Property p = testRootNode.setProperty(propName, getJcrValue(superuser, RepositoryStub.PROP_PROP_VALUE1, RepositoryStub.PROP_PROP_TYPE1, "test"));

        assertEquals(propertyName1, p.getName());
    }

    /**
     * Test whether a node can be created with an expanded name (using a previously unused namespace name).
     * 
     * @throws RepositoryException
     */
    public void testExpandedNameNodeUnmappedNamespace() throws RepositoryException {
        String ns = "urn:uuid:" + UUID.randomUUID().toString();
        String expandedName = "{" + ns + "}test";
        try {
            Node createdNode = testRootNode.addNode(expandedName);
            testRootNode.getSession().save();
            String qualifiedName = createdNode.getName();
            assertEquals(expandedName, getExpandedName(qualifiedName));
        } catch (ItemExistsException | PathNotFoundException | ConstraintViolationException | VersionException | LockException ex) {
            // those are not acceptable here as per API spec
            fail("unexpected exception: " + ex);
        } catch (RepositoryException ex) {
            // acceptable; but a NamespaceException would really be more correct
        }
    }

    /**
     * Test whether a node can be created with something looking like an expanded name which is not
     * 
     * @throws RepositoryException
     */
    public void testReallyNotAndExpandedName() throws RepositoryException {
        String notANamespace = UUID.randomUUID().toString();
        String name = "{" + notANamespace + "}test";
        Node createdNode = testRootNode.addNode(name);
        testRootNode.getSession().save();
        assertEquals(name, createdNode.getName());
    }

    /**
     * Test if creating a node with an expanded node type name returns the proper
     * standard JCR node type name, and that it works for {@link Node#setPrimaryType(String)}.
     * 
     * @throws RepositoryException
     */
    public void testExpandedNodeTypeName() throws RepositoryException {
        String nodeName = getExpandedName(nodeName1);
        String ntName = getExpandedName(testNodeType);
        Node n = testRootNode.addNode(nodeName, ntName);

        assertEquals(nodeName1, n.getName());
        assertEquals(testNodeType, n.getPrimaryNodeType().getName());

        n.setPrimaryType(ntName);
        assertEquals(testNodeType, n.getPrimaryNodeType().getName());
    }
}