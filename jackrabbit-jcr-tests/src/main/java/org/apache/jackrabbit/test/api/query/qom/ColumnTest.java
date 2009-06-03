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

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import javax.jcr.RepositoryException;
import javax.jcr.Node;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.query.qom.Column;
import javax.jcr.query.qom.QueryObjectModel;
import javax.jcr.query.qom.QueryObjectModelConstants;
import javax.jcr.query.QueryResult;
import javax.jcr.query.RowIterator;
import javax.jcr.query.Row;

/**
 * <code>ColumnTest</code> contains test cases related to QOM column.
 */
public class ColumnTest extends AbstractQOMTest {

    private static final String SELECTOR_1 = "s";

    private static final String SELECTOR_2 = "p";

    private static final String TEST_VALUE = "value";

    /**
     * From the spec:
     * <p/>
     * If propertyName is not specified, a column is included for each
     * single-valued non-residual property of the node type specified by the
     * nodeType attribute of the selector selectorName.
     * <p/>
     * [..] If propertyName is not specified,
     * columnName must not be specified, and the included columns will be
     * named "selectorName.propertyName".
     */
    public void testExpandColumnsForNodeType() throws RepositoryException {
        QueryObjectModel qom = qomFactory.createQuery(
                qomFactory.selector(testNodeType, SELECTOR_1),
                null,
                null,
                new Column[]{qomFactory.column(SELECTOR_1, null, null)});
        QueryResult result = qom.execute();
        List names = new ArrayList(Arrays.asList(result.getColumnNames()));
        NodeTypeManager ntMgr = superuser.getWorkspace().getNodeTypeManager();
        NodeType nt = ntMgr.getNodeType(testNodeType);
        PropertyDefinition[] propDefs = nt.getPropertyDefinitions();
        for (int i = 0; i < propDefs.length; i++) {
            PropertyDefinition propDef = propDefs[i];
            if (!propDef.isMultiple() && !propDef.getName().equals("*")) {
                String columnName = SELECTOR_1 + "." + propDef.getName();
                assertTrue("Missing column: " + columnName,
                        names.remove(columnName));
            }
        }
        for (Iterator it = names.iterator(); it.hasNext(); ) {
            fail(it.next() + " is not a property on node type " + testNodeType);
        }
    }

    /**
     * From the spec:
     * <p/>
     * If propertyName is specified, columnName is required and used to name
     * the column in the tabular results.
     */
    public void testColumnNames() throws RepositoryException {
        QueryObjectModel qom = qomFactory.createQuery(
                qomFactory.selector(testNodeType, SELECTOR_1),
                null,
                null,
                new Column[]{qomFactory.column(SELECTOR_1, propertyName1, propertyName1)});
        QueryResult result = qom.execute();
        List names = new ArrayList(Arrays.asList(result.getColumnNames()));
        assertTrue("Missing column: " + propertyName1, names.remove(propertyName1));
        for (Iterator it = names.iterator(); it.hasNext(); ) {
            fail(it.next() + " was not declared as a column");
        }
    }

    public void testMultiColumn() throws RepositoryException {
        Node n = testRootNode.addNode(nodeName1, testNodeType);
        n.setProperty(propertyName1, TEST_VALUE);
        superuser.save();

        String columnName1 = SELECTOR_1 + "." + propertyName1;
        String columnName2 = SELECTOR_2 + "." + propertyName1;
        QueryObjectModel qom = qomFactory.createQuery(
                qomFactory.join(
                        qomFactory.selector(testNodeType, SELECTOR_1),
                        qomFactory.selector(testNodeType, SELECTOR_2),
                        QueryObjectModelConstants.JCR_JOIN_TYPE_INNER,
                        qomFactory.equiJoinCondition(SELECTOR_1, propertyName1, SELECTOR_2, propertyName1)
                ),
                qomFactory.descendantNode(SELECTOR_1, testRoot),
                null,
                new Column[]{
                        qomFactory.column(SELECTOR_1, propertyName1, columnName1),
                        qomFactory.column(SELECTOR_2, propertyName1, columnName2)
                }
        );
        RowIterator rows = qom.execute().getRows();
        assertTrue("empty result", rows.hasNext());
        Row r = rows.nextRow();
        assertEquals("unexpected value", TEST_VALUE, r.getValue(columnName1).getString());
        assertEquals("unexpected value", TEST_VALUE, r.getValue(columnName2).getString());
    }
}
