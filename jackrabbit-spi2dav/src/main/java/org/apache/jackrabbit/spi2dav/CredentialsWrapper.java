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

import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;

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
            this.credentials = new UsernamePasswordCredentials(userId, String.valueOf(sCred.getPassword()));
        } else {
            userId = "";
            this.credentials = new UsernamePasswordCredentials(creds.toString());
        }
    }

    String getUserId() {
        return userId;
    }

    Credentials getHttpCredentials() {
        return credentials;
    }
}