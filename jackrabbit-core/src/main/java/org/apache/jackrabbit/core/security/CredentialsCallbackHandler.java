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
package org.apache.jackrabbit.core.security;

import javax.jcr.Credentials;
import javax.jcr.SimpleCredentials;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import java.io.IOException;

/**
 * A <code>CredentialsCallbackHandler</code> ...
 */
public class CredentialsCallbackHandler implements CallbackHandler {

    protected final Credentials credentials;

    /**
     * Constructor
     *
     * @param credentials
     */
    public CredentialsCallbackHandler(Credentials credentials) {
        this.credentials = credentials;
    }

    //------------------------------------------------------< CallbackHandler >
    /**
     * {@inheritDoc}
     */
    public void handle(Callback[] callbacks) throws IOException,
            UnsupportedCallbackException {
        for (int i = 0; i < callbacks.length; i++) {
            if (callbacks[i] instanceof CredentialsCallback) {
                CredentialsCallback ccb = (CredentialsCallback) callbacks[i];
                // supply credentials
                ccb.setCredentials(credentials);
            } else if (callbacks[i] instanceof NameCallback
                    && credentials instanceof SimpleCredentials) {
                NameCallback ncb = (NameCallback) callbacks[i];
                SimpleCredentials sc = (SimpleCredentials) credentials;
                // supply name
                ncb.setName(sc.getUserID());
            } else if (callbacks[i] instanceof PasswordCallback
                    && credentials instanceof SimpleCredentials) {
                PasswordCallback pcb = (PasswordCallback) callbacks[i];
                SimpleCredentials sc = (SimpleCredentials) credentials;
                // supply password
                pcb.setPassword(sc.getPassword());
            } else {
                throw new UnsupportedCallbackException(callbacks[i], "Unrecognized Callback");
            }
        }
    }
}
