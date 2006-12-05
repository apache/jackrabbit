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

import org.apache.jackrabbit.webdav.lock.LockDiscovery;
import org.apache.jackrabbit.webdav.lock.ActiveLock;
import org.apache.jackrabbit.webdav.lock.Type;
import org.apache.jackrabbit.webdav.lock.Scope;
import org.apache.jackrabbit.webdav.jcr.ItemResourceConstants;
import org.apache.jackrabbit.spi.LockInfo;
import org.apache.jackrabbit.spi.NodeId;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import javax.jcr.lock.LockException;
import javax.jcr.RepositoryException;
import java.util.List;
import java.util.Iterator;

/**
 * <code>LockInfoImpl</code>...
 */
public class LockInfoImpl implements LockInfo {

    private static Logger log = LoggerFactory.getLogger(LockInfoImpl.class);

    private ActiveLock activeLock;

    public LockInfoImpl(LockDiscovery ld, NodeId nodeId) throws LockException, RepositoryException {
        List activeLocks = (List) ld.getValue();
        Iterator it = activeLocks.iterator();
        while (it.hasNext()) {
            ActiveLock l = (ActiveLock) it.next();
            Scope sc = l.getScope();
            if (l.getType() == Type.WRITE && (sc == Scope.EXCLUSIVE || sc == ItemResourceConstants.EXCLUSIVE_SESSION)) {
                if (activeLock != null) {
                    throw new RepositoryException("Node " + nodeId + " contains multiple exclusive write locks.");
                } else {
                    activeLock = l;
                }
            }
        }

        if (activeLock == null) {
            throw new LockException("No lock present on node " + nodeId);
        }
    }

    public String getLockToken() {
        return activeLock.getToken();
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
}