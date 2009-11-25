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

import org.apache.jackrabbit.core.security.SecurityConstants;
import org.apache.jackrabbit.util.Text;

import javax.jcr.Credentials;
import javax.jcr.SimpleCredentials;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Crypted variant of the {@link javax.jcr.SimpleCredentials}.
 */
public class CryptedSimpleCredentials implements Credentials {

    private final String algorithm;
    private final String cryptedPassword;
    private final String userId;
    private final Map<String, Object> attributes;

    /**
     * Take {@link javax.jcr.SimpleCredentials SimpleCredentials} and
     * digest the password if it is plain-text
     *
     * @param credentials
     * @throws NoSuchAlgorithmException
     * @throws UnsupportedEncodingException
     */
    public CryptedSimpleCredentials(SimpleCredentials credentials)
            throws NoSuchAlgorithmException, UnsupportedEncodingException {
        userId = credentials.getUserID();
        if (userId == null || userId.length() == 0) {
            throw new IllegalArgumentException();
        }
        char[] pwd = credentials.getPassword();
        if (pwd == null) {
            throw new IllegalArgumentException();
        }
        String password = new String(pwd);
        String algo =  getAlgorithm(password);
        if (algo == null) {
            // password is plain text
            algorithm = SecurityConstants.DEFAULT_DIGEST;
            cryptedPassword = crypt(password, algorithm);
        } else {
            // password is already encrypted
            algorithm = algo;
            cryptedPassword = password;
        }

        String[] attNames = credentials.getAttributeNames();
        attributes = new HashMap<String, Object>(attNames.length);
        for (String attName : attNames) {
            attributes.put(attName, credentials.getAttribute(attName));
        }
    }

    public CryptedSimpleCredentials(String userId, String password) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        if (userId == null || userId.length() == 0 || password == null) {
            throw new IllegalArgumentException("Invalid userID or password. Neither may be null, the userID must have a length > 0.");
        }
        this.userId = userId;
        String algo =  getAlgorithm(password);
        if (algo == null) {
            // password is plain text
            algorithm = SecurityConstants.DEFAULT_DIGEST;
            cryptedPassword = crypt(password, algorithm);
        } else {
            // password is already encrypted
            algorithm = algo;
            cryptedPassword = password;
        }
        attributes = Collections.emptyMap();
    }

    public String getUserID() {
        return userId;
    }

    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    public String[] getAttributeNames() {
        return attributes.keySet().toArray(new String[attributes.size()]);
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public String getPassword() {
        return cryptedPassword;
    }

    /**
     * Compares this instance with the given <code>SimpleCredentials</code> and
     * returns <code>true</code> if both match.
     *
     * @param credentials
     * @return true if {@link SimpleCredentials#getUserID() UserID} and
     * {@link SimpleCredentials#getPassword() Password} match.
     * @throws NoSuchAlgorithmException
     * @throws UnsupportedEncodingException
     */
    public boolean matches(SimpleCredentials credentials)
            throws NoSuchAlgorithmException, UnsupportedEncodingException {

        if (getUserID().equalsIgnoreCase(credentials.getUserID())) {
            String toMatch = new String(credentials.getPassword());
            String algr = getAlgorithm(toMatch);

            if (algr == null && algorithm != null) {
                // pw to match not crypted -> crypt with algorithm present here.
                return crypt(toMatch, algorithm).equals(cryptedPassword);
            } else if (algr != null && algorithm == null) {
                // crypted pw to match but unknown algorithm here -> crypt this pw
                return crypt(algr, cryptedPassword).equals(toMatch);
            }

            // both pw to compare define a algorithm and are crypted
            // -> simple comparison of the 2 password strings.
            return toMatch.equals(cryptedPassword);
        }
        return false;
    }

    private static String crypt(String pwd, String algorithm)
            throws NoSuchAlgorithmException, UnsupportedEncodingException {

        StringBuilder password = new StringBuilder();
        password.append("{").append(algorithm).append("}");
        password.append(Text.digest(algorithm, pwd.getBytes("UTF-8")));
        return password.toString();
    }

    private static String getAlgorithm(String password) {
        int end = password.indexOf("}");
        if (password.startsWith("{") && end > 0) {
            return password.substring(1, end);
        } else {
            return null;
        }
    }
}
