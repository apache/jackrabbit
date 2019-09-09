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
package org.apache.jackrabbit.core;

import static javax.jcr.PropertyType.BINARY;
import static javax.jcr.PropertyType.NAME;
import static javax.jcr.PropertyType.PATH;
import static javax.jcr.PropertyType.REFERENCE;
import static javax.jcr.PropertyType.STRING;
import static javax.jcr.PropertyType.UNDEFINED;
import static javax.jcr.PropertyType.WEAKREFERENCE;
import static org.apache.jackrabbit.core.ItemValidator.CHECK_CHECKED_OUT;
import static org.apache.jackrabbit.core.ItemValidator.CHECK_CONSTRAINTS;
import static org.apache.jackrabbit.core.ItemValidator.CHECK_HOLD;
import static org.apache.jackrabbit.core.ItemValidator.CHECK_LOCK;
import static org.apache.jackrabbit.core.ItemValidator.CHECK_RETENTION;

import java.io.InputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;

import javax.jcr.Binary;
import javax.jcr.InvalidItemStateException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.ItemVisitor;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.version.VersionException;

import org.apache.jackrabbit.core.id.PropertyId;
import org.apache.jackrabbit.core.security.authorization.Permission;
import org.apache.jackrabbit.core.session.SessionContext;
import org.apache.jackrabbit.core.state.ItemState;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.commons.value.ValueFormat;
import org.apache.jackrabbit.spi.commons.nodetype.PropertyDefinitionImpl;
import org.apache.jackrabbit.value.ValueHelper;
import org.apache.commons.io.input.AutoCloseInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>PropertyImpl</code> implements the <code>Property</code> interface.
 */
public class PropertyImpl extends ItemImpl implements Property {

    private static Logger log = LoggerFactory.getLogger(PropertyImpl.class);

    /** property data (avoids casting <code>ItemImpl.data</code>) */
    private final PropertyData data;

    /**
     * Package private constructor.
     *
     * @param itemMgr    the <code>ItemManager</code> that created this <code>Property</code>
     * @param sessionContext the component context of the associated session
     * @param data       the property data
     */
    PropertyImpl(
            ItemManager itemMgr, SessionContext sessionContext,
            PropertyData data) {
        super(itemMgr, sessionContext, data);
        this.data = data;
        // value will be read on demand
    }

    /**
     * Checks that this property is valid (session not closed, property not
     * removed, etc.) and returns the underlying property state if all is OK.
     *
     * @return property state
     * @throws RepositoryException if the property is not valid
     */
    private PropertyState getPropertyState() throws RepositoryException {
        // JCR-1272: Need to get the state reference now so it
        // doesn't get invalidated after the sanity check
        ItemState state = getItemState();
        sanityCheck();
        return (PropertyState) state;
    }

    @Override
    protected synchronized ItemState getOrCreateTransientItemState()
            throws RepositoryException {

        synchronized (data) {
            if (!isTransient()) {
                // make transient (copy-on-write)
                try {
                    PropertyState transientState =
                            stateMgr.createTransientPropertyState(
                                    data.getPropertyState(), ItemState.STATUS_EXISTING_MODIFIED);
                    // swap persistent with transient state
                    data.setState(transientState);
                } catch (ItemStateException ise) {
                    String msg = "failed to create transient state";
                    log.debug(msg);
                    throw new RepositoryException(msg, ise);
                }
            }
            return getItemState();
        }
    }

    @Override
    protected void makePersistent() throws InvalidItemStateException {
        if (!isTransient()) {
            log.debug(this + " (" + id + "): there's no transient state to persist");
            return;
        }

        PropertyState transientState = data.getPropertyState();
        PropertyState persistentState = (PropertyState) transientState.getOverlayedState();
        if (persistentState == null) {
            // this property is 'new'
            try {
                persistentState = stateMgr.createNew(transientState);
            } catch (ItemStateException e) {
                throw new InvalidItemStateException(e);
            }
        }

        synchronized (persistentState) {
            // check staleness of transient state first
            if (transientState.isStale()) {
                String msg =
                    this + ": the property cannot be saved because it has"
                    + " been modified externally.";
                log.debug(msg);
                throw new InvalidItemStateException(msg);
            }
            // copy state from transient state
            persistentState.setType(transientState.getType());
            persistentState.setMultiValued(transientState.isMultiValued());
            persistentState.setValues(transientState.getValues());
            // make state persistent
            stateMgr.store(persistentState);
        }

        // tell state manager to disconnect item state
        stateMgr.disconnectTransientItemState(transientState);
        // swap transient state with persistent state
        data.setState(persistentState);
        // reset status
        data.setStatus(STATUS_NORMAL);
    }

    protected void restoreTransient(PropertyState transientState)
            throws RepositoryException {
        PropertyState thisState = null;

        if (!isTransient()) {
            thisState = (PropertyState) getOrCreateTransientItemState();
            if (transientState.getStatus() == ItemState.STATUS_NEW
                    && thisState.getStatus() != ItemState.STATUS_NEW) {
                thisState.setStatus(ItemState.STATUS_NEW);
                stateMgr.disconnectTransientItemState(thisState);
            }
        } else {
            // JCR-2503: Re-create transient state in the state manager,
            // because it was removed
            synchronized (data) {
                try {
                    thisState = stateMgr.createTransientPropertyState(
                            transientState.getParentId(),
                            transientState.getName(),
                            PropertyState.STATUS_NEW);
                    data.setState(thisState);
                } catch (ItemStateException e) {
                    throw new RepositoryException(e);
                }
            }
        }

        // reapply transient changes
        thisState.setType(transientState.getType());
        thisState.setMultiValued(transientState.isMultiValued());
        thisState.setValues(transientState.getValues());
        thisState.setModCount(transientState.getModCount());
    }

    protected void onRedefine(QPropertyDefinition def) throws RepositoryException {
        PropertyDefinitionImpl newDef =
                sessionContext.getNodeTypeManager().getPropertyDefinition(def);
        data.setDefinition(newDef);
    }

    /**
     * Determines the length of the given value.
     *
     * @param value value whose length should be determined
     * @return the length of the given value
     * @throws RepositoryException if an error occurs
     * @see javax.jcr.Property#getLength()
     * @see javax.jcr.Property#getLengths()
     */
    protected long getLength(InternalValue value) throws RepositoryException {
        long length;
        switch (value.getType()) {
            case NAME:
            case PATH:
                String str = ValueFormat.getJCRString(value, sessionContext);
                length = str.length();
                break;
            default:
                length = value.getLength();
                break;
        }
        return length;
    }

    /**
     * Checks various pre-conditions that are common to all
     * <code>setValue()</code> methods. The checks performed are:
     * <ul>
     * <li>parent node must be checked-out</li>
     * <li>property must not be protected</li>
     * <li>parent node must not be locked by somebody else</li>
     * <li>property must be multi-valued when set to an array of values
     * (and vice versa)</li>
     * </ul>
     *
     * @param multipleValues flag indicating whether the property is about to
     *                       be set to an array of values
     * @throws ValueFormatException         if a single-valued property is set to an
     *                                      array of values (and vice versa)
     * @throws VersionException             if the parent node is not checked-out
     * @throws LockException                if the parent node is locked by somebody else
     * @throws ConstraintViolationException if the property is protected
     * @throws RepositoryException          if another error occurs
     * @see javax.jcr.Property#setValue
     */
    protected void checkSetValue(boolean multipleValues)
            throws ValueFormatException, VersionException,
            LockException, ConstraintViolationException,
            RepositoryException {
        NodeImpl parent = (NodeImpl) getParent(false);
        // check multi-value flag
        if (multipleValues != isMultiple()) {
            String msg = (multipleValues) ?
                    "Single-valued property can not be set to an array of values:" :
                    "Multivalued property can not be set to a single value (an array of length one is OK): ";
            throw new ValueFormatException(msg + this);
        }

        // check protected flag and for retention/hold
        sessionContext.getItemValidator().checkModify(
                this, CHECK_CONSTRAINTS, Permission.NONE);

        // make sure the parent is checked-out and neither locked nor under retention
        sessionContext.getItemValidator().checkModify(
                parent,
                CHECK_CHECKED_OUT | CHECK_LOCK | CHECK_HOLD | CHECK_RETENTION,
                Permission.NONE);
    }

    /**
     * @param values
     * @param type
     * @throws ConstraintViolationException
     * @throws RepositoryException
     */
    protected void internalSetValue(InternalValue[] values, int type)
            throws ConstraintViolationException, RepositoryException {
        // check for null value
        if (values == null) {
            // setting a property to null removes it automatically
            ((NodeImpl) getParent()).removeChildProperty(((PropertyId) id).getName());
            return;
        }
        ArrayList<InternalValue> list = new ArrayList<InternalValue>();
        // compact array (purge null entries)
        for (InternalValue v : values) {
            if (v != null) {
                list.add(v);
            }
        }
        values = list.toArray(new InternalValue[list.size()]);

        // modify the state of this property
        PropertyState thisState = (PropertyState) getOrCreateTransientItemState();

        // free old values as necessary
        InternalValue[] oldValues = thisState.getValues();
        if (oldValues != null) {
            for (InternalValue old : oldValues) {
                if (old != null && old.getType() == BINARY) {
                    // make sure temporarily allocated data is discarded
                    // before overwriting it
                    old.discard();
                }
            }
        }

        // set new values
        thisState.setValues(values);
        // set type
        if (type == UNDEFINED) {
            // fallback to default type
            type = STRING;
        }
        thisState.setType(type);
    }

    protected Node getParent(boolean checkPermission) throws RepositoryException {
        return (Node) itemMgr.getItem(getPropertyState().getParentId(), checkPermission);
    }

    /**
     * Same as <code>{@link Property#setValue(String)}</code> except that
     * this method takes a <code>Name</code> instead of a <code>String</code>
     * value.
     *
     * @param name
     * @throws ValueFormatException
     * @throws VersionException
     * @throws LockException
     * @throws ConstraintViolationException
     * @throws RepositoryException
     */
    public void setValue(Name name)
            throws ValueFormatException, VersionException,
            LockException, ConstraintViolationException,
            RepositoryException {
        // check state of this instance
        sanityCheck();

        // check pre-conditions for setting property value
        checkSetValue(false);

        // check type according to definition of this property
        final PropertyDefinition definition = data.getPropertyDefinition();
        int reqType = definition.getRequiredType();
        if (reqType == UNDEFINED) {
            reqType = NAME;
        }

        if (name == null) {
            internalSetValue(null, reqType);
            return;
        }

        InternalValue internalValue;
        if (reqType != NAME) {
            // type conversion required
            Value targetValue = ValueHelper.convert(
                    ValueFormat.getJCRValue(InternalValue.create(name), sessionContext, getSession().getValueFactory()),
                    reqType, getSession().getValueFactory());
            internalValue = InternalValue.create(
                    targetValue, sessionContext, sessionContext.getDataStore());
        } else {
            // no type conversion required
            internalValue = InternalValue.create(name);
        }

        internalSetValue(new InternalValue[]{internalValue}, reqType);
    }

    /**
     * Same as <code>{@link Property#setValue(String[])}</code> except that
     * this method takes an array of <code>Name</code> instead of
     * <code>String</code> values.
     *
     * @param names
     * @throws ValueFormatException
     * @throws VersionException
     * @throws LockException
     * @throws ConstraintViolationException
     * @throws RepositoryException
     */
    public void setValue(Name[] names)
            throws ValueFormatException, VersionException,
            LockException, ConstraintViolationException,
            RepositoryException {
        // check state of this instance
        sanityCheck();

        // check pre-conditions for setting property value
        checkSetValue(true);

        // check type according to definition of this property
        final PropertyDefinition definition = data.getPropertyDefinition();
        int reqType = definition.getRequiredType();
        if (reqType == UNDEFINED) {
            reqType = NAME;
        }

        InternalValue[] internalValues = null;
        // convert to internal values of correct type
        if (names != null) {
            internalValues = new InternalValue[names.length];
            for (int i = 0; i < names.length; i++) {
                Name name = names[i];
                InternalValue internalValue = null;
                if (name != null) {
                    if (reqType != NAME) {
                        // type conversion required
                        Value targetValue = ValueHelper.convert(
                                ValueFormat.getJCRValue(InternalValue.create(name), sessionContext, getSession().getValueFactory()),
                                reqType, getSession().getValueFactory());
                        internalValue = InternalValue.create(
                                targetValue, sessionContext,
                                sessionContext.getDataStore());
                    } else {
                        // no type conversion required
                        internalValue = InternalValue.create(name);
                    }
                }
                internalValues[i] = internalValue;
            }
        }

        internalSetValue(internalValues, reqType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Name getQName() {
        return ((PropertyId) id).getName();
    }

    /**
     * Returns the internal values of a multi-valued property.
     *
     * @return array of values
     * @throws ValueFormatException if this property is not multi-valued
     * @throws RepositoryException
     */
    public InternalValue[] internalGetValues() throws RepositoryException {
        final PropertyDefinition definition = data.getPropertyDefinition();
        if (isMultiple()) {
            return getPropertyState().getValues();
        } else {
            throw new ValueFormatException(
                    this + " is a single-valued property,"
                    + " so it's value can not be retrieved as an array");
        }

    }

    /**
     * Returns the internal value of a single-valued property.
     *
     * @return value
     * @throws ValueFormatException if this property is not single-valued
     * @throws RepositoryException
     */
    public InternalValue internalGetValue() throws RepositoryException {
        if (isMultiple()) {
            throw new ValueFormatException(
                    this + " is a multi-valued property,"
                    + " so it's values can only be retrieved as an array");
        } else {
            InternalValue[] values = getPropertyState().getValues();
            if (values.length > 0) {
                return values[0];
            } else {
                // should never be the case, but being a little paranoid can't hurt...
                throw new RepositoryException(this + ": single-valued property with no value");
            }
        }
    }

    //-------------------------------------------------------------< Property >

    public Value[] getValues() throws RepositoryException {
        InternalValue[] internals = internalGetValues();
        Value[] values = new Value[internals.length];
        for (int i = 0; i < internals.length; i++) {
            values[i] = ValueFormat.getJCRValue(internals[i], sessionContext, getSession().getValueFactory());
        }
        return values;
    }

    public Value getValue() throws RepositoryException {
        try {
            return ValueFormat.getJCRValue(internalGetValue(), sessionContext, getSession().getValueFactory());
        } catch (RuntimeException e) {
            String msg = "Internal error while retrieving value of " + this;
            log.error(msg, e);
            throw new RepositoryException(msg, e);
        }
    }

    /** Wrapper around {@link #getValue()} */
    public String getString() throws RepositoryException {
        return getValue().getString();
    }

    /** Wrapper around {@link #getValue()} */
    public InputStream getStream() throws RepositoryException {
        final Binary binary = getValue().getBinary();
        // make sure binary is disposed after stream had been consumed
        return new AutoCloseInputStream(binary.getStream()) {
            @Override
            public void close() throws IOException {
                super.close();
                binary.dispose();
            }
        };
    }

    /** Wrapper around {@link #getValue()} */
    public long getLong() throws RepositoryException {
        return getValue().getLong();
    }

    /** Wrapper around {@link #getValue()} */
    public double getDouble() throws RepositoryException {
        return getValue().getDouble();
    }

    /** Wrapper around {@link #getValue()} */
    public Calendar getDate() throws RepositoryException {
        return getValue().getDate();
    }

    /** Wrapper around {@link #getValue()} */
    public boolean getBoolean() throws RepositoryException {
        return getValue().getBoolean();
    }

    public Node getNode() throws ValueFormatException, RepositoryException {
        Session session = getSession();
        Value value = getValue();
        int type = value.getType();
        switch (type) {
            case REFERENCE:
            case WEAKREFERENCE:
                return session.getNodeByUUID(value.getString());

            case PATH:
            case NAME:
                String path = value.getString();
                Path p = sessionContext.getQPath(path);
                boolean absolute = p.isAbsolute();
                try {
                    return (absolute) ? session.getNode(path) : getParent().getNode(path);
                } catch (PathNotFoundException e) {
                    throw new ItemNotFoundException(path);
                }

            case STRING:
                try {
                    Value refValue = ValueHelper.convert(value, REFERENCE, session.getValueFactory());
                    return session.getNodeByUUID(refValue.getString());
                } catch (RepositoryException e) {
                    // try if STRING value can be interpreted as PATH value
                    Value pathValue = ValueHelper.convert(value, PATH, session.getValueFactory());
                    p = sessionContext.getQPath(pathValue.getString());
                    absolute = p.isAbsolute();
                    try {
                        return (absolute) ? session.getNode(pathValue.getString()) : getParent().getNode(pathValue.getString());
                    } catch (PathNotFoundException e1) {
                        throw new ItemNotFoundException(pathValue.getString());
                    }
                }

            default:
                throw new ValueFormatException("Property value cannot be converted to a PATH, REFERENCE or WEAKREFERENCE");
        }
    }

    public Property getProperty() throws RepositoryException {
        Value value = getValue();
        Value pathValue = ValueHelper.convert(value, PATH, getSession().getValueFactory());
        String path = pathValue.getString();
        boolean absolute;
        try {
            Path p = sessionContext.getQPath(path);
            absolute = p.isAbsolute();
        } catch (RepositoryException e) {
            throw new ValueFormatException("Property value cannot be converted to a PATH");
        }
        try {
            return (absolute) ? getSession().getProperty(path) : getParent().getProperty(path);
        } catch (PathNotFoundException e) {
            throw new ItemNotFoundException(path);
        }
    }

    /** Wrapper around {@link #getValue()} */
    public BigDecimal getDecimal() throws RepositoryException {
        return getValue().getDecimal();
    }

    /** Wrapper around {@link #setValue(Value)} */
    public void setValue(BigDecimal value) throws RepositoryException {
        if (value != null) {
            setValue(getValueFactory().createValue(value));
        } else {
            setValue((Value) null);
        }
    }

    /** Wrapper around {@link #getValue()} */
    public Binary getBinary() throws RepositoryException {
        return getValue().getBinary();
    }

    /** Wrapper around {@link #setValue(Value)} */
    public void setValue(Binary value) throws RepositoryException {
        if (value != null) {
            setValue(getValueFactory().createValue(value));
        } else {
            setValue((Value) null);
        }
    }

    /** Wrapper around {@link #setValue(Value)} */
    public void setValue(Calendar value) throws RepositoryException {
        if (value != null) {
            try {
                setValue(getSession().getValueFactory().createValue(value));
            } catch (IllegalArgumentException e) {
                throw new ValueFormatException(
                        "Value is not an ISO8601 date: " + value, e);
            }
        } else {
            setValue((Value) null);
        }
    }

    /** Wrapper around {@link #setValue(Value)} */
    public void setValue(double value) throws RepositoryException {
        setValue(getValueFactory().createValue(value));
    }

    /** Wrapper around {@link #setValue(Value)} */
    public void setValue(InputStream value) throws RepositoryException {
        if (value != null) {
            Binary binary = getValueFactory().createBinary(value);
            try {
                setValue(getValueFactory().createValue(binary));
            } finally {
                binary.dispose();
            }
        } else {
            setValue((Value) null);
        }
    }

    /** Wrapper around {@link #setValue(Value)} */
    public void setValue(String value) throws RepositoryException {
        if (value != null) {
            setValue(getValueFactory().createValue(value));
        } else {
            setValue((Value) null);
        }
    }

    /** Wrapper around {@link #setValue(Value[])} */
    public void setValue(String[] strings) throws RepositoryException {
        if (strings != null) {
            setValue(getValues(strings, STRING));
        } else {
            setValue((Value[]) null);
        }
    }

    /** Wrapper around {@link #setValue(Value)} */
    public void setValue(boolean value) throws RepositoryException {
        setValue(getValueFactory().createValue(value));
    }

    /** Wrapper around {@link #setValue(Value)} */
    public void setValue(Node value) throws RepositoryException {
        if (value != null) {
            try {
                setValue(getValueFactory().createValue(value));
            } catch (UnsupportedRepositoryOperationException e) {
                throw new ValueFormatException(
                        "Node is not referenceable: " + value, e);
            }
        } else {
            setValue((Value) null);
        }
    }

    /** Wrapper around {@link #setValue(Value)} */
    public void setValue(long value) throws RepositoryException {
        setValue(getValueFactory().createValue(value));
    }

    public synchronized void setValue(Value value)
            throws ValueFormatException, VersionException,
            LockException, ConstraintViolationException,
            RepositoryException {
        // check state of this instance
        sanityCheck();

        // check pre-conditions for setting property value
        checkSetValue(false);

        // check type according to definition of this property
        final PropertyDefinition definition = data.getPropertyDefinition();
        int reqType = definition.getRequiredType();
        if (reqType == UNDEFINED) {
            if (value != null) {
                reqType = value.getType();
            } else {
                reqType = STRING;
            }
        }

        if (value == null) {
            internalSetValue(null, reqType);
            return;
        }

        InternalValue internalValue;
        if (reqType != value.getType()) {
            // type conversion required
            Value targetVal = ValueHelper.convert(
                    value, reqType, getSession().getValueFactory());
            internalValue = InternalValue.create(
                    targetVal, sessionContext, sessionContext.getDataStore());
        } else {
            // no type conversion required
            internalValue = InternalValue.create(
                    value, sessionContext, sessionContext.getDataStore());
        }
        internalSetValue(new InternalValue[]{internalValue}, reqType);
    }

    /**
     * {@inheritDoc}
     */
    public void setValue(Value[] values) throws RepositoryException {
        setValue(values, UNDEFINED);
    }

    /**
     * Sets the values of this property.
     *
     * @param values property values (possibly <code>null</code>)
     * @param valueType default value type if not set in the node type,
     *                  may be {@link PropertyType#UNDEFINED}
     * @throws RepositoryException if the property values could not be set
     */
    public void setValue(Value[] values, int valueType)
            throws RepositoryException {
        // check state of this instance
        sanityCheck();

        // check pre-conditions for setting property value
        checkSetValue(true);

        if (values != null) {
            // check type of values
            int firstValueType = UNDEFINED;
            for (Value value : values) {
                if (value != null) {
                    if (firstValueType == UNDEFINED) {
                        firstValueType = value.getType();
                    } else if (firstValueType != value.getType()) {
                        throw new ValueFormatException(
                                "inhomogeneous type of values");
                    }
                }
            }
        }

        final PropertyDefinition definition = data.getPropertyDefinition();
        int reqType = definition.getRequiredType();
        if (reqType == UNDEFINED) {
            reqType = valueType; // use the given type as property type
        }

        InternalValue[] internalValues = null;
        // convert to internal values of correct type
        if (values != null) {
            internalValues = new InternalValue[values.length];

            // check type of values
            for (int i = 0; i < values.length; i++) {
                Value value = values[i];
                if (value != null) {
                    if (reqType == UNDEFINED) {
                        // Use the type of the fist value as the type
                        reqType = value.getType();
                    }
                    if (reqType != value.getType()) {
                        value = ValueHelper.convert(
                                value, reqType, getSession().getValueFactory());
                    }
                    internalValues[i] = InternalValue.create(
                            value, sessionContext, sessionContext.getDataStore());
                } else {
                    internalValues[i] = null;
                }
            }
        }

        internalSetValue(internalValues, reqType);
    }

    /**
     * {@inheritDoc}
     */
    public long getLength() throws RepositoryException {
        return getLength(internalGetValue());
    }

    /**
     * {@inheritDoc}
     */
    public long[] getLengths() throws RepositoryException {
        InternalValue[] values = internalGetValues();
        long[] lengths = new long[values.length];
        for (int i = 0; i < values.length; i++) {
            lengths[i] = getLength(values[i]);
        }
        return lengths;
    }

    /**
     * {@inheritDoc}
     */
    public PropertyDefinition getDefinition() throws RepositoryException {
        // check state of this instance
        sanityCheck();

        return data.getPropertyDefinition();
    }

    /**
     * {@inheritDoc}
     */
    public int getType() throws RepositoryException {
        return getPropertyState().getType();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isMultiple() throws RepositoryException {
        // check state of this instance
        sanityCheck();

        return getPropertyState().isMultiValued();
    }

    //-----------------------------------------------------------------< Item >
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isNode() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() throws RepositoryException {
        // check state of this instance
        sanityCheck();
        return sessionContext.getJCRName(((PropertyId) id).getName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void accept(ItemVisitor visitor) throws RepositoryException {
        // check state of this instance
        sanityCheck();

        visitor.visit(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Node getParent() throws RepositoryException {
        return getParent(true);
    }

    //--------------------------------------------------------------< Object >

    /**
     * Return a string representation of this property for diagnostic purposes.
     *
     * @return "property /path/to/item"
     */
    public String toString() {
        return "property " + super.toString();
    }

}
