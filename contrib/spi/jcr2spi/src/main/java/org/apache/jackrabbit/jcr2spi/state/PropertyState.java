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

import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.spi.PropertyId;
import org.apache.jackrabbit.spi.ItemId;
import org.apache.jackrabbit.spi.IdFactory;
import org.apache.jackrabbit.value.QValue;
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
     * @param isTransient
     * @param idFactory
     */
    protected PropertyState(PropertyState overlayedState, NodeState parent,
                            int initialStatus, boolean isTransient, IdFactory idFactory) {
        super(overlayedState, parent, initialStatus, isTransient, idFactory);
        pull();
    }

    /**
     * Create a new <code>PropertyState</code>
     *
     * @param name
     * @param parent
     * @param definition
     * @param initialStatus
     * @param isTransient
     * @param idFactory
     */
    protected PropertyState(QName name, NodeState parent, QPropertyDefinition definition, int initialStatus,
                            boolean isTransient, IdFactory idFactory) {
        super(parent, initialStatus, isTransient, idFactory);
        this.name = name;
        this.def = definition;

        type = PropertyType.UNDEFINED;
        values = QValue.EMPTY_ARRAY;
    }

    /**
     * @inheritDoc
     */
    public void remove() {
        if (status == STATUS_NEW) {
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
        if (overlayedState == null) {
            throw new IllegalStateException("revert cannot be called on workspace state");
        }
        switch (status) {
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
     * {@inheritDoc}
     */
    protected synchronized void copy(ItemState state) {
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
     * Sets the type of the property value(s)
     *
     * @param type the type to be set
     * @see PropertyType
     */
    void setType(int type) {
        this.type = type;
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
    void setValues(QValue[] values) {
        internalSetValues(values);
        markModified();
    }

    /**
     * TODO: rather separate PropertyState into interface and implementation
     * TODO: and move internalSetValues to implementation only.
     * Sets the value(s) of this property without marking this property state
     * as modified.
     *
     * @param values the new values
     */
    void internalSetValues(QValue[] values) {
        this.values = values;
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
}
