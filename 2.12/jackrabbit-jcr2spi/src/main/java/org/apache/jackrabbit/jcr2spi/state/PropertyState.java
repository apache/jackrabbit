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

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;
import javax.jcr.nodetype.ConstraintViolationException;

import org.apache.jackrabbit.jcr2spi.hierarchy.PropertyEntry;
import org.apache.jackrabbit.jcr2spi.nodetype.ItemDefinitionProvider;
import org.apache.jackrabbit.spi.ItemId;
import org.apache.jackrabbit.spi.PropertyInfo;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.spi.commons.nodetype.constraint.ValueConstraint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
     * Value(s) and type of an existing property that has been transiently
     * modified.
     */
    private PropertyData transientData;

    /**
     * Original value(s) and type of an existing or a new property.
     */
    private PropertyData data;

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
                            ItemDefinitionProvider definitionProvider,
                            QValue[] values, int propertyType)
            throws ConstraintViolationException, RepositoryException {
        super(Status.NEW, entry, isf, definitionProvider);
        this.multiValued = definition.isMultiple();
        this.definition = definition;
        setValues(values, propertyType);
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
        this.data = new PropertyData(pInfo);
        this.transientData = null;
    }

    //----------------------------------------------------------< ItemState >---
    /**
     * Always returns false.
     *
     * @return always false
     * @see ItemState#isNode
     */
    @Override
    public boolean isNode() {
        return false;
    }

    /**
     * {@inheritDoc}
     * @see ItemState#getId()
     */
    @Override
    public ItemId getId() throws RepositoryException {
        return ((PropertyEntry) getHierarchyEntry()).getId();
    }

    /**
     * {@inheritDoc}
     * @see ItemState#getWorkspaceId()
     */
    @Override
    public ItemId getWorkspaceId() throws RepositoryException {
        return ((PropertyEntry) getHierarchyEntry()).getWorkspaceId();
    }

    /**
     * If <code>keepChanges</code> is true, this method only compares the existing
     * values with the values from 'another' and returns true, if the underlying
     * persistent state is different to the stored persistent values. Otherwise
     * the transient changes will be discarded.
     *
     * @see ItemState#merge(ItemState, boolean)
     */
    @Override
    public MergeResult merge(ItemState another, boolean keepChanges) {
        boolean modified = false;
        if (another != null && another != this) {
            if (another.isNode()) {
                throw new IllegalArgumentException("Attempt to merge property state with node state.");
            }
            PropertyDiffer result = new PropertyDiffer(data, ((PropertyState) another).data);

            // reset the pInfo to point to the pInfo of another state.
            this.data = ((PropertyState) another).data;
            // if transient changes should be preserved OR if there are not
            // transient changes, return the differ and postpone the effort of
            // calculating the diff (the test if this state got internally changed)).
            if (keepChanges || transientData == null) {
                return result;
            } else {
                result.dispose();
                transientData.discardValues();
                transientData = null;
                modified = true;
            }
        }
        return new SimpleMergeResult(modified);
    }

    /**
     * @see ItemState#revert()
     * @return true if
     */
    @Override
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
        return (transientData == null) ? data.type : transientData.type;
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
     * @throws RepositoryException If an error occurs.
     */
    public QPropertyDefinition getDefinition() throws RepositoryException {
        if (definition == null) {
            /*
            Don't pass 'all-node types from parent':
            for NEW-states the definition is always set upon creation.
            for all other states the definition must be retrieved only taking
            the effective node types present on the parent into account
            any kind of transiently added mixins must not have an effect
            on the definition retrieved for an state that has been persisted
            before. The effective NT must be evaluated as if it had been
            evaluated upon creating the workspace state.
            */
            definition = definitionProvider.getQPropertyDefinition(getParent().getNodeTypeNames(), getName(), getType(), multiValued, ((PropertyEntry) getHierarchyEntry()).getWorkspaceId());
        }
        return definition;
    }

    /**
     * Returns the value(s) of this property.
     *
     * @return the value(s) of this property.
     */
    public QValue[] getValues() {
        // if transientData are null the data MUST be present (ev. add check)
        return (transientData == null) ? data.values : transientData.values;
    }

    /**
     * Convenience method for single valued property states.
     *
     * @return the value of a single valued property.
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
     * @param type the value type
     * @throws RepositoryException If an error occurs.
     */
    void setValues(QValue[] values, int type) throws RepositoryException {
        if (getStatus() == Status.NEW) {
            if (data == null) {
                data = new PropertyData(type, values, getDefinition());
            } else {
                data.setValues(type, values, getDefinition());
            }
        } else {
            if (transientData == null) {
                transientData = new PropertyData(type, values, getDefinition());
            } else {
                transientData.setValues(type, values, getDefinition());
            }
            markModified();
        }
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
    private static boolean diff(PropertyData p1, PropertyData p2) {
        // compare type
        if (p1.type != p2.type) {
            return true;
        }

        QValue[] vs1 = p1.values;
        QValue[] vs2 = p2.values;
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
     * Inner class storing property values and their type.
     */
    private static class PropertyData {
        private int type;
        private QValue[] values;
        private boolean discarded;

        private PropertyData(PropertyInfo pInfo) {
            this.type = pInfo.getType();
            this.values = pInfo.getValues();
        }

        private PropertyData(int type, QValue[] values, QPropertyDefinition definition) throws ConstraintViolationException, RepositoryException {
            setValues(type, values, definition);
        }

        private void setValues(int type, QValue[] values, QPropertyDefinition definition) throws ConstraintViolationException, RepositoryException {
            // make sure the arguments are consistent and do not violate the
            // given property definition.
            validate(values, type, definition);
            // note: discarding original values is deferred to operation completion
            // -> see JCR-2880

            this.type = type;
            this.values = (values == null) ? QValue.EMPTY_ARRAY : values;
        }

        private void discardValues() {
            if (!discarded && values != null) {
                for (int i = 0; i < values.length; i++) {
                    if (values[i] != null) {
                        // make sure temporarily allocated data is discarded
                        // before overwriting it (see QValue#discard())
                        values[i].discard();
                    }
                }
                discarded = true;
            }
        }
    }

    /**
     * Helper class for delayed determination of property differences.
     */
    private static class PropertyDiffer implements MergeResult {

        private final PropertyData oldData;
        private final PropertyData newData;

        PropertyDiffer(PropertyData oldData, PropertyData newData) {
            super();
            this.oldData = oldData;
            this.newData = newData;
        }

        public boolean modified() {
            if (oldData.discarded || newData.discarded) {
                // cannot calculate the diff any more -> return true.
                String msg = " Diff cannot be calculated: " + ((oldData.discarded) ? "Old property data" : "New property data") + " have already been discarded.";
                log.debug(msg);
                return true;
            }
            return diff(oldData, newData);
        }

        public void dispose() {
            oldData.discardValues();
        }
    }

}
