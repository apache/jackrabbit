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

package org.apache.jackrabbit.core.security.user;

import org.apache.jackrabbit.core.security.SecurityConstants;
import org.apache.jackrabbit.test.JUnitTest;

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <code>PasswordUtilityTest</code>...
 */
public class PasswordUtilityTest extends JUnitTest {

    private static List<String> PLAIN_PWDS = new ArrayList<String>();
    static {
        PLAIN_PWDS.add("pw");
        PLAIN_PWDS.add("PassWord123");
        PLAIN_PWDS.add("_");
        PLAIN_PWDS.add("{invalidAlgo}");
        PLAIN_PWDS.add("{invalidAlgo}Password");
        PLAIN_PWDS.add("{SHA-256}");
        PLAIN_PWDS.add("pw{SHA-256}");
        PLAIN_PWDS.add("p{SHA-256}w");
        PLAIN_PWDS.add("");
    }

    private static Map<String, String> HASHED_PWDS = new HashMap<String, String>();
    static {
        for (String pw : PLAIN_PWDS) {
            try {
                HASHED_PWDS.put(pw, PasswordUtility.buildPasswordHash(pw));
            } catch (Exception e) {
                // should not get here
            }
        }
    }

    public void testBuildPasswordHash() throws Exception {
        for (String pw : PLAIN_PWDS) {
            String pwHash = PasswordUtility.buildPasswordHash(pw);
            assertFalse(pw.equals(pwHash));
        }

        List<Integer[]> l = new ArrayList<Integer[]>();
        l.add(new Integer[] {0, 1000});
        l.add(new Integer[] {1, 10});
        l.add(new Integer[] {8, 50});
        l.add(new Integer[] {10, 5});
        l.add(new Integer[] {-1, -1});
        for (Integer[] params : l) {
            for (String pw : PLAIN_PWDS) {
                int saltsize = params[0];
                int iterations = params[1];

                String pwHash = PasswordUtility.buildPasswordHash(pw, PasswordUtility.DEFAULT_ALGORITHM, saltsize, iterations);
                assertFalse(pw.equals(pwHash));
            }
        }
    }

    public void testBuildPasswordHashInvalidAlgorithm() throws Exception {
        List<String> invalidAlgorithms = new ArrayList<String>();
        invalidAlgorithms.add("");
        invalidAlgorithms.add("+");
        invalidAlgorithms.add("invalid");

        for (String invalid : invalidAlgorithms) {
            try {
                String pwHash = PasswordUtility.buildPasswordHash("pw", invalid, PasswordUtility.DEFAULT_SALT_SIZE, PasswordUtility.DEFAULT_ITERATIONS);
                fail("Invalid algorithm " + invalid);
            } catch (NoSuchAlgorithmException e) {
                // success
            }
        }

    }

    public void testIsPlainTextPassword() throws Exception {
        for (String pw : PLAIN_PWDS) {
            assertTrue(pw + " should be plain text.", PasswordUtility.isPlainTextPassword(pw));
        }
    }

    public void testIsPlainTextForNull() throws Exception {
        assertTrue(PasswordUtility.isPlainTextPassword(null));
    }

    public void testIsPlainTextForPwHash() throws Exception {
        for (String pwHash : HASHED_PWDS.values()) {
            assertFalse(pwHash + " should not be plain text.", PasswordUtility.isPlainTextPassword(pwHash));
        }
    }

    public void testIsSame() throws Exception {
        for (String pw : HASHED_PWDS.keySet()) {
            String pwHash = HASHED_PWDS.get(pw);
            assertTrue("Not the same " + pw + ", " + pwHash, PasswordUtility.isSame(pwHash, pw));
        }

        String pw = "password";
        String pwHash = PasswordUtility.buildPasswordHash(pw, SecurityConstants.DEFAULT_DIGEST, 4, 50);
        assertTrue("Not the same '" + pw + "', " + pwHash, PasswordUtility.isSame(pwHash, pw));

        pwHash = PasswordUtility.buildPasswordHash(pw, "md5", 0, 5);
        assertTrue("Not the same '" + pw + "', " + pwHash, PasswordUtility.isSame(pwHash, pw));

        pwHash = PasswordUtility.buildPasswordHash(pw, "md5", -1, -1);
        assertTrue("Not the same '" + pw + "', " + pwHash, PasswordUtility.isSame(pwHash, pw));
    }

    public void testIsNotSame() throws Exception {
        String previous = null;
        for (String pw : HASHED_PWDS.keySet()) {
            String pwHash = HASHED_PWDS.get(pw);
            assertFalse(pw, PasswordUtility.isSame(pw, pw));
            assertFalse(pwHash, PasswordUtility.isSame(pwHash, pwHash));
            if (previous != null) {
                assertFalse(previous, PasswordUtility.isSame(pwHash, previous));
            }
            previous = pw;
        }
    }

    public void testExtractAlgorithmFromPlainPw() throws Exception {
        for (String pw : PLAIN_PWDS) {
            assertNull(pw + " is no pw-hash -> no algorithm expected.", PasswordUtility.extractAlgorithm(pw));
        }
    }

    public void testExtractAlgorithmFromNull() throws Exception {
        assertNull("null pw -> no algorithm expected.", PasswordUtility.extractAlgorithm(null));
    }

    public void testExtractAlgorithmFromPwHash() throws Exception {
        for (String pwHash : HASHED_PWDS.values()) {
            String algorithm = PasswordUtility.extractAlgorithm(pwHash);
            assertNotNull(pwHash + " is pw-hash -> algorithm expected.", algorithm);
            assertEquals("Wrong algorithm extracted from " + pwHash, PasswordUtility.DEFAULT_ALGORITHM, algorithm);
        }

        String pwHash = PasswordUtility.buildPasswordHash("pw", SecurityConstants.DEFAULT_DIGEST, 4, 50);
        assertEquals(SecurityConstants.DEFAULT_DIGEST, PasswordUtility.extractAlgorithm(pwHash));

        pwHash = PasswordUtility.buildPasswordHash("pw", "md5", 0, 5);
        assertEquals("md5", PasswordUtility.extractAlgorithm(pwHash));

        pwHash = PasswordUtility.buildPasswordHash("pw", "md5", -1, -1);
        assertEquals("md5", PasswordUtility.extractAlgorithm(pwHash));
    }
}
