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
package org.apache.jackrabbit.core.cluster;

import org.apache.jackrabbit.core.nodetype.PropDefId;
import org.apache.jackrabbit.core.PropertyId;
import org.apache.jackrabbit.core.value.InternalValue;

/**
 * Describes a journal operation for a property change.
 */
abstract class PropertyOperation extends ItemOperation {

    /**
     * Definition id.
     */
    private PropDefId definitionId;

    /**
     * Property id.
     */
    private PropertyId id;

    /**
     * Multivalued flag.
     */
    private Boolean multiValued;

    /**
     * Type.
     */
    private Integer type;

    /**
     * Values.
     */
    private InternalValue[] values;

    /**
     * Creates a new instance of this class. Takes an operation type as paramter.
     * @param operationType operation type
     */
    protected PropertyOperation(int operationType) {
        super(operationType);
    }

    /**
     * Creates a new instance of a known subclass.
     *
     * @param operationType operation type
     * @return instance of this class
     */
    public static PropertyOperation create(int operationType) {
        switch (operationType) {
            case ADDED:
                return new PropertyAddedOperation();
            case MODIFIED:
                return new PropertyModifiedOperation();
            case DELETED:
                return new PropertyDeletedOperation();
            default:
                throw new IllegalArgumentException("Unknown operation type: " + operationType);
        }
    }

    /**
     * Return a flag indicating whether the definiton id is contained in this record.
     * @return <code>true</code> if the definition id is contained;
     *         <code>false</code> otherwise.
     */
    public boolean hasDefinitionId() {
        return definitionId != null;
    }

    /**
     * Return the definition id.
     * @return definition id
     */
    public PropDefId getDefinitionId() {
        return definitionId;
    }

    /**
     * Set the definition id.
     * @param definitionId definition id
     */
    public void setDefinitionId(PropDefId definitionId) {
        this.definitionId = definitionId;
    }

    /**
     * Return a flag indicating whether the property id is contained in this record.
     * @return <code>true</code> if the property id is contained;
     *         <code>false</code> otherwise.
     */
    public boolean hasId() {
        return id != null;
    }

    /**
     * Return the property id.
     * @return property id
     */
    public PropertyId getId() {
        return id;
    }

    /**
     * Set the property id.
     * @param id property id
     */
    public void setId(PropertyId id) {
        this.id = id;
    }

    /**
     * Return a flag indicating whether the multivalued flag is contained in this record.
     * @return <code>true</code> if the multivalued flag is contained;
     *         <code>false</code> otherwise.
     */
    public boolean hasMultiValued() {
        return multiValued != null;
    }

    /**
     * Return the multivalued flag.
     * @return multivalued flag
     */
    public boolean isMultiValued() {
        return multiValued.booleanValue();
    }

    /**
     * Set the multivalued flag.
     * @param multiValued multivalued flag
     */
    public void setMultiValued(boolean multiValued) {
        this.multiValued = new Boolean(multiValued);
    }

    /**
     * Return a flag indicating whether the type is contained.
     * @return <code>true</code> if the type is contained;
     *         <code>false</code> otherwise.
     */
    public boolean hasType() {
        return type != null;
    }

    /**
     * Return the type.
     * @return type
     */
    public int getType() {
        return type.intValue();
    }

    /**
     * Set the type.
     * @param type type
     */
    public void setType(int type) {
        this.type = new Integer(type);
    }

    /**
     * Return a flag indicating whether the values contained in this record.
     * @return <code>true</code> if the values contained contained;
     *         <code>false</code> otherwise.
     */
    public boolean hasValues() {
        return values != null;
    }

    /**
     * Return the values.
     * @return value
     */
    public InternalValue[] getValues() {
        return values;
    }

    /**
     * Set the values.
     * @param values values
     */
    public void setValues(InternalValue[] values) {
        this.values = values;
    }
}