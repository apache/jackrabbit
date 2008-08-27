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
package org.apache.jackrabbit.core.retention;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.jackrabbit.api.jsr283.retention.RetentionManager;
import org.apache.jackrabbit.api.jsr283.retention.Hold;
import org.apache.jackrabbit.api.jsr283.retention.RetentionPolicy;

import javax.jcr.PathNotFoundException;
import javax.jcr.AccessDeniedException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.version.VersionException;
import javax.jcr.lock.LockException;

/**
 * <code>RetentionManagerImpl</code>...
 */
public class RetentionManagerImpl implements RetentionManager {

    private static Logger log = LoggerFactory.getLogger(RetentionManagerImpl.class);

    private final Session session;
    
    public RetentionManagerImpl(Session session) {
        this.session = session;
    }

    public Hold[] getHolds(String absPath) throws PathNotFoundException,
            AccessDeniedException, RepositoryException {
        //TODO
        return new Hold[0];
    }

    public Hold addHold(String absPath, String name, boolean isDeep) throws
            PathNotFoundException, AccessDeniedException, LockException,
            VersionException, RepositoryException {
        //TODO
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public void removeHold(String absPath, Hold hold) throws
            PathNotFoundException, AccessDeniedException, LockException,
            VersionException, RepositoryException {
        //TODO
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public RetentionPolicy getRetentionPolicy(String absPath) throws
            PathNotFoundException, AccessDeniedException, RepositoryException {
        // TODO
        return null;
    }

    public void setRetentionPolicy(String absPath, RetentionPolicy retentionPolicy)
            throws PathNotFoundException, AccessDeniedException, LockException,
            VersionException, RepositoryException {
        //TODO
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public void removeRetentionPolicy(String absPath) throws
            PathNotFoundException, AccessDeniedException, LockException,
            VersionException, RepositoryException {
        //TODO
        throw new UnsupportedOperationException("Not yet implemented");
    }
}