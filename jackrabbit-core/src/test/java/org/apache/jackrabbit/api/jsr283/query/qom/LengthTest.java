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

import org.apache.jackrabbit.test.NotExecutableException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.PropertyType;
import javax.jcr.query.QueryResult;
import javax.jcr.query.InvalidQueryException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Calendar;

/**
 * <code>LengthTest</code> performs tests with the Query Object Model length
 * operand.
 */
public class LengthTest extends AbstractQOMTest {

    private Node node;

    private ValueFactory vf;

    protected void setUp() throws Exception {
        super.setUp();
        node = testRootNode.addNode(nodeName1, testNodeType);
        testRootNode.save();
        vf = superuser.getValueFactory();
    }

    protected void tearDown() throws Exception {
        node = null;
        vf = null;
        super.tearDown();
    }

    public void testStringLength() throws RepositoryException {
        node.setProperty(propertyName1, "abc");
        node.save();
        checkOperators(propertyName1, node.getProperty(propertyName1).getLength());
    }
    
    public void testBinaryLength() throws RepositoryException {
        byte[] data = "abc".getBytes();
        node.setProperty(propertyName1, new ByteArrayInputStream(data));
        node.save();
        checkOperators(propertyName1, node.getProperty(propertyName1).getLength());
    }

    public void testLongLength() throws RepositoryException {
        node.setProperty(propertyName1, 123);
        node.save();
        checkOperators(propertyName1, node.getProperty(propertyName1).getLength());
    }

    public void testDoubleLength() throws RepositoryException {
        node.setProperty(propertyName1, Math.PI);
        node.save();
        checkOperators(propertyName1, node.getProperty(propertyName1).getLength());
    }

    public void testDateLength() throws RepositoryException {
        node.setProperty(propertyName1, Calendar.getInstance());
        node.save();
        checkOperators(propertyName1, node.getProperty(propertyName1).getLength());
    }

    public void testBooleanLength() throws RepositoryException {
        node.setProperty(propertyName1, false);
        node.save();
        checkOperators(propertyName1, node.getProperty(propertyName1).getLength());
    }

    public void testNameLength() throws RepositoryException {
        // TODO
    }

    public void testPathLength() throws RepositoryException {
        // TODO
    }

    public void testReferenceLength() throws RepositoryException, NotExecutableException {
        try {
            if (!node.isNodeType(mixReferenceable)) {
                node.addMixin(mixReferenceable);
                node.save();
            }
        } catch (RepositoryException e) {
            throw new NotExecutableException("Cannot add mix:referenceable to node");
        }
        node.setProperty(propertyName1, node);
        node.save();
        checkOperators(propertyName1, node.getProperty(propertyName1).getLength());
    }

    public void testWeakReferenceLength() throws RepositoryException {
        // TODO
    }

    public void testURILength() throws RepositoryException {
        // TODO
    }

    public void testDecimalLength() throws RepositoryException {
        // TODO
    }

    //------------------------< conversion tests >------------------------------

    public void testLengthStringLiteral() throws RepositoryException {
        node.setProperty(propertyName1, "abc");
        node.save();

        String length = String.valueOf(node.getProperty(propertyName1).getLength());
        executeQuery(propertyName1, OPERATOR_EQUAL_TO, vf.createValue(length));
    }

    public void testLengthBinaryLiteral() throws RepositoryException {
        node.setProperty(propertyName1, "abc");
        node.save();

        String length = String.valueOf(node.getProperty(propertyName1).getLength());
        InputStream in = new ByteArrayInputStream(length.getBytes());
        executeQuery(propertyName1, OPERATOR_EQUAL_TO, vf.createValue(in));
    }

    public void testLengthDoubleLiteral() throws RepositoryException {
        node.setProperty(propertyName1, "abc");
        node.save();

        double length = node.getProperty(propertyName1).getLength();
        executeQuery(propertyName1, OPERATOR_EQUAL_TO, vf.createValue(length));
    }

    public void testLengthDateLiteral() throws RepositoryException {
        node.setProperty(propertyName1, "abc");
        node.save();

        Calendar length = Calendar.getInstance();
        length.setTimeInMillis(node.getProperty(propertyName1).getLength());
        executeQuery(propertyName1, OPERATOR_EQUAL_TO, vf.createValue(length));
    }

    public void testLengthBooleanLiteral() throws RepositoryException {
        try {
            executeQuery(propertyName1, OPERATOR_EQUAL_TO, vf.createValue(false));
            fail("Boolean literal cannot be converted to long");
        } catch (InvalidQueryException e) {
            // expected
        }
    }

    public void testLengthNameLiteral() throws RepositoryException {
        try {
            executeQuery(propertyName1, OPERATOR_EQUAL_TO, vf.createValue(
                    propertyName1, PropertyType.NAME));
            fail("Name literal cannot be converted to long");
        } catch (InvalidQueryException e) {
            // expected
        }
    }

    public void testLengthPathLiteral() throws RepositoryException {
        try {
            executeQuery(propertyName1, OPERATOR_EQUAL_TO, vf.createValue(
                    node.getPath(), PropertyType.PATH));
            fail("Path literal cannot be converted to long");
        } catch (InvalidQueryException e) {
            // expected
        }
    }

    public void testLengthReferenceLiteral() throws RepositoryException, NotExecutableException {
        try {
            if (!node.isNodeType(mixReferenceable)) {
                node.addMixin(mixReferenceable);
                node.save();
            }
        } catch (RepositoryException e) {
            throw new NotExecutableException("Cannot add mix:referenceable to node");
        }
        try {
            executeQuery(propertyName1, OPERATOR_EQUAL_TO, vf.createValue(node));
            fail("Reference literal cannot be converted to long");
        } catch (InvalidQueryException e) {
            // expected
        }
    }

    public void testLengthWeakReferenceLiteral() throws RepositoryException {
        // TODO
    }

    public void testLengthURILiteral() throws RepositoryException {
        // TODO
    }

    public void testLengthDecimalLiteral() throws RepositoryException {
        // TODO
    }

    //------------------------< internal helpers >------------------------------

    private void checkOperators(String propertyName,
                                long length) throws RepositoryException {
        checkLength(propertyName, OPERATOR_EQUAL_TO, length, true);
        checkLength(propertyName, OPERATOR_EQUAL_TO, length - 1, false);

        checkLength(propertyName, OPERATOR_GREATER_THAN, length - 1, true);
        checkLength(propertyName, OPERATOR_GREATER_THAN, length, false);

        checkLength(propertyName, OPERATOR_GREATER_THAN_OR_EQUAL_TO, length, true);
        checkLength(propertyName, OPERATOR_GREATER_THAN_OR_EQUAL_TO, length + 1, false);

        checkLength(propertyName, OPERATOR_LESS_THAN, length + 1, true);
        checkLength(propertyName, OPERATOR_LESS_THAN, length, false);

        checkLength(propertyName, OPERATOR_LESS_THAN_OR_EQUAL_TO, length, true);
        checkLength(propertyName, OPERATOR_LESS_THAN_OR_EQUAL_TO, length - 1, false);

        checkLength(propertyName, OPERATOR_NOT_EQUAL_TO, length - 1, true);
        checkLength(propertyName, OPERATOR_NOT_EQUAL_TO, length, false);
    }

    private void checkLength(String propertyName,
                             int operator,
                             long length,
                             boolean matches) throws RepositoryException {
        Node[] result;
        if (matches) {
            result = new Node[]{node};
        } else {
            result = new Node[0];
        }
        checkResult(executeQuery(propertyName, operator, length), result);
    }

    private QueryResult executeQuery(String propertyName,
                                     int operator,
                                     long length) throws RepositoryException {
        Value v = vf.createValue(length);
        return executeQuery(propertyName, operator, v);
    }

    private QueryResult executeQuery(String propertyName,
                                     int operator,
                                     Value length) throws RepositoryException {
        return qomFactory.createQuery(
                qomFactory.selector(testNodeType, "s"),
                qomFactory.and(
                        qomFactory.childNode("s", testRoot),
                        qomFactory.comparison(
                                qomFactory.length(
                                        qomFactory.propertyValue(
                                                "s", propertyName)),
                                operator,
                                qomFactory.literal(length))

                ), null, null).execute();
    }
}
