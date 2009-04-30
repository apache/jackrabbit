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

import org.apache.jackrabbit.spi.commons.query.jsr283.qom.DynamicOperand;

import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.Node;
import javax.jcr.ValueFactory;
import javax.jcr.PropertyType;
import javax.jcr.query.Query;

/**
 * <code>UpperLowerCaseTest</code> performs tests with upper- and lower-case
 * operands.
 */
public class UpperLowerCaseTest extends AbstractQOMTest {

    private ValueFactory vf;

    private Node node;

    protected void setUp() throws Exception {
        super.setUp();
        vf = superuser.getValueFactory();
        node = testRootNode.addNode(nodeName1, testNodeType);
        node.setProperty(propertyName1, "abc");
        node.setProperty(propertyName2, "ABC");
        testRootNode.save();
    }

    protected void tearDown() throws Exception {
        vf = null;
        node = null;
        super.tearDown();
    }

    public void testFullTextSearchScore() throws RepositoryException {
        // TODO
    }

    public void testLength() throws RepositoryException {
        // TODO
    }

    public void testNodeLocalName() throws RepositoryException {
        // TODO
    }

    public void testNodeName() throws RepositoryException {
        node.setProperty(propertyName1, "abc", PropertyType.NAME);
        node.setProperty(propertyName2, "ABC", PropertyType.NAME);
        node.save();

        // upper case
        checkQueries(qomFactory.propertyValue("s", propertyName1),
                true, OPERATOR_EQUAL_TO,
                new String[]{"abc", "Abc", "aBc", "abC", "ABC"},
                PropertyType.NAME,
                new boolean[]{false, false, false, false, true});

        checkQueries(qomFactory.propertyValue("s", propertyName2),
                true, OPERATOR_EQUAL_TO,
                new String[]{"abc", "Abc", "aBc", "abC", "ABC"},
                PropertyType.NAME,
                new boolean[]{false, false, false, false, true});

        // lower case
        checkQueries(qomFactory.propertyValue("s", propertyName1),
                false, OPERATOR_EQUAL_TO,
                new String[]{"abc", "Abc", "aBc", "abC", "ABC"},
                PropertyType.NAME,
                new boolean[]{true, false, false, false, false});

        checkQueries(qomFactory.propertyValue("s", propertyName2),
                false, OPERATOR_EQUAL_TO,
                new String[]{"abc", "Abc", "aBc", "abC", "ABC"},
                PropertyType.NAME,
                new boolean[]{true, false, false, false, false});
    }

    public void testPropertyValue() throws RepositoryException {
        // upper case
        checkQueries(qomFactory.propertyValue("s", propertyName1),
                true, OPERATOR_EQUAL_TO,
                new String[]{"abc", "Abc", "aBc", "abC", "ABC"},
                PropertyType.STRING,
                new boolean[]{false, false, false, false, true});

        checkQueries(qomFactory.propertyValue("s", propertyName2),
                true, OPERATOR_EQUAL_TO,
                new String[]{"abc", "Abc", "aBc", "abC", "ABC"},
                PropertyType.STRING,
                new boolean[]{false, false, false, false, true});

        // lower case
        checkQueries(qomFactory.propertyValue("s", propertyName1),
                false, OPERATOR_EQUAL_TO,
                new String[]{"abc", "Abc", "aBc", "abC", "ABC"},
                PropertyType.STRING,
                new boolean[]{true, false, false, false, false});

        checkQueries(qomFactory.propertyValue("s", propertyName2),
                false, OPERATOR_EQUAL_TO,
                new String[]{"abc", "Abc", "aBc", "abC", "ABC"},
                PropertyType.STRING,
                new boolean[]{true, false, false, false, false});
    }

    public void testUpperLowerCase() throws RepositoryException {
        // first upper case, then lower case again
        checkQueries(qomFactory.upperCase(qomFactory.propertyValue("s", propertyName1)),
                false, OPERATOR_EQUAL_TO,
                new String[]{"abc", "Abc", "aBc", "abC", "ABC"},
                PropertyType.STRING,
                new boolean[]{true, false, false, false, false});
    }

    public void testUpperCaseTwice() throws RepositoryException {
        // upper case twice
        checkQueries(qomFactory.upperCase(qomFactory.propertyValue("s", propertyName1)),
                true, OPERATOR_EQUAL_TO,
                new String[]{"abc", "Abc", "aBc", "abC", "ABC"},
                PropertyType.STRING,
                new boolean[]{false, false, false, false, true});
    }

    public void testLowerUpperCase() throws RepositoryException {
        // first lower case, then upper case again
        checkQueries(qomFactory.lowerCase(qomFactory.propertyValue("s", propertyName1)),
                true, OPERATOR_EQUAL_TO,
                new String[]{"abc", "Abc", "aBc", "abC", "ABC"},
                PropertyType.STRING,
                new boolean[]{false, false, false, false, true});
    }

    public void testLowerCaseTwice() throws RepositoryException {
        // lower case twice
        checkQueries(qomFactory.lowerCase(qomFactory.propertyValue("s", propertyName1)),
                false, OPERATOR_EQUAL_TO,
                new String[]{"abc", "Abc", "aBc", "abC", "ABC"},
                PropertyType.STRING,
                new boolean[]{true, false, false, false, false});
    }

    //-------------------------------< internal >-------------------------------

    private void checkQueries(DynamicOperand operand,
                              boolean toUpper,
                              int operator,
                              String[] literals,
                              int type,
                              boolean[] matches) throws RepositoryException {
        for (int i = 0; i < literals.length; i++) {
            Query query = createQuery(operand, toUpper, operator, vf.createValue(literals[i], type));
            checkResult(query.execute(), matches[i] ? new Node[]{node} : new Node[0]);
        }
    }
    
    private Query createQuery(DynamicOperand operand,
                              boolean toUpper,
                              int operator,
                              Value literal) throws RepositoryException {
        if (toUpper) {
            operand = qomFactory.upperCase(operand);
        } else {
            operand = qomFactory.lowerCase(operand);
        }
        return qomFactory.createQuery(
                qomFactory.selector(testNodeType, "s"),
                qomFactory.and(
                        qomFactory.childNode("s", testRoot),
                        qomFactory.comparison(
                                operand,
                                operator,
                                qomFactory.literal(literal)
                        )
                ), null, null);
    }
}
