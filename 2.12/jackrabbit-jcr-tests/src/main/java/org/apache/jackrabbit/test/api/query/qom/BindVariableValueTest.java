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

import java.util.Calendar;
import java.math.BigDecimal;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.PropertyType;
import javax.jcr.query.Query;
import javax.jcr.query.qom.QueryObjectModelConstants;

import org.apache.jackrabbit.test.NotExecutableException;

/**
 * <code>BindVariableValueTest</code>...
 */
public class BindVariableValueTest extends AbstractQOMTest {

    private static final String STRING_VALUE = "JSR";

    private static final long LONG_VALUE = 283;

    private static final double DOUBLE_VALUE = Math.PI;

    private static final boolean BOOLEAN_VALUE = true;

    private static final Calendar DATE_VALUE = Calendar.getInstance();

    private static final BigDecimal DECIMAL_VALUE = new BigDecimal(LONG_VALUE);

    private static final String URI_VALUE = "http://example.com/";

    private Query qomQuery;

    private Query sqlQuery;

    protected void setUp() throws Exception {
        super.setUp();
        qomQuery = qf.createQuery(
                qf.selector(testNodeType, "s"),
                qf.and(
                        qf.childNode("s", testRoot),
                        qf.comparison(
                                qf.propertyValue("s", propertyName1),
                                QueryObjectModelConstants.JCR_OPERATOR_EQUAL_TO,
                                qf.bindVariable("v")
                        )
                ), null, null);
        sqlQuery = qm.createQuery(qomQuery.getStatement(), Query.JCR_SQL2);
    }

    protected void tearDown() throws Exception {
        qomQuery = null;
        super.tearDown();
    }

    public void testBindVariableNames() throws RepositoryException {
        String[] names = qomQuery.getBindVariableNames();
        assertNotNull(names);
        assertEquals(1, names.length);
        assertEquals("v", names[0]);
    }

    public void testIllegalArgumentException() throws RepositoryException {
        try {
            bindVariableValue(qomQuery, "x", vf.createValue(STRING_VALUE));
            fail("Query.bindValue() must throw IllegalArgumentException for unknown variable name");
        } catch (IllegalArgumentException e) {
            // expected
        }
        try {
            bindVariableValue(sqlQuery, "x", vf.createValue(STRING_VALUE));
            fail("Query.bindValue() must throw IllegalArgumentException for unknown variable name");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    public void testString() throws RepositoryException {
        Node n = testRootNode.addNode(nodeName1, testNodeType);
        n.setProperty(propertyName1, STRING_VALUE);
        superuser.save();

        bindVariableValue(qomQuery, "v", vf.createValue(STRING_VALUE));
        checkResult(qomQuery.execute(), new Node[]{n});

        bindVariableValue(sqlQuery, "v", vf.createValue(STRING_VALUE));
        checkResult(sqlQuery.execute(), new Node[]{n});
    }

    public void testDate() throws RepositoryException {
        Node n = testRootNode.addNode(nodeName1, testNodeType);
        n.setProperty(propertyName1, DATE_VALUE);
        superuser.save();

        bindVariableValue(sqlQuery, "v", vf.createValue(DATE_VALUE));
        checkResult(sqlQuery.execute(), new Node[]{n});

        bindVariableValue(qomQuery, "v", vf.createValue(DATE_VALUE));
        checkResult(qomQuery.execute(), new Node[]{n});
    }

    public void testLong() throws RepositoryException {
        Node n = testRootNode.addNode(nodeName1, testNodeType);
        n.setProperty(propertyName1, LONG_VALUE);
        superuser.save();

        bindVariableValue(qomQuery, "v", vf.createValue(LONG_VALUE));
        checkResult(qomQuery.execute(), new Node[]{n});

        bindVariableValue(sqlQuery, "v", vf.createValue(LONG_VALUE));
        checkResult(sqlQuery.execute(), new Node[]{n});
    }

    public void testDouble() throws RepositoryException {
        Node n = testRootNode.addNode(nodeName1, testNodeType);
        n.setProperty(propertyName1, DOUBLE_VALUE);
        superuser.save();

        bindVariableValue(qomQuery, "v", vf.createValue(DOUBLE_VALUE));
        checkResult(qomQuery.execute(), new Node[]{n});

        bindVariableValue(sqlQuery, "v", vf.createValue(DOUBLE_VALUE));
        checkResult(sqlQuery.execute(), new Node[]{n});
    }

    public void testBoolean() throws RepositoryException {
        Node n = testRootNode.addNode(nodeName1, testNodeType);
        n.setProperty(propertyName1, BOOLEAN_VALUE);
        superuser.save();

        bindVariableValue(qomQuery, "v", vf.createValue(BOOLEAN_VALUE));
        checkResult(qomQuery.execute(), new Node[]{n});

        bindVariableValue(sqlQuery, "v", vf.createValue(BOOLEAN_VALUE));
        checkResult(sqlQuery.execute(), new Node[]{n});
    }

    public void testName() throws RepositoryException {
        Value name = vf.createValue(STRING_VALUE, PropertyType.NAME);
        Node n = testRootNode.addNode(nodeName1, testNodeType);
        n.setProperty(propertyName1, name);
        superuser.save();

        bindVariableValue(qomQuery, "v", name);
        checkResult(qomQuery.execute(), new Node[]{n});

        bindVariableValue(sqlQuery, "v", name);
        checkResult(sqlQuery.execute(), new Node[]{n});
    }

    public void testPath() throws RepositoryException {
        Node n = testRootNode.addNode(nodeName1, testNodeType);
        Value path = vf.createValue(n.getPath(), PropertyType.PATH);
        n.setProperty(propertyName1, path);
        superuser.save();

        bindVariableValue(qomQuery, "v", path);
        checkResult(qomQuery.execute(), new Node[]{n});

        bindVariableValue(sqlQuery, "v", path);
        checkResult(sqlQuery.execute(), new Node[]{n});
    }

    public void testReference() throws RepositoryException,
            NotExecutableException {
        Node n = testRootNode.addNode(nodeName1, testNodeType);
        superuser.save();

        ensureMixinType(n, mixReferenceable);
        superuser.save();
        n.setProperty(propertyName1, n);
        superuser.save();


        bindVariableValue(qomQuery, "v", vf.createValue(n));
        checkResult(qomQuery.execute(), new Node[]{n});

        bindVariableValue(sqlQuery, "v", vf.createValue(n));
        checkResult(sqlQuery.execute(), new Node[]{n});
    }

    public void testWeakReference() throws RepositoryException,
            NotExecutableException {
        Node n = testRootNode.addNode(nodeName1, testNodeType);
        superuser.save();

        ensureMixinType(n, mixReferenceable);
        superuser.save();
        n.setProperty(propertyName1, vf.createValue(n, true));
        superuser.save();

        bindVariableValue(qomQuery, "v", vf.createValue(n, true));
        checkResult(qomQuery.execute(), new Node[]{n});

        bindVariableValue(sqlQuery, "v", vf.createValue(n, true));
        checkResult(sqlQuery.execute(), new Node[]{n});
    }

    public void testURI() throws RepositoryException {
        Value value = vf.createValue(URI_VALUE, PropertyType.URI);
        Node n = testRootNode.addNode(nodeName1, testNodeType);
        n.setProperty(propertyName1, value);
        superuser.save();

        bindVariableValue(qomQuery, "v", value);
        checkResult(qomQuery.execute(), new Node[]{n});

        bindVariableValue(sqlQuery, "v", value);
        checkResult(sqlQuery.execute(), new Node[]{n});
    }

    public void testDecimal() throws RepositoryException {
        Value value = vf.createValue(DECIMAL_VALUE);
        Node n = testRootNode.addNode(nodeName1, testNodeType);
        n.setProperty(propertyName1, value);
        superuser.save();

        bindVariableValue(qomQuery, "v", value);
        checkResult(qomQuery.execute(), new Node[]{n});

        bindVariableValue(sqlQuery, "v", value);
        checkResult(sqlQuery.execute(), new Node[]{n});
    }
}
