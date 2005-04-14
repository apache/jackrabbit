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
package org.apache.jackrabbit.core;

import org.apache.jackrabbit.core.state.ItemState;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.util.uuid.UUID;
import org.apache.log4j.Logger;

import javax.jcr.AccessDeniedException;
import javax.jcr.BooleanValue;
import javax.jcr.DateValue;
import javax.jcr.DoubleValue;
import javax.jcr.ItemNotFoundException;
import javax.jcr.ItemVisitor;
import javax.jcr.LongValue;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.version.VersionException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;

/**
 * <code>PropertyImpl</code> implements the <code>Property</code> interface.
 */
public class PropertyImpl extends ItemImpl implements Property {

    private static Logger log = Logger.getLogger(PropertyImpl.class);

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
                // remove listener on persistent state
                state.removeListener(this);
                // add listener on transient state
                transientState.addListener(this);
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

    protected void makePersistent() {
        if (!isTransient()) {
            log.debug(safeGetJCRPath() + " (" + id + "): there's no transient state to persist");
            return;
        }

        PropertyState transientState = (PropertyState) state;
        PropertyState persistentState = (PropertyState) transientState.getOverlayedState();
        if (persistentState == null) {
            // this property is 'new'
            persistentState = stateMgr.createNew(transientState.getName(),
                    transientState.getParentUUID());
        }
        // copy state from transient state
        persistentState.setDefinitionId(transientState.getDefinitionId());
        persistentState.setType(transientState.getType());
        persistentState.setMultiValued(transientState.isMultiValued());
        persistentState.setValues(transientState.getValues());
        // make state persistent
        stateMgr.store(persistentState);
        // remove listener from transient state
        transientState.removeListener(this);
        // add listener to persistent state
        persistentState.addListener(this);
        // swap transient state with persistent state
        state = persistentState;
        // reset status
        status = STATUS_NORMAL;
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
        switch (value.getType()) {
            case PropertyType.STRING:
            case PropertyType.LONG:
            case PropertyType.DOUBLE:
                return value.toString().length();

            case PropertyType.NAME:
                QName name = (QName) value.internalValue();
                try {
                    return name.toJCRName(session.getNamespaceResolver()).length();
                } catch (NoPrefixDeclaredException npde) {
                    // should never happen...
                    String msg = safeGetJCRPath()
                            + ": the value represents an invalid name";
                    log.debug(msg);
                    throw new RepositoryException(msg, npde);
                }

            case PropertyType.PATH:
                Path path = (Path) value.internalValue();
                try {
                    return path.toJCRPath(session.getNamespaceResolver()).length();
                } catch (NoPrefixDeclaredException npde) {
                    // should never happen...
                    String msg = safeGetJCRPath()
                            + ": the value represents an invalid path";
                    log.debug(msg);
                    throw new RepositoryException(msg, npde);
                }

            case PropertyType.BINARY:
                BLOBFileValue blob = (BLOBFileValue) value.internalValue();
                return blob.getLength();
        }

        return -1;
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
                if (old != null) {
                    switch (old.getType()) {
                        case PropertyType.BINARY:
                            // BINARY value
                            BLOBFileValue blob = (BLOBFileValue) old.internalValue();
                            blob.delete();
                            break;
                    }
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
     * this method takes a <code>QName</code> instead of a <code>String</code>
     * value.
     *
     * @param name
     * @throws ValueFormatException
     * @throws RepositoryException
     */
    public void setValue(QName name) throws ValueFormatException, RepositoryException {
        // check state of this instance
        sanityCheck();

        // verify that parent node is checked-out
        if (!((NodeImpl) getParent()).internalIsCheckedOut()) {
            throw new VersionException("cannot set the value of a property of a checked-in node " + safeGetJCRPath());
        }

        // check protected flag
        if (definition.isProtected()) {
            throw new ConstraintViolationException("cannot set the value of a protected property " + safeGetJCRPath());
        }

        // check multi-value flag
        if (definition.isMultiple()) {
            throw new ValueFormatException(safeGetJCRPath() + " is multi-valued and can therefore only be set to an array of values");
        }

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
            internalValue =
                    InternalValue.create(InternalValue.create(name).toJCRValue(session.getNamespaceResolver()),
                            reqType, session.getNamespaceResolver());
        } else {
            // no type conversion required
            internalValue = InternalValue.create(name);
        }

        internalSetValue(new InternalValue[]{internalValue}, reqType);
    }

    /**
     * Same as <code>{@link Property#setValue(String[])}</code> except that
     * this method takes an array of <code>QName</code> instead of
     * <code>String</code> values.
     *
     * @param names
     * @throws ValueFormatException
     * @throws RepositoryException
     */
    public void setValue(QName[] names) throws ValueFormatException, RepositoryException {
        // check state of this instance
        sanityCheck();

        // verify that parent node is checked-out
        if (!((NodeImpl) getParent()).internalIsCheckedOut()) {
            throw new VersionException("cannot set the value of a property of a checked-in node " + safeGetJCRPath());
        }

        // check protected flag
        if (definition.isProtected()) {
            throw new ConstraintViolationException("cannot set the value of a protected property " + safeGetJCRPath());
        }

        // check multi-value flag
        if (!definition.isMultiple()) {
            throw new ValueFormatException(safeGetJCRPath() + " is not multi-valued");
        }

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
                QName name = names[i];
                InternalValue internalValue = null;
                if (name != null) {
                    if (reqType != PropertyType.NAME) {
                        // type conversion required
                        internalValue =
                                InternalValue.create(InternalValue.create(name).toJCRValue(session.getNamespaceResolver()),
                                        reqType, session.getNamespaceResolver());
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
    public QName getQName() {
        PropertyId propId = (PropertyId) id;
        return propId.getName();
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
            throw new ValueFormatException(safeGetJCRPath() + " is multi-valued and can therefore only be retrieved as an array of values");
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
            throw new ValueFormatException(safeGetJCRPath() + " is not multi-valued");
        }

        PropertyState state = (PropertyState) getItemState();
        InternalValue[] internalValues = state.getValues();
        Value[] values = new Value[internalValues.length];
        for (int i = 0; i < internalValues.length; i++) {
            values[i] = internalValues[i].toJCRValue(session.getNamespaceResolver());
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
            throw new ValueFormatException(safeGetJCRPath() + " is multi-valued and can therefore only be retrieved as an array of values");
        }

        PropertyState state = (PropertyState) getItemState();
        try {
            InternalValue val = state.getValues()[0];
            return val.toJCRValue(session.getNamespaceResolver());
        } catch (RepositoryException e) {
            throw e;
        } catch (Exception e) {
            //return InternalValue.create(0).toJCRValue(session.getNamespaceResolver());
            throw new RepositoryException("Unable to get value of " + safeGetJCRPath() + ":" + e);
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
            throw new ValueFormatException(safeGetJCRPath() + " is multi-valued and can therefore only be retrieved as an array of values");
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
            throw new ValueFormatException(safeGetJCRPath() + " is multi-valued and can therefore only be retrieved as an array of values");
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
            throw new ValueFormatException(safeGetJCRPath() + " is multi-valued and can therefore only be retrieved as an array of values");
        }

        PropertyState state = (PropertyState) getItemState();
        InternalValue val = state.getValues()[0];
        int type = val.getType();
        if (type == PropertyType.LONG) {
            return ((Long) val.internalValue()).longValue();
        }
        // not a LONG value, delegate conversion to Value object
        return val.toJCRValue(session.getNamespaceResolver()).getLong();
    }

    /**
     * {@inheritDoc}
     */
    public double getDouble() throws ValueFormatException, RepositoryException {
        // check state of this instance
        sanityCheck();

        // check multi-value flag
        if (definition.isMultiple()) {
            throw new ValueFormatException(safeGetJCRPath() + " is multi-valued and can therefore only be retrieved as an array of values");
        }

        // avoid unnecessary object creation if possible
        PropertyState state = (PropertyState) getItemState();
        InternalValue val = state.getValues()[0];
        int type = val.getType();
        if (type == PropertyType.DOUBLE) {
            return ((Double) val.internalValue()).doubleValue();
        }
        // not a DOUBLE value, delegate conversion to Value object
        return val.toJCRValue(session.getNamespaceResolver()).getDouble();
    }

    /**
     * {@inheritDoc}
     */
    public Calendar getDate() throws ValueFormatException, RepositoryException {
        // check state of this instance
        sanityCheck();

        // check multi-value flag
        if (definition.isMultiple()) {
            throw new ValueFormatException(safeGetJCRPath() + " is multi-valued and can therefore only be retrieved as an array of values");
        }

        // avoid unnecessary object creation if possible
        PropertyState state = (PropertyState) getItemState();
        InternalValue val = state.getValues()[0];
        int type = val.getType();
        if (type == PropertyType.DATE) {
            return (Calendar) val.internalValue();
        }
        // not a DATE value, delegate conversion to Value object
        return val.toJCRValue(session.getNamespaceResolver()).getDate();
    }

    /**
     * {@inheritDoc}
     */
    public boolean getBoolean() throws ValueFormatException, RepositoryException {
        // check state of this instance
        sanityCheck();

        // check multi-value flag
        if (definition.isMultiple()) {
            throw new ValueFormatException(safeGetJCRPath() + " is multi-valued and can therefore only be retrieved as an array of values");
        }

        // avoid unnecessary object creation if possible
        PropertyState state = (PropertyState) getItemState();
        InternalValue val = state.getValues()[0];
        int type = val.getType();
        if (type == PropertyType.BOOLEAN) {
            return ((Boolean) val.internalValue()).booleanValue();
        }
        // not a BOOLEAN value, delegate conversion to Value object
        return val.toJCRValue(session.getNamespaceResolver()).getBoolean();
    }

    /**
     * {@inheritDoc}
     */
    public Node getNode() throws ValueFormatException, RepositoryException {
        // check state of this instance
        sanityCheck();

        // check multi-value flag
        if (definition.isMultiple()) {
            throw new ValueFormatException(safeGetJCRPath() + " is multi-valued and can therefore only be retrieved as an array of values");
        }

        PropertyState state = (PropertyState) getItemState();
        InternalValue val = state.getValues()[0];
        if (val.getType() == PropertyType.REFERENCE) {
            // reference, i.e. target UUID
            UUID targetUUID = (UUID) val.internalValue();
            return (Node) itemMgr.getItem(new NodeId(targetUUID.toString()));
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

        // verify that parent node is checked-out
        if (!((NodeImpl) getParent()).internalIsCheckedOut()) {
            throw new VersionException("cannot set the value of a property of a checked-in node " + safeGetJCRPath());
        }

        // check lock status
        ((NodeImpl) getParent()).checkLock();

        // check protected flag
        if (definition.isProtected()) {
            throw new ConstraintViolationException("cannot set the value of a protected property " + safeGetJCRPath());
        }

        // check multi-value flag
        if (definition.isMultiple()) {
            throw new ValueFormatException(safeGetJCRPath() + " is multi-valued and can therefore only be set to an array of values");
        }

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
            value = InternalValue.create(new DateValue(date), reqType, session.getNamespaceResolver());
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

        // verify that parent node is checked-out
        if (!((NodeImpl) getParent()).internalIsCheckedOut()) {
            throw new VersionException("cannot set the value of a property of a checked-in node " + safeGetJCRPath());
        }

        // check protected flag
        if (definition.isProtected()) {
            throw new ConstraintViolationException("cannot set the value of a protected property " + safeGetJCRPath());
        }

        // check multi-value flag
        if (definition.isMultiple()) {
            throw new ValueFormatException(safeGetJCRPath() + " is multi-valued and can therefore only be set to an array of values");
        }

        // check lock status
        ((NodeImpl) getParent()).checkLock();

        // check type according to definition of this property
        int reqType = definition.getRequiredType();
        if (reqType == PropertyType.UNDEFINED) {
            reqType = PropertyType.DOUBLE;
        }

        InternalValue value;
        if (reqType != PropertyType.DOUBLE) {
            // type conversion required
            value = InternalValue.create(new DoubleValue(number), reqType, session.getNamespaceResolver());
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

        // verify that parent node is checked-out
        if (!((NodeImpl) getParent()).internalIsCheckedOut()) {
            throw new VersionException("cannot set the value of a property of a checked-in node " + safeGetJCRPath());
        }

        // check protected flag
        if (definition.isProtected()) {
            throw new ConstraintViolationException("cannot set the value of a protected property " + safeGetJCRPath());
        }

        // check multi-value flag
        if (definition.isMultiple()) {
            throw new ValueFormatException(safeGetJCRPath() + " is multi-valued and can therefore only be set to an array of values");
        }

        // check lock status
        ((NodeImpl) getParent()).checkLock();

        // check type according to definition of this property
        int reqType = definition.getRequiredType();
        if (reqType == PropertyType.UNDEFINED) {
            reqType = PropertyType.BINARY;
        }

        if (stream == null) {
            internalSetValue(null, reqType);
            return;
        }

        InternalValue value;
        try {
            if (reqType != PropertyType.BINARY) {
                // type conversion required
                value = InternalValue.create(new BLOBFileValue(stream), reqType, session.getNamespaceResolver());
            } else {
                // no type conversion required
                value = InternalValue.create(stream);
            }
        } catch (IOException ioe) {
            String msg = "failed to spool stream to internal storage";
            log.debug(msg);
            throw new RepositoryException(msg, ioe);
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

        // verify that parent node is checked-out
        if (!((NodeImpl) getParent()).internalIsCheckedOut()) {
            throw new VersionException("cannot set the value of a property of a checked-in node " + safeGetJCRPath());
        }

        // check protected flag
        if (definition.isProtected()) {
            throw new ConstraintViolationException("cannot set the value of a protected property " + safeGetJCRPath());
        }

        // check multi-value flag
        if (definition.isMultiple()) {
            throw new ValueFormatException(safeGetJCRPath() + " is multi-valued and can therefore only be set to an array of values");
        }

        // check lock status
        ((NodeImpl) getParent()).checkLock();

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
            internalValue = InternalValue.create(string, reqType, session.getNamespaceResolver());
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

        // verify that parent node is checked-out
        if (!((NodeImpl) getParent()).internalIsCheckedOut()) {
            throw new VersionException("cannot set the value of a property of a checked-in node " + safeGetJCRPath());
        }

        // check protected flag
        if (definition.isProtected()) {
            throw new ConstraintViolationException("cannot set the value of a protected property " + safeGetJCRPath());
        }

        // check multi-value flag
        if (!definition.isMultiple()) {
            throw new ValueFormatException(safeGetJCRPath() + " is not multi-valued");
        }

        // check lock status
        ((NodeImpl) getParent()).checkLock();

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
                        internalValue = InternalValue.create(string, reqType, session.getNamespaceResolver());
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

        // verify that parent node is checked-out
        if (!((NodeImpl) getParent()).internalIsCheckedOut()) {
            throw new VersionException("cannot set the value of a property of a checked-in node " + safeGetJCRPath());
        }

        // check protected flag
        if (definition.isProtected()) {
            throw new ConstraintViolationException("cannot set the value of a protected property " + safeGetJCRPath());
        }

        // check multi-value flag
        if (definition.isMultiple()) {
            throw new ValueFormatException(safeGetJCRPath() + " is multi-valued and can therefore only be set to an array of values");
        }

        // check lock status
        ((NodeImpl) getParent()).checkLock();

        // check type according to definition of this property
        int reqType = definition.getRequiredType();
        if (reqType == PropertyType.UNDEFINED) {
            reqType = PropertyType.BOOLEAN;
        }

        InternalValue value;
        if (reqType != PropertyType.BOOLEAN) {
            // type conversion required
            value = InternalValue.create(new BooleanValue(b), reqType, session.getNamespaceResolver());
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

        // verify that parent node is checked-out
        if (!((NodeImpl) getParent()).internalIsCheckedOut()) {
            throw new VersionException("cannot set the value of a property of a checked-in node " + safeGetJCRPath());
        }

        // check protected flag
        if (definition.isProtected()) {
            throw new ConstraintViolationException("cannot set the value of a protected property " + safeGetJCRPath());
        }

        // check multi-value flag
        if (definition.isMultiple()) {
            throw new ValueFormatException(safeGetJCRPath() + " is multi-valued");
        }

        // check lock status
        ((NodeImpl) getParent()).checkLock();

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
                if (targetNode.isNodeType(MIX_REFERENCEABLE)) {
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

        // verify that parent node is checked-out
        if (!((NodeImpl) getParent()).internalIsCheckedOut()) {
            throw new VersionException("cannot set the value of a property of a checked-in node " + safeGetJCRPath());
        }

        // check protected flag
        if (definition.isProtected()) {
            throw new ConstraintViolationException("cannot set the value of a protected property " + safeGetJCRPath());
        }

        // check multi-value flag
        if (definition.isMultiple()) {
            throw new ValueFormatException(safeGetJCRPath() + " is multi-valued and can therefore only be set to an array of values");
        }

        // check lock status
        ((NodeImpl) getParent()).checkLock();

        // check type according to definition of this property
        int reqType = definition.getRequiredType();
        if (reqType == PropertyType.UNDEFINED) {
            reqType = PropertyType.LONG;
        }

        InternalValue value;
        if (reqType != PropertyType.LONG) {
            // type conversion required
            value = InternalValue.create(new LongValue(number), reqType, session.getNamespaceResolver());
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

        // verify that parent node is checked-out
        if (!((NodeImpl) getParent()).internalIsCheckedOut()) {
            String msg = "cannot set the value of a property of a checked-in node " + safeGetJCRPath();
            log.debug(msg);
            throw new VersionException(msg);
        }

        // check protected flag
        if (definition.isProtected()) {
            throw new ConstraintViolationException("cannot set the value of a protected property " + safeGetJCRPath());
        }

        // check multi-value flag
        if (definition.isMultiple()) {
            throw new ValueFormatException(safeGetJCRPath() + " is multi-valued and can therefore only be set to an array of values");
        }

        // check lock status
        ((NodeImpl) getParent()).checkLock();

        // check type according to definition of this property
        int reqType = definition.getRequiredType();
        if (reqType == PropertyType.UNDEFINED) {
            reqType = value != null ? value.getType() : PropertyType.STRING;
        }

        if (value == null) {
            internalSetValue(null, reqType);
            return;
        }

        InternalValue internalValue;
        if (reqType != value.getType()) {
            // type conversion required
            internalValue = InternalValue.create(value, reqType, session.getNamespaceResolver());
        } else {
            // no type conversion required
            internalValue = InternalValue.create(value, session.getNamespaceResolver());
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

        // verify that parent node is checked-out
        if (!((NodeImpl) getParent()).internalIsCheckedOut()) {
            throw new VersionException("cannot set the value of a property of a checked-in node " + safeGetJCRPath());
        }

        // check protected flag
        if (definition.isProtected()) {
            throw new ConstraintViolationException("cannot set the value of a protected property " + safeGetJCRPath());
        }

        // check multi-value flag
        if (!definition.isMultiple()) {
            throw new ValueFormatException(safeGetJCRPath() + " is not multi-valued");
        }

        // check lock status
        ((NodeImpl) getParent()).checkLock();

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
                    if (reqType != PropertyType.UNDEFINED
                            && reqType != value.getType()) {
                        // type conversion required
                        internalValue = InternalValue.create(value, reqType, session.getNamespaceResolver());
                    } else {
                        // no type conversion required
                        internalValue = InternalValue.create(value, session.getNamespaceResolver());
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
    public PropertyDefinition getDefinition() {
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

        PropertyId propId = (PropertyId) id;
        QName name = propId.getName();
        try {
            return name.toJCRName(session.getNamespaceResolver());
        } catch (NoPrefixDeclaredException npde) {
            // should never get here...
            String msg = "internal error: encountered unregistered namespace " + name.getNamespaceURI();
            log.debug(msg);
            throw new RepositoryException(msg, npde);
        }
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

        PropertyState thisState = (PropertyState) state;
        return (Node) itemMgr.getItem(new NodeId(thisState.getParentUUID()));
    }
}
