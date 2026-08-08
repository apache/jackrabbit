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
package org.apache.jackrabbit.spi2dav;

import javax.jcr.SimpleCredentials;

import org.apache.hc.client5.http.auth.Credentials;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;

/**
 * <code>CredentialsWrapper</code>...
 */
class CredentialsWrapper {

    private final String userId;
    private final UsernamePasswordCredentials credentials;

    CredentialsWrapper(javax.jcr.Credentials creds) {

        if (creds == null) {
            // NOTE: null credentials only work if 'missing-auth-mapping' param is set on the server
            userId = "";
            this.credentials = null;
        } else if (creds instanceof SimpleCredentials) {
            SimpleCredentials sCred = (SimpleCredentials) creds;
            userId = sCred.getUserID();
            // copy the password: SimpleCredentials hands out its internal array and
            // UsernamePasswordCredentials keeps the reference, so without a copy a
            // caller zeroing the password after login would break the open session
            this.credentials = new UsernamePasswordCredentials(userId, sCred.getPassword().clone());
        } else {
            userId = "";
            // HttpClient 5 dropped the single-argument "username:password"
            // constructor, so split the pair here instead
            String usernamePassword = creds.toString();
            int colon = usernamePassword.indexOf(':');
            if (colon < 0) {
                this.credentials = new UsernamePasswordCredentials(usernamePassword, new char[0]);
            } else {
                this.credentials = new UsernamePasswordCredentials(usernamePassword.substring(0, colon),
                        usernamePassword.substring(colon + 1).toCharArray());
            }
        }
    }

    String getUserId() {
        return userId;
    }

    Credentials getHttpCredentials() {
        return credentials;
    }
}