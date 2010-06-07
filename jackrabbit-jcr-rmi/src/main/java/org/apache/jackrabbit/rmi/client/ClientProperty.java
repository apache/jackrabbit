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
package org.apache.jackrabbit.rmi.client;

import java.io.InputStream;
import java.math.BigDecimal;
import java.rmi.RemoteException;
import java.util.Calendar;

import javax.jcr.Binary;
import javax.jcr.ItemNotFoundException;
import javax.jcr.ItemVisitor;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.ValueFormatException;
import javax.jcr.nodetype.PropertyDefinition;

import org.apache.jackrabbit.rmi.remote.RemoteProperty;
import org.apache.jackrabbit.rmi.value.SerialValueFactory;

/**
 * Local adapter for the JCR-RMI
 * {@link org.apache.jackrabbit.rmi.remote.RemoteProperty RemoteProperty}
 * interface. This class makes a remote property locally available using
 * the JCR {@link javax.jcr.Property Property} interface.
 *
 * @see javax.jcr.Property
 * @see org.apache.jackrabbit.rmi.remote.RemoteProperty
 */
public class ClientProperty extends ClientItem implements Property {

    /** The adapted remote property. */
    private RemoteProperty remote;

    /**
     * Creates a local adapter for the given remote property.
     *
     * @param session current session
     * @param remote  remote property
     * @param factory local adapter factory
     */
    public ClientProperty(
            Session session, RemoteProperty remote,
            LocalAdapterFactory factory) {
        super(session, remote, factory);
        this.remote = remote;
    }

    /**
     * Calls the {@link ItemVisitor#visit(Property) ItemVisitor.visit(Property}
     * method of the given visitor. Does not contact the remote property, but
     * the visitor may invoke other methods that do contact the remote property.
     *
     * {@inheritDoc}
     */
    public void accept(ItemVisitor visitor) throws RepositoryException {
        visitor.visit(this);
    }

    /**
     * Returns the boolean value of this property. Implemented as
     * getValue().getBoolean().
     *
     * {@inheritDoc}
     */
    public boolean getBoolean() throws RepositoryException {
        return getValue().getBoolean();
    }

    /**
     * Returns the date value of this property. Implemented as
     * getValue().getDate().
     *
     * {@inheritDoc}
     */
    public Calendar getDate() throws RepositoryException {
        return getValue().getDate();
    }

    /**
     * Returns the double value of this property. Implemented as
     * getValue().getDouble().
     *
     * {@inheritDoc}
     */
    public double getDouble() throws RepositoryException {
        return getValue().getDouble();
    }

    /**
     * Returns the long value of this property. Implemented as
     * getValue().getLong().
     *
     * {@inheritDoc}
     */
    public long getLong() throws RepositoryException {
        return getValue().getLong();
    }

    /**
     * Returns the binary value of this property. Implemented as
     * getValue().getBinary().
     *
     * {@inheritDoc}
     */
    public Binary getBinary() throws RepositoryException {
        return getValue().getBinary();
    }

    /**
     * Returns the decimal value of this property. Implemented as
     * getValue().getDecimal().
     *
     * {@inheritDoc}
     */
    public BigDecimal getDecimal() throws RepositoryException {
        return getValue().getDecimal();
    }

    /**
     * Returns the binary value of this property. Implemented as
     * getValue().getStream().
     *
     * {@inheritDoc}
     */
    @SuppressWarnings("deprecation")
    public InputStream getStream() throws RepositoryException {
        return getValue().getStream();
    }

    /**
     * Returns the string value of this property. Implemented as
     * getValue().getString().
     *
     * {@inheritDoc}
     */
    public String getString() throws RepositoryException {
        return getValue().getString();
    }

    /** {@inheritDoc} */
    public Value getValue() throws RepositoryException {
        try {
            return remote.getValue();
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public Value[] getValues() throws RepositoryException {
        try {
            return remote.getValues();
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /**
     * Sets the boolean value of this property. Implemented as
     * setValue(new BooleanValue(value)).
     *
     * {@inheritDoc}
     */
    public void setValue(boolean value) throws RepositoryException {
        setValue(getSession().getValueFactory().createValue(value));
    }

    /**
     * Sets the date value of this property. Implemented as
     * setValue(new DateValue(value)).
     *
     * {@inheritDoc}
     */
    public void setValue(Calendar value) throws RepositoryException {
        if (value == null) {
            setValue((Value) null);
        } else {
            setValue(getSession().getValueFactory().createValue(value));
        }
    }

    /**
     * Sets the double value of this property. Implemented as
     * setValue(new DoubleValue(value)).
     *
     * {@inheritDoc}
     */
    public void setValue(double value) throws RepositoryException {
        setValue(getSession().getValueFactory().createValue(value));
    }

    /**
     * Sets the binary value of this property. Implemented as
     * setValue(new BinaryValue(value)).
     *
     * {@inheritDoc}
     */
    public void setValue(InputStream value) throws RepositoryException {
        if (value == null) {
            setValue((Value) null);
        } else {
            ValueFactory factory = getSession().getValueFactory();
            Binary binary = factory.createBinary(value);
            try {
                setValue(factory.createValue(binary));
            } finally {
                binary.dispose();
            }
        }
    }

    /**
     * Sets the long value of this property. Implemented as
     * setValue(new LongValue(value)).
     *
     * {@inheritDoc}
     */
    public void setValue(long value) throws RepositoryException {
        setValue(getSession().getValueFactory().createValue(value));
    }

    /**
     * Sets the binary value of this property.
     *
     * {@inheritDoc}
     */
    public void setValue(Binary value) throws RepositoryException {
        setValue(getSession().getValueFactory().createValue(value));
    }

    /**
     * Sets the decimal value of this property.
     *
     * {@inheritDoc}
     */
    public void setValue(BigDecimal value) throws RepositoryException {
        setValue(getSession().getValueFactory().createValue(value));
    }


    /**
     * Sets the reference value of this property. Implemented as
     * setValue(new ReferenceValue(value)).
     *
     * {@inheritDoc}
     */
    public void setValue(Node value) throws RepositoryException {
        if (value == null) {
            setValue((Value) null);
        } else {
            setValue(getSession().getValueFactory().createValue(value));
        }
    }

    /**
     * Sets the string value of this property. Implemented as
     * setValue(new StringValue(value)).
     *
     * {@inheritDoc}
     */
    public void setValue(String value) throws RepositoryException {
        if (value == null) {
            setValue((Value) null);
        } else {
            setValue(getSession().getValueFactory().createValue(value));
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setValue(String[] strings) throws RepositoryException {
        try {
            Value[] values = null;
            if (strings != null) {
                values = SerialValueFactory.makeSerialValueArray(strings);
            }
            remote.setValue(values);
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public void setValue(Value value) throws RepositoryException {
        try {
            if (value != null) {
                value = SerialValueFactory.makeSerialValue(value);
            }
            remote.setValue(value);
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public void setValue(Value[] values) throws RepositoryException {
        try {
            if (values != null) {
                values = SerialValueFactory.makeSerialValueArray(values);
            }
            remote.setValue(values);
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /**
     * Returns the reference value of this property. Implemented by
     * converting the reference value to an UUID string and using the
     * current session to look up the referenced node.
     *
     * {@inheritDoc}
     */
    public Node getNode() throws RepositoryException {
        String value = getString();

        switch (getType()) {
            case PropertyType.REFERENCE:
            case PropertyType.WEAKREFERENCE:
                return getSession().getNodeByIdentifier(value);

            case PropertyType.PATH:
                try {
                    if (value.startsWith("/")) {
                        return getSession().getNode(value);
                    } else {
                        return getParent().getNode(value);
                    }
                } catch (PathNotFoundException e) {
                    throw new ItemNotFoundException(value);
                }

            case PropertyType.NAME:
                try {
                    return getParent().getNode(value);
                } catch (PathNotFoundException e) {
                    throw new ItemNotFoundException(value);
                }

            case PropertyType.STRING:
                try {
                    // interpret as identifier
                    Value refValue = getSession().getValueFactory().createValue(value, PropertyType.REFERENCE);
                    return getSession().getNodeByIdentifier(refValue.getString());
                } catch (ItemNotFoundException e) {
                    throw e;
                } catch (RepositoryException e) {
                    // try if STRING value can be interpreted as PATH value
                    Value pathValue = getSession().getValueFactory().createValue(value, PropertyType.PATH);
                    boolean absolute = value.startsWith("/");
                    try {
                        return (absolute) ? getSession().getNode(pathValue.getString()) : getParent().getNode(pathValue.getString());
                    } catch (PathNotFoundException e1) {
                        throw new ItemNotFoundException(pathValue.getString());
                    }
                }

            default:
                throw new ValueFormatException("Property value cannot be converted to a PATH, REFERENCE or WEAKREFERENCE: " + value);
        }
    }

    /** {@inheritDoc} */
    public Property getProperty() throws RepositoryException {
        if (getType() != PropertyType.PATH && getType() != PropertyType.NAME) {
            throw new ValueFormatException("Not a path property");
        } else {
            String value = getString();
            try {
                if (value.startsWith("/")) {
                    return getSession().getProperty(value);
                } else {
                    return getParent().getProperty(value);
                }
            } catch (PathNotFoundException e) {
                throw new ItemNotFoundException(value);
            }
        }
    }

    /** {@inheritDoc} */
    public long getLength() throws RepositoryException {
        try {
            return remote.getLength();
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public long[] getLengths() throws RepositoryException {
        try {
            return remote.getLengths();
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public PropertyDefinition getDefinition() throws RepositoryException {
        try {
            return getFactory().getPropertyDef(remote.getDefinition());
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public int getType() throws RepositoryException {
        try {
            return remote.getType();
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public boolean isMultiple() throws RepositoryException {
        // TODO: Direct remote call for this?
        return getDefinition().isMultiple();
    }

}
