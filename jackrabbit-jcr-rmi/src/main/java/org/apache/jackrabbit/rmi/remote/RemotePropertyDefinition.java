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

import java.rmi.RemoteException;

import javax.jcr.Value;

/**
 * Remote version of the JCR {@link javax.jcr.nodetype.PropertyDefinition PropertyDefinition}
 * interface. Used by the
 * {@link org.apache.jackrabbit.rmi.server.ServerPropertyDefinition ServerPropertyDefinition}
 * and
 * {@link org.apache.jackrabbit.rmi.client.ClientPropertyDefinition ClientPropertyDefinition}
 * adapters to provide transparent RMI access to remote property definitions.
 * <p>
 * The methods in this interface are documented only with a reference
 * to a corresponding PropertyDef method. The remote object will simply
 * forward the method call to the underlying PropertyDef instance. Return
 * values and possible exceptions are copied over the network. RMI errors
 * are signaled with RemoteExceptions.
 * <p>
 * Note that the returned Value objects must be serializable and implemented
 * using classes available on both the client and server side. The
 * {@link org.apache.jackrabbit.rmi.value.SerialValueFactory SerialValueFactory}
 * class provides two convenience methods to satisfy this requirement.
 *
 * @see javax.jcr.nodetype.PropertyDefinition
 * @see org.apache.jackrabbit.rmi.client.ClientPropertyDefinition
 * @see org.apache.jackrabbit.rmi.server.ServerPropertyDefinition
 */
public interface RemotePropertyDefinition extends RemoteItemDefinition {

    /**
     * Remote version of the
     * {@link javax.jcr.nodetype.PropertyDefinition#getRequiredType() PropertyDefinition.getRequiredType()}
     * method.
     *
     * @return required type
     * @throws RemoteException on RMI errors
     */
    int getRequiredType() throws RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.nodetype.PropertyDefinition#getValueConstraints() PropertyDefinition.getValueConstraints()}
     * method.
     *
     * @return value constraints
     * @throws RemoteException on RMI errors
     */
    String[] getValueConstraints() throws RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.nodetype.PropertyDefinition#getDefaultValues() PropertyDefinition.getDefaultValues()}
     * method.
     *
     * @return default values
     * @throws RemoteException on RMI errors
     */
    Value[] getDefaultValues() throws RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.nodetype.PropertyDefinition#isMultiple() PropertyDefinition.isMultiple()}
     * method.
     *
     * @return <code>true</code> if the property is multi-valued,
     *         <code>false</code> otherwise
     * @throws RemoteException on RMI errors
     */
    boolean isMultiple() throws RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.nodetype.PropertyDefinition#getAvailableQueryOperators() PropertyDefinition.getAvailableQueryOperators()}
     * method.
     *
     * @return a String[]
     * @throws RemoteException on RMI errors
     */
	String[] getAvailableQueryOperators() throws RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.nodetype.PropertyDefinition#isFullTextSearchable() PropertyDefinition.isFullTextSearchable()}
     * method.
     *
     * @return a boolean
     * @throws RemoteException on RMI errors
     */
	boolean isFullTextSearchable() throws RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.nodetype.PropertyDefinition#isQueryOrderable() PropertyDefinition.isQueryOrderable()}
     * method.
     *
     * @return a boolean
     * @throws RemoteException on RMI errors
     */
	boolean isQueryOrderable() throws RemoteException;

}
