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

import org.apache.jackrabbit.core.security.principal.PrincipalProviderRegistry;

import javax.jcr.Session;
import javax.security.auth.callback.Callback;

/**
 * Callback for a {@link javax.security.auth.callback.CallbackHandler} to ask for
 * a {@link Session} to access the {@link javax.jcr.Repository}
 */
public class RepositoryCallback implements Callback {

    private Session session;
    private PrincipalProviderRegistry principalProviderRegistry;
    private String adminId;
    private String anonymousId;

    public void setSession(Session session) {
        this.session = session;
    }

    public Session getSession() {
        return session;
    }

    public void setPrincipalProviderRegistry(PrincipalProviderRegistry principalProviderRegistry) {
        this.principalProviderRegistry = principalProviderRegistry;
    }

    public PrincipalProviderRegistry getPrincipalProviderRegistry() {
        return principalProviderRegistry;
    }

    public String getAdminId() {
        return adminId;
    }

    public void setAdminId(String adminId) {
        this.adminId = adminId;
    }

    public String getAnonymousId() {
        return anonymousId;
    }

    public void setAnonymousId(String anonymousId) {
        this.anonymousId = anonymousId;
    }
}
