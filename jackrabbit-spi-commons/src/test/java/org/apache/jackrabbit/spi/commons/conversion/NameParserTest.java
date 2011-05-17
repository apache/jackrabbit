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
package org.apache.jackrabbit.spi.commons.conversion;

import org.apache.jackrabbit.spi.commons.namespace.NamespaceResolver;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.NameFactory;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.apache.jackrabbit.spi.commons.name.JcrName;
import junit.framework.TestCase;

import javax.jcr.NamespaceException;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * <code>NameParserTest</code>...
 */
public class NameParserTest extends TestCase {

    private final NameFactory factory = NameFactoryImpl.getInstance();
    private final NamespaceResolver resolver = new DummyNamespaceResolver();

    private JcrName[] tests = JcrName.getTests();

    private static int NUM_TESTS = 1;


    public void testParse() throws Exception {
        for (JcrName t : tests) {
            long t1 = System.currentTimeMillis();
            for (int j = 0; j < NUM_TESTS; j++) {
                try {
                    Name n = NameParser.parse(t.jcrName, resolver, factory);
                    if (!t.isValid()) {
                        fail("Should throw IllegalNameException: " + t.jcrName);
                    }
                    assertEquals("\"" + t.jcrName + "\".uri", t.prefix, n.getNamespaceURI());
                    assertEquals("\"" + t.jcrName + "\".localName", t.name, n.getLocalName());
                } catch (IllegalNameException e) {
                    if (t.isValid()) {
                        throw e;
                    }
                } catch (NamespaceException e) {
                    if (t.isValid()) {
                        throw e;
                    }
                }
            }
            long t2 = System.currentTimeMillis();
            if (NUM_TESTS > 1) {
                System.out.println("testCreate():\t" + t + "\t" + (t2 - t1) + "\tms");
            }
        }
    }

    public void testCheckFormat() throws Exception {
        for (JcrName t : tests) {
            long t1 = System.currentTimeMillis();
            for (int j = 0; j < NUM_TESTS; j++) {
                // check just creation
                boolean isValid = true;
                try {
                    NameParser.checkFormat(t.jcrName);
                } catch (IllegalNameException e) {
                    isValid = false;
                }
                assertEquals("\"" + t.jcrName + "\".checkFormat()", t.isValid(), isValid);
            }
            long t2 = System.currentTimeMillis();
            if (NUM_TESTS > 1) {
                System.out.println("testCheckFormat():\t" + t + "\t" + (t2 - t1) + "\tms");
            }
        }
    }

    public void testExpandedJcrNames() throws NamespaceException, IllegalNameException {
        NamespaceResolver resolver = new TestNamespaceResolver();

        List<String[]> valid = new ArrayList<String[]>();
        // valid qualified jcr-names:
        // String-array consisting of { jcrName , uri , localName }
        valid.add(new String[] {"abc:{c}", "abc", "{c}"});
        valid.add(new String[] {"abc:}c", "abc", "}c"});
        valid.add(new String[] {"abc:c}", "abc", "c}"});
        valid.add(new String[] {"{ab", "", "{ab"});
        valid.add(new String[] {"ab}", "", "ab}"});
        valid.add(new String[] {"a}bc", "", "a}bc"});
        valid.add(new String[] {"{", "", "{"});
        valid.add(new String[] {"}", "", "}"});
        valid.add(new String[] {"abc", "", "abc"});
        valid.add(new String[] {"abc{abc}", "", "abc{abc}"});
        valid.add(new String[] {"{{abc}", "", "{{abc}"});
        valid.add(new String[] {"{abc{abc}", "", "{abc{abc}"});
        valid.add(new String[] {"abc {", "", "abc {"});
        valid.add(new String[] {"abc { }", "", "abc { }"});
        valid.add(new String[] {"{ ab }", "", "{ ab }"});
        valid.add(new String[] {"{ }abc", "", "{ }abc"});
        // unknown uri -> but valid non-prefixed jcr-name
        valid.add(new String[] {"{test}abc", "", "{test}abc"});
        valid.add(new String[] {"{ab}", "", "{ab}"});
        valid.add(new String[] {".{.}", "", ".{.}"});

        // valid expanded jcr-names:
        // String-array consisting of { jcrName , uri , localName }
        valid.add(new String[] {"{http://jackrabbit.apache.org}abc", "http://jackrabbit.apache.org", "abc"});
        valid.add(new String[] {"{http://jackrabbit.apache.org:80}abc", "http://jackrabbit.apache.org:80", "abc"});
        valid.add(new String[] {"{http://jackrabbit.apache.org/info}abc", "http://jackrabbit.apache.org/info", "abc"});
        valid.add(new String[] {"{jcr:jackrabbit}abc", "jcr:jackrabbit", "abc"});
        valid.add(new String[] {"{abc:}def", "abc:", "def"});
        valid.add(new String[] {"{}abc", "", "abc"});

        for (Object aValid : valid) {
            String[] strs = (String[]) aValid;
            try {
                Name n = NameParser.parse(strs[0], resolver, factory);
                assertEquals("URI mismatch", strs[1], n.getNamespaceURI());
                assertEquals("Local name mismatch", strs[2], n.getLocalName());
            } catch (Exception e) {
                fail(e.getMessage() + " -> " + strs[0]);
            }
        }

        // invalid jcr-names (neither expanded nor qualified form)
        List<String> invalid = new ArrayList<String>();
        // invalid prefix
        invalid.add("{a:b");
        invalid.add("}a:b");
        invalid.add("a{b:c");
        invalid.add("a}b:c");
        // unknown uri -> but invalid local name with ':' and or '/'
        invalid.add("{http//test.apache.org}abc");
        invalid.add("{test/test/test}abc");
        // invalid local name containing '/'
        invalid.add("{http://jackrabbit.apache.org}abc/dfg");
        // invalid local name containing ':'
        invalid.add("{http://jackrabbit.apache.org}abc:dfg");
        // invalid local name containing ':' and '/'
        invalid.add("{{http://jackrabbit.apache.org}abc:dfg}");
        // invalid local name containing '/'
        invalid.add("/a/b/c");
        // known uri but local name missing -> must fail.
        invalid.add("{http://jackrabbit.apache.org}");
        invalid.add("{}");

        for (Object anInvalid : invalid) {
            String jcrName = (String) anInvalid;
            try {
                NameParser.parse(jcrName, resolver, factory);
                fail("Parsing '" + jcrName + "' should fail. Not a valid jcr name.");
            } catch (IllegalNameException e) {
                //ok
            }
        }
    }

    public void testCheckFormatOfExpandedNames() throws NamespaceException, IllegalNameException {
        List<String[]> valid = new ArrayList<String[]>();
        // valid qualified jcr-names:
        // String-array consisting of { jcrName , uri , localName }
        valid.add(new String[] {"abc:{c}", "abc", "{c}"});
        valid.add(new String[] {"abc:}c", "abc", "}c"});
        valid.add(new String[] {"abc:c}", "abc", "c}"});
        valid.add(new String[] {"{ab", "", "{ab"});
        valid.add(new String[] {"ab}", "", "ab}"});
        valid.add(new String[] {"a}bc", "", "a}bc"});
        valid.add(new String[] {"{", "", "{"});
        valid.add(new String[] {"}", "", "}"});
        valid.add(new String[] {"abc", "", "abc"});
        valid.add(new String[] {"abc{abc}", "", "abc{abc}"});
        valid.add(new String[] {"{{abc}", "", "{{abc}"});
        valid.add(new String[] {"{abc{abc}", "", "{abc{abc}"});
        valid.add(new String[] {"abc {", "", "abc {"});
        valid.add(new String[] {"abc { }", "", "abc { }"});
        valid.add(new String[] {"{ }abc", "", "{ }abc"});
        // unknown uri -> but valid non-prefixed jcr-name
        valid.add(new String[] {"{test}abc", "", "{test}abc"});

        // valid expanded jcr-names:
        // String-array consisting of { jcrName , uri , localName }
        valid.add(new String[] {"{http://jackrabbit.apache.org}abc", "http://jackrabbit.apache.org", "abc"});
        valid.add(new String[] {"{http://jackrabbit.apache.org:80}abc", "http://jackrabbit.apache.org:80", "abc"});
        valid.add(new String[] {"{http://jackrabbit.apache.org/info}abc", "http://jackrabbit.apache.org/info", "abc"});
        valid.add(new String[] {"{jcr:jackrabbit}abc", "jcr:jackrabbit", "abc"});
        valid.add(new String[] {"{abc}def", "abc", "def"});
        valid.add(new String[] {"{}abc", "", "abc"});

        for (Object aValid : valid) {
            String[] strs = (String[]) aValid;
            try {
                NameParser.checkFormat(strs[0]);
            } catch (Exception e) {
                fail(e.getMessage() + " -> " + strs[0]);
            }
        }

        // invalid jcr-names (neither expanded nor qualified form)
        List<String> invalid = new ArrayList<String>();
        // invalid prefix
        invalid.add("{a:b");
        invalid.add("}a:b");
        invalid.add("a{b:c");
        invalid.add("a}b:c");
        // invalid local name containing '/'
        invalid.add("{http://jackrabbit.apache.org}abc/dfg");
        // invalid local name containing ':'
        invalid.add("{http://jackrabbit.apache.org}abc:dfg");
        // invalid local name containing ':' and '/'
        invalid.add("{{http://jackrabbit.apache.org}abc:dfg}");
        // invalid local name containing '/'
        invalid.add("/a/b/c");
        // known uri but local name missing -> must fail.
        invalid.add("{http://jackrabbit.apache.org}");
        invalid.add("{}");
        // invalid URI part
        invalid.add("{/jackrabbit/a/b/c}abc");


        for (Object anInvalid : invalid) {
            String jcrName = (String) anInvalid;
            try {
                NameParser.checkFormat(jcrName);
                fail("Checking format of '" + jcrName + "' should fail. Not a valid jcr name.");
            } catch (IllegalNameException e) {
                //ok
            }
        }
    }
    
    /**
     * Dummy NamespaceResolver that only knows the empty namespace and
     * namespaces containing either 'jackrabbit' or 'abc'. Used to test
     * the parsing of the expanded jcr names, which should treat a jcr name with
     * unknown namespace uri qualified jcr names.
     */
    private class TestNamespaceResolver implements NamespaceResolver {

        public String getURI(String prefix) throws NamespaceException {
            if (prefix.length() == 0 ||
                prefix.indexOf("jackrabbit") > -1 ||
                prefix.indexOf("abc") > -1 ){
                return prefix;
            } else {
                throw new NamespaceException("Unknown namespace prefix " + prefix);
            }
        }

        public String getPrefix(String uri) throws NamespaceException {
            if (uri.length() == 0 ||
                uri.indexOf("jackrabbit") > -1 ||
                uri.indexOf("abc") > -1 ){
                return uri;
            } else {
                throw new NamespaceException("Unknown namespace prefix " + uri);
            }
        }
    }
}
