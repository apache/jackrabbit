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
import javax.jcr.ValueFormatException;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.ConstraintViolationException;

import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.PropertyId;
import org.apache.jackrabbit.spi.ItemId;
import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.jcr2spi.nodetype.ValueConstraint;
import org.apache.jackrabbit.jcr2spi.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.jcr2spi.config.CacheBehaviour;
import org.apache.jackrabbit.jcr2spi.hierarchy.PropertyEntry;
import org.apache.jackrabbit.jcr2spi.hierarchy.HierarchyEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;

/**
 * <code>PropertyState</code> represents the state of a <code>Property</code>.
 */
public class PropertyState extends ItemState {

    private static Logger log = LoggerFactory.getLogger(PropertyState.class);

    /**
     * The PropertyEntry associated with the state
     */
    private final PropertyEntry hierarchyEntry;

    /**
     * Property definition
     */
    private QPropertyDefinition definition;

    /**
     * The internal value(s)
     */
    private QValue[] values;

    /**
     * The type of this property state
     */
    private int type;

    /**
     * True if this Property is multiValued
     */
    private final boolean multiValued;

    /**
     * Constructs a new property state that is initially connected to an
     * overlayed state.
     *
     * @param overlayedState
     * @param initialStatus
     */
    protected PropertyState(PropertyState overlayedState, int initialStatus,
                            ItemStateFactory isf) {
        super(overlayedState, initialStatus, isf);

        this.hierarchyEntry = overlayedState.hierarchyEntry;
        this.definition = overlayedState.definition;
        this.multiValued = overlayedState.multiValued;

        init(overlayedState.getType(), overlayedState.getValues());
    }

    /**
     * Create a new <code>PropertyState</code>
     *
     * @param entry
     * @param initialStatus
     * @param isWorkspaceState
     */
    protected PropertyState(PropertyEntry entry, boolean multiValued, QPropertyDefinition definition,
                            int initialStatus, boolean isWorkspaceState,
                            ItemStateFactory isf, NodeTypeRegistry ntReg) {
        super(initialStatus, isWorkspaceState, isf, ntReg);

        this.hierarchyEntry = entry;
        this.definition = definition;
        this.multiValued = multiValued;
        init(PropertyType.UNDEFINED, QValue.EMPTY_ARRAY);
    }

    /**
     *
     * @param type
     * @param values
     */
    void init(int type, QValue[] values) {
        // free old values as necessary
        QValue[] oldValues = this.values;
        if (oldValues != null) {
            for (int i = 0; i < oldValues.length; i++) {
                QValue old = oldValues[i];
                if (old != null) {
                    // make sure temporarily allocated data is discarded
                    // before overwriting it (see QValue#discard())
                    old.discard();
                }
            }
        }
        this.type = type;
        this.values = (values == null) ? QValue.EMPTY_ARRAY : values;
    }


    //----------------------------------------------------------< ItemState >---
    /**
     * @see ItemState#getHierarchyEntry()
     */
    public HierarchyEntry getHierarchyEntry() {
        return hierarchyEntry;
    }

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
        return getPropertyId();
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
        if (keepChanges || !diff(this, (PropertyState) another)) {
            // nothing to do.
            return false;
        }

        synchronized (another) {
            PropertyState pState = (PropertyState) another;
            init(pState.type, pState.values);
        }
        return true;
    }

    //------------------------------------------------------< PropertyState >---
    /**
     * Returns the identifier of this property.
     *
     * @return the id of this property.
     */
    public PropertyId getPropertyId() {
        return getPropertyEntry().getId();
    }

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
        return type;
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
            definition = getEffectiveNodeType().getApplicablePropertyDefinition(getQName(), getType(), isMultiValued());
        }
        return definition;
    }

    /**
     * Returns the value(s) of this property.
     *
     * @return the value(s) of this property.
     */
    public QValue[] getValues() {
        return values;
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
        if (values == null || values.length == 0) {
            return null;
        } else {
            return values[0];
        }
    }

    /**
     * {@inheritDoc}
     * @see ItemState#persisted(ChangeLog, CacheBehaviour)
     */
    void persisted(ChangeLog changeLog, CacheBehaviour cacheBehaviour)
        throws IllegalStateException {
        checkIsSessionState();
        for (Iterator it = changeLog.modifiedStates(); it.hasNext();) {
            ItemState modState = (ItemState) it.next();
            if (modState == this) {
                /*
                NOTE: overlayedState must be existing, otherwise save was not
                possible on prop. Similarly a property can only be the changelog
                target, if it was modified. removal, add and implicit modification
                of protected properties must be persisted by save on parent.
                */
                // push changes to overlayed state and reset status
                ((PropertyState) overlayedState).init(getType(), getValues());
                setStatus(Status.EXISTING);
            }
        }
    }
    
    /**
     * Sets the value(s) of this property.
     *
     * @param values the new values
     */
    void setValues(QValue[] values, int type) throws RepositoryException {
        checkIsSessionState();
        // make sure the arguements are consistent and do not violate the
        // given property definition.
        validate(values, type, getDefinition());
        init(type, values);

        markModified();
    }

    //------------------------------------------------------------< private >---
    /**
     *
     * @return
     */
    private PropertyEntry getPropertyEntry() {
        return (PropertyEntry) getHierarchyEntry();
    }

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
}
