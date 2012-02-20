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

import javax.jcr.RepositoryException;
import javax.jcr.query.QueryResult;

import org.apache.jackrabbit.rmi.remote.RemoteIterator;
import org.apache.jackrabbit.rmi.remote.RemoteQueryResult;

/**
 * Remote adapter for the JCR {@link javax.jcr.query.QueryResult QueryResult} interface.
 * This class makes a local session available as an RMI service using the
 * {@link org.apache.jackrabbit.rmi.remote.RemoteQueryResult RemoteQueryResult}
 * interface.
 *
 * @see javax.jcr.query.QueryResult
 * @see org.apache.jackrabbit.rmi.remote.RemoteQueryResult
 */
public class ServerQueryResult extends ServerObject
        implements RemoteQueryResult {

    /** The adapted local query result. */
    private QueryResult result;

    /**
     * Creates a remote adapter for the given local <code>QueryResult</code>.
     *
     * @param result local <code>QueryResult</code>
     * @param factory remote adapter factory
     * @throws RemoteException on RMI errors
     */
    public ServerQueryResult(QueryResult result, RemoteAdapterFactory factory)
            throws RemoteException {
        super(factory);
        this.result = result;
    }

    /** {@inheritDoc} */
    public String[] getColumnNames()
            throws RepositoryException, RemoteException {
        return result.getColumnNames();
    }

    /** {@inheritDoc} */
    public RemoteIterator getRows() throws RepositoryException, RemoteException {
        return getFactory().getRemoteRowIterator(result.getRows());
    }

    /** {@inheritDoc} */
    public RemoteIterator getNodes() throws RepositoryException, RemoteException {
        return getFactory().getRemoteNodeIterator(result.getNodes());
    }

    /** {@inheritDoc} */
	public String[] getSelectorNames() throws RepositoryException, RemoteException {
		return result.getSelectorNames();
	}

}
