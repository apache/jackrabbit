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

import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;

import org.apache.jackrabbit.rmi.remote.RemoteProperty;
import org.apache.jackrabbit.rmi.remote.RemotePropertyDef;
import org.apache.jackrabbit.rmi.remote.SerialValue;

/**
 * Remote adapter for the JCR {@link javax.jcr.Property Property}
 * interface. This class makes a local property available as an RMI service
 * using the
 * {@link org.apache.jackrabbit.rmi.remote.RemoteProperty RemoteProperty}
 * interface.
 * 
 * @author Jukka Zitting
 * @see javax.jcr.Property
 * @see org.apache.jackrabbit.rmi.remote.RemoteProperty
 */
public class ServerProperty extends ServerItem implements RemoteProperty {

    /** The adapted local property. */
    protected Property property;
    
    /**
     * Creates a remote adapter for the given local property.
     * 
     * @param property local property
     * @param factory remote adapter factory
     * @throws RemoteException on RMI errors
     */
    public ServerProperty(Property property, RemoteAdapterFactory factory)
            throws RemoteException {
        super(property, factory);
        this.property = property;
    }

    /** {@inheritDoc} */
    public Value getValue() throws ValueFormatException, RepositoryException,
            RemoteException {
        try {
            return new SerialValue(property.getValue());
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }
    
    /** {@inheritDoc} */
    public Value[] getValues() throws ValueFormatException,
            RepositoryException, RemoteException {
        try {
            return SerialValue.makeSerialValueArray(property.getValues());
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }
    
    /** {@inheritDoc} */
    public void setValue(Value value) throws ValueFormatException,
            RepositoryException, RemoteException {
        try {
            property.setValue(value);
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }
    
    /** {@inheritDoc} */
    public void setValue(Value[] values) throws ValueFormatException,
            RepositoryException, RemoteException {
        try {
            property.setValue(values);
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }
    
    /** {@inheritDoc} */
    public long getLength() throws ValueFormatException, RepositoryException,
            RemoteException {
        try {
            return property.getLength();
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }
    
    /** {@inheritDoc} */
    public long[] getLengths() throws ValueFormatException,
            RepositoryException, RemoteException {
        try {
            return property.getLengths();
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }
    
    /** {@inheritDoc} */
    public RemotePropertyDef getDefinition() throws RepositoryException,
            RemoteException {
        try {
            return factory.getRemotePropertyDef(property.getDefinition());
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public int getType() throws RepositoryException, RemoteException {
        try {
            return property.getType();
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }
    
}
