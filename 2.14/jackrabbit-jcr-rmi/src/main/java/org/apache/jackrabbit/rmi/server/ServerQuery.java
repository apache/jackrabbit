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
import javax.jcr.Value;
import javax.jcr.query.Query;

import org.apache.jackrabbit.rmi.remote.RemoteQuery;
import org.apache.jackrabbit.rmi.remote.RemoteQueryResult;
import org.apache.jackrabbit.rmi.remote.RemoteNode;

/**
 * Remote adapter for the JCR {@link javax.jcr.query.Query Query} interface.
 * This class makes a local session available as an RMI service using the
 * {@link org.apache.jackrabbit.rmi.remote.RemoteQuery RemoteQuery}
 * interface.
 *
 * @see javax.jcr.query.Query
 * @see org.apache.jackrabbit.rmi.remote.RemoteQuery
 */
public class ServerQuery extends ServerObject implements RemoteQuery {

    /** The adapted local query manager. */
    private Query query;

    /**
     * Creates a remote adapter for the given local <code>Query</code>.
     *
     * @param query local <code>Query</code>
     * @param factory remote adapter factory
     * @throws RemoteException on RMI errors
     */
    public ServerQuery(Query query, RemoteAdapterFactory factory)
            throws RemoteException {
        super(factory);
        this.query = query;
    }

    /** {@inheritDoc} */
    public RemoteQueryResult execute()
            throws RepositoryException, RemoteException {
        return getFactory().getRemoteQueryResult(query.execute());
    }

    /** {@inheritDoc} */
    public String getStatement() throws RemoteException {
        return query.getStatement();
    }

    /** {@inheritDoc} */
    public String getLanguage() throws RemoteException {
        return query.getLanguage();
    }

    /** {@inheritDoc} */
    public String getStoredQueryPath()
            throws RepositoryException, RemoteException {
        return query.getStoredQueryPath();
    }

    /** {@inheritDoc} */
    public RemoteNode storeAsNode(String absPath)
            throws RepositoryException, RemoteException {
        return getRemoteNode(query.storeAsNode(absPath));
    }

    /** {@inheritDoc} */
	public void bindValue(String varName, Value value)
			throws RepositoryException, RemoteException {
		query.bindValue(varName, value);
	}

    /** {@inheritDoc} */
	public String[] getBindVariableNames() 
			throws RepositoryException, RemoteException {
        try {
        	return query.getBindVariableNames();
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
	}

    /** {@inheritDoc} */
	public void setLimit(long limit) throws RemoteException {
    	query.setLimit(limit);
	}

    /** {@inheritDoc} */
	public void setOffset(long offset) throws RemoteException {
		query.setOffset(offset);
	}

}
