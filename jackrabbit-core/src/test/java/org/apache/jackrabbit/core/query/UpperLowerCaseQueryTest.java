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

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.InvalidQueryException;
import java.util.HashSet;
import java.util.Set;
import java.util.Random;

/**
 * <code>UpperLowerCaseQueryTest</code> tests the functions fn:lower-case() and
 * fn:upper-case() in XPath and LOWER() and UPPER() in SQL.
 */
public class UpperLowerCaseQueryTest extends AbstractQueryTest {

    public void testEqualsGeneralComparison() throws RepositoryException {
        check(new String[]{"foo", "Foo", "fOO", "FOO", "fooBar", "fo", "fooo"},
                "=",
                "foo",
                new boolean[]{true, true, true, true, false, false, false});
        check(new String[]{"foo"}, "=", "", new boolean[]{false});
        check(new String[]{""}, "=", "", new boolean[]{true});
    }

    public void testGreaterThanGeneralComparison() throws RepositoryException {
        // check edges
        check(new String[]{"foo", "FOO", "FoO", "fOo", "FON", "fon", "fo", "FO"},
                ">",
                "foo",
                new boolean[]{false, false, false, false, false, false, false, false});
        check(new String[]{"foo ", "FOOa", "FoOO", "fOo1", "FOp", "foP", "fp", "g", "G"},
                ">",
                "foo",
                new boolean[]{true, true, true, true, true, true, true, true, true});
        // check combinations
        check(new String[]{"foo", "fooo", "FooO", "fo", "FON", "fon"},
                ">",
                "foo",
                new boolean[]{false, true, true, false, false, false});
    }

    public void testLessThanGeneralComparison() throws RepositoryException {
        // check edges
        check(new String[]{"foo", "FOO", "FoO", "fOo", "foOo", "foo ", "fooa", "fop"},
                "<",
                "foo",
                new boolean[]{false, false, false, false, false, false, false, false});
        check(new String[]{"fo", "FOn", "FoN", "fO", "FO1", "fn", "fN", "E", "e"},
                "<",
                "foo",
                new boolean[]{true, true, true, true, true, true, true, true, true});
        // check combinations
        check(new String[]{"foo", "fooo", "FooO", "fo", "FON", "fon"},
                "<",
                "foo",
                new boolean[]{false, false, false, true, true, true});
    }

    public void testGreaterEqualsGeneralComparison() throws RepositoryException {
        // check edges
        check(new String[]{"fo", "FO", "Fon", "fONo", "FON", "fO", "fo", "FO"},
                ">=",
                "foo",
                new boolean[]{false, false, false, false, false, false, false, false});
        check(new String[]{"foo", "FoO", "FoOO", "fOo1", "FOp", "foP", "fp", "g", "G"},
                ">=",
                "foo",
                new boolean[]{true, true, true, true, true, true, true, true, true});
        // check combinations
        check(new String[]{"foo", "fooo", "FOo", "fo", "FON", "fon"},
                ">=",
                "foo",
                new boolean[]{true, true, true, false, false, false});
    }

    public void testLessEqualsGeneralComparison() throws RepositoryException {
        // check edges
        check(new String[]{"fooo", "FOoo", "Fop", "fOpo", "FOP", "fOo ", "fp", "G"},
                "<=",
                "foo",
                new boolean[]{false, false, false, false, false, false, false, false});
        check(new String[]{"foo", "FoO", "Foo", "fOn", "FO", "fo", "f", "E", "e"},
                "<=",
                "foo",
                new boolean[]{true, true, true, true, true, true, true, true, true});
        // check combinations
        check(new String[]{"foo", "fo", "FOo", "fop", "FOP", "fooo"},
                "<=",
                "foo",
                new boolean[]{true, true, true, false, false, false});
    }

    public void testNotEqualsGeneralComparison() throws RepositoryException {
        // check edges
        check(new String[]{"fooo", "FOoo", "Fop", "fOpo", "FOP", "fOo ", "fp", "G", ""},
                "!=",
                "foo",
                new boolean[]{true, true, true, true, true, true, true, true, true});
        check(new String[]{"foo", "FoO", "Foo", "foO", "FOO"},
                "!=",
                "foo",
                new boolean[]{false, false, false, false, false});
        // check combinations
        check(new String[]{"foo", "fo", "FOo", "fop", "FOP", "fooo"},
                "!=",
                "foo",
                new boolean[]{false, true, false, true, true, true});
    }

    public void testLikeComparison() throws RepositoryException {
        check(new String[]{"foo", "Foo", "fOO", "FO "},
                "like",
                "fo_",
                new boolean[]{true, true, true, true});
        check(new String[]{"foo", "Foo", "fOO", "FOO"},
                "like",
                "f_o",
                new boolean[]{true, true, true, true});
        check(new String[]{"foo", "Foo", "fOO", " OO"},
                "like",
                "_oo",
                new boolean[]{true, true, true, true});
        check(new String[]{"foo", "Foa", "fOO", "FO", "foRm", "fPo", "fno", "FPo", "Fno"},
                "like",
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
            check(values, "like", pattern, matches);
        }
    }

    public void testRangeWithEmptyString() throws RepositoryException {
        check(new String[]{" ", "a", "A", "1", "3", "!", "@"},
                ">",
                "",
                new boolean[]{true, true, true, true, true, true, true});
        check(new String[]{"", "a", "A", "1", "3", "!", "@"},
                ">=",
                "",
                new boolean[]{true, true, true, true, true, true, true});
        check(new String[]{"", "a", "A", "1", "3", "!", "@"},
                "<",
                "",
                new boolean[]{false, false, false, false, false, false, false});
        check(new String[]{"", "a", "A", "1", "3", "!", "@"},
                "<=",
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

    private void check(String[] values, String operation, String queryTerm, boolean[] matches)
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
        log.write(logMsg.toString());
        log.flush();
        for (NodeIterator it = testRootNode.getNodes(); it.hasNext();) {
            it.nextNode().remove();
        }
        Set matchingNodes = new HashSet();
        for (int i = 0; i < values.length; i++) {
            Node n = testRootNode.addNode("node" + i);
            n.setProperty(propertyName1, values[i]);
            if (matches[i]) {
                matchingNodes.add(n);
            }
        }
        testRootNode.save();

        Node[] nodes = (Node[]) matchingNodes.toArray(new Node[matchingNodes.size()]);
        String sqlOperation = operation;
        if (operation.equals("!=")) {
            sqlOperation = "<>";
        }

        // run queries with lower-case
        String xpath = testPath;
        if (operation.equals("like")) {
            xpath += "/*[jcr:like(fn:lower-case(@" + propertyName1 +
                    "), '" + queryTerm.toLowerCase() + "')]";
        } else {
            xpath += "/*[fn:lower-case(@" + propertyName1 +
                    ") " + operation + " '" + queryTerm.toLowerCase() + "']";
        }
        executeXPathQuery(xpath, nodes);

        String sql = "select * from nt:base where jcr:path like '" +
                testRoot + "/%' and LOWER(" + propertyName1 + ") " +
                sqlOperation + " '" + queryTerm.toLowerCase() + "'";
        executeSQLQuery(sql, nodes);

        // run queries with upper-case
        xpath = testPath;
        if (operation.equals("like")) {
            xpath += "/*[jcr:like(fn:upper-case(@" + propertyName1 +
                    "), '" + queryTerm.toUpperCase() + "')]";
        } else {
            xpath += "/*[fn:upper-case(@" + propertyName1 +
                    ") " + operation + " '" + queryTerm.toUpperCase() + "']";
        }
        executeXPathQuery(xpath, nodes);

        sql = "select * from nt:base where jcr:path like '" +
                testRoot + "/%' and UPPER(" + propertyName1 + ") " +
                sqlOperation + " '" + queryTerm.toUpperCase() + "'";
        executeSQLQuery(sql, nodes);
    }

    private char getRandomChar(String pool, Random random) {
        return pool.charAt(random.nextInt(pool.length()));
    }
}
