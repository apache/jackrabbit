/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.trace;

import java.io.IOException;
import java.io.InputStream;

import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.InvalidSerializedDataException;
import javax.jcr.ItemExistsException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Workspace;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.observation.ObservationManager;
import javax.jcr.query.QueryManager;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;

import org.apache.jackrabbit.decorator.DecoratorFactory;
import org.apache.jackrabbit.decorator.WorkspaceDecorator;
import org.xml.sax.ContentHandler;

/**
 * TODO
 */
public class TraceWorkspaceDecorator extends WorkspaceDecorator implements
        Workspace {
    
    private TraceLogger logger;
    
    public TraceWorkspaceDecorator(DecoratorFactory factory,
            Session session, Workspace workspace, TraceLogger logger) {
        super(factory, session, workspace);
        this.logger = logger;
    }
    

    public Session getSession() {
        logger.trace("getSession");
        return super.getSession();
    }
    
    /**
     * Logs the method call and forwards it to the underlying workspace.
     * {@inheritDoc}
     */
    public void copy(String srcAbsPath, String destAbsPath)
            throws ConstraintViolationException, VersionException,
            AccessDeniedException, PathNotFoundException, ItemExistsException,
            LockException, RepositoryException {
        logger.trace("copy", srcAbsPath, destAbsPath);
        super.copy(srcAbsPath, destAbsPath);
    }
    
    /**
     * Logs the method call and forwards it to the underlying workspace.
     * {@inheritDoc}
     */
    public void copy(String srcWorkspace, String srcAbsPath, String destAbsPath)
            throws NoSuchWorkspaceException, ConstraintViolationException,
            VersionException, AccessDeniedException, PathNotFoundException,
            ItemExistsException, LockException, RepositoryException {
        logger.trace("copy", srcWorkspace, srcAbsPath, destAbsPath);
        super.copy(srcWorkspace, srcAbsPath, destAbsPath);
    }

    /**
     * Logs the method call and forwards it to the underlying workspace.
     * {@inheritDoc}
     */
    public void clone(String srcWorkspace, String srcAbsPath,
            String destAbsPath, boolean removeExisting)
            throws NoSuchWorkspaceException, ConstraintViolationException,
            VersionException, AccessDeniedException, PathNotFoundException,
            ItemExistsException, LockException, RepositoryException {
        logger.trace("clone",
                srcWorkspace, srcAbsPath, destAbsPath, removeExisting);
        super.clone(srcWorkspace, srcAbsPath, destAbsPath, removeExisting);
    }
    
    /**
     * Logs the method call and forwards it to the underlying workspace.
     * {@inheritDoc}
     */
    public String[] getAccessibleWorkspaceNames() throws RepositoryException {
        logger.trace("getAccessibleWorkspaceNames");
        return super.getAccessibleWorkspaceNames();
    }

    /**
     * Logs the method call and forwards it to the underlying workspace.
     * {@inheritDoc}
     */
    public ContentHandler getImportContentHandler(String parentAbsPath,
            int uuidBehaviour) throws PathNotFoundException,
            ConstraintViolationException, VersionException, LockException,
            RepositoryException {
        logger.trace("getImportContentHandler", parentAbsPath, uuidBehaviour);
        return super.getImportContentHandler(parentAbsPath, uuidBehaviour);
    }
    
    /**
     * Logs the method call and forwards it to the underlying workspace.
     * {@inheritDoc}
     */
    public String getName() {
        logger.trace("getName");
        return super.getName();
    }
    
    /**
     * Logs the method call and forwards it to the underlying workspace.
     * {@inheritDoc}
     */
    public NamespaceRegistry getNamespaceRegistry() throws RepositoryException {
        logger.trace("getNamespaceRegistry");
        return super.getNamespaceRegistry();
    }

    /**
     * Logs the method call and forwards it to the underlying workspace.
     * {@inheritDoc}
     */
    public NodeTypeManager getNodeTypeManager() throws RepositoryException {
        logger.trace("getNodeTypeManager");
        return super.getNodeTypeManager();
    }

    /**
     * Logs the method call and forwards it to the underlying workspace.
     * {@inheritDoc}
     */
    public ObservationManager getObservationManager() throws
            UnsupportedRepositoryOperationException, RepositoryException {
        logger.trace("getObservationManager");
        return super.getObservationManager();
    }
    
    /**
     * Logs the method call and forwards it to the underlying workspace.
     * {@inheritDoc}
     */
    public QueryManager getQueryManager() throws RepositoryException {
        logger.trace("getQueryManager");
        return super.getQueryManager();
    }
    
    /**
     * Logs the method call and forwards it to the underlying workspace.
     * {@inheritDoc}
     */
    public void importXML(String parentAbsPath, InputStream in,
            int uuidBehaviour) throws IOException, PathNotFoundException,
            ItemExistsException, ConstraintViolationException,
            InvalidSerializedDataException, LockException, RepositoryException {
        logger.trace("importXML", parentAbsPath, in, uuidBehaviour);
        super.importXML(parentAbsPath, in, uuidBehaviour);
    }
    
    /**
     * Logs the method call and forwards it to the underlying workspace.
     * {@inheritDoc}
     */
    public void move(String srcAbsPath, String destAbsPath)
            throws ConstraintViolationException, VersionException,
            AccessDeniedException, PathNotFoundException, ItemExistsException,
            LockException, RepositoryException {
        logger.trace("move", srcAbsPath, destAbsPath);
        super.move(srcAbsPath, destAbsPath);
    }
    
    /**
     * Logs the method call and forwards it to the underlying workspace.
     * {@inheritDoc}
     */
    public void restore(Version[] versions, boolean removeExisting)
            throws ItemExistsException,
            UnsupportedRepositoryOperationException, VersionException,
            LockException, InvalidItemStateException, RepositoryException {
        logger.trace("restore", versions, removeExisting);
        super.restore(versions, removeExisting);
    }
}
