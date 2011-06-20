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
package org.apache.jackrabbit.spi.commons.query.sql2;

import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.UnsupportedEncodingException;
import java.util.Random;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.NamespaceException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.QueryResult;
import javax.jcr.query.qom.Column;
import javax.jcr.query.qom.Constraint;
import javax.jcr.query.qom.Ordering;
import javax.jcr.query.qom.QueryObjectModel;
import javax.jcr.query.qom.Source;
import javax.jcr.version.VersionException;
import junit.framework.TestCase;
import org.apache.jackrabbit.commons.query.sql2.Parser;
import org.apache.jackrabbit.commons.query.sql2.QOMFormatter;
import org.apache.jackrabbit.spi.commons.conversion.DefaultNamePathResolver;
import org.apache.jackrabbit.spi.commons.conversion.DummyNamespaceResolver;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.query.qom.QueryObjectModelFactoryImpl;
import org.apache.jackrabbit.spi.commons.query.qom.QueryObjectModelTree;
import org.apache.jackrabbit.spi.commons.value.ValueFactoryQImpl;
import org.apache.jackrabbit.spi.commons.value.QValueFactoryImpl;

/**
 * This class runs function tests on the JCR-SQL2 parser.
 */
public class ParserTest extends TestCase {

    protected org.apache.jackrabbit.commons.query.sql2.Parser parser;

    protected Random random = new Random();

    public static class QOM implements QueryObjectModel {

        protected QueryObjectModelTree qomTree;

        QOM(QueryObjectModelTree qomTree) {
            this.qomTree = qomTree;
        }

        public Source getSource() {
            return qomTree.getSource();
        }

        public Constraint getConstraint() {
            return qomTree.getConstraint();
        }

        public Ordering[] getOrderings() {
            return qomTree.getOrderings();
        }

        public Column[] getColumns() {
            return qomTree.getColumns();
        }

        public void bindValue(String varName, Value value) throws IllegalArgumentException, RepositoryException {
            // ignore
        }

        public QueryResult execute() throws InvalidQueryException, RepositoryException {
            return null;
        }

        public String[] getBindVariableNames() throws RepositoryException {
            return null;
        }

        public String getLanguage() {
            return null;
        }

        public String getStatement() {
            return null;
        }

        public String getStoredQueryPath() throws ItemNotFoundException, RepositoryException {
            return null;
        }

        public void setLimit(long limit) {
            // ignore
        }

        public void setOffset(long offset) {
            // ignore
        }

        public Node storeAsNode(String absPath) throws ItemExistsException, PathNotFoundException, VersionException,
                ConstraintViolationException, LockException, UnsupportedRepositoryOperationException,
                RepositoryException {
            return null;
        }
    }

    static class QOMF extends QueryObjectModelFactoryImpl  {

        public QOMF(NamePathResolver resolver) {
            super(resolver);
        }

        protected QueryObjectModel createQuery(QueryObjectModelTree qomTree) throws InvalidQueryException,
                RepositoryException {
            return new QOM(qomTree);
        }

    }

    protected void setUp() throws Exception {
        super.setUp();
        NamePathResolver resolver = new DefaultNamePathResolver(new DummyNamespaceResolver());
        QueryObjectModelFactoryImpl factory = new QOMF(resolver);
        ValueFactory vf = new ValueFactoryQImpl(QValueFactoryImpl.getInstance(), resolver);
        parser = new Parser(factory, vf);
    }

    private LineNumberReader openScript(String name) {
        try {
            return new LineNumberReader(new InputStreamReader(
                    getClass().getResourceAsStream(name), "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("UTF-8 not supported", e);
        }
    }

    public void testFormatLiterals() throws Exception {
        formatLiteral("true", "true");
        formatLiteral("false", "false");

        formatLiteral("CAST('2000-01-01T12:00:00.000Z' AS DATE)",
                "CAST('2000-01-01T12:00:00.000Z' AS DATE)");
        formatLiteral("CAST(0 AS DATE)",
                "CAST('1970-01-01T00:00:00.000Z' AS DATE)");

        formatLiteral("1", "CAST('1' AS LONG)");
        formatLiteral("-1", "CAST('-1' AS LONG)");
        formatLiteral("CAST(" + Long.MAX_VALUE + " AS LONG)",
                "CAST('" + Long.MAX_VALUE + "' AS LONG)");
        formatLiteral("CAST(" + Long.MIN_VALUE + " AS LONG)",
                "CAST('" + Long.MIN_VALUE + "' AS LONG)");

        formatLiteral("1.0", "CAST('1.0' AS DECIMAL)");
        formatLiteral("-1.0", "CAST('-1.0' AS DECIMAL)");
        formatLiteral("100000000000000000000",
            "CAST('100000000000000000000' AS DECIMAL)");
        formatLiteral("-100000000000000000000",
            "CAST('-100000000000000000000' AS DECIMAL)");

        formatLiteral("CAST(1.0 AS DOUBLE)", "CAST('1.0' AS DOUBLE)");
        formatLiteral("CAST(-1.0 AS DOUBLE)", "CAST('-1.0' AS DOUBLE)");

        formatLiteral("CAST('X' AS NAME)", "CAST('X' AS NAME)");
        formatLiteral("CAST('X' AS PATH)", "CAST('X' AS PATH)");
        formatLiteral("CAST('X' AS REFERENCE)", "CAST('X' AS REFERENCE)");
        formatLiteral("CAST('X' AS WEAKREFERENCE)", "CAST('X' AS WEAKREFERENCE)");
        formatLiteral("CAST('X' AS URI)", "CAST('X' AS URI)");

        formatLiteral("''", "''");
        formatLiteral("' '", "' '");
        formatLiteral("CAST(0 AS STRING)", "'0'");
        formatLiteral("CAST(-1000000000000 AS STRING)", "'-1000000000000'");
        formatLiteral("CAST(false AS STRING)", "'false'");
        formatLiteral("CAST(true AS STRING)", "'true'");
    }

    private void formatLiteral(String literal, String cast) throws Exception {
        String s = "SELECT TEST.* FROM TEST WHERE ID=" + literal;
        QueryObjectModel qom = parser.createQueryObjectModel(s);
        String s2 = QOMFormatter.format(qom);
        String cast2 = s2.substring(s2.indexOf('=') + 1).trim();
        assertEquals(cast, cast2);
        qom = parser.createQueryObjectModel(s);
        s2 = QOMFormatter.format(qom);
        cast2 = s2.substring(s2.indexOf('=') + 1).trim();
        assertEquals(cast, cast2);
    }

    public void testParseScript() throws Exception {
        LineNumberReader reader = openScript("test.sql2.txt");
        while (true) {
            String line = reader.readLine();
            if (line == null) {
                break;
            }
            line = line.trim();
            if (line.length() == 0 || line.startsWith("#")) {
                continue;
            }
            // System.out.println(line);
            String query = line;
            try {
                QueryObjectModel qom = parser.createQueryObjectModel(line);
                String s = QOMFormatter.format(qom);
                qom = parser.createQueryObjectModel(s);
                String s2 = QOMFormatter.format(qom);
                assertEquals(s, s2);
                fuzz(line);
            } catch (Exception e) {
                line = reader.readLine();
                String message = e.getMessage();
                message = message.replace('\n', ' ');
                if (line == null || !line.startsWith("> exception")) {
                    e.printStackTrace();
                    assertTrue("Unexpected exception for query " + query + ": "
                            + e, false);
                }
                assertEquals("Expected exception message: " + message, "> exception: " + message, line);
            }
        }
        reader.close();
    }

    public void fuzz(String query) throws Exception {
        for (int i = 0; i < 100; i++) {
            StringBuffer buff = new StringBuffer(query);
            int changes = 1 + (int) Math.abs(random.nextGaussian() * 2);
            for (int j = 0; j < changes; j++) {
                char newChar;
                if (random.nextBoolean()) {
                    String s = "<>_.+\"*%&/()=?[]{}_:;,.-1234567890.qersdf";
                    newChar = s.charAt(random.nextInt(s.length()));
                } else {
                    newChar = (char) random.nextInt(255);
                }
                int pos = random.nextInt(buff.length());
                if (random.nextBoolean()) {
                    // 50%: change one character
                    buff.setCharAt(pos, newChar);
                } else {
                    if (random.nextBoolean()) {
                        // 25%: delete one character
                        buff.deleteCharAt(pos);
                    } else {
                        // 25%: insert one character
                        buff.insert(pos, newChar);
                    }
                }
            }
            String q = buff.toString();
            try {
                parser.createQueryObjectModel(q);
            } catch (ValueFormatException e) {
                // OK
            } catch (InvalidQueryException e) {
                // OK
            } catch (NamespaceException e) {
                // OK?
            } catch (Throwable t) {
                t.printStackTrace();
                assertTrue("Unexpected exception for query " + q + ": " + t,
                        false);
            }
        }
    }

}
