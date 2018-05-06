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

import javax.jcr.Node;
import javax.jcr.Value;
import javax.jcr.RepositoryException;
import javax.jcr.PropertyIterator;
import javax.jcr.Property;

import org.apache.jackrabbit.test.AbstractJCRTest;
import org.apache.jackrabbit.test.NotExecutableException;

/**
 * <code>GetWeakReferencesTest</code> checks implementation of
 * {@link Node#getWeakReferences()} and {@link Node#getWeakReferences(String)}.
 */
public class GetWeakReferencesTest extends AbstractJCRTest {

    private Node target;

    private Node referring;

    protected void setUp() throws Exception {
        super.setUp();
        target = testRootNode.addNode(nodeName1, testNodeType);
        ensureMixinType(target, mixReferenceable);
        referring = testRootNode.addNode(nodeName2, testNodeType);
        superuser.save();
    }

    public void testSingleValue() throws RepositoryException {
        Value weakRef = vf.createValue(target, true);
        referring.setProperty(propertyName1, weakRef);
        superuser.save();

        PropertyIterator it = target.getWeakReferences();
        assertTrue("no weak references returned", it.hasNext());
        Property p = it.nextProperty();
        assertEquals("wrong weak reference property", referring.getProperty(propertyName1).getPath(), p.getPath());
        assertFalse("no more weak references expected", it.hasNext());
    }

    public void testSingleValueWithName() throws RepositoryException {
        Value weakRef = vf.createValue(target, true);
        referring.setProperty(propertyName1, weakRef);
        superuser.save();

        PropertyIterator it = target.getWeakReferences(propertyName1);
        assertTrue("no weak references returned", it.hasNext());
        Property p = it.nextProperty();
        assertEquals("wrong weak reference property", referring.getProperty(propertyName1).getPath(), p.getPath());
        assertFalse("no more weak references expected", it.hasNext());
    }

    public void testMultiValues() throws RepositoryException {
        Value weakRef = vf.createValue(target, true);
        Value[] refs = new Value[]{weakRef, weakRef};
        referring.setProperty(propertyName1, refs);
        superuser.save();

        PropertyIterator it = target.getWeakReferences();
        assertTrue("no weak references returned", it.hasNext());
        Property p = it.nextProperty();
        assertEquals("wrong weak reference property", referring.getProperty(propertyName1).getPath(), p.getPath());
        assertFalse("no more weak references expected", it.hasNext());
    }

    public void testMultiValuesWithName() throws RepositoryException {
        Value weakRef = vf.createValue(target, true);
        Value[] refs = new Value[]{weakRef, weakRef};
        referring.setProperty(propertyName1, refs);
        superuser.save();

        PropertyIterator it = target.getWeakReferences(propertyName1);
        assertTrue("no weak references returned", it.hasNext());
        Property p = it.nextProperty();
        assertEquals("wrong weak reference property", referring.getProperty(propertyName1).getPath(), p.getPath());
        assertFalse("no more weak references expected", it.hasNext());
    }

    public void testNonReferenceable() throws RepositoryException, NotExecutableException {
        Node nonReferenceable = null;
        if (testRootNode.isNodeType(mixReferenceable)) {
            Node child = testRootNode.addNode(nodeName1, testNodeType);
            superuser.save();
            if (!child.isNodeType(mixReferenceable)) {
                nonReferenceable = child;
            }
        } else {
            nonReferenceable = testRootNode;
        }

        if (nonReferenceable == null) {
            throw new NotExecutableException("Test node is referenceable.");
        }

        // getWeakReferences must return an empty iterator and must not throw.
        assertFalse(nonReferenceable.getWeakReferences().hasNext());
    }
}
