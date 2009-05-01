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
package org.apache.jackrabbit.api.jsr283.query.qom;

import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.query.Query;

import org.apache.jackrabbit.spi.commons.query.qom.Operator;

/**
 * <code>NodeLocalNameTest</code> checks if conversion of literals is correctly
 * performed and operators work as specified.
 * TODO: assumes https://jsr-283.dev.java.net/issues/show_bug.cgi?id=483 gets resolved as initially proposed
 */
public class NodeLocalNameTest extends AbstractQOMTest {

    private Node node1;

    private String nodeLocalName;

    protected void setUp() throws Exception {
        super.setUp();
        node1 = testRootNode.addNode(nodeName1, testNodeType);
        testRootNode.save();
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
        Query q = createQuery(Operator.EQ, literal);
        checkResult(q.execute(), new Node[]{node1});
    }

    public void testStringLiteralInvalidName() throws RepositoryException {
        Value literal = superuser.getValueFactory().createValue("[" + nodeLocalName);
        Query q = createQuery(Operator.EQ, literal);
        checkResult(q.execute(), new Node[]{});
    }

    public void testBinaryLiteral() throws RepositoryException {
        Value literal = superuser.getValueFactory().createValue(
                nodeLocalName, PropertyType.BINARY);
        Query q = createQuery(Operator.EQ, literal);
        checkResult(q.execute(), new Node[]{node1});
    }

    public void testDateLiteral() throws RepositoryException {
        Value literal = superuser.getValueFactory().createValue(Calendar.getInstance());
        Query q = createQuery(Operator.EQ, literal);
        checkResult(q.execute(), new Node[]{});
    }

    public void testDoubleLiteral() throws RepositoryException {
        Value literal = superuser.getValueFactory().createValue(Math.PI);
        Query q = createQuery(Operator.EQ, literal);
        checkResult(q.execute(), new Node[]{});
    }

    public void testDecimalLiteral() throws RepositoryException {
        // TODO must not match node
    }

    public void testLongLiteral() throws RepositoryException {
        Value literal = superuser.getValueFactory().createValue(283);
        Query q = createQuery(Operator.EQ, literal);
        checkResult(q.execute(), new Node[]{});
    }

    public void testBooleanLiteral() throws RepositoryException {
        Value literal = superuser.getValueFactory().createValue(true);
        Query q = createQuery(Operator.EQ, literal);
        checkResult(q.execute(), new Node[]{});
    }

    public void testNameLiteral() throws RepositoryException {
        Value literal = superuser.getValueFactory().createValue(
                nodeLocalName, PropertyType.NAME);
        Query q = createQuery(Operator.EQ, literal);
        checkResult(q.execute(), new Node[]{node1});
    }

    public void testPathLiteral() throws RepositoryException {
        Value literal = superuser.getValueFactory().createValue(
                nodeLocalName, PropertyType.PATH);
        Query q = createQuery(Operator.EQ, literal);
        checkResult(q.execute(), new Node[]{node1});

        literal = superuser.getValueFactory().createValue(
                node1.getPath(), PropertyType.PATH);
        q = createQuery(Operator.EQ, literal);
        checkResult(q.execute(), new Node[]{});

        literal = superuser.getValueFactory().createValue(
                nodeName1 + "/" + nodeName1, PropertyType.PATH);
        q = createQuery(Operator.EQ, literal);
        checkResult(q.execute(), new Node[]{});
    }

    public void testReferenceLiteral() throws RepositoryException {
        if (!node1.isNodeType(mixReferenceable)) {
            node1.addMixin(mixReferenceable);
        }
        node1.save();
        Value literal = superuser.getValueFactory().createValue(node1);
        Query q = createQuery(Operator.EQ, literal);
        checkResult(q.execute(), new Node[]{});
    }

    public void testWeakReferenceLiteral() throws RepositoryException {
        // TODO must not match node
    }

    public void testURILiteral() throws RepositoryException {
        // TODO must not match node
    }

    public void testEqualTo() throws RepositoryException {
        checkOperator(Operator.EQ, false, true, false);
    }

    public void testGreaterThan() throws RepositoryException {
        checkOperator(Operator.GT, true, false, false);
    }

    public void testGreaterThanOrEqualTo() throws RepositoryException {
        checkOperator(Operator.GE, true, true, false);
    }

    public void testLessThan() throws RepositoryException {
        checkOperator(Operator.LT, false, false, true);
    }

    public void testLessThanOrEqualTo() throws RepositoryException {
        checkOperator(Operator.LE, false, true, true);
    }

    public void testLike() throws RepositoryException {
        checkOperator(Operator.LIKE, false, true, false);
    }

    public void testNotEqualTo() throws RepositoryException {
        checkOperator(Operator.NE, true, false, true);
    }

    //------------------------------< helper >----------------------------------

    private void checkOperator(Operator operator,
                               boolean matchesLesser,
                               boolean matchesEqual,
                               boolean matchesGreater)
            throws RepositoryException {
        checkOperatorSingleLiteral(createLexicographicallyLesser(nodeLocalName), operator, matchesLesser);
        checkOperatorSingleLiteral(nodeLocalName, operator, matchesEqual);
        checkOperatorSingleLiteral(createLexicographicallyGreater(nodeLocalName), operator, matchesGreater);
    }

    private void checkOperatorSingleLiteral(
            String literal, Operator operator, boolean matches)
            throws RepositoryException {
        Value value = superuser.getValueFactory().createValue(literal);
        Query q = createQuery(operator, value);
        checkResult(q.execute(), matches ? new Node[]{node1} : new Node[0]);
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

    private Query createQuery(Operator operator, Value literal)
            throws RepositoryException {
        return qomFactory.createQuery(
                qomFactory.selector(testNodeType, "s"),
                qomFactory.and(
                        qomFactory.childNode("s", testRoot),
                        operator.comparison(
                                qomFactory,
                                qomFactory.nodeLocalName("s"),
                                qomFactory.literal(literal))
                ), null, null);
    }
}
