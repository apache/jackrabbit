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
package org.apache.jackrabbit.jcr2spi.state;

import org.apache.jackrabbit.jcr2spi.hierarchy.PropertyEntry;
import org.apache.jackrabbit.jcr2spi.nodetype.ItemDefinitionProvider;
import org.apache.jackrabbit.jcr2spi.nodetype.ValueConstraint;
import org.apache.jackrabbit.spi.ItemId;
import org.apache.jackrabbit.spi.PropertyInfo;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.QValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;
import javax.jcr.nodetype.ConstraintViolationException;
import java.util.Iterator;

/**
 * <code>PropertyState</code> represents the state of a <code>Property</code>.
 */
public class PropertyState extends ItemState {

    private static Logger log = LoggerFactory.getLogger(PropertyState.class);

    /**
     * Property definition
     */
    private QPropertyDefinition definition;

    /**
     * True if this Property is multiValued
     */
    private final boolean multiValued;

    /**
     *
     */
    private TransientData transientData;

    /**
     *
     */
    private PropertyInfo pInfo;

    /**
     * Create a NEW PropertyState
     *
     * @param entry
     * @param isf
     * @param definition
     * @param definitionProvider
     */
    protected PropertyState(PropertyEntry entry, ItemStateFactory isf,
                            QPropertyDefinition definition,
                            ItemDefinitionProvider definitionProvider) {
        super(Status.NEW, entry, isf, definitionProvider);
        this.multiValued = definition.isMultiple();
        this.definition = definition;
        this.transientData = null; // TODO: maybe type/values should be passed to constructor
        this.pInfo = null;
    }

    /**
     * Create an EXISTING PropertyState
     *
     * @param entry
     * @param pInfo
     * @param isf
     * @param definitionProvider
     */
    protected PropertyState(PropertyEntry entry, PropertyInfo pInfo,
                            ItemStateFactory isf,
                            ItemDefinitionProvider definitionProvider) {
        super(entry, isf, definitionProvider);
        this.multiValued = pInfo.isMultiValued();
        this.transientData = null;
        this.pInfo = pInfo;
    }

    //----------------------------------------------------------< ItemState >---
    /**
     * Always returns false.
     *
     * @return always false
     * @see ItemState#isNode
     */
    public boolean isNode() {
        return false;
    }

    /**
     * {@inheritDoc}
     * @see ItemState#getId()
     */
    public ItemId getId() {
        return ((PropertyEntry) getHierarchyEntry()).getId();
    }

    /**
     * {@inheritDoc}
     * @see ItemState#getWorkspaceId()
     */
    public ItemId getWorkspaceId() {
        return ((PropertyEntry) getHierarchyEntry()).getWorkspaceId();
    }

    /**
     * If <code>keepChanges</code> is true, this method does nothing and returns
     * false. Otherwise type and values of the other property state are compared
     * to this state. If they differ, they will be copied to this state and
     * this method returns true.
     *
     * @see ItemState#merge(ItemState, boolean)
     */
    public boolean merge(ItemState another, boolean keepChanges) {
        if (another == null || another == this) {
            return false;
        }
        if (another.isNode()) {
            throw new IllegalArgumentException("Attempt to merge property state with node state.");
        }
        boolean modified = diff(this, (PropertyState) another);
        this.pInfo = ((PropertyState) another).pInfo;
        if (!keepChanges && transientData != null) {
            modified = true;
            transientData.discardValues();
            transientData = null;
        }
        return modified;
    }

    /**
     * @see ItemState#revert()
     * @return true if
     */
    public boolean revert() {
        if (getStatus() == Status.NEW) {
            throw new IllegalStateException("Cannot call revert on a NEW property state.");
        }
        if (transientData == null) {
            return false;
        } else {
            transientData.discardValues();
            transientData = null;
            return true;
        }
    }


    /**
     * {@inheritDoc}
     * @see ItemState#persisted(ChangeLog)
     */
    void persisted(ChangeLog changeLog)
        throws IllegalStateException {
        for (Iterator it = changeLog.modifiedStates(); it.hasNext();) {
            ItemState modState = (ItemState) it.next();
            if (modState == this) {
                /*
                NOTE: Property can only be the changelog target, if it was
                existing and has been modified. removal, add and implicit modification
                of protected properties must be persisted by save on parent.
                */
                setStatus(Status.EXISTING);
            }
        }
    }

    //------------------------------------------------------< PropertyState >---
    /**
     * Returns the type of the property value(s).
     *
     * @return the type of the property value(s).
     * @see PropertyType
     * @see QPropertyDefinition#getRequiredType() for the type required by the
     * property definition. The effective type may differ from the required
     * type if the latter is {@link PropertyType#UNDEFINED}.
     */
    public int getType() {
        return (transientData == null) ? pInfo.getType() : transientData.type;
    }

    /**
     * Returns true if this property is multi-valued, otherwise false.
     *
     * @return true if this property is multi-valued, otherwise false.
     */
    public boolean isMultiValued() {
        return multiValued;
    }

    /**
     * Returns the {@link QPropertyDefinition definition} defined for this
     * property state. Note that the definition has been set upon creation of
     * this <code>PropertyState</code>.
     *
     * @return definition of this state
     */
    public QPropertyDefinition getDefinition() throws RepositoryException {
        if (definition == null) {
            definition = definitionProvider.getQPropertyDefinition(this);
        }
        return definition;
    }

    /**
     * Returns the value(s) of this property.
     *
     * @return the value(s) of this property.
     */
    public QValue[] getValues() {
        // if transientData are null the pInfo MUST be present (ev. add check)
        return (transientData == null) ? pInfo.getValues() : transientData.values;
    }

    /**
     * Convenience method for single valued property states.
     *
     * @return
     * @throws ValueFormatException if {@link #isMultiValued()} returns true.
     */
    public QValue getValue() throws ValueFormatException {
        if (isMultiValued()) {
            throw new ValueFormatException("'getValue' may not be called on a multi-valued state.");
        }
        QValue[] values = getValues();
        if (values == null || values.length == 0) {
            return null;
        } else {
            return values[0];
        }
    }

    /**
     * Sets the value(s) of this property.
     *
     * @param values the new values
     */
    void setValues(QValue[] values, int type) throws RepositoryException {
        if (transientData == null) {
            transientData = new TransientData(type, values);
        } else {
            transientData.setValues(type, values);
        }
        markModified();
    }

    //------------------------------------------------------------< private >---
    /**
     * Checks whether the given property parameters are consistent and satisfy
     * the constraints specified by the given definition. The following
     * validations/checks are performed:
     * <ul>
     * <li>make sure the type is not undefined and matches the type of all
     * values given</li>
     * <li>make sure all values have the same type.</li>
     * <li>check if the type of the property values does comply with the
     * requiredType specified in the property's definition</li>
     * <li>check if the property values satisfy the value constraints
     * specified in the property's definition</li>
     * </ul>
     *
     * @param values
     * @param propertyType
     * @param definition
     * @throws ConstraintViolationException If any of the validations fails.
     * @throws RepositoryException If another error occurs.
     */
    private static void validate(QValue[] values, int propertyType, QPropertyDefinition definition)
        throws ConstraintViolationException, RepositoryException {
        if (propertyType == PropertyType.UNDEFINED) {
            throw new RepositoryException("'Undefined' is not a valid property type for existing values.");
        }
        for (int i = 0; i < values.length; i++) {
            if (values[i] != null && propertyType != values[i].getType()) {
                throw new ConstraintViolationException("Inconsistent value types: Required type = " + PropertyType.nameFromValue(propertyType) + "; Found value with type = " + PropertyType.nameFromValue(values[i].getType()));
            }
        }
        if (definition.getRequiredType() != PropertyType.UNDEFINED && definition.getRequiredType() != propertyType) {
            throw new ConstraintViolationException("RequiredType constraint is not satisfied");
        }
        ValueConstraint.checkValueConstraints(definition, values);
    }

    /**
     * Returns true, if type and/or values of the given property states differ.
     *
     * @param p1
     * @param p2
     * @return if the 2 <code>PropertyState</code>s are different in terms of
     * type and/or values.
     */
    private static boolean diff(PropertyState p1, PropertyState p2) {
        // compare type
        if (p1.getType() != p2.getType()) {
            return true;
        }

        QValue[] vs1 = p1.getValues();
        QValue[] vs2 = p2.getValues();
        if (vs1.length != vs2.length) {
            return true;
        } else {
            for (int i = 0; i < vs1.length; i++) {
                boolean eq = (vs1[i] == null) ? vs2[i] == null : vs1[i].equals(vs2[i]);
                if (!eq) {
                    return true;
                }
            }
        }
        // no difference
        return false;
    }

    //--------------------------------------------------------< inner class >---
    /**
     * Inner class storing transient property values an their type.
     */
    private class TransientData {

        private int type;
        private QValue[] values;

        private TransientData(int type, QValue[] values) throws RepositoryException {
            setValues(type, values);
        }

        private void setValues(int type, QValue[] values) throws RepositoryException {
            // make sure the arguements are consistent and do not violate the
            // given property definition.
            validate(values, type, getDefinition());
            // free old values if existing
            discardValues();

            this.type = type;
            this.values = (values == null) ? QValue.EMPTY_ARRAY : values;
        }

        private void discardValues() {
            if (values != null) {
                for (int i = 0; i < values.length; i++) {
                    if (values[i] != null) {
                        // make sure temporarily allocated data is discarded
                        // before overwriting it (see QValue#discard())
                        values[i].discard();
                    }
                }
            }
        }
    }
}
