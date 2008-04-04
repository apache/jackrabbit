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
package org.apache.jackrabbit.core.xml;

import java.io.InputStream;
import java.util.Calendar;

import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.Item;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.ItemVisitor;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.version.VersionException;

/**
 * Provides a wrapper to an existing <code>Property</code> implementation.
 * Methods default to calling through to the wrapped property object.
 */
class PropertyWrapper implements Property {

    /** Wrapped property */
    private final Property property;

    /**
     * Constructs a property adaptor wrapping the given property.
     *
     * @param property property to wrap
     */
    public PropertyWrapper(Property property) {
        this.property = property;
    }

    /** {@inheritDoc} */
    public boolean getBoolean() throws ValueFormatException,
            RepositoryException {
        return property.getBoolean();
    }

    /** {@inheritDoc} */
    public Calendar getDate() throws ValueFormatException, RepositoryException {
        return property.getDate();
    }

    /** {@inheritDoc} */
    public PropertyDefinition getDefinition() throws RepositoryException {
        return property.getDefinition();
    }

    /** {@inheritDoc} */
    public double getDouble() throws ValueFormatException, RepositoryException {
        return property.getDouble();
    }

    /** {@inheritDoc} */
    public long getLength() throws ValueFormatException, RepositoryException {
        return property.getLength();
    }

    /** {@inheritDoc} */
    public long[] getLengths() throws ValueFormatException, RepositoryException {
        return property.getLengths();
    }

    /** {@inheritDoc} */
    public long getLong() throws ValueFormatException, RepositoryException {
        return property.getLong();
    }

    /** {@inheritDoc} */
    public Node getNode() throws ValueFormatException, RepositoryException {
        return property.getNode();
    }

    /** {@inheritDoc} */
    public InputStream getStream() throws ValueFormatException,
            RepositoryException {
        return property.getStream();
    }

    /** {@inheritDoc} */
    public String getString() throws ValueFormatException, RepositoryException {
        return property.getString();
    }

    /** {@inheritDoc} */
    public int getType() throws RepositoryException {
        return property.getType();
    }

    /** {@inheritDoc} */
    public Value getValue() throws ValueFormatException, RepositoryException {
        return property.getValue();
    }

    /** {@inheritDoc} */
    public Value[] getValues() throws ValueFormatException, RepositoryException {
        return property.getValues();
    }

    /** {@inheritDoc} */
    public void setValue(Value value) throws ValueFormatException,
            VersionException, LockException, ConstraintViolationException,
            RepositoryException {
        property.setValue(value);
    }

    /** {@inheritDoc} */
    public void setValue(Value[] values) throws ValueFormatException,
            VersionException, LockException, ConstraintViolationException,
            RepositoryException {
        property.setValue(values);
    }

    /** {@inheritDoc} */
    public void setValue(String value) throws ValueFormatException,
            VersionException, LockException, ConstraintViolationException,
            RepositoryException {
        property.setValue(value);
    }

    /** {@inheritDoc} */
    public void setValue(String[] values) throws ValueFormatException,
            VersionException, LockException, ConstraintViolationException,
            RepositoryException {
        property.setValue(values);
    }

    /** {@inheritDoc} */
    public void setValue(InputStream value) throws ValueFormatException,
            VersionException, LockException, ConstraintViolationException,
            RepositoryException {
        property.setValue(value);
    }

    /** {@inheritDoc} */
    public void setValue(long value) throws ValueFormatException,
            VersionException, LockException, ConstraintViolationException,
            RepositoryException {
        property.setValue(value);
    }

    /** {@inheritDoc} */
    public void setValue(double value) throws ValueFormatException,
            VersionException, LockException, ConstraintViolationException,
            RepositoryException {
        property.setValue(value);
    }

    /** {@inheritDoc} */
    public void setValue(Calendar value) throws ValueFormatException,
            VersionException, LockException, ConstraintViolationException,
            RepositoryException {
        property.setValue(value);
    }

    /** {@inheritDoc} */
    public void setValue(boolean value) throws ValueFormatException,
            VersionException, LockException, ConstraintViolationException,
            RepositoryException {
        property.setValue(value);
    }

    /** {@inheritDoc} */
    public void setValue(Node value) throws ValueFormatException,
            VersionException, LockException, ConstraintViolationException,
            RepositoryException {
        property.setValue(value);
    }

    /** {@inheritDoc} */
    public void accept(ItemVisitor visitor) throws RepositoryException {
        property.accept(visitor);
    }

    /** {@inheritDoc} */
    public Item getAncestor(int depth) throws ItemNotFoundException,
            AccessDeniedException, RepositoryException {
        return property.getAncestor(depth);
    }

    /** {@inheritDoc} */
    public int getDepth() throws RepositoryException {
        return property.getDepth();
    }

    /** {@inheritDoc} */
    public String getName() throws RepositoryException {
        return property.getName();
    }

    /** {@inheritDoc} */
    public Node getParent() throws ItemNotFoundException,
            AccessDeniedException, RepositoryException {
        return property.getParent();
    }

    /** {@inheritDoc} */
    public String getPath() throws RepositoryException {
        return property.getPath();
    }

    /** {@inheritDoc} */
    public Session getSession() throws RepositoryException {
        return property.getSession();
    }

    /** {@inheritDoc} */
    public boolean isModified() {
        return property.isModified();
    }

    /** {@inheritDoc} */
    public boolean isNew() {
        return property.isNew();
    }

    public boolean isNode() {
        return property.isNode();
    }

    /** {@inheritDoc} */
    public boolean isSame(Item otherItem) throws RepositoryException {
        return property.isSame(otherItem);
    }

    /** {@inheritDoc} */
    public void refresh(boolean keepChanges) throws InvalidItemStateException,
            RepositoryException {
        refresh(keepChanges);
    }

    /** {@inheritDoc} */
    public void remove() throws VersionException, LockException,
            ConstraintViolationException, RepositoryException {
        property.remove();
    }

    /** {@inheritDoc} */
    public void save() throws AccessDeniedException, ItemExistsException,
            ConstraintViolationException, InvalidItemStateException,
            ReferentialIntegrityException, VersionException, LockException,
            NoSuchNodeTypeException, RepositoryException {
        property.save();
    }
}
