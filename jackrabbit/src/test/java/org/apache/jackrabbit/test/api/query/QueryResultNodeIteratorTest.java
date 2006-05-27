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
package org.apache.jackrabbit.test.api.query;

import org.apache.jackrabbit.test.NotExecutableException;

import javax.jcr.RepositoryException;
import javax.jcr.NodeIterator;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import java.util.NoSuchElementException;

/**
 * Tests methods on {@link javax.jcr.NodeIterator} returned by
 * {@link javax.jcr.query.QueryResult#getNodes()}.
 *
 * @test
 * @sources QueryResultNodeIteratorTest.java
 * @executeClass org.apache.jackrabbit.test.api.query.QueryResultNodeIteratorTest
 * @keywords level2
 */
public class QueryResultNodeIteratorTest extends AbstractQueryTest {

    /**
     * Sets up the fixture for test cases.
     */
    protected void setUp() throws Exception {
        super.setUp();
        testRootNode.addNode(nodeName1, testNodeType);
        testRootNode.addNode(nodeName2, testNodeType);
        testRootNode.addNode(nodeName3, testNodeType);
        testRootNode.save();
    }

    /**
     * Tests if {@link javax.jcr.NodeIterator#getSize()} returns the correct
     * size.
     *
     * @throws org.apache.jackrabbit.test.NotExecutableException
     *          if getSize() returns -1 (unavailable).
     */
    public void testGetSize() throws RepositoryException, NotExecutableException {
        NodeIterator it = execute(testPath + "//*", Query.XPATH).getNodes();
        long size = testRootNode.getNodes().getSize();
        if (size != -1) {
            long count = 0;
            while (it.hasNext()) {
                it.nextNode();
                count++;
            }
            assertEquals("NodeIterator.getSize does not return correct number.", size, count);
        } else {
            throw new NotExecutableException("NodeIterator.getSize() does not return size information.");
        }
    }

    /**
     * Tests the method <code>NodeIterator.getPosition()</code>.
     */
    public void testGetPosition() throws RepositoryException {
        QueryResult rs = execute(testPath + "//*", Query.XPATH);

        // getPosition initially returns 0
        NodeIterator it = rs.getNodes();
        assertEquals("Initial call to getPosition() must return 0.", 0, it.getPosition());

        // check getPosition while iterating
        int index = 0;
        while (it.hasNext()) {
            it.nextNode();
            assertEquals("Wrong position returned by getPosition()", ++index, it.getPosition());
        }
    }

    /**
     * Tests the method <code>NodeIterator.getPosition()</code> on an empty
     * <code>NodeIterator</code>.
     */
    public void testGetPositionEmptyIterator() throws RepositoryException {
        QueryResult rs = execute(testPath + "/" + nodeName4, Query.XPATH);

        NodeIterator it = rs.getNodes();
        assertFalse("NodeIterator must be empty.", it.hasNext());

        assertEquals("Empty NodeIterator must return 0 on getPosition()", 0, it.getPosition());
    }

    /**
     * Tests if a {@link java.util.NoSuchElementException} is thrown when {@link
     * javax.jcr.NodeIterator#nextNode()} is called and there are no more nodes
     * available.
     */
    public void testNoSuchElementException() throws RepositoryException {
        NodeIterator it = execute(testPath + "//*", Query.XPATH).getNodes();
        while (it.hasNext()) {
            it.nextNode();
        }
        try {
            it.nextNode();
            fail("nextNode() must throw a NoSuchElementException when no nodes are available");
        } catch (NoSuchElementException e) {
            // success
        }
    }

    /**
     * Tests if {@link javax.jcr.NodeIterator#skip(long)} works correctly.
     */
    public void testSkip() throws RepositoryException {
        QueryResult rs = execute(testPath + "//*", Query.XPATH);
        NodeIterator it = rs.getNodes();

        // find out if there is anything we can skip
        int count = 0;
        while (it.hasNext()) {
            it.nextNode();
            count++;
        }
        if (count > 1) {
            // re-aquire iterator
            it = rs.getNodes();
            // skip all but one
            it.skip(count - 1);
            // get last one
            it.nextNode();
            try {
                it.nextNode();
                fail("nextNode() must throw a NoSuchElementException when no nodes are available");
            } catch (NoSuchElementException e) {
                // success
            }

            // re-aquire iterator
            it = rs.getNodes();
            try {
                it.skip(count + 1);
                fail("skip() must throw a NoSuchElementException if one tries to skip past the end of the iterator");
            } catch (NoSuchElementException e) {
                // success
            }
        }
    }
}
