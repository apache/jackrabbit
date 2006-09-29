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

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import org.apache.jackrabbit.spi.SessionInfo;

import java.util.HashSet;
import java.util.Set;

/**
 * <code>SessionInfoImpl</code>...
 */
public class SessionInfoImpl implements SessionInfo {

    private static Logger log = LoggerFactory.getLogger(SessionInfoImpl.class);

    private final CredentialsWrapper credentials;
    private final String workspaceName;
    private final SubscriptionManager subscrMgr;

    private final Set lockTokens = new HashSet();

    SessionInfoImpl(CredentialsWrapper creds, String workspaceName, SubscriptionManager subscrMgr) {
        this.credentials = creds;
        this.workspaceName = workspaceName;
        this.subscrMgr = subscrMgr;
        subscrMgr.setSessionInfo(this);
    }

    //--------------------------------------------------------< SessionInfo >---
    /**
     * @inheritDoc
     */
    public String getUserID() {
        return credentials.getUserId();
    }

    /**
     * @inheritDoc
     */
    public String getWorkspaceName() {
        return workspaceName;
    }

    /**
     * @inheritDoc
     */
    public String[] getLockTokens() {
        return (String[]) lockTokens.toArray(new String[lockTokens.size()]);
    }

    /**
     * @inheritDoc
     */
    public void addLockToken(String lockToken) {
        lockTokens.add(lockToken);
    }

    /**
     * @inheritDoc
     */
    public void removeLockToken(String lockToken) {
        lockTokens.remove(lockToken);
    }

    //--------------------------------------------------------------------------
    CredentialsWrapper getCredentials() {
        return credentials;
    }

    SubscriptionManager getSubscriptionManager() {
        return subscrMgr;
    }
}