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

import javax.jcr.Value;

/**
 * Remote version of the JCR {@link javax.jcr.nodetype.PropertyDef PropertyDef}
 * interface. Used by the
 * {@link org.apache.jackrabbit.rmi.server.ServerPropertyDef ServerPropertyDef}
 * and
 * {@link org.apache.jackrabbit.rmi.client.ClientPropertyDef ClientPropertyDef}
 * adapters to provide transparent RMI access to remote property definitions.
 * <p>
 * The methods in this interface are documented only with a reference
 * to a corresponding PropertyDef method. The remote object will simply
 * forward the method call to the underlying PropertyDef instance. Return
 * values and possible exceptions are copied over the network. RMI errors
 * are signalled with RemoteExceptions.
 * <p>
 * Note that returned Value objects must be serializable and implemented
 * using classes available on both the client and server side. The
 * {@link org.apache.jackrabbit.rmi.remote.SerialValue SerialValue}
 * decorator utility provides a convenient way to satisfy these
 * requirements.
 *
 * @author Jukka Zitting
 * @see javax.jcr.nodetype.PropertyDef
 * @see org.apache.jackrabbit.rmi.client.ClientPropertyDef
 * @see org.apache.jackrabbit.rmi.server.ServerPropertyDef
 */
public interface RemotePropertyDef extends RemoteItemDef {

    /**
     * Remote version of the
     * {@link javax.jcr.nodetype.PropertyDef#getRequiredType() PropertyDef.getRequiredType()}
     * method.
     *
     * @return required type
     * @throws RemoteException on RMI errors
     */
    int getRequiredType() throws RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.nodetype.PropertyDef#getValueConstraints() PropertyDef.getValueConstraints()}
     * method.
     *
     * @return value constraints
     * @throws RemoteException on RMI errors
     */
    String[] getValueConstraints() throws RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.nodetype.PropertyDef#getDefaultValues() PropertyDef.getDefaultValues()}
     * method.
     *
     * @return default values
     * @throws RemoteException on RMI errors
     */
    Value[] getDefaultValues() throws RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.nodetype.PropertyDef#isMultiple() PropertyDef.isMultiple()}
     * method.
     *
     * @return <code>true</code> if the property is multi-valued,
     *         <code>false</code> otherwise
     * @throws RemoteException on RMI errors
     */
    boolean isMultiple() throws RemoteException;

}
