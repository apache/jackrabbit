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

import javax.jcr.Item;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFactory;
import javax.jcr.Value;
import javax.jcr.PropertyType;
import javax.jcr.Property;
import javax.jcr.Workspace;

/**
 * <code>PathTest</code>...
 */
public class PathTest extends AbstractJCRTest {

    private String identifier;

    protected void setUp() throws Exception {
        super.setUp();

        identifier = testRootNode.getIdentifier();
    }

    public void testGetItem() throws RepositoryException {
        Item item = superuser.getItem("[" + identifier + "]");
        assertTrue(item.isSame(testRootNode));
    }
    
    public void testCreatePathValue() throws RepositoryException {
        ValueFactory vf = superuser.getValueFactory();
        Value pathValue = vf.createValue("[" +identifier+ "]", PropertyType.PATH);

        assertEquals(PropertyType.PATH, pathValue.getType());
        assertEquals("[" +identifier+ "]", pathValue.getString());
        assertFalse(pathValue.equals(vf.createValue(testRootNode.getPath(), PropertyType.PATH)));
    }

    public void testCreateMultiplePathValue() throws RepositoryException {
        ValueFactory vf = superuser.getValueFactory();
        Value vID1 = vf.createValue("[" +identifier+ "]", PropertyType.PATH);
        Value v = vf.createValue(testRootNode.getPath(), PropertyType.PATH);
        Value vID2 = vf.createValue("[" +identifier+ "]", PropertyType.PATH);
        Value v2 = vf.createValue(testRootNode.getPath(), PropertyType.PATH);

        assertEquals(vID1, vID2);
        assertEquals(v, v2);

        assertFalse(v.equals(vID1));
        assertFalse(v.equals(vID2));
    }
    
    public void testIdentifierBasedPropertyValue() throws RepositoryException {
        ValueFactory vf = superuser.getValueFactory();
        Value pathValue = vf.createValue("[" +identifier+ "]", PropertyType.PATH);

        Property p = testRootNode.setProperty(propertyName1, pathValue);

        assertEquals(PropertyType.PATH, p.getType());
        assertEquals(pathValue.getString(), p.getValue().getString());
        assertEquals(pathValue, p.getValue());
    }

    public void testResolvedIdentifierBasedPropertyValue() throws RepositoryException {
        ValueFactory vf = superuser.getValueFactory();
        Value pathValue = vf.createValue("[" +identifier+ "]", PropertyType.PATH);

        Property p = testRootNode.setProperty(propertyName1, pathValue);
        assertTrue("Identifier-based PATH value must resolve to the Node the identifier has been obtained from.",
                testRootNode.isSame(p.getNode()));
    }

    public void testExtendedNameBasedPathValue() throws RepositoryException {
        ValueFactory vf = superuser.getValueFactory();
        Value pathValue = vf.createValue(Workspace.PATH_VERSION_STORAGE_NODE, PropertyType.PATH);

        Property p = testRootNode.setProperty(propertyName1, pathValue);
        assertEquals("/jcr:system/jcr:versionStorage", p.getString());

        String path = Workspace.PATH_VERSION_STORAGE_NODE + "/a/b/c/jcr:frozenNode";
        pathValue = vf.createValue(path, PropertyType.PATH);

        p = testRootNode.setProperty(propertyName1, pathValue);
        assertEquals("/jcr:system/jcr:versionStorage/a/b/c/jcr:frozenNode", p.getString());
    }

    public void testNotNormalizedPathValue() throws RepositoryException {
        ValueFactory vf = superuser.getValueFactory();
        Value pathValue = vf.createValue("/a/../b/./c/dd/..", PropertyType.PATH);

        Property p = testRootNode.setProperty(propertyName1, pathValue);

        assertEquals(PropertyType.PATH, p.getType());
        assertEquals(pathValue.getString(), p.getValue().getString());
        assertEquals(pathValue, p.getValue());                
    }
}