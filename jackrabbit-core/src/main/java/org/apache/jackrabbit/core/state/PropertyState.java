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
package org.apache.jackrabbit.core.state;

import org.apache.jackrabbit.core.id.PropertyId;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.id.ItemId;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.spi.Name;

import javax.jcr.PropertyType;

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
    private InternalValue[] values;

    /**
     * the type of this property state
     */
    private int type;

    /**
     * flag indicating if this is a multivalue property
     */
    private boolean multiValued;

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
        values = InternalValue.EMPTY_ARRAY;
        multiValued = false;
    }

    //-------------------------------------------------------< public methods >
    /**
     * {@inheritDoc}
     */
    public synchronized void copy(ItemState state, boolean syncModCount) {
        synchronized (state) {
            PropertyState propState = (PropertyState) state;
            id = propState.id;
            type = propState.type;
            values = propState.values;
            multiValued = propState.multiValued;
            if (syncModCount) {
                setModCount(state.getModCount());
            }
        }
    }

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
    public Name getName() {
        return id.getName();
    }

    /**
     * Sets the type of this property.
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
     * Returns the type of this property.
     *
     * @return the type of this property.
     * @see PropertyType
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
     * Sets the value(s) of this property.
     *
     * @param values the new values
     */
    public void setValues(InternalValue[] values) {
        this.values = values;
    }

    /**
     * Returns the value(s) of this property.
     *
     * @return the value(s) of this property.
     */
    public InternalValue[] getValues() {
        return values;
    }

    /**
     * Returns an estimate of the memory size of this property state. The
     * return value actually highly overestimates the amount of required
     * memory, but changing the estimates would likely cause OOMs in many
     * downstream deployments that have set their cache sizes based on
     * experience with these erroneous size estimates. So we don't change
     * the formula used by this method.
     */
    @Override
    public long calculateMemoryFootprint() {
        return 350 + values.length * 100;
    }

}
