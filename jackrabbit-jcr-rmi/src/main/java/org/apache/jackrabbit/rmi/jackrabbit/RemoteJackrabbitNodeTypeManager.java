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
package org.apache.jackrabbit.rmi.jackrabbit;

import java.rmi.RemoteException;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.api.JackrabbitNodeTypeManager;
import org.apache.jackrabbit.rmi.remote.RemoteNodeType;
import org.apache.jackrabbit.rmi.remote.RemoteNodeTypeManager;

/**
 * Remote version of the {@link JackrabbitNodeTypeManager} extension interface.
 */
public interface RemoteJackrabbitNodeTypeManager extends RemoteNodeTypeManager {

    /**
     * Checks if the named node type exists.
     *
     * @param name node type name
     * @return <code>true</code> if the named node type exists,
     *         <code>false</code> otherwise
     * @throws RepositoryException if a repository error occurs
     * @throws RemoteException if a remote error occurs.
     */
    boolean hasNodeType(String name)
        throws RepositoryException, RemoteException;

    /**
     * Registers node types defined in the given node type definitions.
     *
     * @param content node type definitions
     * @param contentType type of the node type definitions
     * @return registered node types
     * @throws RepositoryException if a repository error occurs
     * @throws RemoteException if a remote error occurs.
     */
    RemoteNodeType[] registerNodeTypes(byte[] content, String contentType)
        throws RepositoryException, RemoteException;

}
