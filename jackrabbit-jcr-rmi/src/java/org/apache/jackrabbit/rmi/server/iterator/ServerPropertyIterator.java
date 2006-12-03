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
package org.apache.jackrabbit.rmi.server.iterator;

import java.rmi.RemoteException;

import javax.jcr.Property;
import javax.jcr.PropertyIterator;

import org.apache.jackrabbit.rmi.server.RemoteAdapterFactory;

/**
 * A ServerIterator for iterating properties.
 */
public class ServerPropertyIterator extends ServerIterator {

    /**
     * Creates a ServerPropertyIterator instance.
     *
     * @param iterator      local property iterator
     * @param factory       remote adapter factory
     * @param maxBufferSize maximum size of the element buffer
     * @throws RemoteException on RMI errors
     */
    public ServerPropertyIterator(
            PropertyIterator iterator, RemoteAdapterFactory factory,
            int maxBufferSize) throws RemoteException {
        super(iterator, factory, maxBufferSize);
    }

    /**
     * Creates and returns a remote adapter for the given property.
     *
     * @param object local object
     * @return remote adapter
     * @throws RemoteException on RMI errors
     * @see ServerIterator#getRemoteObject(Object)
     */
    protected Object getRemoteObject(Object object) throws RemoteException {
        return getFactory().getRemoteProperty((Property) object);
    }

}
