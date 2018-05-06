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
import javax.jcr.Value;

/**
 * Remote version of the JCR {@link javax.jcr.query.Query Query} interface.
 * Used by the  {@link org.apache.jackrabbit.rmi.server.ServerQuery ServerQuery}
 * and {@link org.apache.jackrabbit.rmi.client.ClientQuery ClientQuery}
 * adapter base classes to provide transparent RMI access to remote items.
 * <p>
 * RMI errors are signaled with RemoteExceptions.
 *
 * @see javax.jcr.query.Query
 * @see org.apache.jackrabbit.rmi.client.ClientQuery
 * @see org.apache.jackrabbit.rmi.server.ServerQuery
 */
public interface RemoteQuery extends Remote {

    /**
     * @see javax.jcr.query.Query#execute()
     *
     * @return a <code>QueryResult</code>
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
    RemoteQueryResult execute() throws RepositoryException, RemoteException;

    /**
     * @see javax.jcr.query.Query#setLimit(long)
     *
     * @param limit a <code>long</code>
     * @throws RemoteException on RMI errors
     */
    void setLimit(long limit) throws RemoteException;

    /**
     * @see javax.jcr.query.Query#setOffset(long)
     *
     * @param offset a <code>long</code>
     * @throws RemoteException on RMI errors
     */
    void setOffset(long offset) throws RemoteException;

    /**
     * @see javax.jcr.query.Query#getStatement()
     *
     * @return the query statement.
     * @throws RemoteException on RMI errors
     */
    String getStatement() throws RemoteException;

    /**
     * @see javax.jcr.query.Query#getLanguage()
     *
     * @return the query language.
     * @throws RemoteException on RMI errors
     */
    String getLanguage() throws RemoteException;

    /**
     * @see javax.jcr.query.Query#getStoredQueryPath()
     *
     * @return path of the node representing this query.
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
    String getStoredQueryPath() throws RepositoryException, RemoteException;

    /**
     * @see javax.jcr.query.Query#storeAsNode(String)
     *
     * @param absPath path at which to persist this query.
     * @return stored node
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
    RemoteNode storeAsNode(String absPath) throws RepositoryException, RemoteException;

    /**
     * @see javax.jcr.query.Query#bindValue(String, Value)
     *
     * @param varName name of variable in query
     * @param value   value to bind
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
    void bindValue(String varName, Value value) throws RepositoryException, RemoteException;

    /**
     * @see javax.jcr.query.Query#getBindVariableNames()
     *
     * @return the names of the bind variables in this query.
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
    public String[] getBindVariableNames() throws RepositoryException, RemoteException;
}
