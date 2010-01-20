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
package org.apache.jackrabbit.rmi.server;

import java.rmi.RemoteException;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;

import org.apache.jackrabbit.rmi.remote.RemoteQuery;
import org.apache.jackrabbit.rmi.remote.RemoteQueryManager;

/**
 * Remote adapter for the JCR {@link javax.jcr.query.QueryManager QueryManager}
 * interface. This class makes a local query manager available as an RMI
 * service using the
 * {@link org.apache.jackrabbit.rmi.remote.RemoteQueryManager RemoteQueryManager}
 * interface.
 *
 * @see javax.jcr.query.QueryManager
 * @see org.apache.jackrabbit.rmi.remote.RemoteQueryManager
 */
public class ServerQueryManager extends ServerObject
        implements RemoteQueryManager {

    /** The current session. */
    private Session session;

    /** The adapted local query manager. */
    private QueryManager manager;

    /**
     * Creates a remote adapter for the given local query manager.
     *
     * @param session current session
     * @param manager local query manager
     * @param factory remote adapter factory
     * @throws RemoteException on RMI errors
     */
    public ServerQueryManager(
            Session session, QueryManager manager, ServerAdapterFactory factory)
            throws RemoteException {
        super(factory);
        this.session = session;
        this.manager = manager;
    }

    /** {@inheritDoc} */
    public RemoteQuery createQuery(String statement, String language)
            throws RepositoryException, RemoteException {
        try {
            Query query = manager.createQuery(statement, language);
            return getFactory().getRemoteQuery(query);
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public RemoteQuery getQuery(String absPath)
            throws RepositoryException, RemoteException {
        try {
            Item item = session.getItem(absPath);
            if (!item.isNode()) {
                throw new InvalidQueryException("Not a query node: " + absPath);
            }
            return getFactory().getRemoteQuery(manager.getQuery((Node) item));
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public String[] getSupportedQueryLanguages()
            throws RepositoryException, RemoteException {
        try {
        return manager.getSupportedQueryLanguages();
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
    }
    }

}
