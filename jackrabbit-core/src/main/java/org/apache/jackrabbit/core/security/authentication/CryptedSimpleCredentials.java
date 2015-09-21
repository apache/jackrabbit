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
import org.apache.jackrabbit.core.security.user.PasswordUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger log = LoggerFactory.getLogger(CryptedSimpleCredentials.class);

    private final String hashedPassword;
    private final String userId;
    private final Map<String, Object> attributes;

    /**
     * Build a new instance of <code>CryptedSimpleCredentials</code> from the
     * given {@link javax.jcr.SimpleCredentials SimpleCredentials} and create
     * the crypted password field using the {@link SecurityConstants#DEFAULT_DIGEST
     * default digest}.
     *
     * @param credentials
     * @throws NoSuchAlgorithmException
     * @throws UnsupportedEncodingException
     * @deprecated
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
        hashedPassword = PasswordUtility.buildPasswordHash(password);

        String[] attNames = credentials.getAttributeNames();
        attributes = new HashMap<String, Object>(attNames.length);
        for (String attName : attNames) {
            attributes.put(attName, credentials.getAttribute(attName));
        }
    }

    /**
     * Create a new instanceof <code>CryptedSimpleCredentials</code> from the
     * given <code>userId</code> and <code>hashedPassword</code> strings.
     * In contrast to {@link CryptedSimpleCredentials(SimpleCredentials)} that
     * expects the password to be plain text this constructor expects the
     * password to be already crypted. However, it performs a simple validation
     * and calls {@link PasswordUtility#buildPasswordHash(String)} in case the
     * given password is found to be plain text.
     *
     * @param userId
     * @param hashedPassword
     * @throws NoSuchAlgorithmException
     * @throws UnsupportedEncodingException
     */
    public CryptedSimpleCredentials(String userId, String hashedPassword) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        if (userId == null || userId.length() == 0) {
            throw new IllegalArgumentException("Invalid userID: The userID must have a length > 0.");
        }
        if (hashedPassword == null) {
            throw new IllegalArgumentException("Password may not be null.");
        }
        this.userId = userId;
        if (PasswordUtility.isPlainTextPassword(hashedPassword)) {
            // password is plain text (including those starting with {invalidAlgorithm})
            log.warn("Plain text password -> Using default algorithm to create digest.");
            this.hashedPassword = PasswordUtility.buildPasswordHash(hashedPassword);
        } else {
            this.hashedPassword = hashedPassword;
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
        return PasswordUtility.extractAlgorithm(hashedPassword);
    }

    public String getPassword() {
        return hashedPassword;
    }

    /**
     * Compares this instance with the given <code>SimpleCredentials</code> and
     * returns <code>true</code> if both match. Successful match is defined to
     * be the result of
     * <ul>
     * <li>Case-insensitive comparison of the UserIDs</li>
     * <li>Equality of the passwords if the password contained in the simple
     * credentials is hashed with the algorithm defined in this credentials object.</li>
     * </ul>
     *
     * NOTE, that the simple credentials are exptected to contain the plain text
     * password.
     *
     * @param credentials An instance of simple credentials.
     * @return true if {@link SimpleCredentials#getUserID() UserID} and
     * {@link SimpleCredentials#getPassword() Password} match.
     * @throws NoSuchAlgorithmException
     * @throws UnsupportedEncodingException
     */
    public boolean matches(SimpleCredentials credentials)
            throws NoSuchAlgorithmException, UnsupportedEncodingException {

        if (getUserID().equalsIgnoreCase(credentials.getUserID())) {
            // crypt the password retrieved from the given simple credentials
            // and test if it is equal to the password hash defined with this
            // CryptedSimpleCredentials instance.
            return PasswordUtility.isSame(hashedPassword, String.valueOf(credentials.getPassword()));
        }
        return false;
    }
}
