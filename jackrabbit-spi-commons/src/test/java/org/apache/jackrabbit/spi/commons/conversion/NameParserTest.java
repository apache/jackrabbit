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

/**
 * <code>NameParserTest</code>...
 */
public class NameParserTest extends TestCase {

    private final NameFactory factory = NameFactoryImpl.getInstance();
    private final NamespaceResolver resolver = new DummyNamespaceResolver();

    private JcrName[] tests = JcrName.getTests();

    private static int NUM_TESTS = 1;


    public void testParse() throws Exception {
        for (int i=0; i<tests.length; i++) {
            JcrName t = tests[i];
            long t1 = System.currentTimeMillis();
            for (int j=0; j<NUM_TESTS; j++) {
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
            if (NUM_TESTS>1) {
                System.out.println("testCreate():\t" + t + "\t" + (t2-t1) + "\tms");
            }
        }
    }

    public void testCheckFormat() throws Exception {
        for (int i=0; i<tests.length; i++) {
            JcrName t = tests[i];
            long t1 = System.currentTimeMillis();
            for (int j=0; j<NUM_TESTS; j++) {
                // check just creation
                boolean isValid = true;
                try {
                    NameParser.checkFormat(t.jcrName);
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
}