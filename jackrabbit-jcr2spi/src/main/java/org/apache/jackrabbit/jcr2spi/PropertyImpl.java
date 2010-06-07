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
package org.apache.jackrabbit.jcr2spi;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Calendar;

import javax.jcr.Binary;
import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.ItemVisitor;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.version.VersionException;

import org.apache.jackrabbit.jcr2spi.operation.Operation;
import org.apache.jackrabbit.jcr2spi.operation.SetPropertyValue;
import org.apache.jackrabbit.jcr2spi.state.PropertyState;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.spi.commons.conversion.NameResolver;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.spi.commons.value.ValueFormat;
import org.apache.jackrabbit.value.ValueHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>PropertyImpl</code>...
 */
public class PropertyImpl extends ItemImpl implements Property {

    private static Logger log = LoggerFactory.getLogger(PropertyImpl.class);

    public static final int UNDEFINED_PROPERTY_LENGTH = -1;

    public PropertyImpl(SessionImpl session, PropertyState state, ItemLifeCycleListener[] listeners) {
        super(session, state, listeners);
        // NOTE: JCR value(s) will be read (and converted from the internal value
        // representation) on demand.
    }

    //-----------------------------------------------------< Item interface >---
    /**
     * @see Item#getName()
     */
    @Override
    public String getName() throws RepositoryException {
        checkStatus();
        Name name = getQName();
        return session.getNameResolver().getJCRName(name);
    }

    /**
     * Implementation of {@link Item#accept(javax.jcr.ItemVisitor)} for property.
     *
     * @param visitor
     * @see Item#accept(javax.jcr.ItemVisitor)
     */
    @Override
    public void accept(ItemVisitor visitor) throws RepositoryException {
        checkStatus();
        visitor.visit(this);
    }

    /**
     * Returns false
     *
     * @return false
     * @see javax.jcr.Item#isNode()
     */
    @Override
    public boolean isNode() {
        return false;
    }

    //-------------------------------------------------< Property interface >---
    /**
     * @see Property#setValue(javax.jcr.Value)
     */
    public void setValue(Value value) throws ValueFormatException, VersionException, LockException, RepositoryException {
        checkIsWritable(false);
        int valueType = (value != null) ? value.getType() : PropertyType.UNDEFINED;
        int reqType = getRequiredType(valueType);
        setValue(value, reqType);
    }

    /**
     * @see Property#setValue(javax.jcr.Value[])
     */
    public void setValue(Value[] values) throws ValueFormatException, VersionException, LockException, RepositoryException {
        checkIsWritable(true);
        // assert equal types for all values entries
        int valueType = PropertyType.UNDEFINED;
        if (values != null) {
            for (int i = 0; i < values.length; i++) {
                if (values[i] == null) {
                    // skip null values as those will be purged later
                    continue;
                }
                if (valueType == PropertyType.UNDEFINED) {
                    valueType = values[i].getType();
                } else if (valueType != values[i].getType()) {
                    String msg = "Inhomogeneous type of values (" + safeGetJCRPath() + ")";
                    log.debug(msg);
                    throw new ValueFormatException(msg);
                }
            }
        }

        int targetType = getDefinition().getRequiredType();
        if (targetType == PropertyType.UNDEFINED) {
            targetType = (valueType == PropertyType.UNDEFINED) ?  PropertyType.STRING : valueType;
        }
        // convert to internal values of correct type
        QValue[] qValues = null;
        if (values != null) {
            Value[] vs = ValueHelper.convert(values, targetType, session.getValueFactory());
            qValues = ValueFormat.getQValues(vs, session.getNamePathResolver(), session.getQValueFactory());
        }
        setInternalValues(qValues, targetType);
    }

    /**
     * @see Property#setValue(String)
     */
    public void setValue(String value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        checkIsWritable(false);
        int reqType = getRequiredType(PropertyType.STRING);
        if (value == null) {
            setInternalValues(null, reqType);
        } else {
            setValue(session.getValueFactory().createValue(value), reqType);
        }
    }

    /**
     * @see Property#setValue(String[])
     */
    public void setValue(String[] values) throws ValueFormatException, VersionException, LockException, RepositoryException {
        checkIsWritable(true);
        int reqType = getRequiredType(PropertyType.STRING);

        QValue[] qValues = null;
        // convert to internal values of correct type
        if (values != null) {
            qValues = new QValue[values.length];
            for (int i = 0; i < values.length; i++) {
                String string = values[i];
                QValue qValue = null;
                if (string != null) {
                    if (reqType != PropertyType.STRING) {
                        // type conversion required
                        Value v = ValueHelper.convert(string, reqType, session.getValueFactory());
                        qValue = ValueFormat.getQValue(v, session.getNamePathResolver(), session.getQValueFactory());
                    } else {
                        // no type conversion required
                        qValue = session.getQValueFactory().create(string, PropertyType.STRING);
                    }
                }
                qValues[i] = qValue;
            }
        }
        setInternalValues(qValues, reqType);
    }

    /**
     * @see Property#setValue(InputStream)
     */
    public void setValue(InputStream value) throws ValueFormatException, VersionException, LockException, RepositoryException {
        checkIsWritable(false);
        int reqType = getRequiredType(PropertyType.BINARY);
        if (value == null) {
            setInternalValues(null, reqType);
        } else {
            setValue(session.getValueFactory().createValue(value), reqType);
        }
    }

    /**
     * @see Property#setValue(Binary)
     */
    public void setValue(Binary value) throws RepositoryException {
        checkIsWritable(false);
        int reqType = getRequiredType(PropertyType.BINARY);
        if (value == null) {
            setInternalValues(null, reqType);
        } else {
            setValue(session.getValueFactory().createValue(value), reqType);
        }
    }

    /**
     * @see Property#setValue(long)
     */
    public void setValue(long value) throws ValueFormatException, VersionException, LockException, RepositoryException {
        checkIsWritable(false);
        int reqType = getRequiredType(PropertyType.LONG);
        setValue(session.getValueFactory().createValue(value), reqType);
    }

    /**
     * @see Property#setValue(double)
     */
    public void setValue(double value) throws ValueFormatException, VersionException, LockException, RepositoryException {
        checkIsWritable(false);
        int reqType = getRequiredType(PropertyType.DOUBLE);
        setValue(session.getValueFactory().createValue(value), reqType);
    }

    /**
     * @see Property#setValue(BigDecimal)
     */
    public void setValue(BigDecimal value) throws RepositoryException {
        checkIsWritable(false);
        int reqType = getRequiredType(PropertyType.DECIMAL);
        setValue(session.getValueFactory().createValue(value), reqType);
    }

    /**
     * @see Property#setValue(Calendar)
     */
    public void setValue(Calendar value) throws ValueFormatException, VersionException, LockException, RepositoryException {
        checkIsWritable(false);
        int reqType = getRequiredType(PropertyType.DATE);
        if (value == null) {
            setInternalValues(null, reqType);
        } else {
            setValue(session.getValueFactory().createValue(value), reqType);
        }
    }

    /**
     * @see Property#setValue(boolean)
     */
    public void setValue(boolean value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        checkIsWritable(false);
        int reqType = getRequiredType(PropertyType.BOOLEAN);
        setValue(session.getValueFactory().createValue(value), reqType);
    }

    /**
     * @see Property#setValue(Node)
     */
    public void setValue(Node value) throws ValueFormatException, VersionException, LockException, RepositoryException {
        checkIsWritable(false);
        int reqType = getRequiredType(PropertyType.REFERENCE);
        if (value == null) {
            setInternalValues(null, reqType);
        } else {
            checkValidReference(value, reqType, session.getNameResolver());
            QValue qValue = session.getQValueFactory().create(value.getUUID(), PropertyType.REFERENCE);
            setInternalValues(new QValue[]{qValue}, reqType);
        }
    }

    /**
     * @see Property#getValue()
     */
    public Value getValue() throws ValueFormatException, RepositoryException {
        QValue value = getQValue();
        return ValueFormat.getJCRValue(value, session.getNamePathResolver(), session.getJcrValueFactory());
    }

    /**
     * @see Property#getValues()
     */
    public Value[] getValues() throws ValueFormatException, RepositoryException {
        QValue[] qValues = getQValues();
        Value[] values = new Value[qValues.length];
        for (int i = 0; i < qValues.length; i++) {
            values[i] = ValueFormat.getJCRValue(qValues[i], session.getNamePathResolver(), session.getJcrValueFactory());
        }
        return values;
    }

    /**
     * @see Property#getString()
     */
    public String getString() throws ValueFormatException, RepositoryException {
        return getValue().getString();
    }

    /**
     * @see Property#getStream()
     */
    public InputStream getStream() throws ValueFormatException, RepositoryException {
        return getValue().getStream();
    }

    /**
     * @see Property#getBinary()
     */
    public Binary getBinary() throws RepositoryException {
        return getValue().getBinary();
    }

    /**
     * @see Property#getLong()
     */
    public long getLong() throws ValueFormatException, RepositoryException {
        return getValue().getLong();
    }

    /**
     * @see Property#getDouble()
     */
    public double getDouble() throws ValueFormatException, RepositoryException {
        return getValue().getDouble();
    }

    /**
     * @see Property#getDecimal()
     */
    public BigDecimal getDecimal() throws RepositoryException {
        return getValue().getDecimal();
    }

    /**
     * @see Property#getDate()
     */
    public Calendar getDate() throws ValueFormatException, RepositoryException {
        return getValue().getDate();
    }

    /**
     * @see Property#getBoolean()
     */
    public boolean getBoolean() throws ValueFormatException, RepositoryException {
        return getValue().getBoolean();
    }

    /**
     * @see Property#getNode()
     */
    public Node getNode() throws ValueFormatException, RepositoryException {
        Value value = getValue();
        switch (value.getType()) {
            case PropertyType.REFERENCE:
            case PropertyType.WEAKREFERENCE:
                return session.getNodeByIdentifier(value.getString());

            case PropertyType.PATH:
            case PropertyType.NAME:
                String path = value.getString();
                Path p = session.getPathResolver().getQPath(path);
                try {
                    return (p.isAbsolute()) ? session.getNode(path) : getParent().getNode(path);
                } catch (PathNotFoundException e) {
                    throw new ItemNotFoundException(path);
                }

            case PropertyType.STRING:
                try {
                    Value refValue = ValueHelper.convert(value, PropertyType.REFERENCE, session.getValueFactory());
                    return session.getNodeByIdentifier(refValue.getString());
                } catch (ItemNotFoundException e) {
                    throw e;
                } catch (RepositoryException e) {
                    // try if STRING value can be interpreted as PATH value
                    Value pathValue = ValueHelper.convert(value, PropertyType.PATH, session.getValueFactory());
                    p = session.getPathResolver().getQPath(pathValue.getString());
                    try {
                        return (p.isAbsolute()) ? session.getNode(pathValue.getString()) : getParent().getNode(pathValue.getString());
                    } catch (PathNotFoundException e1) {
                        throw new ItemNotFoundException(pathValue.getString());
                    }
                }

            default:
                throw new ValueFormatException("Property value cannot be converted to a PATH, REFERENCE or WEAKREFERENCE");
        }
    }

    /**
     * @see Property#getProperty()
     */
    public Property getProperty() throws RepositoryException {
        Value value = getValue();
        Value pathValue = ValueHelper.convert(value, PropertyType.PATH, session.getValueFactory());
        String path = pathValue.getString();
        boolean absolute;
        try {
            Path p = session.getPathResolver().getQPath(path);
            absolute = p.isAbsolute();
        } catch (RepositoryException e) {
            throw new ValueFormatException("Property value cannot be converted to a PATH");
        }
        try {
            return (absolute) ? session.getProperty(path) : getParent().getProperty(path);
        } catch (PathNotFoundException e) {
            throw new ItemNotFoundException(path);
        }
    }

    /**
     * @see Property#getLength
     */
    public long getLength() throws ValueFormatException, RepositoryException {
        return getLength(getQValue());
    }

    /**
     * @see Property#getLengths
     */
    public long[] getLengths() throws ValueFormatException, RepositoryException {
        QValue[] values = getQValues();
        long[] lengths = new long[values.length];
        for (int i = 0; i < values.length; i++) {
            lengths[i] = getLength(values[i]);
        }
        return lengths;
    }

    /**
     *
     * @param value
     * @return
     * @throws RepositoryException
     */
    private long getLength(QValue value) throws RepositoryException {
        long length;
        switch (value.getType()) {
            case PropertyType.NAME:
            case PropertyType.PATH:
                String jcrString = ValueFormat.getJCRString(value, session.getNamePathResolver());
                length = jcrString.length();
                break;
            default:
                length = value.getLength();
                break;
        }
        return length;
    }

    /**
     * @see javax.jcr.Property#getDefinition()
     */
    public PropertyDefinition getDefinition() throws RepositoryException {
        checkStatus();
        QPropertyDefinition qpd = getPropertyState().getDefinition();
        return session.getNodeTypeManager().getPropertyDefinition(qpd);
    }

    /**
     * @see javax.jcr.Property#getType()
     */
    public int getType() throws RepositoryException {
        checkStatus();
        return getPropertyState().getType();
    }

    /**
     *
     * @return true if the definition indicates that this Property is multivalued.
     */
    public boolean isMultiple() {
        return getPropertyState().isMultiValued();
    }

   //-----------------------------------------------------------< ItemImpl >---
    /**
     * Returns the Name defined with this <code>PropertyState</code>
     *
     * @return
     * @see PropertyState#getName()
     * @see ItemImpl#getName()
     */
    @Override
    Name getQName() {
        return getPropertyState().getName();
    }

    //------------------------------------------------------< check methods >---
    /**
     *
     * @param multiValues
     * @throws RepositoryException
     */
    private void checkIsWritable(boolean multiValues) throws RepositoryException {
        // check common to properties and nodes
        checkIsWritable();

        // property specific check
        if (isMultiple() != multiValues) {
            throw new ValueFormatException(getPath() + "Multivalue definition of " + safeGetJCRPath() + " does not match to given value(s).");
        }
    }

    //---------------------------------------------< private implementation >---

    /**
     *
     * @param defaultType
     * @return the required type for this property.
     */
    private int getRequiredType(int defaultType) throws RepositoryException {
        // check type according to definition of this property
        int reqType = getDefinition().getRequiredType();
        if (reqType == PropertyType.UNDEFINED) {
            if (defaultType == PropertyType.UNDEFINED) {
                reqType = PropertyType.STRING;
            } else {
                reqType = defaultType;
            }
        }
        return reqType;
    }

    /**
     *
     * @return
     * @throws ValueFormatException
     * @throws RepositoryException
     */
    private QValue getQValue() throws ValueFormatException, RepositoryException {
        checkStatus();
        if (isMultiple()) {
            throw new ValueFormatException(safeGetJCRPath() + " is multi-valued and can therefore only be retrieved as an array of values");
        }
        // avoid unnecessary object creation if possible
        return getPropertyState().getValue();
    }

    /**
     *
     * @return
     * @throws ValueFormatException
     * @throws RepositoryException
     */
    private QValue[] getQValues() throws ValueFormatException, RepositoryException {
        checkStatus();
        if (!isMultiple()) {
            throw new ValueFormatException(safeGetJCRPath() + " is not multi-valued and can therefore only be retrieved as single value");
        }
        // avoid unnecessary object creation if possible
        return getPropertyState().getValues();
    }

    /**
     *
     * @param value
     * @param requiredType
     * @throws RepositoryException
     */
    private void setValue(Value value, int requiredType) throws RepositoryException {
        if (requiredType == PropertyType.UNDEFINED) {
            // should never get here since calling methods assert valid type
            throw new IllegalArgumentException("Property type of a value cannot be undefined (" + safeGetJCRPath() + ").");
        }
        if (value == null) {
            setInternalValues(null, requiredType);
            return;
        }

        QValue qValue;
        if (requiredType != value.getType()) {
            // type conversion required
            Value v = ValueHelper.convert(value, requiredType, session.getValueFactory());
            qValue = ValueFormat.getQValue(v, session.getNamePathResolver(), session.getQValueFactory());
        } else {
            // no type conversion required
            qValue = ValueFormat.getQValue(value, session.getNamePathResolver(), session.getQValueFactory());
        }
        setInternalValues(new QValue[]{qValue}, requiredType);
    }

    /**
     *
     * @param qValues
     * @param valueType
     * @throws ConstraintViolationException
     * @throws RepositoryException
     */
    private void setInternalValues(QValue[] qValues, int valueType) throws ConstraintViolationException, RepositoryException {
        // check for null value
        if (qValues == null) {
            // setting a property to null removes it automatically
            remove();
            return;
        }
        // modify the state of this property
        Operation op = SetPropertyValue.create(getPropertyState(), qValues, valueType);
        session.getSessionItemStateManager().execute(op);
    }

    /**
     * Private helper to access the <code>PropertyState</code> directly
     *
     * @return state for this Property
     */
    private PropertyState getPropertyState() {
        return (PropertyState) getItemState();
    }

    /**
     *
     * @param value
     * @param propertyType
     * @throws ValueFormatException
     * @throws RepositoryException
     */
    static void checkValidReference(Node value, int propertyType, NameResolver resolver) throws ValueFormatException, RepositoryException {
        if (propertyType == PropertyType.REFERENCE) {
            String jcrName = resolver.getJCRName(NameConstants.MIX_REFERENCEABLE);
            if (!value.isNodeType(jcrName)) {
                throw new ValueFormatException("Target node must be of node type mix:referenceable");
            }
        } else {
            throw new ValueFormatException("Property must be of type REFERENCE.");
        }
    }
}
