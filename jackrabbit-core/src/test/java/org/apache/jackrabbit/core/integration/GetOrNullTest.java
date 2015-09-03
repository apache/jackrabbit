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
package org.apache.jackrabbit.core.integration;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.jackrabbit.test.AbstractJCRTest;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

/**
 * @see <a href="https://issues.apache.org/jira/browse/JCR-3870">JCR-3870</a>
 */
public class GetOrNullTest extends AbstractJCRTest {

    public static String NAME_EXISTING_PROPERTY = "property1";
    public static String PATH_EXISTING_NODE = "/node1";
    public static String PATH_NON_EXISTING_NODE = "/non-existing-node";
    public static String PATH_EXISTING_PROPERTY = PATH_EXISTING_NODE + "/" + NAME_EXISTING_PROPERTY;
    public static String PATH_NON_EXISTING_PROPERTY = PATH_EXISTING_NODE + "/non-existing-property";

    public void setUp() throws Exception {
        super.setUp();

        Node node = JcrUtils.getOrCreateByPath(PATH_EXISTING_NODE, "nt:unstructured", superuser);
        node.setProperty(NAME_EXISTING_PROPERTY, "value");
        superuser.save();
    }

    public void testGetItemOrNullExistingNode() throws RepositoryException {
        JackrabbitSession js = (JackrabbitSession) superuser;
        Item item = js.getItemOrNull(PATH_EXISTING_NODE);
        assertNotNull(item);
        assertTrue(item instanceof Node);
        assertEquals(item.getPath(), PATH_EXISTING_NODE);
    }

    public void testGetItemOrNullNonExistingNode() throws RepositoryException {
        JackrabbitSession js = (JackrabbitSession) superuser;
        Item item = js.getItemOrNull(PATH_NON_EXISTING_NODE);
        assertNull(item);
    }

    public void testGetItemOrNullExistingProperty() throws RepositoryException {
        JackrabbitSession js = (JackrabbitSession) superuser;
        Item item = js.getItemOrNull(PATH_EXISTING_PROPERTY);
        assertNotNull(item);
        assertTrue(item instanceof Property);
        assertEquals(item.getPath(), PATH_EXISTING_PROPERTY);
    }

    public void testGetItemOrNullNonExistingProperty() throws RepositoryException {
        JackrabbitSession js = (JackrabbitSession) superuser;
        Item item = js.getItemOrNull(PATH_NON_EXISTING_PROPERTY);
        assertNull(item);
    }

    public void testGetNodeOrNullExisting() throws RepositoryException {
        JackrabbitSession js = (JackrabbitSession) superuser;
        Node node = js.getNodeOrNull(PATH_EXISTING_NODE);
        assertNotNull(node);
        assertEquals(node.getPath(), PATH_EXISTING_NODE);
    }

    public void testGetNodeOrNullNonExisting() throws RepositoryException {
        JackrabbitSession js = (JackrabbitSession) superuser;
        Node node = js.getNodeOrNull(PATH_NON_EXISTING_NODE);
        assertNull(node);
    }

    public void testGetPropertyOrNullExisting() throws RepositoryException {
        JackrabbitSession js = (JackrabbitSession) superuser;
        Property property = js.getPropertyOrNull(PATH_EXISTING_PROPERTY);
        assertNotNull(property);
        assertEquals(property.getPath(), PATH_EXISTING_PROPERTY);
    }

    public void testGetPropertyOrNullNonExisting() throws RepositoryException {
        JackrabbitSession js = (JackrabbitSession) superuser;
        Property property = js.getPropertyOrNull(PATH_NON_EXISTING_PROPERTY);
        assertNull(property);
    }

}
