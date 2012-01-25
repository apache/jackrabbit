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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Credentials;
import javax.jcr.RepositoryException;
import javax.jcr.SimpleCredentials;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Crypted variant of the {@link javax.jcr.SimpleCredentials}.
 */
public class CryptedSimpleCredentials implements Credentials {

    private static final Logger log = LoggerFactory.getLogger(CryptedSimpleCredentials.class);

    private final String algorithm;
    private final String salt;

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
        algorithm = SecurityConstants.DEFAULT_DIGEST;
        salt = null; // backwards compatibility.
        hashedPassword = generateHash(password, algorithm, salt);

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
     * and calls {@link Text#digest} using the
     * {@link SecurityConstants#DEFAULT_DIGEST default digest} in case the
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
        String algo =  extractAlgorithm(hashedPassword);
        if (algo == null) {
            // password is plain text (including those starting with {invalidAlgorithm})
            log.debug("Plain text password -> Using " + SecurityConstants.DEFAULT_DIGEST + " to create digest.");
            algorithm = SecurityConstants.DEFAULT_DIGEST;
            salt = generateSalt();
            this.hashedPassword = generateHash(hashedPassword, algorithm, salt);
        } else {
            // password is already hashed and started with {validAlgorithm}
            algorithm = algo;
            salt = extractSalt(hashedPassword, algorithm);
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
        return algorithm;
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
            // and test if it is equal to the cryptedPassword field.
            return hashedPassword.equals(generateHash(String.valueOf(credentials.getPassword()), algorithm, salt));
        }
        return false;
    }

    /**
     * Creates a hash of the specified password if it is found to be plain text.
     *
     * @param password
     * @return
     * @throws javax.jcr.RepositoryException
     */
    public static String buildPasswordHash(String password) throws RepositoryException {
        try {
            return new CryptedSimpleCredentials("_", password).getPassword();
        } catch (NoSuchAlgorithmException e) {
            throw new RepositoryException(e);
        } catch (UnsupportedEncodingException e) {
            throw new RepositoryException(e);
        }
    }

    /**
     * @param pwd Plain text password
     * @param algorithm The algorithm to be used for the digest.
     * @param salt The salt to be used for the digest.
     * @return Digest of the given password with leading algorithm and optionally
     * salt information.
     * @throws NoSuchAlgorithmException
     * @throws UnsupportedEncodingException
     */
    private static String generateHash(String pwd, String algorithm, String salt)
            throws NoSuchAlgorithmException, UnsupportedEncodingException {

        StringBuilder password = new StringBuilder();
        password.append("{").append(algorithm).append("}");
        if (salt != null && salt.length() > 0) {
            password.append(salt).append("-");
            StringBuilder data = new StringBuilder();
            data.append(salt).append(pwd);
            password.append(Text.digest(algorithm, data.toString().getBytes("UTF-8")));
        } else {
            password.append(Text.digest(algorithm, pwd.getBytes("UTF-8")));            
        }
        return password.toString();
    }

    /**
     * Extract the algorithm from the given crypted password string. Returns the
     * algorithm or <code>null</code> if the given string doesn't have a
     * leading <code>{algorithm}</code> such as created by {@link #generateHash(String, String, String)
     * or if the extracted string doesn't represent an available algorithm.
     *
     * @param hashedPwd
     * @return The algorithm or <code>null</code> if the given string doesn't have a
     * leading <code>{algorith}</code> such as created by {@link #crypt(String, String)
     * or if the extracted string isn't an available algorithm. 
     */
    private static String extractAlgorithm(String hashedPwd) {
        int end = hashedPwd.indexOf('}');
        if (hashedPwd.startsWith("{") && end > 0) {
            String algorithm = hashedPwd.substring(1, end);
            try {
                MessageDigest.getInstance(algorithm);
                return algorithm;
            } catch (NoSuchAlgorithmException e) {
                log.debug("Invalid algorithm detected " + algorithm);
            }
        }

        // not starting with {} or invalid algorithm
        return null;
    }

    /**
     * Extract the salt from the password hash.
     *
     * @param hashedPwd
     * @param algorithm
     * @return salt or <code>null</code>
     */
    private static String extractSalt(String hashedPwd, String algorithm) {
        int start = algorithm.length()+2;
        int end = hashedPwd.indexOf('-', start);
        if (end > -1) {
            return hashedPwd.substring(start, end);
        }

        // no salt 
        return null;
    }

    /**
     * Generate a new random salt for password digest.
     *
     * @return a new random salt.
     */
    private static String generateSalt() {
        SecureRandom random = new SecureRandom();
        byte salt[] = new byte[8];
        random.nextBytes(salt);

        StringBuffer res = new StringBuffer(salt.length * 2);
        for (byte b : salt) {
            res.append(Text.hexTable[(b >> 4) & 15]);
            res.append(Text.hexTable[b & 15]);
        }
        return res.toString();
    }
}
