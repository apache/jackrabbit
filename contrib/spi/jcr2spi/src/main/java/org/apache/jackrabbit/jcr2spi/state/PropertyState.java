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

import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.spi.PropertyId;
import org.apache.jackrabbit.spi.ItemId;
import org.apache.jackrabbit.spi.IdFactory;
import org.apache.jackrabbit.value.QValue;
import org.apache.jackrabbit.jcr2spi.nodetype.NodeTypeConflictException;
import org.apache.jackrabbit.jcr2spi.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.jcr2spi.nodetype.EffectiveNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    protected PropertyState(PropertyState overlayedState, NodeState parent, int initialStatus,
                         boolean isTransient, IdFactory idFactory) {
        super(overlayedState, parent, initialStatus, isTransient, idFactory);
        pull();
    }

    /**
     * Create a new <code>PropertyState</code>
     *
     * @param name          the name of the property
     * @param initialStatus the initial status of the property state object
     * @param isTransient   flag indicating whether this state is transient or
     *                      not
     */
    protected PropertyState(QName name, NodeState parent, int initialStatus,
                            boolean isTransient, IdFactory idFactory) {
        super(parent, initialStatus, isTransient, idFactory);
        this.name = name;
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
            name = propState.name;
            parent = propState.parent;
            type = propState.type;
            def = propState.def;
            values = propState.values;
            multiValued = propState.multiValued;
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
        return multiValued;
    }

    /**
     * Sets the flag indicating whether this property is multi-valued.
     *
     * @param multiValued flag indicating whether this property is multi-valued
     */
    void setMultiValued(boolean multiValued) {
        this.multiValued = multiValued;
    }


    /**
     * Returns the {@link QPropertyDefinition definition} defined for this
     * property state or <code>null</code> if the definition has not been
     * set before (i.e. the corresponding item has not been accessed before).
     *
     * @return definition of this state
     * @see #getDefinition(NodeTypeRegistry) for the corresponding method
     * that never returns <code>null</code>.
     */
    public QPropertyDefinition getDefinition() {
        return def;
    }

    /**
     * Returns the definition applicable to this property state. Since the definition
     * is not defined upon state creation this state may have to retrieve
     * the definition from the given <code>NodeTypeRegistry</code> first.
     *
     * @param ntRegistry
     * @return definition of this state
     * @see #getDefinition()
     */
    public QPropertyDefinition getDefinition(NodeTypeRegistry ntRegistry)
        throws RepositoryException {
        if (def == null) {
            try {
                NodeState parentState = getParent();
                if (parentState == null) {
                    String msg = "Internal error: cannot determine definition for orphaned state.";
                    log.debug(msg);
                    throw new RepositoryException(msg);
                }
                EffectiveNodeType ent = ntRegistry.getEffectiveNodeType(parentState.getNodeTypeNames());
                setDefinition(ent.getApplicablePropertyDefinition(getQName(), getType(), isMultiValued()));
            } catch (NodeTypeConflictException e) {
                String msg = "internal error: failed to build effective node type.";
                log.debug(msg);
                throw new RepositoryException(msg, e);
            }
        }
        return def;
    }

    /**
     * Sets the id of the definition applicable to this property state.
     *
     * @param def the id of the definition
     */
    void setDefinition(QPropertyDefinition def) {
        this.def = def;
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
