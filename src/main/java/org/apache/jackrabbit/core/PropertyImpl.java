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

import org.apache.jackrabbit.core.state.ItemState;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.value.BLOBFileValue;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.uuid.UUID;
import org.apache.jackrabbit.value.BooleanValue;
import org.apache.jackrabbit.value.DateValue;
import org.apache.jackrabbit.value.DoubleValue;
import org.apache.jackrabbit.value.LongValue;
import org.apache.jackrabbit.value.ValueHelper;
import org.apache.jackrabbit.value.ValueFactoryImpl;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.ItemVisitor;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.version.VersionException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;

/**
 * <code>PropertyImpl</code> implements the <code>Property</code> interface.
 */
public class PropertyImpl extends ItemImpl implements Property {

    private static Logger log = LoggerFactory.getLogger(PropertyImpl.class);

    private PropertyDefinition definition;

    /**
     * Package private constructor.
     *
     * @param itemMgr    the <code>ItemManager</code> that created this <code>Property</code>
     * @param session    the <code>Session</code> through which this <code>Property</code> is acquired
     * @param id         id of this <code>Property</code>
     * @param state      state associated with this <code>Property</code>
     * @param definition definition of <i>this</i> <code>Property</code>
     * @param listeners  listeners on life cylce changes of this <code>PropertyImpl</code>
     */
    PropertyImpl(ItemManager itemMgr, SessionImpl session, PropertyId id,
                 PropertyState state, PropertyDefinition definition,
                 ItemLifeCycleListener[] listeners) {
        super(itemMgr, session, id, state, listeners);
        this.definition = definition;
        // value will be read on demand
    }

    protected synchronized ItemState getOrCreateTransientItemState()
            throws RepositoryException {
        if (!isTransient()) {
            // make transient (copy-on-write)
            try {
                PropertyState transientState =
                        stateMgr.createTransientPropertyState((PropertyState) state, ItemState.STATUS_EXISTING_MODIFIED);
                // swap persistent with transient state
                state = transientState;
            } catch (ItemStateException ise) {
                String msg = "failed to create transient state";
                log.debug(msg);
                throw new RepositoryException(msg, ise);
            }
        }
        return state;
    }

    protected void makePersistent() throws InvalidItemStateException {
        if (!isTransient()) {
            log.debug(safeGetJCRPath() + " (" + id + "): there's no transient state to persist");
            return;
        }

        PropertyState transientState = (PropertyState) state;
        PropertyState persistentState = (PropertyState) transientState.getOverlayedState();
        if (persistentState == null) {
            // this property is 'new'
            persistentState = stateMgr.createNew(transientState);
        }

        synchronized (persistentState) {
            // check staleness of transient state first
            if (transientState.isStale()) {
                String msg = safeGetJCRPath()
                        + ": the property cannot be saved because it has been modified externally.";
                log.debug(msg);
                throw new InvalidItemStateException(msg);
            }
            // copy state from transient state
            persistentState.setDefinitionId(transientState.getDefinitionId());
            persistentState.setType(transientState.getType());
            persistentState.setMultiValued(transientState.isMultiValued());
            persistentState.setValues(transientState.getValues());
            // make state persistent
            stateMgr.store(persistentState);
        }

        // tell state manager to disconnect item state
        stateMgr.disconnectTransientItemState(transientState);
        // swap transient state with persistent state
        state = persistentState;
        // reset status
        status = STATUS_NORMAL;
    }

    protected void restoreTransient(PropertyState transientState)
            throws RepositoryException {
        PropertyState thisState = (PropertyState) getOrCreateTransientItemState();
        if (transientState.getStatus() == ItemState.STATUS_NEW
                && thisState.getStatus() != ItemState.STATUS_NEW) {
            thisState.setStatus(ItemState.STATUS_NEW);
            stateMgr.disconnectTransientItemState(thisState);
        }
        // reapply transient changes
        thisState.setDefinitionId(transientState.getDefinitionId());
        thisState.setType(transientState.getType());
        thisState.setMultiValued(transientState.isMultiValued());
        thisState.setValues(transientState.getValues());
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
        // TODO maybe move method to InternalValue
        switch (value.getType()) {
            case PropertyType.STRING:
            case PropertyType.LONG:
            case PropertyType.DOUBLE:
                return value.toString().length();

            case PropertyType.NAME:
                Name name = value.getQName();
                return session.getJCRName(name).length();

            case PropertyType.PATH:
                Path path = value.getPath();
                return session.getJCRPath(path).length();

            case PropertyType.BINARY:
                BLOBFileValue blob = value.getBLOBFileValue();
                return blob.getLength();

            default:
                return -1;
        }
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
        NodeImpl parent = (NodeImpl) getParent();

        // verify that parent node is checked-out
        if (!parent.internalIsCheckedOut()) {
            throw new VersionException("cannot set the value of a property of a checked-in node "
                    + safeGetJCRPath());
        }

        // check protected flag
        if (definition.isProtected()) {
            throw new ConstraintViolationException("cannot set the value of a protected property "
                    + safeGetJCRPath());
        }

        // check multi-value flag
        if (multipleValues) {
            if (!definition.isMultiple()) {
                throw new ValueFormatException(safeGetJCRPath()
                        + " is not multi-valued");
            }
        } else {
            if (definition.isMultiple()) {
                throw new ValueFormatException(safeGetJCRPath()
                        + " is multi-valued and can therefore only be set to an array of values");
            }
        }

        // check lock status
        parent.checkLock();
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
        ArrayList list = new ArrayList();
        // compact array (purge null entries)
        for (int i = 0; i < values.length; i++) {
            if (values[i] != null) {
                list.add(values[i]);
            }
        }
        values = (InternalValue[]) list.toArray(new InternalValue[list.size()]);

        // modify the state of this property
        PropertyState thisState = (PropertyState) getOrCreateTransientItemState();

        // free old values as necessary
        InternalValue[] oldValues = thisState.getValues();
        if (oldValues != null) {
            for (int i = 0; i < oldValues.length; i++) {
                InternalValue old = oldValues[i];
                if (old != null && old.getType() == PropertyType.BINARY) {
                    // make sure temporarily allocated data is discarded
                    // before overwriting it
                    old.getBLOBFileValue().discard();
                }
            }
        }

        // set new values
        thisState.setValues(values);
        // set type
        if (type == PropertyType.UNDEFINED) {
            // fallback to default type
            type = PropertyType.STRING;
        }
        thisState.setType(type);
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
        int reqType = definition.getRequiredType();
        if (reqType == PropertyType.UNDEFINED) {
            reqType = PropertyType.NAME;
        }

        if (name == null) {
            internalSetValue(null, reqType);
            return;
        }

        InternalValue internalValue;
        if (reqType != PropertyType.NAME) {
            // type conversion required
            Value targetValue = ValueHelper.convert(
                    InternalValue.create(name).toJCRValue(session.getNamePathResolver()),
                    reqType,
                    ValueFactoryImpl.getInstance());
            internalValue = InternalValue.create(targetValue, session.getNamePathResolver(), rep.getDataStore());
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
        int reqType = definition.getRequiredType();
        if (reqType == PropertyType.UNDEFINED) {
            reqType = PropertyType.NAME;
        }

        InternalValue[] internalValues = null;
        // convert to internal values of correct type
        if (names != null) {
            internalValues = new InternalValue[names.length];
            for (int i = 0; i < names.length; i++) {
                Name name = names[i];
                InternalValue internalValue = null;
                if (name != null) {
                    if (reqType != PropertyType.NAME) {
                        // type conversion required
                        Value targetValue = ValueHelper.convert(
                                InternalValue.create(name).toJCRValue(session.getNamePathResolver()),
                                reqType,
                                ValueFactoryImpl.getInstance());
                        internalValue = InternalValue.create(targetValue, session.getNamePathResolver(), rep.getDataStore());
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
    public Name getQName() {
        return ((PropertyId) id).getName();
    }

    /**
     * Returns the internal values of this property
     *
     * @return
     * @throws RepositoryException
     */
    public InternalValue[] internalGetValues() throws RepositoryException {

        // check state of this instance
        sanityCheck();

        // check multi-value flag
        if (!definition.isMultiple()) {
            throw new ValueFormatException(safeGetJCRPath() + " is not multi-valued");
        }

        PropertyState state = (PropertyState) getItemState();
        return state.getValues();
    }

    /**
     * Returns the internal values of this property
     *
     * @return
     * @throws RepositoryException
     */
    public InternalValue internalGetValue() throws RepositoryException {
        // check state of this instance
        sanityCheck();

        // check multi-value flag
        if (definition.isMultiple()) {
            throw new ValueFormatException(safeGetJCRPath()
                    + " is multi-valued and can therefore only be retrieved as an array of values");
        }

        PropertyState state = (PropertyState) getItemState();
        return state.getValues()[0];
    }

    //-------------------------------------------------------------< Property >
    /**
     * {@inheritDoc}
     */
    public Value[] getValues() throws ValueFormatException, RepositoryException {
        // check state of this instance
        sanityCheck();

        // check multi-value flag
        if (!definition.isMultiple()) {
            throw new ValueFormatException(safeGetJCRPath()
                    + " is not multi-valued");
        }

        PropertyState state = (PropertyState) getItemState();
        InternalValue[] internalValues = state.getValues();
        Value[] values = new Value[internalValues.length];
        for (int i = 0; i < internalValues.length; i++) {
            values[i] = internalValues[i].toJCRValue(session.getNamePathResolver());
        }
        return values;
    }

    /**
     * {@inheritDoc}
     */
    public Value getValue() throws ValueFormatException, RepositoryException {
        // check state of this instance
        sanityCheck();

        // check multi-value flag
        if (definition.isMultiple()) {
            throw new ValueFormatException(safeGetJCRPath()
                    + " is multi-valued and can therefore only be retrieved as an array of values");
        }

        PropertyState state = (PropertyState) getItemState();
        try {
            InternalValue val = state.getValues()[0];
            return val.toJCRValue(session.getNamePathResolver());
        } catch (RepositoryException e) {
            throw e;
        } catch (Exception e) {
            String msg = "Internal error while retrieving value of "
                    + safeGetJCRPath();
            log.error(msg, e);
            throw new RepositoryException(msg, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getString() throws ValueFormatException, RepositoryException {
        // check state of this instance
        sanityCheck();

        // check multi-value flag
        if (definition.isMultiple()) {
            throw new ValueFormatException(safeGetJCRPath()
                    + " is multi-valued and can therefore only be retrieved as an array of values");
        }

        return getValue().getString();
    }

    /**
     * {@inheritDoc}
     */
    public InputStream getStream() throws ValueFormatException, RepositoryException {
        // check state of this instance
        sanityCheck();

        // check multi-value flag
        if (definition.isMultiple()) {
            throw new ValueFormatException(safeGetJCRPath()
                    + " is multi-valued and can therefore only be retrieved as an array of values");
        }

        return getValue().getStream();
    }

    /**
     * {@inheritDoc}
     */
    public long getLong() throws ValueFormatException, RepositoryException {
        // check state of this instance
        sanityCheck();

        // check multi-value flag
        if (definition.isMultiple()) {
            throw new ValueFormatException(safeGetJCRPath()
                    + " is multi-valued and can therefore only be retrieved as an array of values");
        }

        PropertyState state = (PropertyState) getItemState();
        InternalValue val = state.getValues()[0];
        int type = val.getType();
        if (type == PropertyType.LONG) {
            return val.getLong();
        }
        // not a LONG value, delegate conversion to Value object
        return val.toJCRValue(session.getNamePathResolver()).getLong();
    }

    /**
     * {@inheritDoc}
     */
    public double getDouble() throws ValueFormatException, RepositoryException {
        // check state of this instance
        sanityCheck();

        // check multi-value flag
        if (definition.isMultiple()) {
            throw new ValueFormatException(safeGetJCRPath()
                    + " is multi-valued and can therefore only be retrieved as an array of values");
        }

        // avoid unnecessary object creation if possible
        PropertyState state = (PropertyState) getItemState();
        InternalValue val = state.getValues()[0];
        int type = val.getType();
        if (type == PropertyType.DOUBLE) {
            return val.getDouble();
        }
        // not a DOUBLE value, delegate conversion to Value object
        return val.toJCRValue(session.getNamePathResolver()).getDouble();
    }

    /**
     * {@inheritDoc}
     */
    public Calendar getDate() throws ValueFormatException, RepositoryException {
        // check state of this instance
        sanityCheck();

        // check multi-value flag
        if (definition.isMultiple()) {
            throw new ValueFormatException(safeGetJCRPath()
                    + " is multi-valued and can therefore only be retrieved as an array of values");
        }

        // avoid unnecessary object creation if possible
        PropertyState state = (PropertyState) getItemState();
        InternalValue val = state.getValues()[0];
        int type = val.getType();
        if (type == PropertyType.DATE) {
            return val.getDate();
        }
        // not a DATE value, delegate conversion to Value object
        return val.toJCRValue(session.getNamePathResolver()).getDate();
    }

    /**
     * {@inheritDoc}
     */
    public boolean getBoolean() throws ValueFormatException, RepositoryException {
        // check state of this instance
        sanityCheck();

        // check multi-value flag
        if (definition.isMultiple()) {
            throw new ValueFormatException(safeGetJCRPath()
                    + " is multi-valued and can therefore only be retrieved as an array of values");
        }

        // avoid unnecessary object creation if possible
        PropertyState state = (PropertyState) getItemState();
        InternalValue val = state.getValues()[0];
        int type = val.getType();
        if (type == PropertyType.BOOLEAN) {
            return val.getBoolean();
        }
        // not a BOOLEAN value, delegate conversion to Value object
        return val.toJCRValue(session.getNamePathResolver()).getBoolean();
    }

    /**
     * {@inheritDoc}
     */
    public Node getNode() throws ValueFormatException, RepositoryException {
        // check state of this instance
        sanityCheck();

        // check multi-value flag
        if (definition.isMultiple()) {
            throw new ValueFormatException(safeGetJCRPath()
                    + " is multi-valued and can therefore only be retrieved as an array of values");
        }

        PropertyState state = (PropertyState) getItemState();
        InternalValue val = state.getValues()[0];
        if (val.getType() == PropertyType.REFERENCE) {
            // reference, i.e. target UUID
            UUID targetUUID = val.getUUID();
            return (Node) itemMgr.getItem(new NodeId(targetUUID));
        } else {
            throw new ValueFormatException("property must be of type REFERENCE");
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setValue(Calendar date)
            throws ValueFormatException, VersionException,
            LockException, ConstraintViolationException,
            RepositoryException {
        // check state of this instance
        sanityCheck();

        // check pre-conditions for setting property value
        checkSetValue(false);

        // check type according to definition of this property
        int reqType = definition.getRequiredType();
        if (reqType == PropertyType.UNDEFINED) {
            reqType = PropertyType.DATE;
        }

        if (date == null) {
            internalSetValue(null, reqType);
            return;
        }

        InternalValue value;
        if (reqType != PropertyType.DATE) {
            // type conversion required
            Value targetVal = ValueHelper.convert(
                    new DateValue(date), reqType,
                    ValueFactoryImpl.getInstance());
            value = InternalValue.create(targetVal, session.getNamePathResolver(), rep.getDataStore());
        } else {
            // no type conversion required
            value = InternalValue.create(date);
        }

        internalSetValue(new InternalValue[]{value}, reqType);
    }

    /**
     * {@inheritDoc}
     */
    public void setValue(double number)
            throws ValueFormatException, VersionException,
            LockException, ConstraintViolationException,
            RepositoryException {
        // check state of this instance
        sanityCheck();

        // check pre-conditions for setting property value
        checkSetValue(false);

        // check type according to definition of this property
        int reqType = definition.getRequiredType();
        if (reqType == PropertyType.UNDEFINED) {
            reqType = PropertyType.DOUBLE;
        }

        InternalValue value;
        if (reqType != PropertyType.DOUBLE) {
            // type conversion required
            Value targetVal = ValueHelper.convert(
                    new DoubleValue(number), reqType,
                    ValueFactoryImpl.getInstance());
            value = InternalValue.create(targetVal, session.getNamePathResolver(), rep.getDataStore());
        } else {
            // no type conversion required
            value = InternalValue.create(number);
        }

        internalSetValue(new InternalValue[]{value}, reqType);
    }

    /**
     * {@inheritDoc}
     */
    public void setValue(InputStream stream)
            throws ValueFormatException, VersionException,
            LockException, ConstraintViolationException,
            RepositoryException {
        // check state of this instance
        sanityCheck();

        // check pre-conditions for setting property value
        checkSetValue(false);

        // check type according to definition of this property
        int reqType = definition.getRequiredType();
        if (reqType == PropertyType.UNDEFINED) {
            reqType = PropertyType.BINARY;
        }

        if (stream == null) {
            internalSetValue(null, reqType);
            return;
        }

        InternalValue value = InternalValue.createTemporary(stream, rep.getDataStore());
        if (reqType != PropertyType.BINARY) {
            // type conversion required
            Value jcrValue = value.toJCRValue(session.getNamePathResolver());
            Value targetVal = ValueHelper.convert(
                    jcrValue, reqType, ValueFactoryImpl.getInstance());
            value = InternalValue.create(targetVal, session.getNamePathResolver(), rep.getDataStore());
        }

        internalSetValue(new InternalValue[]{value}, reqType);
    }

    /**
     * {@inheritDoc}
     */
    public void setValue(String string)
            throws ValueFormatException, VersionException,
            LockException, ConstraintViolationException,
            RepositoryException {
        // check state of this instance
        sanityCheck();

        // check pre-conditions for setting property value
        checkSetValue(false);

        // check type according to definition of this property
        int reqType = definition.getRequiredType();
        if (reqType == PropertyType.UNDEFINED) {
            reqType = PropertyType.STRING;
        }

        if (string == null) {
            internalSetValue(null, reqType);
            return;
        }

        InternalValue internalValue;
        if (reqType != PropertyType.STRING) {
            // type conversion required
            Value targetValue = ValueHelper.convert(
                    string, reqType,
                    ValueFactoryImpl.getInstance());
            internalValue = InternalValue.create(targetValue, session.getNamePathResolver(), rep.getDataStore());
        } else {
            // no type conversion required
            internalValue = InternalValue.create(string);
        }
        internalSetValue(new InternalValue[]{internalValue}, reqType);
    }

    /**
     * {@inheritDoc}
     */
    public void setValue(String[] strings)
            throws ValueFormatException, VersionException,
            LockException, ConstraintViolationException,
            RepositoryException {
        // check state of this instance
        sanityCheck();

        // check pre-conditions for setting property value
        checkSetValue(true);

        // check type according to definition of this property
        int reqType = definition.getRequiredType();
        if (reqType == PropertyType.UNDEFINED) {
            reqType = PropertyType.STRING;
        }

        InternalValue[] internalValues = null;
        // convert to internal values of correct type
        if (strings != null) {
            internalValues = new InternalValue[strings.length];
            for (int i = 0; i < strings.length; i++) {
                String string = strings[i];
                InternalValue internalValue = null;
                if (string != null) {
                    if (reqType != PropertyType.STRING) {
                        // type conversion required
                        Value targetValue = ValueHelper.convert(
                                string, reqType,
                                ValueFactoryImpl.getInstance());
                        internalValue = InternalValue.create(targetValue, session.getNamePathResolver(), rep.getDataStore());
                    } else {
                        // no type conversion required
                        internalValue = InternalValue.create(string);
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
    public void setValue(boolean b)
            throws ValueFormatException, VersionException,
            LockException, ConstraintViolationException,
            RepositoryException {
        // check state of this instance
        sanityCheck();

        // check pre-conditions for setting property value
        checkSetValue(false);

        // check type according to definition of this property
        int reqType = definition.getRequiredType();
        if (reqType == PropertyType.UNDEFINED) {
            reqType = PropertyType.BOOLEAN;
        }

        InternalValue value;
        if (reqType != PropertyType.BOOLEAN) {
            // type conversion required
            Value targetVal = ValueHelper.convert(
                    new BooleanValue(b), reqType,
                    ValueFactoryImpl.getInstance());
            value = InternalValue.create(targetVal, session.getNamePathResolver(), rep.getDataStore());
        } else {
            // no type conversion required
            value = InternalValue.create(b);
        }

        internalSetValue(new InternalValue[]{value}, reqType);
    }

    /**
     * {@inheritDoc}
     */
    public void setValue(Node target)
            throws ValueFormatException, VersionException,
            LockException, ConstraintViolationException,
            RepositoryException {
        // check state of this instance
        sanityCheck();

        // check pre-conditions for setting property value
        checkSetValue(false);

        // check type according to definition of this property
        int reqType = definition.getRequiredType();
        if (reqType == PropertyType.UNDEFINED) {
            reqType = PropertyType.REFERENCE;
        }

        if (target == null) {
            internalSetValue(null, reqType);
            return;
        }

        if (reqType == PropertyType.REFERENCE) {
            if (target instanceof NodeImpl) {
                NodeImpl targetNode = (NodeImpl) target;
                if (targetNode.isNodeType(NameConstants.MIX_REFERENCEABLE)) {
                    InternalValue value = InternalValue.create(new UUID(targetNode.getUUID()));
                    internalSetValue(new InternalValue[]{value}, reqType);
                } else {
                    throw new ValueFormatException("target node must be of node type mix:referenceable");
                }
            } else {
                String msg = "incompatible Node object: " + target;
                log.debug(msg);
                throw new RepositoryException(msg);
            }
        } else {
            throw new ValueFormatException("property must be of type REFERENCE");
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setValue(long number)
            throws ValueFormatException, VersionException,
            LockException, RepositoryException {
        // check state of this instance
        sanityCheck();

        // check pre-conditions for setting property value
        checkSetValue(false);

        // check type according to definition of this property
        int reqType = definition.getRequiredType();
        if (reqType == PropertyType.UNDEFINED) {
            reqType = PropertyType.LONG;
        }

        InternalValue value;
        if (reqType != PropertyType.LONG) {
            // type conversion required
            Value targetVal = ValueHelper.convert(
                    new LongValue(number), reqType,
                    ValueFactoryImpl.getInstance());
            value = InternalValue.create(targetVal, session.getNamePathResolver(), rep.getDataStore());
        } else {
            // no type conversion required
            value = InternalValue.create(number);
        }

        internalSetValue(new InternalValue[]{value}, reqType);
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void setValue(Value value)
            throws ValueFormatException, VersionException,
            LockException, ConstraintViolationException,
            RepositoryException {
        // check state of this instance
        sanityCheck();

        // check pre-conditions for setting property value
        checkSetValue(false);

        // check type according to definition of this property
        int reqType = definition.getRequiredType();
        if (reqType == PropertyType.UNDEFINED) {
            if (value != null) {
                reqType = value.getType();
            } else {
                reqType = PropertyType.STRING;
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
                    value, reqType,
                    ValueFactoryImpl.getInstance());
            internalValue = InternalValue.create(targetVal, session.getNamePathResolver(), rep.getDataStore());
        } else {
            // no type conversion required
            internalValue = InternalValue.create(value, session.getNamePathResolver(), rep.getDataStore());
        }
        internalSetValue(new InternalValue[]{internalValue}, reqType);
    }

    /**
     * {@inheritDoc}
     */
    public void setValue(Value[] values)
            throws ValueFormatException, VersionException,
            LockException, ConstraintViolationException,
            RepositoryException {
        // check state of this instance
        sanityCheck();

        // check pre-conditions for setting property value
        checkSetValue(true);

        if (values != null) {
            // check type of values
            int valueType = PropertyType.UNDEFINED;
            for (int i = 0; i < values.length; i++) {
                if (values[i] == null) {
                    // skip null values as those will be purged later
                    continue;
                }
                if (valueType == PropertyType.UNDEFINED) {
                    valueType = values[i].getType();
                } else if (valueType != values[i].getType()) {
                    // inhomogeneous types
                    String msg = "inhomogeneous type of values";
                    log.debug(msg);
                    throw new ValueFormatException(msg);
                }
            }
        }

        int reqType = definition.getRequiredType();

        InternalValue[] internalValues = null;
        // convert to internal values of correct type
        if (values != null) {
            internalValues = new InternalValue[values.length];
            for (int i = 0; i < values.length; i++) {
                Value value = values[i];
                InternalValue internalValue = null;
                if (value != null) {
                    // check type according to definition of this property
                    if (reqType == PropertyType.UNDEFINED) {
                        // use the value's type as property type
                        reqType = value.getType();
                    }
                    if (reqType != value.getType()) {
                        // type conversion required
                        Value targetVal = ValueHelper.convert(
                                value, reqType,
                                ValueFactoryImpl.getInstance());
                        internalValue = InternalValue.create(targetVal, session.getNamePathResolver(), rep.getDataStore());
                    } else {
                        // no type conversion required
                        internalValue = InternalValue.create(value, session.getNamePathResolver(), rep.getDataStore());
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
    public long getLength() throws ValueFormatException, RepositoryException {
        // check state of this instance
        sanityCheck();

        // check multi-value flag
        if (definition.isMultiple()) {
            throw new ValueFormatException(safeGetJCRPath() + " is multi-valued");
        }

        InternalValue[] values = ((PropertyState) state).getValues();
        if (values.length == 0) {
            // should never be the case, but being a little paranoid can't hurt...
            log.warn(safeGetJCRPath() + ": single-valued property with no value");
            return -1;
        }
        return getLength(values[0]);
    }

    /**
     * {@inheritDoc}
     */
    public long[] getLengths() throws ValueFormatException, RepositoryException {
        // check state of this instance
        sanityCheck();

        // check multi-value flag
        if (!definition.isMultiple()) {
            throw new ValueFormatException(safeGetJCRPath() + " is not multi-valued");
        }

        InternalValue[] values = ((PropertyState) state).getValues();
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

        return definition;
    }

    /**
     * {@inheritDoc}
     */
    public int getType() throws RepositoryException {
        // check state of this instance
        sanityCheck();

        return ((PropertyState) state).getType();
    }

    //-----------------------------------------------------------------< Item >
    /**
     * {@inheritDoc}
     */
    public boolean isNode() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public String getName() throws RepositoryException {
        // check state of this instance
        sanityCheck();
        return session.getJCRName(((PropertyId) id).getName());
    }

    /**
     * {@inheritDoc}
     */
    public void accept(ItemVisitor visitor) throws RepositoryException {
        // check state of this instance
        sanityCheck();

        visitor.visit(this);
    }

    /**
     * {@inheritDoc}
     */
    public Node getParent()
            throws ItemNotFoundException, AccessDeniedException, RepositoryException {
        // check state of this instance
        sanityCheck();

        return (Node) itemMgr.getItem(state.getParentId());
    }
}
