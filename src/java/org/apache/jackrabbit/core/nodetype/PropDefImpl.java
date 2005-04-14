/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

import org.apache.jackrabbit.core.InternalValue;
import org.apache.jackrabbit.core.QName;

import javax.jcr.PropertyType;
import java.util.Arrays;

/**
 * this class implements the PropDef interface.
 */
public class PropDefImpl extends ItemDefImpl implements PropDef {

    /**
     * The required type
     */
    private int requiredType = PropertyType.UNDEFINED;

    /**
     * The value constrsints.
     */
    private ValueConstraint[] valueConstraints = ValueConstraint.EMPTY;

    /**
     * the default values
     */
    private InternalValue[] defaultValues = InternalValue.EMPTY;

    /**
     * the 'multiple' flag
     */
    private boolean multiple = false;

    /**
     * the id of this property def
     */
    private PropDefId id;

    /**
     * Sets the required type
     *
     * @param requiredType
     */
    public void setRequiredType(int requiredType) {
        if (id != null) {
            throw new IllegalStateException("Unable to set attribute. PropDef already compiled.");
        }
        this.requiredType = requiredType;
    }

    /**
     * Sets the value constraints
     *
     * @param valueConstraints
     */
    public void setValueConstraints(ValueConstraint valueConstraints[]) {
        if (id != null) {
            throw new IllegalStateException("Unable to set attribute. PropDef already compiled.");
        }
        this.valueConstraints =
                (valueConstraints == null) ? ValueConstraint.EMPTY : valueConstraints;
    }

    /**
     * Sets the default values
     *
     * @param defaultValues
     */
    public void setDefaultValues(InternalValue[] defaultValues) {
        if (id != null) {
            throw new IllegalStateException("Unable to set attribute. PropDef already compiled.");
        }
        this.defaultValues =
                (defaultValues == null) ? InternalValue.EMPTY : defaultValues;
    }

    /**
     * Sets the 'multiple' flag.
     *
     * @param multiple
     */
    public void setMultiple(boolean multiple) {
        if (id != null) {
            throw new IllegalStateException("Unable to set attribute. PropDef already compiled.");
        }
        this.multiple = multiple;
    }

    /**
     * {@inheritDoc}
     */
    public void setDeclaringNodeType(QName declaringNodeType) {
        if (id != null) {
            throw new IllegalStateException("Unable to set attribute. PropDef already compiled.");
        }
        super.setDeclaringNodeType(declaringNodeType);
    }

    /**
     * {@inheritDoc}
     */
    public void setName(QName name) {
        if (id != null) {
            throw new IllegalStateException("Unable to set attribute. PropDef already compiled.");
        }
        super.setName(name);
    }

    /**
     * {@inheritDoc}
     */
    public void setAutoCreated(boolean autoCreated) {
        if (id != null) {
            throw new IllegalStateException("Unable to set attribute. PropDef already compiled.");
        }
        super.setAutoCreated(autoCreated);
    }

    /**
     * {@inheritDoc}
     */
    public void setOnParentVersion(int onParentVersion) {
        if (id != null) {
            throw new IllegalStateException("Unable to set attribute. PropDef already compiled.");
        }
        super.setOnParentVersion(onParentVersion);
    }

    /**
     * {@inheritDoc}
     */
    public void setProtected(boolean writeProtected) {
        if (id != null) {
            throw new IllegalStateException("Unable to set attribute. PropDef already compiled.");
        }
        super.setProtected(writeProtected);
    }

    /**
     * {@inheritDoc}
     */
    public void setMandatory(boolean mandatory) {
        if (id != null) {
            throw new IllegalStateException("Unable to set attribute. PropDef already compiled.");
        }
        super.setMandatory(mandatory);
    }

    /**
     * {@inheritDoc}
     */
    public PropDefId getId() {
        if (id == null) {
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
     */
    public boolean definesNode() {
        return false;
    }

    /**
     * Checks if this prop def is equal to the given one.
     * @param obj the object to compare to
     * @return <code>true</code> if equals; <code>false</code> otherwise
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

}
