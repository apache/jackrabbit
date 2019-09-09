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
package org.apache.jackrabbit.jca.test;

import org.apache.jackrabbit.jca.JCAConnectionRequestInfo;

import javax.jcr.SimpleCredentials;

import java.util.HashMap;

import junit.framework.TestCase;

/**
 * This case executes tests on the connection request info.
 */
public final class ConnectionRequestInfoTest
        extends TestCase {

    private SimpleCredentials creds1 = new SimpleCredentials("user", "password".toCharArray());
    private SimpleCredentials creds2 = new SimpleCredentials("user", "password".toCharArray());
    private SimpleCredentials creds3 = new SimpleCredentials("another_user", "password".toCharArray());
    private JCAConnectionRequestInfo info1 = new JCAConnectionRequestInfo(creds1, "default");
    private JCAConnectionRequestInfo info2 = new JCAConnectionRequestInfo(creds2, "default");
    private JCAConnectionRequestInfo info3 = new JCAConnectionRequestInfo(creds3, "default");

    /**
     * Test the JCAConnectionRequestInfo equals() method.
     */
    public void testEquals() throws Exception {
        assertEquals("Object must be equal to itself", info1, info1);
        assertEquals("Infos with the same auth data must be equal", info1, info2);
        assertTrue("Infos with different auth data must not be equal", !info1.equals(info3));
    }

    /**
     * Test the JCAConnectionRequestInfo hashCode() method.
     */
    public void testHashCode() throws Exception {
        assertEquals("Object must be equal to itself", info1.hashCode(), info1.hashCode());
        assertEquals("Infos with the same auth data must have same hashCode", info1.hashCode(), info2.hashCode());
        assertTrue("Infos with different auth data must not have same hashCode", info1.hashCode() != info3.hashCode());
    }

    /**
     * Tests that JCAConnectionRequestInfo works as a HashMap key correctly.
     */
    public void testPutToHashMap() throws Exception {
        HashMap map = new HashMap();
        map.put(info1, new Object());
        assertTrue("Map must contain the info", map.containsKey(info2));
    }
}
