/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
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
package org.apache.jackrabbit.rmi.client;

import java.rmi.RemoteException;

import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.jcr.version.VersionException;

import org.apache.jackrabbit.rmi.remote.RemoteQuery;

/**
 * Local adapter for the JCR-RMI
 * {@link RemoteQuery RemoteQuery}
 * inteface. This class makes a remote query locally available using
 * the JCR {@link Query Query} interface.
 *
 * @author Philipp Koch
 * @see javax.jcr.query.Query Query
 * @see org.apache.jackrabbit.rmi.remote.RemoteQuery
 */
public class ClientQuery extends ClientObject implements Query {

    /** The current session */
    private Session session;
    
    /** The adapted remote query manager. */
    private RemoteQuery remote;

    /**
     * Creates a client adapter for the given query.
     *
     * @param session current session
     * @param remote remote query
     * @param factory adapter factory
     */
    public ClientQuery(
            Session session, RemoteQuery remote, LocalAdapterFactory factory) {
        super(factory);
        this.session = session;
        this.remote = remote;
    }

    /** {@inheritDoc} */
    public QueryResult execute() throws RepositoryException {
        try {
            return factory.getQueryResult(session, remote.execute());
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public String getStatement() {
        try {
            return remote.getStatement();
        } catch (RemoteException ex) {
            throw new RemoteRuntimeException(ex);
        }
    }

    /** {@inheritDoc} */
    public String getLanguage() {
        try {
            return remote.getLanguage();
        } catch (RemoteException ex) {
            throw new RemoteRuntimeException(ex);
        }
    }

    /** {@inheritDoc} */
    public String getPersistentQueryPath()
            throws ItemNotFoundException, RepositoryException {
        try {
            return remote.getPersistentQueryPath();
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }
    
    /** {@inheritDoc} */
    public void save(String absPath) throws ItemExistsException,
            PathNotFoundException, VersionException,
            ConstraintViolationException, LockException,
            UnsupportedRepositoryOperationException, RepositoryException {
        try {
            remote.save(absPath);
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }
}
