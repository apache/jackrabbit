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

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;

import org.apache.jackrabbit.rmi.iterator.ArrayNodeIterator;
import org.apache.jackrabbit.rmi.iterator.ArrayRowIterator;
import org.apache.jackrabbit.rmi.remote.RemoteNode;
import org.apache.jackrabbit.rmi.remote.RemoteQueryResult;
import org.apache.jackrabbit.rmi.remote.RemoteRow;

/**
 * Local adapter for the JCR-RMI
 * {@link RemoteQueryResult RemoteQueryResult}
 * inteface. This class makes a remote query result locally available using
 * the JCR {@link QueryResult QueryResult} interface.
 *
 * @author Philipp Koch
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
    public String[] getPropertyNames() throws RepositoryException {
        try {
            return remote.getPropertyNames();
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public RowIterator getRows() throws RepositoryException {
        try {
            RemoteRow[] remotes =  remote.getRows();
            if (remotes != null) {
                Row[] rows = new Row[remotes.length];
                for (int i = 0; i < rows.length; i++) {
                    rows[i] = getFactory().getRow(remotes[i]);
                }
                return new ArrayRowIterator(rows);
            } else {
                return new ArrayRowIterator(new Row[0]);
            }
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public NodeIterator getNodes() throws RepositoryException {
        try {
            RemoteNode[] remotes = remote.getNodes();
            if (remotes != null) {
                Node[] nodes = new Node[remotes.length];
                for (int i = 0; i < nodes.length; i++) {
                    nodes[i] = getFactory().getNode(session, remotes[i]);
                }
                return new ArrayNodeIterator(nodes);
            } else {
                return new ArrayNodeIterator(new Node[0]);
            }
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

}
