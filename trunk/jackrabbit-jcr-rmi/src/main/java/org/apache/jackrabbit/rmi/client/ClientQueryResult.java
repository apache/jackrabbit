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
package org.apache.jackrabbit.rmi.client;

import java.rmi.RemoteException;

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.QueryResult;
import javax.jcr.query.RowIterator;

import org.apache.jackrabbit.rmi.remote.RemoteQueryResult;

/**
 * Local adapter for the JCR-RMI
 * {@link RemoteQueryResult RemoteQueryResult}
 * interface. This class makes a remote query result locally available using
 * the JCR {@link QueryResult QueryResult} interface.
 *
 * @see javax.jcr.query.QueryResult QueryResult
 * @see org.apache.jackrabbit.rmi.remote.RemoteQueryResult
 */
public class ClientQueryResult extends ClientObject implements QueryResult {

    /** The current session */
    private Session session;

    /** The adapted remote query result. */
    private RemoteQueryResult remote;

    /**
     * Creates a client adapter for the given remote query result.
     *
     * @param session current session
     * @param remote remote query result
     * @param factory adapter factory
     */
    public ClientQueryResult(
            Session session, RemoteQueryResult remote,
            LocalAdapterFactory factory) {
        super(factory);
        this.session = session;
        this.remote = remote;
    }

    /** {@inheritDoc} */
    public String[] getColumnNames() throws RepositoryException {
        try {
            return remote.getColumnNames();
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public RowIterator getRows() throws RepositoryException {
        try {
            return getFactory().getRowIterator(session, remote.getRows());
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public NodeIterator getNodes() throws RepositoryException {
        try {
            return getFactory().getNodeIterator(session, remote.getNodes());
        } catch (RemoteException e) {
            throw new RemoteRepositoryException(e);
        }
    }

    /** {@inheritDoc} */
    public String[] getSelectorNames() throws RepositoryException {
        try {
            return remote.getSelectorNames();
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

}
