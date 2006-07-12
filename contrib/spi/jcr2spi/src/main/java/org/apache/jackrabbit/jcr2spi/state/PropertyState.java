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
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.value.QValue;

/**
 * <code>PropertyState</code> represents the state of a <code>Property</code>.
 */
public class PropertyState extends ItemState {

    /**
     * the id of this property state
     */
    private PropertyId id;

    /**
     * the internal values
     */
    private QValue[] values;

    /**
     * the type of this property state
     */
    private int type;

    /**
     * flag indicating if this is a multivalue property
     */
    private boolean multiValued;

    private QPropertyDefinition def;

    /**
     * Constructs a new property state that is initially connected to an
     * overlayed state.
     *
     * @param overlayedState the backing property state being overlayed
     * @param initialStatus  the initial status of the property state object
     * @param isTransient    flag indicating whether this state is transient or not
     */
    public PropertyState(PropertyState overlayedState, int initialStatus,
                         boolean isTransient) {
        super(overlayedState, initialStatus, isTransient);
        pull();
    }

    /**
     * Create a new <code>PropertyState</code>
     *
     * @param id            id of the property
     * @param initialStatus the initial status of the property state object
     * @param isTransient   flag indicating whether this state is transient or not
     */
    public PropertyState(PropertyId id, int initialStatus, boolean isTransient) {
        super(initialStatus, isTransient);
        this.id = id;
        type = PropertyType.UNDEFINED;
        values = QValue.EMPTY_ARRAY;
        multiValued = false;
    }

    /**
     * {@inheritDoc}
     */
    protected synchronized void copy(ItemState state) {
        synchronized (state) {
            PropertyState propState = (PropertyState) state;
            id = propState.id;
            type = propState.type;
            def = propState.getDefinition();
            values = propState.values;
            multiValued = propState.multiValued;
        }
    }

    //-------------------------------------------------------< public methods >
    /**
     * Determines if this item state represents a node.
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
        return id;
    }

    /**
     * Returns the identifier of this property.
     * 
     * @return the id of this property.
     */
    public PropertyId getPropertyId() {
        return id;
    }

    /**
     * {@inheritDoc}
     */
    public NodeId getParentId() {
        return id.getParentId();
    }

    /**
     * Returns the name of this property.
     *
     * @return the name of this property.
     */
    public QName getName() {
        return id.getQName();
    }

    /**
     * Sets the type of the property value(s)
     *
     * @param type the type to be set
     * @see PropertyType
     */
    public void setType(int type) {
        this.type = type;
    }

    /**
     * Sets the flag indicating whether this property is multi-valued.
     *
     * @param multiValued flag indicating whether this property is multi-valued
     */
    public void setMultiValued(boolean multiValued) {
        this.multiValued = multiValued;
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
     * Returns the id of the definition applicable to this property state.
     *
     * @return the id of the definition
     */
    public QPropertyDefinition getDefinition() {
        return def;
    }

    /**
     * Sets the id of the definition applicable to this property state.
     *
     * @param def the id of the definition
     */
    public void setDefinition(QPropertyDefinition def) {
        this.def = def;
    }

    /**
     * Sets the value(s) of this property.
     *
     * @param values the new values
     */
    public void setValues(QValue[] values) {
        this.values = values;
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
}
