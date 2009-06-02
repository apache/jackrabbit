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

import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;

import java.util.HashSet;
import java.util.Set;
import java.util.Arrays;

/**
 * <code>SessionInfoImpl</code>...
 */
public class SessionInfoImpl extends org.apache.jackrabbit.spi.commons.SessionInfoImpl {

    private final CredentialsWrapper credentials;
    private final Set sessionScopedTokens = new HashSet();

    private String lastBatchId;
    private NamePathResolver resolver;

    SessionInfoImpl(CredentialsWrapper creds, String workspaceName) {
        this.credentials = creds;

        super.setWorkspacename(workspaceName);
    }

    //--------------------------------------------------------< SessionInfo >---
    /**
     * @inheritDoc
     */
    public String getUserID() {
        return credentials.getUserId();
    }

    //--------------------------------------------------------------------------

    CredentialsWrapper getCredentials() {
        return credentials;
    }

    /**
     * Returns the id of the most recently submitted batch or <code>null</code>
     * it no batch has been submitted yet.
     *
     * @return the batch id of the most recently submitted batch.
     */
    String getLastBatchId() {
        return lastBatchId;
    }

    /**
     * Sets the id of the most recently submitted batch.
     *
     * @param batchId the batch id.
     */
    void setLastBatchId(String batchId) {
        lastBatchId = batchId;
    }

    NamePathResolver getNamePathResolver() {
        return resolver;
    }

    void setNamePathResolver(NamePathResolver resolver) {
        this.resolver = resolver;
    }

    /**
     * @return All tokens that this session info needs to communicate with
     * the DAV-server. This includes all tokens obtained through both LOCK
     * request(s) as well as those tokens that have been added to the
     * corresponding JCR session.
     * Note, that the <code>sessionScopedTokens</code> are only used for
     * communiation with the DAV server and are never exposed through the
     * JCR API for they belong to session-scoped locks.
     */
    String[] getAllLockTokens() {
        Set s = new HashSet(Arrays.asList(getLockTokens()));
        s.addAll(sessionScopedTokens);
        return (String[]) s.toArray(new String[s.size()]);
    }

    void addLockToken(String token, boolean sessionScoped) {
        if (sessionScoped) {
            sessionScopedTokens.add(token);
        } else {
            super.addLockToken(token);
        }
    }

    void removeLockToken(String token, boolean sessionScoped) {
        if (sessionScoped) {
            sessionScopedTokens.remove(token);
        } else {
            super.removeLockToken(token);
        }
    }
}