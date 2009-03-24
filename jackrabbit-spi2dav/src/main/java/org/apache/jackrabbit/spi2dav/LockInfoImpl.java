/*
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements.  The ASF licenses this file to You
* under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License.
* You may obtain a copy of the License at
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

import org.apache.jackrabbit.webdav.lock.ActiveLock;
import org.apache.jackrabbit.webdav.jcr.ItemResourceConstants;
import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.spi.LockInfo;
import org.apache.jackrabbit.spi.NodeId;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 * <code>LockInfoImpl</code>...
 */
public class LockInfoImpl implements LockInfo {

    private static Logger log = LoggerFactory.getLogger(LockInfoImpl.class);

    private final ActiveLock activeLock;
    private final NodeId nodeId;

    public LockInfoImpl(ActiveLock activeLock, NodeId nodeId) {
        this.activeLock = activeLock;
        this.nodeId = nodeId;
    }

    ActiveLock getActiveLock() {
        return activeLock;
    }
    
    //-----------------------------------------------------------< LockInfo >---
    public String getLockToken() {
        return (isSessionScoped()) ? null : activeLock.getToken();
    }

    public String getOwner() {
        return activeLock.getOwner();
    }

    public boolean isDeep() {
        return activeLock.isDeep();
    }

    public boolean isSessionScoped() {
        return activeLock.getScope() == ItemResourceConstants.EXCLUSIVE_SESSION;
    }

    public long getSecondsRemaining() {
        long timeout = activeLock.getTimeout();
        return (timeout == DavConstants.INFINITE_TIMEOUT) ? Long.MAX_VALUE : timeout / 1000;
    }

    public boolean isLockOwner() {
        return activeLock.getToken() != null;
    }

    public NodeId getNodeId() {
        return nodeId;
    }
}