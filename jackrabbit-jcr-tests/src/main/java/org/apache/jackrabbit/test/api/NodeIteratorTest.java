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
import org.apache.jackrabbit.test.NotExecutableException;

import javax.jcr.RepositoryException;
import javax.jcr.NodeIterator;
import java.util.NoSuchElementException;

/**
 * Tests the {@link javax.jcr.NodeIterator} implementation.
 *
 * @test
 * @sources NodeIteratorTest.java
 * @executeClass org.apache.jackrabbit.test.api.NodeIteratorTest
 * @keywords level1
 */
public class NodeIteratorTest extends AbstractJCRTest {

    protected void setUp() throws Exception {
        isReadOnly = true;
        super.setUp();
    }

    /**
     * Tests if {@link javax.jcr.NodeIterator#getSize()} returns the correct
     * size.
     * @throws NotExecutableException if getSize() returns -1 (unavailable).
     */
    public void testGetSize() throws RepositoryException, NotExecutableException {
        NodeIterator iter = testRootNode.getNodes();
        long size = testRootNode.getNodes().getSize();
        if (size != -1) {
            long count = 0;
            while (iter.hasNext()) {
                iter.nextNode();
                count++;
            }
            assertEquals("NodeIterator.getSize does not return correct number.", size, count);
        } else {
            throw new NotExecutableException("NodeIterator.getSize() does not return size information.");
        }
    }

    /**
     * Tests if {@link javax.jcr.NodeIterator#getPosition()} return correct values.
     */
    public void testGetPos() throws RepositoryException {
        NodeIterator iter = testRootNode.getNodes();
        assertEquals("Initial call to getPos() must return zero", 0, iter.getPosition());
        int index = 0;
        while (iter.hasNext()) {
            iter.nextNode();
            assertEquals("Wrong position returned by getPos()", ++index, iter.getPosition());
        }
    }

    /**
     * Tests if a {@link java.util.NoSuchElementException} is thrown when {@link
     * javax.jcr.NodeIterator#nextNode()} is called and there are no more nodes
     * available.
     */
    public void testNoSuchElementException() throws RepositoryException {
        NodeIterator iter = testRootNode.getNodes();
        while (iter.hasNext()) {
            iter.nextNode();
        }
        try {
            iter.nextNode();
            fail("nextNode() must throw a NoSuchElementException when no nodes are available");
        } catch (NoSuchElementException e) {
            // success
        }
    }

    /**
     * Tests if {@link javax.jcr.NodeIterator#skip(long)} works correctly.
     */
    public void testSkip() throws RepositoryException {
        NodeIterator iter = testRootNode.getNodes();
        // find out if there is anything we can skip
        int count = 0;
        while (iter.hasNext()) {
            iter.nextNode();
            count++;
        }
        if (count > 0) {
            // re-aquire iterator
            iter = testRootNode.getNodes();
            iter.skip(count);
            try {
                iter.nextNode();
                fail("nextNode() must throw a NoSuchElementException when no nodes are available");
            } catch (NoSuchElementException e) {
                // success
            }

            // re-aquire iterator
            iter = testRootNode.getNodes();
            try {
                iter.skip(count + 1);
                fail("skip() must throw a NoSuchElementException if one tries to skip past the end of the iterator");
            } catch (NoSuchElementException e) {
                // success
            }
        }
    }
}