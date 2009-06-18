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

import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.query.qom.DynamicOperand;
import javax.jcr.query.qom.QueryObjectModelConstants;
import javax.jcr.query.qom.QueryObjectModel;

/**
 * <code>UpperLowerCaseTest</code> performs tests with upper- and lower-case
 * operands.
 */
public class UpperLowerCaseTest extends AbstractQOMTest {

    private Node node;

    protected void setUp() throws Exception {
        super.setUp();
        node = testRootNode.addNode(nodeName1, testNodeType);
        node.setProperty(propertyName1, "abc");
        node.setProperty(propertyName2, "ABC");
        superuser.save();
    }

    protected void tearDown() throws Exception {
        node = null;
        super.tearDown();
    }

    public void testLength() throws RepositoryException {
        String lenStr = String.valueOf(node.getProperty(propertyName1).getLength());
        // upper case
        checkQueries(qf.length(qf.propertyValue("s", propertyName1)),
                true, QueryObjectModelConstants.JCR_OPERATOR_EQUAL_TO,
                new String[]{lenStr.toUpperCase()},
                PropertyType.STRING,
                new boolean[]{true});

        // lower case
        checkQueries(qf.length(qf.propertyValue("s", propertyName1)),
                false, QueryObjectModelConstants.JCR_OPERATOR_EQUAL_TO,
                new String[]{lenStr.toLowerCase()},
                PropertyType.STRING,
                new boolean[]{true});
    }

    public void testNodeLocalName() throws RepositoryException {
        String localName = getLocalName(node.getName());
        // upper case
        checkQueries(qf.nodeLocalName("s"),
                true, QueryObjectModelConstants.JCR_OPERATOR_EQUAL_TO,
                new String[]{localName.toLowerCase(), localName.toUpperCase()},
                PropertyType.STRING,
                new boolean[]{false, true});

        // lower case
        checkQueries(qf.nodeLocalName("s"),
                false, QueryObjectModelConstants.JCR_OPERATOR_EQUAL_TO,
                new String[]{localName.toLowerCase(), localName.toUpperCase()},
                PropertyType.STRING,
                new boolean[]{true, false});
    }

    public void testNodeName() throws RepositoryException {
        // upper case
        checkQueries(qf.nodeName("s"),
                true, QueryObjectModelConstants.JCR_OPERATOR_EQUAL_TO,
                new String[]{node.getName().toLowerCase(), node.getName().toUpperCase()},
                PropertyType.NAME,
                new boolean[]{false, true});

        // lower case
        checkQueries(qf.nodeName("s"),
                false, QueryObjectModelConstants.JCR_OPERATOR_EQUAL_TO,
                new String[]{node.getName().toLowerCase(), node.getName().toUpperCase()},
                PropertyType.NAME,
                new boolean[]{true, false});
    }

    public void testPropertyValue() throws RepositoryException {
        // upper case
        checkQueries(qf.propertyValue("s", propertyName1),
                true, QueryObjectModelConstants.JCR_OPERATOR_EQUAL_TO,
                new String[]{"abc", "Abc", "aBc", "abC", "ABC"},
                PropertyType.STRING,
                new boolean[]{false, false, false, false, true});

        checkQueries(qf.propertyValue("s", propertyName2),
                true, QueryObjectModelConstants.JCR_OPERATOR_EQUAL_TO,
                new String[]{"abc", "Abc", "aBc", "abC", "ABC"},
                PropertyType.STRING,
                new boolean[]{false, false, false, false, true});

        // lower case
        checkQueries(qf.propertyValue("s", propertyName1),
                false, QueryObjectModelConstants.JCR_OPERATOR_EQUAL_TO,
                new String[]{"abc", "Abc", "aBc", "abC", "ABC"},
                PropertyType.STRING,
                new boolean[]{true, false, false, false, false});

        checkQueries(qf.propertyValue("s", propertyName2),
                false, QueryObjectModelConstants.JCR_OPERATOR_EQUAL_TO,
                new String[]{"abc", "Abc", "aBc", "abC", "ABC"},
                PropertyType.STRING,
                new boolean[]{true, false, false, false, false});
    }

    public void testUpperLowerCase() throws RepositoryException {
        // first upper case, then lower case again
        checkQueries(qf.upperCase(qf.propertyValue("s", propertyName1)),
                false, QueryObjectModelConstants.JCR_OPERATOR_EQUAL_TO,
                new String[]{"abc", "Abc", "aBc", "abC", "ABC"},
                PropertyType.STRING,
                new boolean[]{true, false, false, false, false});
    }

    public void testUpperCaseTwice() throws RepositoryException {
        // upper case twice
        checkQueries(qf.upperCase(qf.propertyValue("s", propertyName1)),
                true, QueryObjectModelConstants.JCR_OPERATOR_EQUAL_TO,
                new String[]{"abc", "Abc", "aBc", "abC", "ABC"},
                PropertyType.STRING,
                new boolean[]{false, false, false, false, true});
    }

    public void testLowerUpperCase() throws RepositoryException {
        // first lower case, then upper case again
        checkQueries(qf.lowerCase(qf.propertyValue("s", propertyName1)),
                true, QueryObjectModelConstants.JCR_OPERATOR_EQUAL_TO,
                new String[]{"abc", "Abc", "aBc", "abC", "ABC"},
                PropertyType.STRING,
                new boolean[]{false, false, false, false, true});
    }

    public void testLowerCaseTwice() throws RepositoryException {
        // lower case twice
        checkQueries(qf.lowerCase(qf.propertyValue("s", propertyName1)),
                false, QueryObjectModelConstants.JCR_OPERATOR_EQUAL_TO,
                new String[]{"abc", "Abc", "aBc", "abC", "ABC"},
                PropertyType.STRING,
                new boolean[]{true, false, false, false, false});
    }

    //-------------------------------< internal >-------------------------------

    private void checkQueries(DynamicOperand operand,
                              boolean toUpper,
                              String operator,
                              String[] literals,
                              int type,
                              boolean[] matches) throws RepositoryException {
        for (int i = 0; i < literals.length; i++) {
            QueryObjectModel qom = createQuery(operand, toUpper, operator, vf.createValue(literals[i], type));
            checkQOM(qom, matches[i] ? new Node[]{node} : new Node[0]);
        }
    }
    
    private QueryObjectModel createQuery(DynamicOperand operand,
                                         boolean toUpper,
                                         String operator,
                                         Value literal)
            throws RepositoryException {
        if (toUpper) {
            operand = qf.upperCase(operand);
        } else {
            operand = qf.lowerCase(operand);
        }
        return qf.createQuery(
                qf.selector(testNodeType, "s"),
                qf.and(
                        qf.childNode("s", testRoot),
                        qf.comparison(
                                operand,
                                operator,
                                qf.literal(literal)
                        )
                ), null, null);
    }
}
