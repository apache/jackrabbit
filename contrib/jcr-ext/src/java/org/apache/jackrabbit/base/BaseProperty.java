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
package org.apache.jackrabbit.base;

import java.io.InputStream;
import java.util.Calendar;

import javax.jcr.ItemVisitor;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.version.VersionException;

/**
 * Property base class.
 */
public class BaseProperty extends BaseItem implements Property {

    /** Protected constructor. This class is only useful when extended. */
    protected BaseProperty() {
    }

    /**
     * Implemented by calling <code>visitor.visit(this)</code>.
     * {@inheritDoc}
     */
    public void accept(ItemVisitor visitor) throws RepositoryException {
        visitor.visit(this);
    }

    /** Always returns <code>false</code>. {@inheritDoc} */
    public boolean isNode() {
        return false;
    }

    /** Not implemented. {@inheritDoc} */
    public void setValue(Value value) throws ValueFormatException,
            VersionException, LockException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /** Not implemented. {@inheritDoc} */
    public void setValue(Value[] values) throws ValueFormatException,
            VersionException, LockException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * Implemented by calling
     * <code>setValue(getSession().getValueFactory().createValue(value))</code>.
     * {@inheritDoc}
     */
    public void setValue(String value) throws ValueFormatException,
            VersionException, LockException, RepositoryException {
        setValue(getSession().getValueFactory().createValue(value));
    }

    /**
     * Implemented by calling <code>setValue(stringValues)</code> with
     * an array of Values that were created from the given strings by
     * <code>getSession().getValueFactory().createValue(values[i]))</code>.
     * {@inheritDoc}
     */
    public void setValue(String[] values) throws ValueFormatException,
            VersionException, LockException, RepositoryException {
        ValueFactory factory = getSession().getValueFactory();
        Value[] stringValues = new Value[values.length];
        for (int i = 0; i < values.length; i++) {
            stringValues[i] = factory.createValue(values[i]);
        }
        setValue(stringValues);
    }

    /**
     * Implemented by calling
     * <code>setValue(getSession().getValueFactory().createValue(value))</code>.
     * {@inheritDoc}
     */
    public void setValue(InputStream value) throws ValueFormatException,
            VersionException, LockException, RepositoryException {
        setValue(getSession().getValueFactory().createValue(value));
    }

    /**
     * Implemented by calling
     * <code>setValue(getSession().getValueFactory().createValue(value))</code>.
     * {@inheritDoc}
     */
    public void setValue(long value) throws ValueFormatException,
            VersionException, LockException, RepositoryException {
        setValue(getSession().getValueFactory().createValue(value));
    }

    /**
     * Implemented by calling
     * <code>setValue(getSession().getValueFactory().createValue(value))</code>.
     * {@inheritDoc}
     */
    public void setValue(double value) throws ValueFormatException,
            VersionException, LockException, RepositoryException {
        setValue(getSession().getValueFactory().createValue(value));
    }

    /**
     * Implemented by calling
     * <code>setValue(getSession().getValueFactory().createValue(value))</code>.
     * {@inheritDoc}
     */
    public void setValue(Calendar value) throws ValueFormatException,
            VersionException, LockException, RepositoryException {
        setValue(getSession().getValueFactory().createValue(value));
    }

    /**
     * Implemented by calling
     * <code>setValue(getSession().getValueFactory().createValue(value))</code>.
     * {@inheritDoc}
     */
    public void setValue(boolean value) throws ValueFormatException,
            VersionException, LockException, RepositoryException {
        setValue(getSession().getValueFactory().createValue(value));
    }

    /**
     * Implemented by calling
     * <code>setValue(getSession().getValueFactory().createValue(value))</code>.
     * {@inheritDoc}
     */
    public void setValue(Node value) throws ValueFormatException,
            VersionException, LockException, RepositoryException {
        setValue(getSession().getValueFactory().createValue(value));
    }

    /** Not implemented. {@inheritDoc} */
    public Value getValue() throws ValueFormatException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /** Not implemented. {@inheritDoc} */
    public Value[] getValues() throws ValueFormatException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * Implemented by calling <code>getValue().getString()</code>.
     * {@inheritDoc}
     */
    public String getString() throws ValueFormatException, RepositoryException {
        return getValue().getString();
    }

    /**
     * Implemented by calling <code>getValue().getStream()</code>.
     * {@inheritDoc}
     */
    public InputStream getStream() throws ValueFormatException,
            RepositoryException {
        return getValue().getStream();
    }

    /**
     * Implemented by calling <code>getValue().getLong()</code>.
     * {@inheritDoc}
     */
    public long getLong() throws ValueFormatException, RepositoryException {
        return getValue().getLong();
    }

    /**
     * Implemented by calling <code>getValue().getDouble()</code>.
     * {@inheritDoc}
     */
    public double getDouble() throws ValueFormatException, RepositoryException {
        return getValue().getDouble();
    }

    /**
     * Implemented by calling <code>getValue().getDate()</code>.
     * {@inheritDoc}
     */
    public Calendar getDate() throws ValueFormatException, RepositoryException {
        return getValue().getDate();
    }

    /**
     * Implemented by calling <code>getValue().getBoolean()</code>.
     * {@inheritDoc}
     */
    public boolean getBoolean() throws ValueFormatException,
            RepositoryException {
        return getValue().getBoolean();
    }

    /**
     * Implemented by calling
     * <code>getSession().getNodeByUUID(getString())</code>.
     * {@inheritDoc}
     */
    public Node getNode() throws ValueFormatException, RepositoryException {
        return getSession().getNodeByUUID(getString());
    }

    /**
     * Implemented by calling <code>getType()</code> and returning
     * <code>-1</code> if type is binary or <code>getString().length()</code>
     * otherwise.
     * {@inheritDoc}
     */
    public long getLength() throws ValueFormatException, RepositoryException {
        if (getType() == PropertyType.BINARY) {
            return -1;
        } else {
            return getString().length();
        }
    }

    /** {@inheritDoc} */
    public long[] getLengths() throws ValueFormatException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /** Not implemented. {@inheritDoc} */
    public PropertyDefinition getDefinition() throws RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * Implemented by calling <code>getValue().getType()</code>.
     * {@inheritDoc}
     */
    public int getType() throws RepositoryException {
        return getValue().getType();
    }

}
