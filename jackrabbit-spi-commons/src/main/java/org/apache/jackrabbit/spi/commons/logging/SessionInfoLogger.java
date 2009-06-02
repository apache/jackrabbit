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
package org.apache.jackrabbit.spi.commons.logging;

import org.apache.jackrabbit.spi.SessionInfo;

import javax.jcr.RepositoryException;

/**
 * Log wrapper for a {@link SessionInfo}.
 */
public class SessionInfoLogger extends AbstractLogger implements SessionInfo {
    private final SessionInfo sessionInfo;

    /**
     * Create a new instance for the given <code>sessionInfo</code> which uses
     * <code>writer</code> for persisting log messages.
     * @param sessionInfo
     * @param writer
     */
    public SessionInfoLogger(SessionInfo sessionInfo, LogWriter writer) {
        super(writer);
        this.sessionInfo = sessionInfo;
    }

    /**
     * @return  the wrapped SessionInfo
     */
    public SessionInfo getSessionInfo() {
        return sessionInfo;
    }

    // -----------------------------------------------------< SessionInfo >---

    public String getUserID() {
        return (String) execute(new SafeCallable() {
            public Object call() {
                return sessionInfo.getUserID();
            }
        }, "getUserID()", new Object[]{});
    }

    public String getWorkspaceName() {
        return (String) execute(new SafeCallable() {
            public Object call() {
                return sessionInfo.getWorkspaceName();
            }
        }, "getWorkspaceName()", new Object[]{});
    }

    public String[] getLockTokens() throws RepositoryException {
        return (String[]) execute(new Callable() {
            public Object call() throws RepositoryException {
                return sessionInfo.getLockTokens();
            }
        }, "getLockTokens()", new Object[]{});
    }

    public void addLockToken(final String lockToken) throws RepositoryException {
        execute(new Callable() {
            public Object call() throws RepositoryException {
                sessionInfo.addLockToken(lockToken);
                return null;
            }
        }, "addLockToken(String)", new Object[]{lockToken});
    }

    public void removeLockToken(final String lockToken) throws RepositoryException {
        execute(new Callable() {
            public Object call() throws RepositoryException {
                sessionInfo.removeLockToken(lockToken);
                return null;
            }
        }, "removeLockToken(String)", new Object[]{lockToken});
    }

    public void setUserData(final String userData) throws RepositoryException {
        execute(new Callable() {
            public Object call() throws RepositoryException {
                sessionInfo.setUserData(userData);
                return null;
            }
        }, "setUserData(String)", new Object[]{userData});
    }


}
