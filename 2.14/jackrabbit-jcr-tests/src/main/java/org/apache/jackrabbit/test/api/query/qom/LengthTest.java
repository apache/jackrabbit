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

import org.apache.jackrabbit.test.NotExecutableException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.PropertyType;
import javax.jcr.Binary;
import javax.jcr.query.QueryResult;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;
import javax.jcr.query.qom.QueryObjectModelConstants;
import javax.jcr.query.qom.QueryObjectModel;

import java.io.ByteArrayInputStream;
import java.util.Calendar;
import java.math.BigDecimal;

/**
 * <code>LengthTest</code> performs tests with the Query Object Model length
 * operand.
 */
public class LengthTest extends AbstractQOMTest {

    private Node node;


    protected void setUp() throws Exception {
        super.setUp();
        node = testRootNode.addNode(nodeName1, testNodeType);
        superuser.save();
    }

    protected void tearDown() throws Exception {
        node = null;
        super.tearDown();
    }

    public void testStringLength() throws RepositoryException {
        node.setProperty(propertyName1, "abc");
        superuser.save();
        checkOperators(propertyName1, node.getProperty(propertyName1).getLength());
    }
    
    public void testBinaryLength() throws RepositoryException {
        byte[] data = "abc".getBytes();
        Binary b = vf.createBinary(new ByteArrayInputStream(data));
        try {
            node.setProperty(propertyName1, b);
        } finally {
            b.dispose();
        }
        superuser.save();
        checkOperators(propertyName1, node.getProperty(propertyName1).getLength());
    }

    public void testLongLength() throws RepositoryException {
        node.setProperty(propertyName1, 123);
        superuser.save();
        checkOperators(propertyName1, node.getProperty(propertyName1).getLength());
    }

    public void testDoubleLength() throws RepositoryException {
        node.setProperty(propertyName1, Math.PI);
        superuser.save();
        checkOperators(propertyName1, node.getProperty(propertyName1).getLength());
    }

    public void testDateLength() throws RepositoryException {
        node.setProperty(propertyName1, Calendar.getInstance());
        superuser.save();
        checkOperators(propertyName1, node.getProperty(propertyName1).getLength());
    }

    public void testBooleanLength() throws RepositoryException {
        node.setProperty(propertyName1, false);
        superuser.save();
        checkOperators(propertyName1, node.getProperty(propertyName1).getLength());
    }

    public void testNameLength() throws RepositoryException {
        node.setProperty(propertyName1, vf.createValue(node.getName(), PropertyType.NAME));
        superuser.save();
        checkOperators(propertyName1, node.getProperty(propertyName1).getLength());
    }

    public void testPathLength() throws RepositoryException {
        node.setProperty(propertyName1, vf.createValue(node.getPath(), PropertyType.PATH));
        superuser.save();
        checkOperators(propertyName1, node.getProperty(propertyName1).getLength());
    }

    public void testReferenceLength() throws RepositoryException, NotExecutableException {
        ensureMixinType(node, mixReferenceable);
        superuser.save();
        node.setProperty(propertyName1, node);
        superuser.save();
        checkOperators(propertyName1, node.getProperty(propertyName1).getLength());
    }

    public void testWeakReferenceLength()
            throws RepositoryException, NotExecutableException {
        ensureMixinType(node, mixReferenceable);
        superuser.save();
        node.setProperty(propertyName1, vf.createValue(node, true));
        superuser.save();
        checkOperators(propertyName1, node.getProperty(propertyName1).getLength());
    }

    public void testURILength() throws RepositoryException {
        node.setProperty(propertyName1, vf.createValue("http://example.com", PropertyType.URI));
        superuser.save();
        checkOperators(propertyName1, node.getProperty(propertyName1).getLength());
    }

    public void testDecimalLength() throws RepositoryException {
        node.setProperty(propertyName1, new BigDecimal(123));
        superuser.save();
        checkOperators(propertyName1, node.getProperty(propertyName1).getLength());
    }

    //------------------------< conversion tests >------------------------------

    public void testLengthStringLiteral() throws RepositoryException {
        node.setProperty(propertyName1, "abc");
        superuser.save();

        String length = String.valueOf(node.getProperty(propertyName1).getLength());
        executeQueries(propertyName1, QueryObjectModelConstants.JCR_OPERATOR_EQUAL_TO, vf.createValue(length));
    }

    public void testLengthBinaryLiteral() throws RepositoryException {
        node.setProperty(propertyName1, "abc");
        superuser.save();

        String length = String.valueOf(node.getProperty(propertyName1).getLength());
        Binary b = vf.createBinary(new ByteArrayInputStream(length.getBytes()));
        try {
            executeQueries(propertyName1, QueryObjectModelConstants.JCR_OPERATOR_EQUAL_TO,
                    vf.createValue(b));
        } finally {
            b.dispose();
        }
    }

    public void testLengthDoubleLiteral() throws RepositoryException {
        node.setProperty(propertyName1, "abc");
        superuser.save();

        double length = node.getProperty(propertyName1).getLength();
        executeQueries(propertyName1, QueryObjectModelConstants.JCR_OPERATOR_EQUAL_TO, vf.createValue(length));
    }

    public void testLengthDateLiteral() throws RepositoryException {
        node.setProperty(propertyName1, "abc");
        superuser.save();

        Calendar length = Calendar.getInstance();
        length.setTimeInMillis(node.getProperty(propertyName1).getLength());
        executeQueries(propertyName1, QueryObjectModelConstants.JCR_OPERATOR_EQUAL_TO, vf.createValue(length));
    }

    public void testLengthBooleanLiteral() throws RepositoryException {
        try {
            executeQueries(propertyName1, QueryObjectModelConstants.JCR_OPERATOR_EQUAL_TO, vf.createValue(false));
            fail("Boolean literal cannot be converted to long");
        } catch (InvalidQueryException e) {
            // expected
        }
    }

    public void testLengthNameLiteral() throws RepositoryException {
        try {
            executeQueries(propertyName1, QueryObjectModelConstants.JCR_OPERATOR_EQUAL_TO, vf.createValue(
                    propertyName1, PropertyType.NAME));
            fail("Name literal cannot be converted to long");
        } catch (InvalidQueryException e) {
            // expected
        }
    }

    public void testLengthPathLiteral() throws RepositoryException {
        try {
            executeQueries(propertyName1, QueryObjectModelConstants.JCR_OPERATOR_EQUAL_TO, vf.createValue(
                    node.getPath(), PropertyType.PATH));
            fail("Path literal cannot be converted to long");
        } catch (InvalidQueryException e) {
            // expected
        }
    }

    public void testLengthReferenceLiteral() throws RepositoryException, NotExecutableException {
        ensureMixinType(node, mixReferenceable);
        superuser.save();
        try {
            executeQueries(propertyName1, QueryObjectModelConstants.JCR_OPERATOR_EQUAL_TO, vf.createValue(node));
            fail("Reference literal cannot be converted to long");
        } catch (InvalidQueryException e) {
            // expected
        }
    }

    public void testLengthWeakReferenceLiteral() throws RepositoryException, NotExecutableException {
        ensureMixinType(node, mixReferenceable);
        superuser.save();
        try {
            executeQueries(propertyName1, QueryObjectModelConstants.JCR_OPERATOR_EQUAL_TO, vf.createValue(node, true));
            fail("Reference literal cannot be converted to long");
        } catch (InvalidQueryException e) {
            // expected
        }
    }

    public void testLengthURILiteral() throws RepositoryException {
        try {
            executeQueries(propertyName1, QueryObjectModelConstants.JCR_OPERATOR_EQUAL_TO,
                    vf.createValue(node.getPath(), PropertyType.URI));
            fail("URI literal cannot be converted to long");
        } catch (InvalidQueryException e) {
            // expected
        }
    }

    public void testLengthDecimalLiteral() throws RepositoryException {
        node.setProperty(propertyName1, "abc");
        superuser.save();

        BigDecimal length = new BigDecimal(node.getProperty(propertyName1).getLength());
        executeQueries(propertyName1, QueryObjectModelConstants.JCR_OPERATOR_EQUAL_TO, vf.createValue(length));
    }

    //------------------------< internal helpers >------------------------------

    private void checkOperators(String propertyName,
                                long length) throws RepositoryException {
        checkLength(propertyName, QueryObjectModelConstants.JCR_OPERATOR_EQUAL_TO, length, true);
        checkLength(propertyName, QueryObjectModelConstants.JCR_OPERATOR_EQUAL_TO, length - 1, false);

        checkLength(propertyName, QueryObjectModelConstants.JCR_OPERATOR_GREATER_THAN, length - 1, true);
        checkLength(propertyName, QueryObjectModelConstants.JCR_OPERATOR_GREATER_THAN, length, false);

        checkLength(propertyName, QueryObjectModelConstants.JCR_OPERATOR_GREATER_THAN_OR_EQUAL_TO, length, true);
        checkLength(propertyName, QueryObjectModelConstants.JCR_OPERATOR_GREATER_THAN_OR_EQUAL_TO, length + 1, false);

        checkLength(propertyName, QueryObjectModelConstants.JCR_OPERATOR_LESS_THAN, length + 1, true);
        checkLength(propertyName, QueryObjectModelConstants.JCR_OPERATOR_LESS_THAN, length, false);

        checkLength(propertyName, QueryObjectModelConstants.JCR_OPERATOR_LESS_THAN_OR_EQUAL_TO, length, true);
        checkLength(propertyName, QueryObjectModelConstants.JCR_OPERATOR_LESS_THAN_OR_EQUAL_TO, length - 1, false);

        checkLength(propertyName, QueryObjectModelConstants.JCR_OPERATOR_NOT_EQUAL_TO, length - 1, true);
        checkLength(propertyName, QueryObjectModelConstants.JCR_OPERATOR_NOT_EQUAL_TO, length, false);
    }

    private void checkLength(String propertyName,
                             String operator,
                             long length,
                             boolean matches) throws RepositoryException {
        Node[] expected;
        if (matches) {
            expected = new Node[]{node};
        } else {
            expected = new Node[0];
        }
        QueryResult[] results = executeQueries(propertyName, operator, length);
        for (int i = 0; i < results.length; i++) {
            checkResult(results[i], expected);
        }
    }

    private QueryResult[] executeQueries(String propertyName,
                                         String operator,
                                         long length)
            throws RepositoryException {
        Value v = vf.createValue(length);
        return executeQueries(propertyName, operator, v);
    }

    private QueryResult[] executeQueries(String propertyName,
                                         String operator,
                                         Value length)
            throws RepositoryException {
        QueryObjectModel qom = qf.createQuery(
                qf.selector(testNodeType, "s"),
                qf.and(
                        qf.childNode("s", testRoot),
                        qf.comparison(
                                qf.length(
                                        qf.propertyValue(
                                                "s", propertyName)),
                                operator,
                                qf.literal(length))

                ), null, null);
        QueryResult[] results = new QueryResult[2];
        results[0] = qom.execute();
        results[1] = qm.createQuery(qom.getStatement(), Query.JCR_SQL2).execute();
        return results;
    }
}
