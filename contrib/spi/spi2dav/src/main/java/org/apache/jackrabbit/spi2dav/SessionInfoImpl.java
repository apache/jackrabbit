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

    private final Set lockTokens = new HashSet();

    private String lastBatchId;

    /**
     * The subscriptionId if this session info is subscribed to observation
     * events.
     */
    private String subscriptionId;

    SessionInfoImpl(CredentialsWrapper creds, String workspaceName) {
        this.credentials = creds;
        this.workspaceName = workspaceName;
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

    /**
     * Returns the subscriptionId for this <code>SessionInfo</code> or
     * <code>null</code> if no subscription is present.
     *
     * @return the subscriptionId for this <code>SessionInfo</code>.
     */
    String getSubscriptionId() {
        return subscriptionId;
    }

    /**
     * Sets a new subscriptionId for this <code>SessionInfo</code>.
     *
     * @param subscriptionId the new subscriptionId.
     * @return the old subscriptionId or <code>null</code> if there was none.
     */
    String setSubscriptionId(String subscriptionId) {
        String old = this.subscriptionId;
        this.subscriptionId = subscriptionId;
        return old;
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
        this.lastBatchId = batchId;
    }
}