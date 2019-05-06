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
package org.apache.jackrabbit.value;

import junit.framework.TestCase;

/**
 * Test cases for binary values.
 */
public class BinaryValueTest extends TestCase {

    private static final byte[] DATA = "abc".getBytes();

    public void testBinaryValueEquals() throws Exception {
        assertFalse(new BinaryValue(DATA).equals(null));
        assertFalse(new BinaryValue(DATA).equals(new Object()));

        // TODO: JCR-320 Binary value equality
        // assertTrue(new BinaryValue(DATA).equals(new BinaryValue(DATA)));
        // assertTrue(new BinaryValue(DATA).equals(
        //         new BinaryValue(new ByteArrayInputStream(DATA))));
        // assertTrue(new BinaryValue(new ByteArrayInputStream(DATA)).equals(
        //         new BinaryValue(DATA)));
        // assertTrue(new BinaryValue(DATA).equals(
        //         new BinaryValue(new String(DATA))));
        // assertTrue(new BinaryValue(new String(DATA)).equals(
        //         new BinaryValue(DATA)));
    }

}
