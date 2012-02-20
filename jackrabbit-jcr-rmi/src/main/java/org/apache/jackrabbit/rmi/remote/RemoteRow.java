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
 * Remote version of the JCR {@link javax.jcr.query.Row Row} interface.
 * Used by the  {@link org.apache.jackrabbit.rmi.server.ServerRow ServerRow}
 * and {@link org.apache.jackrabbit.rmi.client.ClientRow ClientRow}
 * adapter base classes to provide transparent RMI access to remote items.
 * <p>
 * RMI errors are signaled with RemoteExceptions.
 *
 * @see javax.jcr.query.Row
 * @see org.apache.jackrabbit.rmi.client.ClientRow
 * @see org.apache.jackrabbit.rmi.server.ServerRow
 */
public interface RemoteRow extends Remote {

    /**
     * @see javax.jcr.query.Row#getValues()
     *
     * @return row values
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
    Value[] getValues() throws RepositoryException, RemoteException;

    /**
     * @see javax.jcr.query.Row#getValue(String)
     *
     * @param propertyName property name
     * @return identified value
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
    Value getValue(String propertyName)
            throws RepositoryException, RemoteException;

    /**
     * @see javax.jcr.query.Row#getNode()
     *
     * @return a node
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
	RemoteNode getNode() throws RepositoryException, RemoteException;

    /**
     * @see javax.jcr.query.Row#getNode(String)
     *
     * @param selectorName
     * @return a node
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
	RemoteNode getNode(String selectorName) throws RepositoryException, RemoteException;

    /**
     * @see javax.jcr.query.Row#getPath()
     *
     * @return the path
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
	String getPath() throws RepositoryException, RemoteException;

    /**
     * @see javax.jcr.query.Row#getPath(String)
     *
     * @param selectorName
     * @return the path
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
	String getPath(String selectorName) throws RepositoryException, RemoteException;

    /**
     * @see javax.jcr.query.Row#getScore()
     *
     * @return the score
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
	double getScore() throws RepositoryException, RemoteException;

    /**
     * @see javax.jcr.query.Row#getScore(String)
     *
     * @param selectorName
     * @return the score
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
	double getScore(String selectorName) throws RepositoryException, RemoteException;
}
