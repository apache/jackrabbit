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

import org.apache.jackrabbit.api.security.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Credentials;
import javax.jcr.RepositoryException;
import javax.jcr.SimpleCredentials;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;

/**
 * This <code>Authentication</code> implementation compare
 * {@link javax.jcr.SimpleCredentials SimpleCredentials} stored
 * for a given {@link org.apache.jackrabbit.api.security.user.User#getCredentials() User}
 * to the credentials passed to {@link #authenticate(Credentials)}.
 *
 * @see org.apache.jackrabbit.core.security.authentication.Authentication
 * @see javax.jcr.SimpleCredentials
 */
class SimpleCredentialsAuthentication implements Authentication {

    private static final Logger log = LoggerFactory.getLogger(SimpleCredentialsAuthentication.class);

    private final CryptedSimpleCredentials creds;

    /**
     * Create a new <code>Authentication</code> instance for the given <code>User</code>.
     *
     * @param user to create the Authentication.
     * @throws javax.jcr.RepositoryException If an error occurs.
     */
    SimpleCredentialsAuthentication(User user) throws RepositoryException {
        Credentials creds = user.getCredentials();
        if (creds instanceof CryptedSimpleCredentials) {
            this.creds = (CryptedSimpleCredentials) creds;
        } else if (creds instanceof SimpleCredentials) {
            try {
                this.creds = new CryptedSimpleCredentials((SimpleCredentials) creds);
            } catch (NoSuchAlgorithmException e) {
                throw new RepositoryException(e);
            } catch (UnsupportedEncodingException e) {
                throw new RepositoryException(e);
            }
        } else {
            log.warn("No Credentials found with user " + user.getID());
            this.creds = null;
        }
    }

    //------------------------------------------------< Authentication >--------
    /**
     * This Authentication is able to handle the validation of SimpleCredentials.
     *
     * @param credentials to test
     * @return <code>true</code> if the specified Credentials are
     * <code>SimpleCredentials</code> and if the <code>User</code> used to
     * construct this instance provides credentials that can be compared to
     * <code>SimpleCredentials</code>.
     * @see Authentication#canHandle(Credentials)
     */
    public boolean canHandle(Credentials credentials) {
        return creds != null && credentials instanceof SimpleCredentials;
    }

    /**
     * Compare the
     * {@link org.apache.jackrabbit.api.security.user.User#getCredentials() Credentials} obtained from the <code>User</code>
     * with the specified <code>credentials</code>.<br>
     * If the specified <code>credentials</code> are an instance of
     * <code>SimpleCredentials</code> and match the user's credentials this
     * method returns <code>true</code>; otherwise <code>false</code>.
     *
     * @param credentials Credentials to be used for the authentication.
     * @return true if the given Credentials' UserID/Password pair match
     * the credentials attached to the user this SimpleCredentialsAuthentication
     * has been built for.
     * @throws RepositoryException If an error occurs.
     */
    public boolean authenticate(Credentials credentials) throws RepositoryException {
        if (!(credentials instanceof SimpleCredentials)) {
            throw new RepositoryException("SimpleCredentials expected. Cannot handle " + credentials.getClass().getName());
        }
        try {
            if (creds != null && creds.matches((SimpleCredentials) credentials)) {
                return true;
            }
        } catch (NoSuchAlgorithmException e) {
            log.debug("Failed to verify Credentials with {}: {}.", credentials.toString(), e);
        } catch (UnsupportedEncodingException e) {
            log.debug("Failed to verify Credentials with {}: {}.", credentials.toString(), e);
        }
        return false;
    }
}
