/*
 * Copyright 2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
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

package org.apache.jackrabbit.server.simple.dav;

import org.apache.jackrabbit.webdav.*;
import org.apache.jackrabbit.webdav.spi.JcrDavException;
import org.apache.jackrabbit.webdav.lock.LockManager;

import javax.jcr.RepositoryException;

/**
 * ResourceFactoryImpl implements a simple DavResourceFactory
 */
public class ResourceFactoryImpl implements DavResourceFactory {

    private final LockManager lockMgr;

    public ResourceFactoryImpl(LockManager lockMgr) {
        this.lockMgr = lockMgr;
    }

    public DavResource createResource(DavResourceLocator locator, DavServletRequest request,
                                      DavServletResponse response) throws DavException {
        return createResource(locator, request.getDavSession());
    }

    public DavResource createResource(DavResourceLocator locator, DavSession session) throws DavException {
        try {
            DavResource res = new DavResourceImpl(locator, this, session);
            res.addLockManager(lockMgr);
            return res;
        } catch (RepositoryException e) {
            throw new JcrDavException(e);
        }
    }
}
