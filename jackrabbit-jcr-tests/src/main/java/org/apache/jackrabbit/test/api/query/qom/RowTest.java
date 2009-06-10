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
package org.apache.jackrabbit.test.api.query.qom;

import javax.jcr.RepositoryException;
import javax.jcr.Node;
import javax.jcr.Value;
import javax.jcr.query.RowIterator;
import javax.jcr.query.Row;
import javax.jcr.query.qom.QueryObjectModel;
import javax.jcr.query.qom.Column;

/**
 * <code>RowIteratorTest</code> contains test cases for {@link Row}.
 */
public class RowTest extends AbstractQOMTest {

    private static final String TEST_VALUE = "value";

    private static final String SELECTOR_NAME = "s";

    protected void setUp() throws Exception {
        super.setUp();
        Node n = testRootNode.addNode(nodeName1, testNodeType);
        n.setProperty(propertyName1, TEST_VALUE);
        superuser.save();
    }

    public void testGetValues() throws RepositoryException {
        Row r = getRow();
        Value[] values = r.getValues();
        assertEquals("wrong number of columns", 1, values.length);
        assertEquals("property value does not match", TEST_VALUE, values[0].getString());
    }

    public void testGetValue() throws RepositoryException {
        Row r = getRow();
        assertEquals("property value does not match", TEST_VALUE, r.getValue(propertyName1).getString());
    }

    public void testGetNode() throws RepositoryException {
        Row r = getRow();
        String expectedPath = testRootNode.getNode(nodeName1).getPath();
        assertEquals("unexpected result node", expectedPath, r.getNode().getPath());
    }

    public void testGetNodeWithSelector() throws RepositoryException {
        Row r = getRow();
        String expectedPath = testRootNode.getNode(nodeName1).getPath();
        assertEquals("unexpected result node", expectedPath, r.getNode(SELECTOR_NAME).getPath());
    }

    public void testGetPath() throws RepositoryException {
        Row r = getRow();
        String expectedPath = testRootNode.getNode(nodeName1).getPath();
        assertEquals("unexpected result node", expectedPath, r.getPath());
    }

    public void testGetPathWithSelector() throws RepositoryException {
        Row r = getRow();
        String expectedPath = testRootNode.getNode(nodeName1).getPath();
        assertEquals("unexpected result node", expectedPath, r.getPath(SELECTOR_NAME));
    }

    public void testGetScore() throws RepositoryException {
        Row r = getRow();
        // value is implementation dependent, simply call method...
        r.getScore();
    }

    public void testGetScoreWithSelector() throws RepositoryException {
        Row r = getRow();
        // value is implementation dependent, simply call method...
        r.getScore(SELECTOR_NAME);
    }

    private Row getRow() throws RepositoryException {
        QueryObjectModel qom = qf.createQuery(
                qf.selector(testNodeType, SELECTOR_NAME),
                qf.descendantNode(SELECTOR_NAME, testRoot),
                null,
                new Column[]{qf.column(SELECTOR_NAME, propertyName1, propertyName1)});
        RowIterator rows = qom.execute().getRows();
        assertTrue("empty result", rows.hasNext());
        Row r = rows.nextRow();
        assertFalse("result must not contain more than one row", rows.hasNext());
        return r;
    }
}
