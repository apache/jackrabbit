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
package org.apache.jackrabbit.rmi.server;

import java.rmi.RemoteException;

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.QueryResult;
import javax.jcr.query.RowIterator;

import org.apache.jackrabbit.rmi.remote.RemoteNode;
import org.apache.jackrabbit.rmi.remote.RemoteQueryResult;
import org.apache.jackrabbit.rmi.remote.RemoteRow;

/**
 * Remote adapter for the JCR {@link javax.jcr.query.QueryResult QueryResult} interface.
 * This class makes a local session available as an RMI service using the
 * {@link org.apache.jackrabbit.rmi.remote.RemoteQueryResult RemoteQueryResult}
 * interface.
 *
 * @author Philipp Koch
 * @see javax.jcr.query.QueryResult
 * @see org.apache.jackrabbit.rmi.remote.RemoteQueryResult
 */
public class ServerQueryResult extends ServerObject implements RemoteQueryResult {

    /** The adapted local query result. */
    protected QueryResult result;

    /**
     * Creates a remote adapter for the given local <code>QueryResult</code>.
     *
     * @param result local <code>QueryResult</code>
     * @param factory remote adapter factory
     * @throws RemoteException on RMI errors
     */
    public ServerQueryResult(QueryResult result, RemoteAdapterFactory factory) throws RemoteException {
        super(factory);
        this.result = result;
    }

    /** {@inheritDoc} */
    public String[] getPropertyNames() throws RepositoryException, RemoteException {
        return result.getPropertyNames();
    }

    /** {@inheritDoc} */
    public RemoteRow[] getRows() throws RepositoryException, RemoteException {
        RowIterator iterator = result.getRows();
        if (iterator == null) {
            return new RemoteRow[0]; // for safety
        }

        RemoteRow[] remotes = new RemoteRow[(int) iterator.getSize()];
        for (int i = 0; iterator != null && iterator.hasNext(); i++) {
            remotes[i] = new ServerRow(iterator.nextRow(), factory);
        }
        return remotes;

    }

    /** {@inheritDoc} */
    public RemoteNode[] getNodes() throws RepositoryException, RemoteException {
        NodeIterator iterator = result.getNodes();
        if (iterator == null) {
            return new RemoteNode[0]; // for safety
        }

        RemoteNode[] remotes = new RemoteNode[(int) iterator.getSize()];
        for (int i = 0; iterator != null && iterator.hasNext(); i++) {
            remotes[i] = factory.getRemoteNode(iterator.nextNode());
        }
        return remotes;
    }
}
