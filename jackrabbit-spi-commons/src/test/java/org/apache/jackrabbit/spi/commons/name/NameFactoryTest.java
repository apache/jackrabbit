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
package org.apache.jackrabbit.spi.commons.name;

import org.apache.jackrabbit.spi.NameFactory;
import org.apache.jackrabbit.spi.Name;
import junit.framework.TestCase;

/**
 * This Class implements a test case for the 'NameFactoryImpl' class.
 */
public class NameFactoryTest extends TestCase {

    private final NameFactory factory;
    private JcrName[] tests = JcrName.getTests();

    public NameFactoryTest() {
        factory = NameFactoryImpl.getInstance();
        tests = JcrName.getTests();
    }

    public void testEquality() throws Exception {
        for (int i=0; i<tests.length; i++) {
            final JcrName t = tests[i];
            if (t.isValid()) {
                Name n1 = new Name() {
                    public String getLocalName() {
                        return t.jcrName;
                    }

                    public String getNamespaceURI() {
                        return t.prefix;
                    }

                    public int compareTo(Object o) {
                        throw new UnsupportedOperationException();
                    }
                };

                Name n2 = factory.create(t.prefix, t.jcrName);
                assertTrue(n2.equals(n1));
            }
        }
    }

    public void testCreationFromString() throws Exception {
        for (int i=0; i<tests.length; i++) {
            final JcrName t = tests[i];
            if (t.isValid()) {
                Name n1 = factory.create(t.prefix, t.jcrName);
                Name n2 = factory.create(n1.toString());

                assertTrue(n1.getLocalName().equals(n2.getLocalName()));
                assertTrue(n1.getNamespaceURI().equals(n2.getNamespaceURI()));
                assertTrue(n2.equals(n1));
            }
        }
    }

    public void testCreationFromOtherString() throws Exception {
        for (int i=0; i<tests.length; i++) {
            final JcrName t = tests[i];
            if (t.isValid()) {
                Name n1 = new Name() {
                    public String getLocalName() {
                        return t.jcrName;
                    }

                    public String getNamespaceURI() {
                        return t.prefix;
                    }

                    public int compareTo(Object o) {
                        throw new UnsupportedOperationException();
                    }

                    public String toString() {
                        return "{" + t.prefix + "}" + t.jcrName;
                    }

                    public boolean equals(Object obj) {
                        if (obj instanceof Name) {
                            Name n = (Name) obj;
                            return n.getLocalName().equals(t.jcrName) && n.getNamespaceURI().equals(t.prefix);
                        }
                        return false;
                    }
                };

                Name n2 = factory.create(n1.toString());

                assertTrue(n1.getLocalName().equals(n2.getLocalName()));
                assertTrue(n1.getNamespaceURI().equals(n2.getNamespaceURI()));
                assertTrue(n2.equals(n1));
            }
        }
    }
}
