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
package org.apache.jackrabbit.rmi.remote;

import java.rmi.Remote;
import java.rmi.RemoteException;

import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;

/**
 * Remote version of the JCR {@link javax.jcr.query.Query Query} interface.
 * Used by the  {@link org.apache.jackrabbit.rmi.server.ServerQuery ServerQuery}
 * and {@link org.apache.jackrabbit.rmi.client.ClientQuery ClientQuery}
 * adapter base classes to provide transparent RMI access to remote items.
 * <p>
 * RMI errors are signalled with RemoteExceptions.
 *
 * @author Philipp Koch
 * @see javax.jcr.query.Query
 * @see org.apache.jackrabbit.rmi.client.ClientQuery
 * @see org.apache.jackrabbit.rmi.server.ServerQuery
 */
public interface RemoteQuery extends Remote {

    /**
     * @see javax.jcr.query.Query#execute()
     *
     * @return a <code>QueryResult</code>
     * @throws javax.jcr.RepositoryException if an error occurs
     */
    public RemoteQueryResult execute()
        throws RepositoryException, RemoteException;

    /**
     * @see javax.jcr.query.Query#getStatement()
     *
     * @return the query statement.
     */
    public String getStatement() throws RemoteException;

    /**
     * @see javax.jcr.query.Query#getLanguage()
     *
     * @return the query language.
     */
    public String getLanguage() throws RemoteException;

    /**
     * @see javax.jcr.query.Query#getPersistentQueryPath()
     *
     * @return path of the node representing this query.
     * @throws ItemNotFoundException if this query is not a persistent query.
     * @throws RepositoryException if another error occurs.
     */
    public String getPersistentQueryPath()
        throws ItemNotFoundException, RepositoryException, RemoteException;

    /**
     * @see javax.jcr.query.Query#save(String)
     *
     * @param absPath path at which to persist this query.
     * @throws ItemExistsException If an item already exists at the indicated position
     * @throws PathNotFoundException If the path cannot be found
     * @throws VersionException if the parent node of absPath is versionable and checked-in (and therefore read-only).
     * @throws ConstraintViolationException If creating the node would violate a
     * node type (or other implementation specific) constraint.
     * @throws LockException if a lock prevents the save.
     * @throws UnsupportedRepositoryOperationException in a level 1 implementation.
     * @throws RepositoryException If another error occurs.
     */
    public void save(String absPath) throws ItemExistsException,
        PathNotFoundException, VersionException, ConstraintViolationException,
        LockException, UnsupportedRepositoryOperationException,
        RepositoryException, RemoteException;
    
}
