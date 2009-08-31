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
import javax.jcr.Value;
import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.query.Query;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.qom.QueryObjectModelConstants;
import javax.jcr.query.qom.QueryObjectModel;

import java.util.Calendar;
import java.math.BigDecimal;

import org.apache.jackrabbit.test.NotExecutableException;

/**
 * <code>NodeNameTest</code> checks if conversion of literals is correctly
 * performed and operators work as specified.
 */
public class NodeNameTest extends AbstractQOMTest {

    private Node node1;

    protected void setUp() throws Exception {
        super.setUp();
        node1 = testRootNode.addNode(nodeName1, testNodeType);
        superuser.save();
    }

    protected void tearDown() throws Exception {
        node1 = null;
        super.tearDown();
    }

    public void testStringLiteral() throws RepositoryException {
        Value literal = vf.createValue(nodeName1);
        QueryObjectModel qom = createQuery(QueryObjectModelConstants.JCR_OPERATOR_EQUAL_TO, literal);
        checkQOM(qom, new Node[]{node1});
    }

    public void testStringLiteralInvalidName() throws RepositoryException {
        Value literal = vf.createValue("[" + nodeName1);
        try {
            createQuery(QueryObjectModelConstants.JCR_OPERATOR_EQUAL_TO, literal).execute();
            fail("NodeName comparison with STRING that cannot be converted to NAME must fail with InvalidQueryException");
        } catch (InvalidQueryException e) {
            // expected
        }
        try {
            String stmt = "SELECT * FROM [" + testNodeType + "] AS s " +
                    "WHERE NAME(s) = '" + literal.getString() + "'";
            qm.createQuery(stmt, Query.JCR_SQL2).execute();
            fail("NAME() comparison with STRING that cannot be converted to NAME must fail with InvalidQueryException");
        } catch (InvalidQueryException e) {
            // expected
        }
    }

    public void testBinaryLiteral() throws RepositoryException {
        Value literal = vf.createValue(
                nodeName1, PropertyType.BINARY);
        QueryObjectModel qom = createQuery(QueryObjectModelConstants.JCR_OPERATOR_EQUAL_TO, literal);
        checkQOM(qom, new Node[]{node1});
    }

    public void testDateLiteral() throws RepositoryException {
        Value literal = vf.createValue(Calendar.getInstance());
        try {
            createQuery(QueryObjectModelConstants.JCR_OPERATOR_EQUAL_TO, literal).execute();
            fail("NodeName comparison with DATE must fail with InvalidQueryException");
        } catch (InvalidQueryException e) {
            // expected
        }
        try {
            String stmt = "SELECT * FROM [" + testNodeType + "] AS s " +
                    "WHERE NAME(s) = CAST('" + literal.getString() + "' AS DATE)";
            qm.createQuery(stmt, Query.JCR_SQL2).execute();
            fail("NAME() comparison with DATE must fail with InvalidQueryException");
        } catch (InvalidQueryException e) {
            // expected
        }
    }

    public void testDoubleLiteral() throws RepositoryException {
        Value literal = vf.createValue(Math.PI);
        try {
            createQuery(QueryObjectModelConstants.JCR_OPERATOR_EQUAL_TO, literal).execute();
            fail("NodeName comparison with DOUBLE must fail with InvalidQueryException");
        } catch (InvalidQueryException e) {
            // expected
        }
        try {
            String stmt = "SELECT * FROM [" + testNodeType + "] AS s " +
                    "WHERE NAME(s) = CAST('" + literal.getString() + "' AS DOUBLE)";
            qm.createQuery(stmt, Query.JCR_SQL2).execute();
            fail("NAME() comparison with DOUBLE must fail with InvalidQueryException");
        } catch (InvalidQueryException e) {
            // expected
        }
    }

    public void testDecimalLiteral() throws RepositoryException {
        Value literal = vf.createValue(new BigDecimal(283));
        try {
            createQuery(QueryObjectModelConstants.JCR_OPERATOR_EQUAL_TO, literal).execute();
            fail("NodeName comparison with DECIMAL must fail with InvalidQueryException");
        } catch (InvalidQueryException e) {
            // expected
        }
        try {
            String stmt = "SELECT * FROM [" + testNodeType + "] AS s " +
                    "WHERE NAME(s) = CAST('" + literal.getString() + "' AS DECIMAL)";
            qm.createQuery(stmt, Query.JCR_SQL2).execute();
            fail("NAME() comparison with DECIMAL must fail with InvalidQueryException");
        } catch (InvalidQueryException e) {
            // expected
        }
    }

    public void testLongLiteral() throws RepositoryException {
        Value literal = vf.createValue(283);
        try {
            createQuery(QueryObjectModelConstants.JCR_OPERATOR_EQUAL_TO, literal).execute();
            fail("NodeName comparison with LONG must fail with InvalidQueryException");
        } catch (InvalidQueryException e) {
            // expected
        }
        try {
            String stmt = "SELECT * FROM [" + testNodeType + "] AS s " +
                    "WHERE NAME(s) = CAST(" + literal.getString() + " AS LONG)";
            qm.createQuery(stmt, Query.JCR_SQL2).execute();
            fail("NAME() comparison with LONG must fail with InvalidQueryException");
        } catch (InvalidQueryException e) {
            // expected
        }
    }

    public void testBooleanLiteral() throws RepositoryException {
        Value literal = vf.createValue(true);
        try {
            createQuery(QueryObjectModelConstants.JCR_OPERATOR_EQUAL_TO, literal).execute();
            fail("NodeName comparison with BOOLEAN must fail with InvalidQueryException");
        } catch (InvalidQueryException e) {
            // expected
        }
        try {
            String stmt = "SELECT * FROM [" + testNodeType + "] AS s " +
                    "WHERE NAME(s) = CAST(" + literal.getString() + " AS BOOLEAN)";
            qm.createQuery(stmt, Query.JCR_SQL2).execute();
            fail("NAME() comparison with BOOLEAN must fail with InvalidQueryException");
        } catch (InvalidQueryException e) {
            // expected
        }
    }

    public void testNameLiteral() throws RepositoryException {
        Value literal = vf.createValue(nodeName1, PropertyType.NAME);
        QueryObjectModel qom = createQuery(QueryObjectModelConstants.JCR_OPERATOR_EQUAL_TO, literal);
        checkQOM(qom, new Node[]{node1});
    }

    public void testPathLiteral() throws RepositoryException {
        Value literal = vf.createValue(nodeName1, PropertyType.PATH);
        QueryObjectModel qom = createQuery(QueryObjectModelConstants.JCR_OPERATOR_EQUAL_TO, literal);
        checkQOM(qom, new Node[]{node1});

        literal = vf.createValue(node1.getPath(), PropertyType.PATH);
        try {
            createQuery(QueryObjectModelConstants.JCR_OPERATOR_EQUAL_TO, literal).execute();
            fail("NodeName comparison with absolute PATH must fail with InvalidQueryException");
        } catch (InvalidQueryException e) {
            // expected
        }
        try {
            String stmt = "SELECT * FROM [" + testNodeType + "] AS s " +
                    "WHERE NAME(s) = CAST('" + literal.getString() + "' AS PATH)";
            qm.createQuery(stmt, Query.JCR_SQL2).execute();
            fail("NAME() comparison with absolute PATH must fail with InvalidQueryException");
        } catch (InvalidQueryException e) {
            // expected
        }

        literal = vf.createValue(nodeName1 + "/" + nodeName1, PropertyType.PATH);
        try {
            createQuery(QueryObjectModelConstants.JCR_OPERATOR_EQUAL_TO, literal).execute();
            fail("NodeName comparison with PATH length >1 must fail with InvalidQueryException");
        } catch (InvalidQueryException e) {
            // expected
        }
        try {
            String stmt = "SELECT * FROM [" + testNodeType + "] AS s " +
                    "WHERE NAME(s) = CAST('" + literal.getString() + "' AS PATH)";
            qm.createQuery(stmt, Query.JCR_SQL2).execute();
            fail("NAME() comparison with PATH length >1 must fail with InvalidQueryException");
        } catch (InvalidQueryException e) {
            // expected
        }
    }

    public void testReferenceLiteral() throws RepositoryException,
            NotExecutableException {
        ensureMixinType(node1, mixReferenceable);
        superuser.save();
        Value literal = vf.createValue(node1);
        try {
            createQuery(QueryObjectModelConstants.JCR_OPERATOR_EQUAL_TO, literal).execute();
            fail("NodeName comparison with REFERENCE must fail with InvalidQueryException");
        } catch (InvalidQueryException e) {
            // expected
        }
        try {
            String stmt = "SELECT * FROM [" + testNodeType + "] AS s " +
                    "WHERE NAME(s) = CAST('" + literal.getString() + "' AS REFERENCE)";
            qm.createQuery(stmt, Query.JCR_SQL2).execute();
            fail("NAME() comparison with REFERENCE must fail with InvalidQueryException");
        } catch (InvalidQueryException e) {
            // expected
        }
    }

    public void testWeakReferenceLiteral() throws RepositoryException,
            NotExecutableException {
        ensureMixinType(node1, mixReferenceable);
        superuser.save();
        Value literal = vf.createValue(node1, true);
        try {
            createQuery(QueryObjectModelConstants.JCR_OPERATOR_EQUAL_TO, literal).execute();
            fail("NodeName comparison with WEAKREFERENCE must fail with InvalidQueryException");
        } catch (InvalidQueryException e) {
            // expected
        }
        try {
            String stmt = "SELECT * FROM [" + testNodeType + "] AS s " +
                    "WHERE NAME(s) = CAST('" + literal.getString() + "' AS WEAKREFERENCE)";
            qm.createQuery(stmt, Query.JCR_SQL2).execute();
            fail("NAME() comparison with absolute WEAKREFERENCE must fail with InvalidQueryException");
        } catch (InvalidQueryException e) {
            // expected
        }
    }

    /**
     * If the URI consists of a single path segment without a colon (for
     * example, simply bar) it is converted to a NAME by percent-unescaping
     * followed by UTF-8-decoding of the byte sequence. If it has a redundant
     * leading ./ followed by a single segment (with or without a colon, like
     * ./bar or ./foo:bar ) the redundant ./ is removed and the remainder is
     * converted to a NAME in the same way. Otherwise a ValueFormatException is
     * thrown.
     */
    public void testURILiteral() throws RepositoryException {
        Value literal = vf.createValue("./" + nodeName1, PropertyType.URI);
        QueryObjectModel qom = createQuery(QueryObjectModelConstants.JCR_OPERATOR_EQUAL_TO, literal);
        checkQOM(qom, new Node[]{node1});

        literal = vf.createValue("http://example.com", PropertyType.URI);
        try {
            createQuery(QueryObjectModelConstants.JCR_OPERATOR_EQUAL_TO, literal).execute();
            fail("NodeName comparison with URI that cannot be converted to NAME must fail with InvalidQueryException");
        } catch (InvalidQueryException e) {
            // expected
        }
        try {
            String stmt = "SELECT * FROM [" + testNodeType + "] AS s " +
                    "WHERE NAME(s) = CAST('" + literal.getString() + "' AS URI)";
            qm.createQuery(stmt, Query.JCR_SQL2).execute();
            fail("NAME() comparison with URI that cannot be converted to NAME must fail with InvalidQueryException");
        } catch (InvalidQueryException e) {
            // expected
        }
    }

    public void testEqualTo() throws RepositoryException {
        checkOperator(QueryObjectModelConstants.JCR_OPERATOR_EQUAL_TO, false, true, false);
    }

    public void testNotEqualTo() throws RepositoryException {
        checkOperator(QueryObjectModelConstants.JCR_OPERATOR_NOT_EQUAL_TO, true, false, true);
    }

    //------------------------------< helper >----------------------------------

    private void checkOperator(String operator,
                               boolean matchesLesser,
                               boolean matchesEqual,
                               boolean matchesGreater)
            throws RepositoryException {
        checkOperatorSingleLiteral(createLexicographicallyLesser(nodeName1), operator, matchesLesser);
        checkOperatorSingleLiteral(nodeName1, operator, matchesEqual);
        checkOperatorSingleLiteral(createLexicographicallyGreater(nodeName1), operator, matchesGreater);
    }

    private void checkOperatorSingleLiteral(String literal,
                                            String operator,
                                            boolean matches)
            throws RepositoryException {
        Value value = vf.createValue(literal);
        QueryObjectModel qom = createQuery(operator, value);
        checkQOM(qom, matches ? new Node[]{node1} : new Node[0]);
    }

    private String createLexicographicallyGreater(String name) {
        StringBuffer tmp = new StringBuffer(name);
        tmp.setCharAt(tmp.length() - 1, (char) (tmp.charAt(tmp.length() - 1) + 1));
        return tmp.toString();
    }

    private String createLexicographicallyLesser(String name) {
        StringBuffer tmp = new StringBuffer(name);
        tmp.setCharAt(tmp.length() - 1, (char) (tmp.charAt(tmp.length() - 1) - 1));
        return tmp.toString();
    }

    private QueryObjectModel createQuery(String operator, Value literal)
            throws RepositoryException {
        return qf.createQuery(
                qf.selector(testNodeType, "s"),
                qf.and(
                        qf.childNode("s", testRoot),
                        qf.comparison(
                                qf.nodeName("s"),
                                operator,
                                qf.literal(literal)
                        )
                ), null, null);
    }
}
