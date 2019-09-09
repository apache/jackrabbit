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
package org.apache.jackrabbit.rmi.server;

import java.rmi.RemoteException;

import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.nodetype.PropertyDefinition;

import org.apache.jackrabbit.rmi.remote.RemotePropertyDefinition;

/**
 * Remote adapter for the JCR
 * {@link javax.jcr.nodetype.PropertyDefinition PropertyDefinition} interface. This
 * class makes a local property definition available as an RMI service
 * using the
 * {@link org.apache.jackrabbit.rmi.remote.RemotePropertyDefinition RemotePropertyDefinition}
 * interface.
 *
 * @see javax.jcr.nodetype.PropertyDefinition
 * @see org.apache.jackrabbit.rmi.remote.RemotePropertyDefinition
 */
public class ServerPropertyDefinition extends ServerItemDefinition
        implements RemotePropertyDefinition {

    /** The adapted local property definition. */
    private PropertyDefinition def;

    /**
     * Creates a remote adapter for the given local property definition.
     *
     * @param def local property definition
     * @param factory remote adapter factory
     * @throws RemoteException on RMI errors
     */
    public ServerPropertyDefinition(PropertyDefinition def, RemoteAdapterFactory factory)
            throws RemoteException {
        super(def, factory);
        this.def = def;
    }

    /** {@inheritDoc} */
    public int getRequiredType() throws RemoteException {
        return def.getRequiredType();
    }

    /** {@inheritDoc} */
    public String[] getValueConstraints() throws RemoteException {
        return def.getValueConstraints();
    }

    /** {@inheritDoc} */
    public Value[] getDefaultValues() throws RemoteException {
        try {
            return getSerialValues(def.getDefaultValues());
        } catch (RepositoryException e) {
            throw new RemoteException("Unable to serialize default values");
        }
    }

    /** {@inheritDoc} */
    public boolean isMultiple() throws RemoteException {
        return def.isMultiple();
    }

    /** {@inheritDoc} */
	public String[] getAvailableQueryOperators() throws RemoteException {
		return def.getAvailableQueryOperators();
	}

    /** {@inheritDoc} */
	public boolean isFullTextSearchable() throws RemoteException {
		return def.isFullTextSearchable();
	}

    /** {@inheritDoc} */
	public boolean isQueryOrderable() throws RemoteException {
		return def.isQueryOrderable();
	}

}
