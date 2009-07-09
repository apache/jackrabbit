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
import javax.jcr.NamespaceException;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFactory;
import javax.jcr.ValueFormatException;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.qom.QueryObjectModel;
import junit.framework.TestCase;
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

    protected Parser parser;

    protected Random random = new Random();

    static class QOMF extends QueryObjectModelFactoryImpl  {

        public QOMF(NamePathResolver resolver) {
            super(resolver);
        }

        protected QueryObjectModel createQuery(QueryObjectModelTree qomTree) throws InvalidQueryException,
                RepositoryException {
            return null;
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
                parser.createQueryObjectModel(line);
                fuzz(line);
            } catch (Exception e) {
                line = reader.readLine();
                if (line == null || !line.startsWith("> exception")) {
                    e.printStackTrace();
                    assertTrue("Unexpected exception for query " + query + ": "
                            + e, false);
                }
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
