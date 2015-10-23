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
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;
import javax.jcr.query.qom.QueryObjectModelConstants;
import javax.jcr.query.qom.QueryObjectModel;

import java.util.Calendar;
import java.math.BigDecimal;

import org.apache.jackrabbit.test.NotExecutableException;

/**
 * <code>NodeLocalNameTest</code> checks if conversion of literals is correctly
 * performed and operators work as specified.
 */
public class NodeLocalNameTest extends AbstractQOMTest {

    private Node node1;

    private String nodeLocalName;

    protected void setUp() throws Exception {
        super.setUp();
        node1 = testRootNode.addNode(nodeName1, testNodeType);
        superuser.save();
        int colon = nodeName1.indexOf(':');
        if (colon != -1) {
            nodeLocalName = nodeName1.substring(colon + 1);
        } else {
            nodeLocalName = nodeName1;
        }
    }

    protected void tearDown() throws Exception {
        node1 = null;
        super.tearDown();
    }

    public void testStringLiteral() throws RepositoryException {
        Value literal = superuser.getValueFactory().createValue(nodeLocalName);
        Query q = createQuery(QueryObjectModelConstants.JCR_OPERATOR_EQUAL_TO, literal);
        checkResult(q.execute(), new Node[]{node1});
    }

    public void testStringLiteralInvalidName() throws RepositoryException {
        Value literal = superuser.getValueFactory().createValue("[" + nodeLocalName);
        try {
            createQuery(QueryObjectModelConstants.JCR_OPERATOR_EQUAL_TO, literal).execute();
            fail("NodeName comparison with STRING that cannot be converted to NAME must fail with InvalidQueryException");
        } catch (InvalidQueryException e) {
            // expected
        }
    }

    public void testBinaryLiteral() throws RepositoryException {
        Value literal = superuser.getValueFactory().createValue(
                nodeLocalName, PropertyType.BINARY);
        QueryObjectModel qom = createQuery(QueryObjectModelConstants.JCR_OPERATOR_EQUAL_TO, literal);
        checkQOM(qom, new Node[]{node1});
    }

    public void testDateLiteral() throws RepositoryException {
        Value literal = superuser.getValueFactory().createValue(Calendar.getInstance());
        QueryObjectModel qom = createQuery(QueryObjectModelConstants.JCR_OPERATOR_EQUAL_TO, literal);
        checkQOM(qom, new Node[]{});
    }

    public void testDoubleLiteral() throws RepositoryException {
        Value literal = superuser.getValueFactory().createValue(Math.PI);
        QueryObjectModel qom = createQuery(QueryObjectModelConstants.JCR_OPERATOR_EQUAL_TO, literal);
        checkQOM(qom, new Node[]{});
    }

    public void testDecimalLiteral() throws RepositoryException {
        Value literal = superuser.getValueFactory().createValue(new BigDecimal(283));
        QueryObjectModel qom = createQuery(QueryObjectModelConstants.JCR_OPERATOR_EQUAL_TO, literal);
        checkQOM(qom, new Node[]{});
    }

    public void testLongLiteral() throws RepositoryException {
        Value literal = superuser.getValueFactory().createValue(283);
        QueryObjectModel qom = createQuery(QueryObjectModelConstants.JCR_OPERATOR_EQUAL_TO, literal);
        checkQOM(qom, new Node[]{});
    }

    public void testBooleanLiteral() throws RepositoryException {
        Value literal = superuser.getValueFactory().createValue(true);
        QueryObjectModel qom = createQuery(QueryObjectModelConstants.JCR_OPERATOR_EQUAL_TO, literal);
        checkQOM(qom, new Node[]{});
    }

    public void testNameLiteral() throws RepositoryException {
        Value literal = superuser.getValueFactory().createValue(
                nodeLocalName, PropertyType.NAME);
        QueryObjectModel qom = createQuery(QueryObjectModelConstants.JCR_OPERATOR_EQUAL_TO, literal);
        checkQOM(qom, new Node[]{node1});
    }

    public void testPathLiteral() throws RepositoryException {
        Value literal = superuser.getValueFactory().createValue(
                nodeLocalName, PropertyType.PATH);
        QueryObjectModel qom = createQuery(QueryObjectModelConstants.JCR_OPERATOR_EQUAL_TO, literal);
        checkQOM(qom, new Node[]{node1});

        literal = superuser.getValueFactory().createValue(
                node1.getPath(), PropertyType.PATH);
        try {
            createQuery(QueryObjectModelConstants.JCR_OPERATOR_EQUAL_TO, literal).execute();
            fail("NodeName comparison with absolute PATH must fail with InvalidQueryException");
        } catch (InvalidQueryException e) {
            // expected
        }

        literal = superuser.getValueFactory().createValue(
                nodeName1 + "/" + nodeName1, PropertyType.PATH);
        try {
            createQuery(QueryObjectModelConstants.JCR_OPERATOR_EQUAL_TO, literal).execute();
            fail("NodeName comparison with PATH length >1 must fail with InvalidQueryException");
        } catch (InvalidQueryException e) {
            // expected
        }
    }

    public void testReferenceLiteral() throws RepositoryException,
            NotExecutableException {
        ensureMixinType(node1, mixReferenceable);
        superuser.save();
        Value literal = superuser.getValueFactory().createValue(node1);
        QueryObjectModel qom = createQuery(QueryObjectModelConstants.JCR_OPERATOR_EQUAL_TO, literal);
        checkQOM(qom, new Node[]{});
    }

    public void testWeakReferenceLiteral() throws RepositoryException,
            NotExecutableException {
        ensureMixinType(node1, mixReferenceable);
        superuser.save();
        Value literal = superuser.getValueFactory().createValue(node1, true);
        QueryObjectModel qom = createQuery(QueryObjectModelConstants.JCR_OPERATOR_EQUAL_TO, literal);
        checkQOM(qom, new Node[]{});
    }

    public void testURILiteral() throws RepositoryException {
        Value literal = superuser.getValueFactory().createValue("http://example.com", PropertyType.URI);
        try {
            createQuery(QueryObjectModelConstants.JCR_OPERATOR_EQUAL_TO, literal).execute();
            fail("NodeName comparison with URI that cannot be converted to NAME must fail with InvalidQueryException");
        } catch (InvalidQueryException e) {
            // expected
        }
    }

    public void testEqualTo() throws RepositoryException {
        checkOperator(QueryObjectModelConstants.JCR_OPERATOR_EQUAL_TO, false, true, false);
    }

    public void testGreaterThan() throws RepositoryException {
        checkOperator(QueryObjectModelConstants.JCR_OPERATOR_GREATER_THAN, true, false, false);
    }

    public void testGreaterThanOrEqualTo() throws RepositoryException {
        checkOperator(QueryObjectModelConstants.JCR_OPERATOR_GREATER_THAN_OR_EQUAL_TO, true, true, false);
    }

    public void testLessThan() throws RepositoryException {
        checkOperator(QueryObjectModelConstants.JCR_OPERATOR_LESS_THAN, false, false, true);
    }

    public void testLessThanOrEqualTo() throws RepositoryException {
        checkOperator(QueryObjectModelConstants.JCR_OPERATOR_LESS_THAN_OR_EQUAL_TO, false, true, true);
    }

    public void testLike() throws RepositoryException {
        checkOperator(QueryObjectModelConstants.JCR_OPERATOR_LIKE, false, true, false);
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
        checkOperatorSingleLiteral(createLexicographicallyLesser(nodeLocalName), operator, matchesLesser);
        checkOperatorSingleLiteral(nodeLocalName, operator, matchesEqual);
        checkOperatorSingleLiteral(createLexicographicallyGreater(nodeLocalName), operator, matchesGreater);
    }

    private void checkOperatorSingleLiteral(String literal,
                                            String operator,
                                            boolean matches)
            throws RepositoryException {
        Value value = superuser.getValueFactory().createValue(literal);
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
                                qf.nodeLocalName("s"),
                                operator,
                                qf.literal(literal)
                        )
                ), null, null);
    }
}
