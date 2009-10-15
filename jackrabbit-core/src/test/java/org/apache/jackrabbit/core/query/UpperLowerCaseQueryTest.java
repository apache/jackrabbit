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
package org.apache.jackrabbit.core.query;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.QueryResult;

import org.apache.jackrabbit.commons.query.qom.Operator;

/**
 * <code>UpperLowerCaseQueryTest</code> tests the functions fn:lower-case() and
 * fn:upper-case() in XPath, LOWER() and UPPER() in SQL and UpperCase and
 * LowerCase in JQOM.
 */
public class UpperLowerCaseQueryTest extends AbstractQueryTest {

    public void testEqualsGeneralComparison() throws RepositoryException {
        check(new String[]{"foo", "Foo", "fOO", "FOO", "fooBar", "fo", "fooo"},
                Operator.EQ,
                "foo",
                new boolean[]{true, true, true, true, false, false, false});
        check(new String[]{"foo"}, Operator.EQ, "", new boolean[]{false});
        check(new String[]{""}, Operator.EQ, "", new boolean[]{true});
    }

    public void testGreaterThanGeneralComparison() throws RepositoryException {
        // check edges
        check(new String[]{"foo", "FOO", "FoO", "fOo", "FON", "fon", "fo", "FO"},
                Operator.GT,
                "foo",
                new boolean[]{false, false, false, false, false, false, false, false});
        check(new String[]{"foo ", "FOOa", "FoOO", "fOo1", "FOp", "foP", "fp", "g", "G"},
                Operator.GT,
                "foo",
                new boolean[]{true, true, true, true, true, true, true, true, true});
        // check combinations
        check(new String[]{"foo", "fooo", "FooO", "fo", "FON", "fon"},
                Operator.GT,
                "foo",
                new boolean[]{false, true, true, false, false, false});
    }

    public void testLessThanGeneralComparison() throws RepositoryException {
        // check edges
        check(new String[]{"foo", "FOO", "FoO", "fOo", "foOo", "foo ", "fooa", "fop"},
                Operator.LT,
                "foo",
                new boolean[]{false, false, false, false, false, false, false, false});
        check(new String[]{"fo", "FOn", "FoN", "fO", "FO1", "fn", "fN", "E", "e"},
                Operator.LT,
                "foo",
                new boolean[]{true, true, true, true, true, true, true, true, true});
        // check combinations
        check(new String[]{"foo", "fooo", "FooO", "fo", "FON", "fon"},
                Operator.LT,
                "foo",
                new boolean[]{false, false, false, true, true, true});
    }

    public void testGreaterEqualsGeneralComparison() throws RepositoryException {
        // check edges
        check(new String[]{"fo", "FO", "Fon", "fONo", "FON", "fO", "fo", "FO"},
                Operator.GE,
                "foo",
                new boolean[]{false, false, false, false, false, false, false, false});
        check(new String[]{"foo", "FoO", "FoOO", "fOo1", "FOp", "foP", "fp", "g", "G"},
                Operator.GE,
                "foo",
                new boolean[]{true, true, true, true, true, true, true, true, true});
        // check combinations
        check(new String[]{"foo", "fooo", "FOo", "fo", "FON", "fon"},
                Operator.GE,
                "foo",
                new boolean[]{true, true, true, false, false, false});
    }

    public void testLessEqualsGeneralComparison() throws RepositoryException {
        // check edges
        check(new String[]{"fooo", "FOoo", "Fop", "fOpo", "FOP", "fOo ", "fp", "G"},
                Operator.LE,
                "foo",
                new boolean[]{false, false, false, false, false, false, false, false});
        check(new String[]{"foo", "FoO", "Foo", "fOn", "FO", "fo", "f", "E", "e"},
                Operator.LE,
                "foo",
                new boolean[]{true, true, true, true, true, true, true, true, true});
        // check combinations
        check(new String[]{"foo", "fo", "FOo", "fop", "FOP", "fooo"},
                Operator.LE,
                "foo",
                new boolean[]{true, true, true, false, false, false});
    }

    public void testNotEqualsGeneralComparison() throws RepositoryException {
        // check edges
        check(new String[]{"fooo", "FOoo", "Fop", "fOpo", "FOP", "fOo ", "fp", "G", ""},
                Operator.NE,
                "foo",
                new boolean[]{true, true, true, true, true, true, true, true, true});
        check(new String[]{"foo", "FoO", "Foo", "foO", "FOO"},
                Operator.NE,
                "foo",
                new boolean[]{false, false, false, false, false});
        // check combinations
        check(new String[]{"foo", "fo", "FOo", "fop", "FOP", "fooo"},
                Operator.NE,
                "foo",
                new boolean[]{false, true, false, true, true, true});
    }

    public void testLikeComparison() throws RepositoryException {
        check(new String[]{"foo", "Foo", "fOO", "FO "},
                Operator.LIKE,
                "fo_",
                new boolean[]{true, true, true, true});
        check(new String[]{"foo", "Foo", "fOO", "FOO"},
                Operator.LIKE,
                "f_o",
                new boolean[]{true, true, true, true});
        check(new String[]{"foo", "Foo", "fOO", " OO"},
                Operator.LIKE,
                "_oo",
                new boolean[]{true, true, true, true});
        check(new String[]{"foo", "Foa", "fOO", "FO", "foRm", "fPo", "fno", "FPo", "Fno"},
                Operator.LIKE,
                "fo%",
                new boolean[]{true, true, true, true, true, false, false, false, false});
    }

    public void testLikeComparisonRandom() throws RepositoryException {
        String abcd = "abcd";
        Random random = new Random();
        for (int i = 0; i < 50; i++) {
            String pattern = "";
            pattern += getRandomChar(abcd, random);
            pattern += getRandomChar(abcd, random);

            // create 10 random values with 4 characters
            String[] values = new String[10];
            boolean[] matches = new boolean[10];
            for (int n = 0; n < 10; n++) {
                // at least the first character always matches
                String value = String.valueOf(pattern.charAt(0));
                for (int r = 1; r < 4; r++) {
                    char c = getRandomChar(abcd, random);
                    if (random.nextBoolean()) {
                        c = Character.toUpperCase(c);
                    }
                    value += c;
                }
                matches[n] = value.toLowerCase().startsWith(pattern);
                values[n] = value;
            }
            pattern += "%";
            check(values, Operator.LIKE, pattern, matches);
        }
    }

    public void testRangeWithEmptyString() throws RepositoryException {
        check(new String[]{" ", "a", "A", "1", "3", "!", "@"},
                Operator.GT,
                "",
                new boolean[]{true, true, true, true, true, true, true});
        check(new String[]{"", "a", "A", "1", "3", "!", "@"},
                Operator.GE,
                "",
                new boolean[]{true, true, true, true, true, true, true});
        check(new String[]{"", "a", "A", "1", "3", "!", "@"},
                Operator.LT,
                "",
                new boolean[]{false, false, false, false, false, false, false});
        check(new String[]{"", "a", "A", "1", "3", "!", "@"},
                Operator.LE,
                "",
                new boolean[]{true, false, false, false, false, false, false});
    }

    public void testInvalidQuery() throws RepositoryException {
        try {
            executeXPathQuery("//*[fn:lower-case(@foo) = 123]", new Node[]{});
            fail("must throw InvalidQueryException");
        } catch (InvalidQueryException e) {
            // correct
        }

        try {
            executeSQLQuery("select * from nt:base where LOWER(foo) = 123", new Node[]{});
            fail("must throw InvalidQueryException");
        } catch (InvalidQueryException e) {
            // correct
        }
    }

    public void testWrongCaseNeverMatches() throws RepositoryException {
        Node n = testRootNode.addNode("node");
        n.setProperty("foo", "Bar");
        testRootNode.save();
        executeXPathQuery(testPath + "/*[jcr:like(fn:lower-case(@foo), 'BA%')]", new Node[]{});
    }

    //----------------------------< internal >----------------------------------

    private void check(String[] values, Operator operator, String queryTerm, boolean[] matches)
            throws RepositoryException {
        if (values.length != matches.length) {
            throw new IllegalArgumentException("values and matches must have same length");
        }
        // create log message
        StringBuffer logMsg = new StringBuffer();
        logMsg.append("queryTerm: ").append(queryTerm);
        logMsg.append(" values: ");
        String separator = "";
        for (int i = 0; i < values.length; i++) {
            logMsg.append(separator);
            separator = ", ";
            if (matches[i]) {
                logMsg.append("+");
            } else {
                logMsg.append("-");
            }
            logMsg.append(values[i]);
        }
        log.println(logMsg.toString());
        for (NodeIterator it = testRootNode.getNodes(); it.hasNext();) {
            it.nextNode().remove();
        }
        Set<Node> matchingNodes = new HashSet<Node>();
        for (int i = 0; i < values.length; i++) {
            Node n = testRootNode.addNode("node" + i);
            n.setProperty(propertyName1, values[i]);
            if (matches[i]) {
                matchingNodes.add(n);
            }
        }
        testRootNode.save();

        Node[] nodes = matchingNodes.toArray(new Node[matchingNodes.size()]);

        // run queries with lower-case
        String xpath = operator.formatXpath(
                "fn:lower-case(@" + propertyName1 + ")",
                "'" + queryTerm.toLowerCase() + "'");
        executeXPathQuery(testPath + "/*[" + xpath + "]", nodes);

        String sql = "select * from nt:base where "
            + "jcr:path like '" + testRoot + "/%' and "
            + operator.formatSql(
                    "LOWER(" + propertyName1 + ")",
                    "'" + queryTerm.toLowerCase() + "'");
        executeSQLQuery(sql, nodes);

        QueryResult result = qomFactory.createQuery(
                qomFactory.selector(testNodeType, "s"),
                qomFactory.and(
                        qomFactory.childNode("s", testRoot),
                        qomFactory.comparison(
                                qomFactory.lowerCase(
                                        qomFactory.propertyValue("s", propertyName1)),
                                operator.toString(),
                                qomFactory.literal(
                                        superuser.getValueFactory().createValue(
                                                queryTerm.toLowerCase()))
                        )
                ), null, null).execute();
        checkResult(result, nodes);

        // run queries with upper-case
        xpath = operator.formatXpath(
                "fn:upper-case(@" + propertyName1 + ")",
                "'" + queryTerm.toUpperCase() + "'");
        executeXPathQuery(testPath + "/*[" + xpath + "]", nodes);

        sql = "select * from nt:base where "
            + "jcr:path like '" + testRoot + "/%' and "
            + operator.formatSql(
                    "UPPER(" + propertyName1 + ")",
                    "'" + queryTerm.toUpperCase() + "'");
        executeSQLQuery(sql, nodes);

        result = qomFactory.createQuery(
                qomFactory.selector(testNodeType, "s"),
                qomFactory.and(
                        qomFactory.childNode("s", testRoot),
                        operator.comparison(
                                qomFactory,
                                qomFactory.upperCase(
                                        qomFactory.propertyValue("s", propertyName1)),
                                qomFactory.literal(
                                        superuser.getValueFactory().createValue(
                                                queryTerm.toUpperCase()))
                        )
                ), null, null).execute();
        checkResult(result, nodes);
    }

    private char getRandomChar(String pool, Random random) {
        return pool.charAt(random.nextInt(pool.length()));
    }

}
