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

import javax.jcr.ItemNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

/**
 * Remote version of the JCR {@link javax.jcr.query.Row Row} interface.
 * Used by the  {@link org.apache.jackrabbit.rmi.server.ServerRow ServerRow}
 * and {@link org.apache.jackrabbit.rmi.client.ClientRow ClientRow}
 * adapter base classes to provide transparent RMI access to remote items.
 * <p>
 * RMI errors are signalled with RemoteExceptions.
 *
 * @author Philipp Koch
 * @see javax.jcr.query.Row
 * @see org.apache.jackrabbit.rmi.client.ClientRow
 * @see org.apache.jackrabbit.rmi.server.ServerRow
 */
public interface RemoteRow extends Remote {
    /**
     * @see javax.jcr.query.Row#getValues()
     *
     * @return a <code>Value</code> array.
     * @throws RepositoryException if an error occurs
     */
    public Value[] getValues() throws RepositoryException, RemoteException;

    /**
     * @see javax.jcr.query.Row#getValue(String) 
     *
     * @return a <code>Value</code>
     * @throws ItemNotFoundException if <code>propertyName</code> s not among the
     * column names of the query result table
     * @throws RepositoryException if anopther error occurs.
     */
    public Value getValue(String propertyName) throws ItemNotFoundException, RepositoryException, RemoteException;
}
