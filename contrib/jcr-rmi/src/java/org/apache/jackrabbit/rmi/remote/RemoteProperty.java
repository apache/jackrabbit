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

import java.rmi.RemoteException;

import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;


/**
 * Remote version of the JCR {@link javax.jcr.Property Property} interface.
 * Used by the
 * {@link org.apache.jackrabbit.rmi.server.ServerProperty ServerProperty}
 * and {@link org.apache.jackrabbit.rmi.client.ClientProperty ClientProperty}
 * adapters to provide transparent RMI access to remote properties.
 * <p>
 * The methods in this interface are documented only with a reference
 * to a corresponding Property method. The remote object will simply forward
 * the method call to the underlying Property instance. Argument and return
 * values, as well as possible exceptions, are copied over the network.
 * Compex {@link javax.jcr.nodetype.PropertyDef PropertyDef} return values
 * are returned as remote references to the corresponding
 * {@link org.apache.jackrabbit.rmi.remote.RemotePropertyDef RemotePropertyDef}
 * interface. RMI errors are signalled with RemoteExceptions.
 * <p>
 * Note that only the generic getValue and setValue methods are included
 * in this interface. Clients should implement the type-specific value
 * getters and setters wrapping using the generic methods. Note also that
 * the Value objects must be serializable and implemented using classes
 * available on both the client and server side. The
 * {@link org.apache.jackrabbit.rmi.remote.SerialValue SerialValue}
 * decorator utility provides a convenient way to satisfy these
 * requirements.
 *
 * @author Jukka Zitting
 * @see javax.jcr.Property
 * @see org.apache.jackrabbit.rmi.client.ClientProperty
 * @see org.apache.jackrabbit.rmi.server.ServerProperty
 */
public interface RemoteProperty extends RemoteItem {
    
    /**
     * @see javax.jcr.Property#getValue()
     * @throws RemoteException on RMI exceptions
     */
    public Value getValue()
        throws ValueFormatException, RepositoryException, RemoteException;

    /**
     * @see javax.jcr.Property#getValues()
     * @throws RemoteException on RMI exceptions
     */
    public Value[] getValues()
        throws ValueFormatException, RepositoryException, RemoteException;

    /**
     * @see javax.jcr.Property#setValue(javax.jcr.Value)
     * @throws RemoteException on RMI exceptions
     */
    public void setValue(Value value)
        throws ValueFormatException, RepositoryException, RemoteException;

    /**
     * @see javax.jcr.Property#setValue(javax.jcr.Value[])
     * @throws RemoteException on RMI exceptions
     */
    public void setValue(Value[] values)
        throws ValueFormatException, RepositoryException, RemoteException;

    /**
     * @see javax.jcr.Property#getLength()
     * @throws RemoteException on RMI exceptions
     */
    public long getLength()
        throws ValueFormatException, RepositoryException, RemoteException;

    /**
     * @see javax.jcr.Property#getLengths()
     * @throws RemoteException on RMI exceptions
     */
    public long[] getLengths()
        throws ValueFormatException, RepositoryException, RemoteException;

    /**
     * @see javax.jcr.Property#getDefinition()
     * @throws RemoteException on RMI exceptions
     */
    public RemotePropertyDef getDefinition() throws RepositoryException,
        RemoteException;

    /**
     * @see javax.jcr.Property#getType()
     * @throws RemoteException on RMI exceptions
     */
    public int getType() throws RepositoryException, RemoteException;

}
