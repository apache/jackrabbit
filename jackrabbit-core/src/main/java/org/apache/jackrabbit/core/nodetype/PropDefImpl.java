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
package org.apache.jackrabbit.core.nodetype;

import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.spi.Name;

import javax.jcr.PropertyType;
import java.util.Arrays;

/**
 * This class implements the <code>PropDef</code> interface and additionally
 * provides setter methods for the various property definition attributes.
 */
public class PropDefImpl extends ItemDefImpl implements PropDef {

    /**
     * The required type.
     */
    private int requiredType = PropertyType.UNDEFINED;

    /**
     * The value constraints.
     */
    private ValueConstraint[] valueConstraints = ValueConstraint.EMPTY_ARRAY;

    /**
     * The default values.
     */
    private InternalValue[] defaultValues = InternalValue.EMPTY_ARRAY;

    /**
     * The 'multiple' flag
     */
    private boolean multiple = false;

    /**
     * The identifier of this property definition. The identifier is lazily
     * computed based on the characteristics of this property definition and
     * reset on every attribute change.
     */
    private PropDefId id = null;

    /**
     * Default constructor.
     */
    public PropDefImpl() {
    }

    /**
     * Sets the required type
     *
     * @param requiredType
     */
    public void setRequiredType(int requiredType) {
        // reset id field in order to force lazy recomputation of identifier
        id = null;
        this.requiredType = requiredType;
    }

    /**
     * Sets the value constraints.
     *
     * @param valueConstraints
     */
    public void setValueConstraints(ValueConstraint[] valueConstraints) {
        // reset id field in order to force lazy recomputation of identifier
        id = null;
        if (valueConstraints != null) {
            this.valueConstraints = valueConstraints;
        } else {
            this.valueConstraints = ValueConstraint.EMPTY_ARRAY;
        }
    }

    /**
     * Sets the default values.
     *
     * @param defaultValues
     */
    public void setDefaultValues(InternalValue[] defaultValues) {
        // reset id field in order to force lazy recomputation of identifier
        id = null;
        if (defaultValues != null) {
            this.defaultValues = defaultValues;
        } else {
            this.defaultValues = InternalValue.EMPTY_ARRAY;
        }
    }

    /**
     * Sets the 'multiple' flag.
     *
     * @param multiple
     */
    public void setMultiple(boolean multiple) {
        // reset id field in order to force lazy recomputation of identifier
        id = null;
        this.multiple = multiple;
    }

    //------------------------------------------------< ItemDefImpl overrides >
    /**
     * {@inheritDoc}
     */
    public void setDeclaringNodeType(Name declaringNodeType) {
        // reset id field in order to force lazy recomputation of identifier
        id = null;
        super.setDeclaringNodeType(declaringNodeType);
    }

    /**
     * {@inheritDoc}
     */
    public void setName(Name name) {
        // reset id field in order to force lazy recomputation of identifier
        id = null;
        super.setName(name);
    }

    /**
     * {@inheritDoc}
     */
    public void setAutoCreated(boolean autoCreated) {
        // reset id field in order to force lazy recomputation of identifier
        id = null;
        super.setAutoCreated(autoCreated);
    }

    /**
     * {@inheritDoc}
     */
    public void setOnParentVersion(int onParentVersion) {
        // reset id field in order to force lazy recomputation of identifier
        id = null;
        super.setOnParentVersion(onParentVersion);
    }

    /**
     * {@inheritDoc}
     */
    public void setProtected(boolean writeProtected) {
        // reset id field in order to force lazy recomputation of identifier
        id = null;
        super.setProtected(writeProtected);
    }

    /**
     * {@inheritDoc}
     */
    public void setMandatory(boolean mandatory) {
        // reset id field in order to force lazy recomputation of identifier
        id = null;
        super.setMandatory(mandatory);
    }

    //--------------------------------------------------------------< PropDef >
    /**
     * {@inheritDoc}
     * <p/>
     * The identifier is computed based on the characteristics of this property
     * definition, i.e. modifying attributes of this property definition will
     * have impact on the identifier returned by this method.
     */
    public PropDefId getId() {
        if (id == null) {
            // generate new identifier based on this property definition
            id = new PropDefId(this);
        }
        return id;
    }

    /**
     * {@inheritDoc}
     */
    public int getRequiredType() {
        return requiredType;
    }

    /**
     * {@inheritDoc}
     */
    public ValueConstraint[] getValueConstraints() {
        return valueConstraints;
    }

    /**
     * {@inheritDoc}
     */
    public InternalValue[] getDefaultValues() {
        return defaultValues;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isMultiple() {
        return multiple;
    }

    /**
     * {@inheritDoc}
     *
     * @return always <code>false</code>
     */
    public boolean definesNode() {
        return false;
    }

    //-------------------------------------------< java.lang.Object overrides >
    /**
     * Compares two property definitions for equality. Returns <code>true</code>
     * if the given object is a property defintion and has the same attributes
     * as this property definition.
     *
     * @param obj the object to compare this property definition with
     * @return <code>true</code> if the object is equal to this property definition,
     *         <code>false</code> otherwise
     * @see Object#equals(Object)
     */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof PropDefImpl) {
            PropDefImpl other = (PropDefImpl) obj;
            return super.equals(obj)
                    && requiredType == other.requiredType
                    && Arrays.equals(valueConstraints, other.valueConstraints)
                    && Arrays.equals(defaultValues, other.defaultValues)
                    && multiple == other.multiple;
        }
        return false;
    }

    /**
     * Returns zero to satisfy the Object equals/hashCode contract.
     * This class is mutable and not meant to be used as a hash key.
     *
     * @return always zero
     * @see Object#hashCode()
     */
    public int hashCode() {
        return 0;
    }

}
