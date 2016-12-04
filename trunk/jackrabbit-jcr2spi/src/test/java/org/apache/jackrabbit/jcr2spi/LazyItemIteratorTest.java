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
package org.apache.jackrabbit.jcr2spi;

import javax.jcr.Node;
import javax.jcr.RangeIterator;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.test.AbstractJCRTest;
import org.apache.jackrabbit.test.NotExecutableException;

/**
 * <code>LazyItemIteratorTest</code> contains implementation specific test
 * cases, that check if the <code>LazyItemIterator</code> returns a better
 * estimate for the number of <code>Item</code>s to be available in the
 * iteration than -1.
 */
public class LazyItemIteratorTest extends AbstractJCRTest {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    public void testSizeGreaterMinusOne() throws RepositoryException {
        RangeIterator it = testRootNode.getProperties();
        // a node always has at least a single property
        assertTrue(it.getSize() > 0);

        if (testRootNode.hasNodes()) {
            it = testRootNode.getNodes();
            // a node always has at least a single property
            assertTrue(it.getSize() > 0);
        }
    }

    public void testSizeOfEmptyIteratorIsZero() throws RepositoryException {
        int i = 0;
        String nameHint = "noExisting";
        String name = nameHint;
        while (testRootNode.hasProperty(name)) {
            name = name + i;
            i++;
        }
        // retrieve PropertyIterator for a name that does not exist as Property
        RangeIterator it = testRootNode.getProperties(name);
        assertTrue(it.getSize() == 0);

        name = nameHint;
        while (testRootNode.hasNode(name)) {
            name = name + i;
            i++;
        }
        // retrieve NodeIterator for a name that does not exist as Node
        it = testRootNode.getNodes(name);
        assertTrue(it.getSize() == 0);
    }

    public void testSizeShrinksIfInvalidItemFound() throws NotExecutableException, RepositoryException {
        RangeIterator it;
        try {
            testRootNode.addNode(nodeName1, testNodeType);
            testRootNode.addNode(nodeName2, testNodeType);
            Node child = testRootNode.addNode(nodeName3, testNodeType);
            testRootNode.save();

            it = testRootNode.getNodes();
            // remove 1 child -> force the iterator to contain an entry that
            // cannot be resolved into a node.
            child.remove();

        } catch (RepositoryException e) {
            throw new NotExecutableException();
        }

        // now the original size is off by one and will be adjusted automatically
        long size = it.getSize();
        long zise = 0;
        while (it.hasNext()) {
            it.next();
            zise++;
        }
        // original size is bigger by 1 than the calculated size during the
        // iteration.
        assertTrue(size == zise+1);
        // retrieve size again and check if it has been been adjusted.
        assertTrue(it.getSize() == zise);
    }
}