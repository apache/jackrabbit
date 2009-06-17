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
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.QValueConstraint;
import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.spi.commons.query.qom.Operator;
import org.apache.jackrabbit.spi.commons.QPropertyDefinitionImpl;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

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
    private QValueConstraint[] valueConstraints = QValueConstraint.EMPTY_ARRAY;

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

    /*
     * The 'fulltext searchable' flag.
     */
    private boolean fullTextSearchable = true;

    /*
     * The 'query orderable' flag.
     */
    private boolean queryOrderable = true;

    /*
     * The 'query operators.
     */
    private String[] queryOperators = Operator.getAllQueryOperators();


    /**
     * Default constructor.
     */
    public PropDefImpl() {
    }

    public PropDefImpl(QPropertyDefinition pd) {
        super(pd);
        requiredType = pd.getRequiredType();
        valueConstraints = pd.getValueConstraints();
        QValue[] vs = pd.getDefaultValues();
        if (vs != null) {
            defaultValues = new InternalValue[vs.length];
            for (int i=0; i<vs.length; i++) {
                try {
                    defaultValues[i] = InternalValue.create(vs[i]);
                } catch (RepositoryException e) {
                    throw new IllegalStateException("Error while converting default values.", e);
                }
            }
        }
        multiple = pd.isMultiple();
        fullTextSearchable = pd.isFullTextSearchable();
        queryOrderable = pd.isQueryOrderable();
        queryOperators = pd.getAvailableQueryOperators();
    }

    /**
     * Returns the QPropertyDefinition of this PropDef
     * @return the QPropertyDefinition
     */
    public QPropertyDefinition getQPropertyDefinition() {
        return new QPropertyDefinitionImpl(
                getName(),
                getDeclaringNodeType(),
                isAutoCreated(),
                isMandatory(),
                getOnParentVersion(),
                isProtected(),
                getDefaultValues(),
                isMultiple(),
                getRequiredType(),
                getValueConstraints(),
                getAvailableQueryOperators(),
                isFullTextSearchable(),
                isQueryOrderable()
        );
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
    public void setValueConstraints(QValueConstraint[] valueConstraints) {
        // reset id field in order to force lazy recomputation of identifier
        id = null;
        if (valueConstraints != null) {
            this.valueConstraints = valueConstraints;
        } else {
            this.valueConstraints = QValueConstraint.EMPTY_ARRAY;
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

    /**
     * Sets the 'fulltext searchable' flag.
     *
     * @param fullTextSearchable
     */
    public void setFullTextSearchable(boolean fullTextSearchable) {
        // reset id field in order to force lazy recomputation of identifier
        id = null;
        this.fullTextSearchable = fullTextSearchable;
    }

    /**
     * Sets the 'fulltext searchable' flag.
     *
     * @param queryOrderable
     */
    public void setQueryOrderable(boolean queryOrderable) {
        // reset id field in order to force lazy recomputation of identifier
        id = null;
        this.queryOrderable = queryOrderable;
    }

    /**
     * Sets the 'available' query operators.
     *
     * @param queryOperators
     */
    public void setAvailableQueryOperators(String[] queryOperators) {
        // reset id field in order to force lazy recomputation of identifier
        id = null;
        if (queryOperators != null) {
            this.queryOperators = queryOperators;
        } else {
            this.queryOperators = new String[0];
        }
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
    public QValueConstraint[] getValueConstraints() {
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

    /**
     * {@inheritDoc}
     */
    public String[] getAvailableQueryOperators() {
        return queryOperators;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isFullTextSearchable() {
        return fullTextSearchable;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isQueryOrderable() {
        return queryOrderable;
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
                    && multiple == other.multiple
                    && Arrays.equals(queryOperators, other.queryOperators)
                    && queryOrderable == other.queryOrderable
                    && fullTextSearchable == other.fullTextSearchable;
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
