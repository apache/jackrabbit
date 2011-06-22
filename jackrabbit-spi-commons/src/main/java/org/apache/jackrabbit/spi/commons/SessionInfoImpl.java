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
package org.apache.jackrabbit.spi.commons;

import org.apache.jackrabbit.spi.SessionInfo;

import javax.jcr.RepositoryException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * <code>SessionInfoImpl</code> is a serializable bean based implementation of
 * <code>SessionInfo</code>.
 */
public class SessionInfoImpl implements SessionInfo, Serializable {

    /**
     * The userId or <code>null</code> if unknown.
     */
    private String userId;

    /**
     * The user data or <code>null</code>.
     */
    private String userData;

    /**
     * The name of the workspace to connect to or <code>null</code> if this
     * session info refers to the default workspace.
     */
    private String workspaceName;

    /**
     * The list of lock tokens.
     */
    private List<String> lockTokens = new ArrayList<String>();

    /**
     * Default constructor
     */
    public SessionInfoImpl() {
    }

    /**
     * Sets the userId.
     *
     * @param userId the userId or <code>null</code> if unknown.
     */
    public void setUserID(String userId) {
        this.userId = userId;
    }

    /**
     * Sets the name of the workspace to connect to.
     *
     * @param workspaceName the name of the workspace or <code>null</code> if
     *                      this session info refers to the default workspace.
     */
    public void setWorkspacename(String workspaceName) {
        this.workspaceName = workspaceName;
    }

    //-------------------------< SessionInfo >----------------------------------

    /**
     * {@inheritDoc}
     */
    public String getUserID() {
        return userId;
    }

    /**
     * {@inheritDoc}
     */
    public String getWorkspaceName() {
        return workspaceName;
    }

    /**
     * {@inheritDoc}
     */
    public String[] getLockTokens() {
        return lockTokens.toArray(new String[lockTokens.size()]);
    }

    /**
     * {@inheritDoc}
     */
    public void addLockToken(String s) {
        lockTokens.add(s);
    }

    /**
     * {@inheritDoc}
     */
    public void removeLockToken(String s) {
        lockTokens.remove(s);
    }

    /**
     * {@inheritDoc}
     */
    public void setUserData(String userData) throws RepositoryException {
        this.userData = userData;
    }

    /**
     * Return the user data set via {@link #setUserData(String)}
     * 
     * @return  userData
     */
    public String getUserData() {
        return userData;
    }
}
