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
package org.apache.jackrabbit.rmi.remote;

import java.rmi.Remote;
import java.rmi.RemoteException;

import javax.jcr.RepositoryException;
import javax.jcr.query.Query;

/**
 * Remote version of the JCR {@link javax.jcr.query.QueryManager QueryManager} interface.
 * Used by the  {@link org.apache.jackrabbit.rmi.server.ServerQueryManager ServerQueryManager}
 * and {@link org.apache.jackrabbit.rmi.client.ClientQueryManager ClientQueryManager}
 * adapter base classes to provide transparent RMI access to remote items.
 * <p>
 * RMI errors are signaled with RemoteExceptions.
 *
 * @see javax.jcr.query.QueryManager
 * @see org.apache.jackrabbit.rmi.client.ClientQueryManager
 * @see org.apache.jackrabbit.rmi.server.ServerQueryManager
 */
public interface RemoteQueryManager extends Remote {

    /**
     * @see javax.jcr.query.QueryManager#createQuery
     *
     * @param statement query statement
     * @param language query language
     * @return query
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
    RemoteQuery createQuery(String statement, String language)
            throws RepositoryException, RemoteException;

    /**
     * @see javax.jcr.query.QueryManager#getQuery
     *
     * @param absPath node path of a persisted query (that is, a node of type <code>nt:query</code>).
     * @return a <code>Query</code> object.
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
    RemoteQuery getQuery(String absPath)
            throws RepositoryException, RemoteException;

    /**
     * See {@link Query}.
     * 
     * @see javax.jcr.query.QueryManager#getSupportedQueryLanguages
     * @return An string array.
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
    String[] getSupportedQueryLanguages()
            throws RepositoryException, RemoteException;

}
