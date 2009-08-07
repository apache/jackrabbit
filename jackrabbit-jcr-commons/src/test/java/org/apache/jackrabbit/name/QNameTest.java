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
package org.apache.jackrabbit.name;

import junit.framework.TestCase;

import java.util.ArrayList;

/**
 * This Class implements a test case for the 'Path' class.
 *
 * Actually, this should be below the {@link org.apache.jackrabbit.test} package,
 * but it needs package protected methods of that class.
 */
public class QNameTest extends TestCase {

    private final NamespaceResolver resolver;

    private Test[] tests;

    private static int NUM_TESTS = 1;

    public QNameTest() {

        // create dummy namespace resolver
        resolver = new AbstractNamespaceResolver(){
            public String getURI(String prefix) {
                return prefix;
            }

            public String getPrefix(String uri) {
                return uri;
            }
        };

        // create tests
        ArrayList list = new ArrayList();

        // valid names
        list.add(new Test("name", "", "name"));
        list.add(new Test("prefix:name", "prefix", "name"));
        list.add(new Test("prefix:na me", "prefix", "na me"));

        // invalid names
        list.add(new Test(":name"));
        list.add(new Test("."));
        list.add(new Test(".."));
        list.add(new Test("pre:"));
        list.add(new Test(""));
        list.add(new Test(" name"));
        list.add(new Test(" prefix: name"));
        list.add(new Test("prefix: name"));
        list.add(new Test("prefix:name "));
        list.add(new Test("pre fix:name"));
        list.add(new Test("prefix :name"));
        list.add(new Test("name/name"));
        list.add(new Test("name[name"));
        list.add(new Test("name]name"));
        list.add(new Test("name*name"));
        list.add(new Test("prefix:name:name"));

        tests = (Test[]) list.toArray(new Test[list.size()]);
    }

    public void testCreate() throws Exception {
        for (int i=0; i<tests.length; i++) {
            Test t = tests[i];
            long t1 = System.currentTimeMillis();
            for (int j=0; j<NUM_TESTS; j++) {
                try {
                    QName n = NameFormat.parse(t.jcrName, resolver);
                    if (!t.isValid()) {
                        fail("Should throw IllegalNameException: " + t.jcrName);
                    }
                    assertEquals("\"" + t.jcrName + "\".uri", t.prefix, n.getNamespaceURI());
                    assertEquals("\"" + t.jcrName + "\".localName", t.name, n.getLocalName());
                } catch (IllegalNameException e) {
                    if (t.isValid()) {
                        throw e;
                    }
                }
            }
            long t2 = System.currentTimeMillis();
            if (NUM_TESTS>1) {
                System.out.println("testCreate():\t" + t + "\t" + (t2-t1) + "\tms");
            }
        }
    }

    public void testCheckFormat() throws Exception {
        for (int i=0; i<tests.length; i++) {
            Test t = tests[i];
            long t1 = System.currentTimeMillis();
            for (int j=0; j<NUM_TESTS; j++) {
                // check just creation
                boolean isValid = true;
                try {
                    NameFormat.checkFormat(t.jcrName);
                } catch (IllegalNameException e) {
                    isValid = false;
                }
                assertEquals("\"" + t.jcrName + "\".checkFormat()", t.isValid(),  isValid);
            }
            long t2 = System.currentTimeMillis();
            if (NUM_TESTS>1) {
                System.out.println("testCheckFormat():\t" + t + "\t" + (t2-t1) + "\tms");
            }
        }
    }

    private static class Test {

        private final String jcrName;

        private final String prefix;

        private final String name;

        public Test(String jcrName) {
            this(jcrName, null, null);
        }

        public Test(String jcrName, String prefix, String name) {
            this.jcrName = jcrName;
            this.prefix = prefix;
            this.name = name;
        }

        public boolean isValid() {
            return name!=null;
        }

        public String toString() {
            StringBuffer b = new StringBuffer(jcrName);
            if (isValid()) {
                b.append(",VAL");
            }
            return b.toString();
        }
    }
}
