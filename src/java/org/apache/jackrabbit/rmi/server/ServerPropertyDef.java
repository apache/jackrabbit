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
package org.apache.jackrabbit.rmi.server;

import java.rmi.RemoteException;

import javax.jcr.Value;
import javax.jcr.nodetype.PropertyDef;

import org.apache.jackrabbit.rmi.remote.RemotePropertyDef;

/**
 * Remote adapter for the JCR
 * {@link javax.jcr.nodetype.PropertyDef PropertyDef} interface. This
 * class makes a local property definition available as an RMI service
 * using the
 * {@link org.apache.jackrabbit.rmi.remote.RemotePropertyDef RemotePropertyDef}
 * interface.
 *
 * @author Jukka Zitting
 * @see javax.jcr.nodetype.PropertyDef
 * @see org.apache.jackrabbit.rmi.remote.RemotePropertyDef
 */
public class ServerPropertyDef extends ServerItemDef
        implements RemotePropertyDef {

    /** The adapted local property definition. */
    private PropertyDef def;

    /**
     * Creates a remote adapter for the given local property definition.
     *
     * @param def local property definition
     * @param factory remote adapter factory
     * @throws RemoteException on RMI errors
     */
    public ServerPropertyDef(PropertyDef def, RemoteAdapterFactory factory)
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
        return def.getDefaultValues();
    }

    /** {@inheritDoc} */
    public boolean isMultiple() throws RemoteException {
        return def.isMultiple();
    }

}
