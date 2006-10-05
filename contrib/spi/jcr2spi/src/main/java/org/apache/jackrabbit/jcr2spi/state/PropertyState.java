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
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.spi.PropertyId;
import org.apache.jackrabbit.spi.ItemId;
import org.apache.jackrabbit.spi.IdFactory;
import org.apache.jackrabbit.value.QValue;
import org.apache.jackrabbit.jcr2spi.nodetype.ValueConstraint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * <code>PropertyState</code> represents the state of a <code>Property</code>.
 */
public class PropertyState extends ItemState {

    private static Logger log = LoggerFactory.getLogger(PropertyState.class);

    /**
     * The name of this property state.
     */
    private QName name;

    /**
     * The internal value(s)
     */
    private QValue[] values;

    /**
     * The type of this property state
     */
    private int type;

    /**
     * Property definition
     */
    private QPropertyDefinition def;

    /**
     * Constructs a new property state that is initially connected to an
     * overlayed state.
     *
     * @param overlayedState
     * @param parent
     * @param initialStatus
     * @param idFactory
     */
    protected PropertyState(PropertyState overlayedState, NodeState parent,
                            int initialStatus, IdFactory idFactory) {
        super(overlayedState, parent, initialStatus, idFactory);
        pull();
    }

    /**
     * Create a new <code>PropertyState</code>
     *
     * @param name
     * @param parent
     * @param definition
     * @param initialStatus
     * @param idFactory
     */
    protected PropertyState(QName name, NodeState parent, QPropertyDefinition definition,
                            int initialStatus, IdFactory idFactory) {
        super(parent, initialStatus, idFactory);
        this.name = name;
        this.def = definition;

        type = PropertyType.UNDEFINED;
        values = QValue.EMPTY_ARRAY;
    }

    /**
     *
     * @param type
     * @param values
     */
    void init(int type, QValue[] values) {
        this.type = type;
        this.values = (values == null) ? QValue.EMPTY_ARRAY : values;
    }

    /**
     * @inheritDoc
     */
    public void remove() {
        if (getStatus() == STATUS_NEW) {
            setStatus(STATUS_REMOVED);
        } else {
            setStatus(STATUS_EXISTING_REMOVED);
        }
        parent.propertyStateRemoved(this);
    }

    /**
     * @inheritDoc
     * @see ItemState#revert(Set)
     */
    public void revert(Set affectedItemStates) {
        // all states except for 'new' ones must have an overlayed state in order
        // to be 'reverted'.
        if (getStatus() != STATUS_NEW && overlayedState == null) {
            throw new IllegalStateException("revert cannot be called on workspace state");
        }
        switch (getStatus()) {
            case STATUS_EXISTING:
                // nothing to do
                break;
            case STATUS_EXISTING_MODIFIED:
            case STATUS_EXISTING_REMOVED:
            case STATUS_STALE_MODIFIED:
                // revert state from overlayed
                pull();
                setStatus(STATUS_EXISTING);
                affectedItemStates.add(this);
                break;
            case STATUS_NEW:
                // set removed
                setStatus(STATUS_REMOVED);
                // and remove from parent
                parent.propertyStateRemoved(this);
                affectedItemStates.add(this);
                break;
            case STATUS_REMOVED:
                // shouldn't happen actually, because a 'removed' state is not
                // accessible anymore
                log.warn("trying to revert an already removed property state");
                parent.propertyStateRemoved(this);
                break;
            case STATUS_STALE_DESTROYED:
                // overlayed does not exist anymore
                parent.propertyStateRemoved(this);
                affectedItemStates.add(this);
                break;
        }
    }

    /**
     * @inheritDoc
     * @see ItemState#collectTransientStates(Set)
     */
    public void collectTransientStates(Set transientStates) {
        switch (getStatus()) {
            case STATUS_EXISTING_MODIFIED:
            case STATUS_EXISTING_REMOVED:
            case STATUS_NEW:
            case STATUS_STALE_DESTROYED:
            case STATUS_STALE_MODIFIED:
                transientStates.add(this);
                break;
            case STATUS_EXISTING:
            case STATUS_REMOVED:
                log.debug("Collecting transient states: Ignored PropertyState with status " + getStatus());
                break;
            default:
                // should never occur. status is validated upon setStatus(int)
        }
    }

    /**
     * {@inheritDoc}
     */
    protected synchronized void copyFrom(ItemState state) {
        synchronized (state) {
            PropertyState propState = (PropertyState) state;
            name = propState.name;
            //parent = propState.parent; // TODO: parent from wrong layer
            type = propState.type;
            def = propState.def;
            values = propState.values;
        }
    }

    //--------------------< public READ methods and package private Setters >---
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
     */
    public ItemId getId() {
        return getPropertyId();
    }

    /**
     * Returns the identifier of this property.
     *
     * @return the id of this property.
     */
    public PropertyId getPropertyId() {
        return idFactory.createPropertyId(parent.getNodeId(), getQName());
    }

    /**
     * Returns the name of this property.
     *
     * @return the name of this property.
     */
    public QName getQName() {
        return name;
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
        return def.isMultiple();
    }

    /**
     * Returns the {@link QPropertyDefinition definition} defined for this
     * property state. Note that the definition has been set upon creation of
     * this <code>PropertyState</code>.
     *
     * @return definition of this state
     */
    public QPropertyDefinition getDefinition() {
        return def;
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
     * Sets the value(s) of this property.
     *
     * @param values the new values
     */
    void setValues(QValue[] values, int type) throws RepositoryException {
        // make sure the arguements are consistent and do not violate the
        // given property definition.
        validate(values, type, this.def);
        this.values = values;
        this.type = type;
        markModified();
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
        if (definition.getRequiredType() != PropertyType.UNDEFINED && definition.getRequiredType() != propertyType) {
            throw new ConstraintViolationException("RequiredType constraint is not satisfied");
        }
        for (int i = 0; i < values.length; i++) {
            if (propertyType != values[i].getType()) {
                throw new ConstraintViolationException("Inconsistent value types: Required type = " + PropertyType.nameFromValue(propertyType) + "; Found value with type = " + PropertyType.nameFromValue(values[i].getType()));
            }
        }
        ValueConstraint.checkValueConstraints(definition, values);
    }
}
