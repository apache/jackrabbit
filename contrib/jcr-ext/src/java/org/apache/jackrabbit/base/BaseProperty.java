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
package org.apache.jackrabbit.base;

import java.io.InputStream;
import java.util.Calendar;

import javax.jcr.BinaryValue;
import javax.jcr.BooleanValue;
import javax.jcr.DateValue;
import javax.jcr.DoubleValue;
import javax.jcr.Item;
import javax.jcr.ItemVisitor;
import javax.jcr.LongValue;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.ReferenceValue;
import javax.jcr.RepositoryException;
import javax.jcr.StringValue;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.PropertyDef;
import javax.jcr.version.VersionException;

/**
 * TODO
 */
public class BaseProperty extends BaseItem implements Property {

    protected BaseProperty(Item item) {
        super(item);
    }

    public void accept(ItemVisitor visitor) throws RepositoryException {
        visitor.visit(this);
    }

    public boolean isNode() {
        return false;
    }

    /** {@inheritDoc} */
    public void setValue(Value value) throws ValueFormatException,
            VersionException, LockException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /** {@inheritDoc} */
    public void setValue(Value[] values) throws ValueFormatException,
            VersionException, LockException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /** {@inheritDoc} */
    public void setValue(String value) throws ValueFormatException,
            VersionException, LockException, RepositoryException {
        setValue(new StringValue(value));
    }

    /** {@inheritDoc} */
    public void setValue(String[] values) throws ValueFormatException,
            VersionException, LockException, RepositoryException {
        Value[] stringValues = new Value[values.length];
        for (int i = 0; i < values.length; i++) {
            stringValues[i] = new StringValue(values[i]);
        }
        setValue(stringValues);
    }

    /** {@inheritDoc} */
    public void setValue(InputStream value) throws ValueFormatException,
            VersionException, LockException, RepositoryException {
        setValue(new BinaryValue(value));
    }

    /** {@inheritDoc} */
    public void setValue(long value) throws ValueFormatException,
            VersionException, LockException, RepositoryException {
        setValue(new LongValue(value));
    }

    /** {@inheritDoc} */
    public void setValue(double value) throws ValueFormatException,
            VersionException, LockException, RepositoryException {
        setValue(new DoubleValue(value));
    }

    /** {@inheritDoc} */
    public void setValue(Calendar value) throws ValueFormatException,
            VersionException, LockException, RepositoryException {
        setValue(new DateValue(value));
    }

    /** {@inheritDoc} */
    public void setValue(boolean value) throws ValueFormatException,
            VersionException, LockException, RepositoryException {
        setValue(new BooleanValue(value));
    }

    /** {@inheritDoc} */
    public void setValue(Node value) throws ValueFormatException,
            VersionException, LockException, RepositoryException {
        try {
            setValue(new ReferenceValue(value));
        } catch (IllegalArgumentException e) {
            throw new ValueFormatException("Invalid reference target", e);
        }
    }

    /** {@inheritDoc} */
    public Value getValue() throws ValueFormatException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /** {@inheritDoc} */
    public Value[] getValues() throws ValueFormatException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /** {@inheritDoc} */
    public String getString() throws ValueFormatException, RepositoryException {
        return getValue().getString();
    }

    /** {@inheritDoc} */
    public InputStream getStream() throws ValueFormatException,
            RepositoryException {
        return getValue().getStream();
    }

    /** {@inheritDoc} */
    public long getLong() throws ValueFormatException, RepositoryException {
        return getValue().getLong();
    }

    /** {@inheritDoc} */
    public double getDouble() throws ValueFormatException, RepositoryException {
        return getValue().getDouble();
    }

    /** {@inheritDoc} */
    public Calendar getDate() throws ValueFormatException, RepositoryException {
        return getValue().getDate();
    }

    /** {@inheritDoc} */
    public boolean getBoolean() throws ValueFormatException,
            RepositoryException {
        return getValue().getBoolean();
    }

    /** {@inheritDoc} */
    public Node getNode() throws ValueFormatException, RepositoryException {
        return getSession().getNodeByUUID(getString());
    }

    /** {@inheritDoc} */
    public long getLength() throws ValueFormatException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /** {@inheritDoc} */
    public long[] getLengths() throws ValueFormatException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /** {@inheritDoc} */
    public PropertyDef getDefinition() throws RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /** {@inheritDoc} */
    public int getType() throws RepositoryException {
        return getValue().getType();
    }

}
