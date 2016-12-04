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
package org.apache.jackrabbit.core.security.authentication;

import javax.jcr.SimpleCredentials;
import java.security.NoSuchAlgorithmException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;
import org.apache.jackrabbit.util.Text;

/**
 * <code>CryptedSimpleCredentialsTest</code>...
 */
public class CryptedSimpleCredentialsTest extends TestCase {

    private final String userID = "anyUserID";
    private final String pw = "somePw";

    private SimpleCredentials sCreds;
    private List<CryptedSimpleCredentials> cCreds = new ArrayList<CryptedSimpleCredentials>();

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        sCreds = new SimpleCredentials(userID, pw.toCharArray());
        // build crypted credentials from the simple credentials
        CryptedSimpleCredentials cc = new CryptedSimpleCredentials(sCreds);
        cCreds.add(cc);
        // build from uid/pw
        cCreds.add(new CryptedSimpleCredentials(userID, pw));
        // build from uid and crypted pw
        cCreds.add(new CryptedSimpleCredentials(userID, cc.getPassword()));
    }

    public void testSimpleMatch() throws NoSuchAlgorithmException, UnsupportedEncodingException {
        for (CryptedSimpleCredentials cc : cCreds) {
            assertTrue(cc.matches(sCreds));
        }
    }

    public void testUserIDMatchesCaseInsensitive() throws Exception {
        String uid = userID.toUpperCase();
        for (CryptedSimpleCredentials cc : cCreds) {
            assertTrue(cc.matches(new SimpleCredentials(uid, pw.toCharArray())));
        }

        uid = userID.toLowerCase();
        for (CryptedSimpleCredentials cc : cCreds) {
            assertTrue(cc.matches(new SimpleCredentials(uid, pw.toCharArray())));
        }
    }

    public void testGetUserID() {
        for (CryptedSimpleCredentials cc : cCreds) {
            assertEquals(userID, cc.getUserID());
        }
    }

    public void testGetPassword() throws NoSuchAlgorithmException, UnsupportedEncodingException {
        // build crypted credentials from the simple credentials
        CryptedSimpleCredentials cc = new CryptedSimpleCredentials(userID, pw);
        assertFalse(pw.equals(cc.getPassword()));

        // build from uid and crypted pw
        CryptedSimpleCredentials cc2 = new CryptedSimpleCredentials(userID, cc.getPassword());
        assertFalse(pw.equals(cc2.getPassword()));

        assertEquals(cc.getPassword(), cc2.getPassword());

        CryptedSimpleCredentials cc3 = new CryptedSimpleCredentials(sCreds);
        assertFalse(pw.equals(cc3.getPassword()));
        assertFalse(cc.getPassword().equals(cc3.getPassword()));
    }

    public void testGetPassword2() throws NoSuchAlgorithmException, UnsupportedEncodingException {
        CryptedSimpleCredentials prev = cCreds.get(0);
        // build crypted credentials from the uid and the crypted pw contained
        // in simple credentials -> simple-c-password must be treated plain-text
        SimpleCredentials sc = new SimpleCredentials(userID, prev.getPassword().toCharArray());
        CryptedSimpleCredentials diff = new CryptedSimpleCredentials(sc);

        assertFalse(prev.getPassword().equals(diff.getPassword()));
        assertFalse(String.valueOf(sc.getPassword()).equals(diff.getPassword()));
    }

    public void testGetAlgorithm() {
        CryptedSimpleCredentials prev = null;
        for (CryptedSimpleCredentials cc : cCreds) {
            assertNotNull(cc.getAlgorithm());
            if (prev != null) {
                assertEquals(prev.getAlgorithm(), cc.getAlgorithm());
            }
            prev = cc;
        }
    }

    public void testPasswordMatch() throws NoSuchAlgorithmException, UnsupportedEncodingException {
        // simple credentials containing the crypted pw must not match.
        SimpleCredentials sc = new SimpleCredentials(userID, cCreds.get(0).getPassword().toCharArray());
        for (CryptedSimpleCredentials cc : cCreds) {
            assertFalse(cc.matches(sc));
        }

        // simple credentials containing different pw must not match.
        SimpleCredentials sc2 = new SimpleCredentials(userID, "otherPw".toCharArray());
        for (CryptedSimpleCredentials cc : cCreds) {
            assertFalse(cc.matches(sc2));
        }

        // simple credentials with pw in digested form must not match.
        SimpleCredentials sc3 = new SimpleCredentials(userID, "{unknown}somePw".toCharArray());
        for (CryptedSimpleCredentials cc : cCreds) {
            assertFalse(cc.matches(sc3));
        }

        // simple credentials with pw with different digest must not match
        SimpleCredentials sc4 = new SimpleCredentials(userID, ("{md5}"+Text.digest("md5", pw.getBytes("UTF-8"))).toCharArray());
        for (CryptedSimpleCredentials cc : cCreds) {
            assertFalse(cc.matches(sc4));
        }
    }

    public void testUserIdMatch()  throws NoSuchAlgorithmException, UnsupportedEncodingException {
        // simple credentials containing a different uid must not match
        SimpleCredentials sc = new SimpleCredentials("another", pw.toCharArray());
        for (CryptedSimpleCredentials cc : cCreds) {
            assertFalse(cc.matches(sc));
        }
    }
}