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
package org.apache.jackrabbit.webdav.simple;

import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavMethods;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.DavResourceFactory;
import org.apache.jackrabbit.webdav.DavResourceLocator;
import org.apache.jackrabbit.webdav.DavServletRequest;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.DavSession;
import org.apache.jackrabbit.webdav.jcr.JcrDavSession;
import org.apache.jackrabbit.webdav.lock.LockManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;

/**
 * ResourceFactoryImpl implements a simple DavResourceFactory
 */
public class ResourceFactoryImpl implements DavResourceFactory {

    private static Logger log =LoggerFactory.getLogger(ResourceFactoryImpl.class);

    private final LockManager lockMgr;
    private final ResourceConfig resourceConfig;

    /**
     * Create a new <code>ResourceFactory</code> that uses the given lock
     * manager and resource filter.
     *
     * @param lockMgr
     * @param resourceConfig
     */
    public ResourceFactoryImpl(LockManager lockMgr, ResourceConfig resourceConfig) {
        this.lockMgr = lockMgr;
        this.resourceConfig = resourceConfig;
    }

    /**
     * Create a new <code>DavResource</code> from the given locator and
     * request.
     *
     * @param locator
     * @param request
     * @param response
     * @return DavResource
     * @throws DavException
     * @see DavResourceFactory#createResource(DavResourceLocator,
     *      DavServletRequest, DavServletResponse)
     */
    public DavResource createResource(DavResourceLocator locator, DavServletRequest request,
                                      DavServletResponse response) throws DavException {
        try {
            Node node = getNode(request.getDavSession(), locator);
            DavResource resource;
            if (node == null) {
                log.debug("Creating resource for non-existing repository node.");
                boolean isCollection = DavMethods.isCreateCollectionRequest(request);
                resource = createNullResource(locator, request.getDavSession(), isCollection);
            } else {
                resource = createResource(node, locator, request.getDavSession());
            }
            resource.addLockManager(lockMgr);
            return resource;
        } catch (RepositoryException e) {
            throw new DavException(DavServletResponse.SC_INTERNAL_SERVER_ERROR, e);
        }
    }

    /**
     * Create a new <code>DavResource</code> from the given locator and webdav
     * session.
     *
     * @param locator
     * @param session
     * @return
     * @throws DavException
     * @see DavResourceFactory#createResource(DavResourceLocator, DavSession)
     */
    public DavResource createResource(DavResourceLocator locator, DavSession session) throws DavException {
        try {
            Node node = getNode(session, locator);
            DavResource resource = createResource(node, locator, session);
            resource.addLockManager(lockMgr);
            return resource;
        } catch (RepositoryException e) {
            throw new DavException(DavServletResponse.SC_INTERNAL_SERVER_ERROR, e);
        }
    }

    /**
     * Returns the <code>Node</code> corresponding to the given locator or
     * <code>null</code> if it does not exist or if the existing item represents
     * a <code>Property</code>.
     *
     * @param sessionImpl
     * @param locator
     * @return
     * @throws RepositoryException
     */
    private Node getNode(DavSession sessionImpl, DavResourceLocator locator)
            throws RepositoryException {
        Node node = null;
        try {
            String repoPath = locator.getRepositoryPath();
            if (repoPath != null) {
                Session session = ((JcrDavSession)sessionImpl).getRepositorySession();
                Item item = session.getItem(repoPath);
                if (item instanceof Node) {
                    node = (Node)item;
                } // else: item is a property -> return null
            }
        } catch (PathNotFoundException e) {
            // item does not exist (yet). return null -> create null-resource
        }
        return node;
    }

    /**
     * Create a 'null resource'
     *
     * @param locator
     * @param session
     * @param isCollection
     * @return
     * @throws DavException
     */
    private DavResource createNullResource(DavResourceLocator locator,
                                           DavSession session,
                                           boolean isCollection) throws DavException {
        JcrDavSession.checkImplementation(session);
        JcrDavSession sessionImpl = (JcrDavSession)session;

        DavResource resource;
        if (versioningSupported(sessionImpl.getRepositorySession())) {
            resource = new VersionControlledResourceImpl(locator, this, sessionImpl, resourceConfig, isCollection);
        } else {
            resource = new DavResourceImpl(locator, this, sessionImpl, resourceConfig, isCollection);
        }
        return resource;
    }

    /**
     * Tries to retrieve the repository item defined by the locator's resource
     * path and build the corresponding WebDAV resource. If the repository
     * supports the versioning option different resources are created for
     * version, versionhistory and common nodes.
     *
     * @param node
     * @param locator
     * @param session
     * @return
     * @throws DavException
     */
    private DavResource createResource(Node node, DavResourceLocator locator,
                                       DavSession session) throws DavException {
        JcrDavSession.checkImplementation(session);
        JcrDavSession sessionImpl = (JcrDavSession)session;

        DavResource resource;
        if (versioningSupported(sessionImpl.getRepositorySession())) {
            // create special resources for Version and VersionHistory
            if (node instanceof Version) {
                resource = new VersionResourceImpl(locator, this, sessionImpl, resourceConfig, node);
            }  else if (node instanceof VersionHistory) {
                resource = new VersionHistoryResourceImpl(locator, this, sessionImpl, resourceConfig, node);
            } else {
                resource = new VersionControlledResourceImpl(locator, this, sessionImpl, resourceConfig, node);
            }
        } else {
            resource = new DavResourceImpl(locator, this, session, resourceConfig, node);
        }
        return resource;
    }

    /**
     * @param repoSession
     * @return true if the JCR repository supports versioning.
     */
    private static boolean versioningSupported(Session repoSession) {
        String desc = repoSession.getRepository().getDescriptor(Repository.OPTION_VERSIONING_SUPPORTED);
        return Boolean.valueOf(desc);
    }
}
