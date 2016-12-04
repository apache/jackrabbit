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
package org.apache.jackrabbit.spi.commons;

import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.QValueConstraint;
import org.apache.jackrabbit.spi.QValueFactory;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.spi.commons.nodetype.constraint.ValueConstraint;
import org.apache.jackrabbit.spi.commons.value.ValueFormat;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.nodetype.PropertyDefinition;

/**
 * <code>QPropertyDefinitionImpl</code> implements SPI property
 * definition interface.
 */
public class QPropertyDefinitionImpl extends QItemDefinitionImpl
        implements QPropertyDefinition {

    private static final long serialVersionUID = 1064686456661663541L;

    /**
     * The required type.
     */
    private final int requiredType;

    /**
     * The value constraints.
     */
    private final QValueConstraint[] valueConstraints;

    /**
     * The default values.
     */
    private final QValue[] defaultValues;

    /**
     * The 'multiple' flag
     */
    private final boolean multiple;

    /**
     * The available query operators
     */
    private final String[] availableQueryOperators;

    /**
     * The 'fullTextSearcheable' flag
     */
    private final boolean fullTextSearchable;
    /**
     * The 'queryOrderable' flag
     */
    private final boolean queryOrderable;

    /**
     * Copy constructor.
     *
     * @param propDef some other property definition.
     */
    public QPropertyDefinitionImpl(QPropertyDefinition propDef) {
        this(propDef.getName(), propDef.getDeclaringNodeType(),
                propDef.isAutoCreated(), propDef.isMandatory(),
                propDef.getOnParentVersion(), propDef.isProtected(),
                propDef.getDefaultValues(), propDef.isMultiple(),
                propDef.getRequiredType(), propDef.getValueConstraints(),
                propDef.getAvailableQueryOperators(),
                propDef.isFullTextSearchable(),
                propDef.isQueryOrderable());
    }

    /**
     * Creates a new serializable property definition.
     *
     * @param name              the name of the child item.
     * @param declaringNodeType the declaring node type
     * @param isAutoCreated     if this item is auto created.
     * @param isMandatory       if this is a mandatory item.
     * @param onParentVersion   the on parent version behaviour.
     * @param isProtected       if this item is protected.
     * @param defaultValues     the default values or <code>null</code> if there
     *                          are none.
     * @param isMultiple        if this property is multi-valued.
     * @param requiredType      the required type for this property.
     * @param valueConstraints  the value constraints for this property. If none
     *                          exist an empty array must be passed.
     * @param availableQueryOperators the available query operators
     * @param isFullTextSearchable if this is fulltext searchable
     * @param isQueryOrderable   if this is queryable
     * @throws NullPointerException if <code>valueConstraints</code> or
     *                              <code>availableQueryOperators</code> is
     *                              <code>null</code>.
     * @since JCR 2.0
     */
    public QPropertyDefinitionImpl(Name name, Name declaringNodeType,
                                   boolean isAutoCreated, boolean isMandatory,
                                   int onParentVersion, boolean isProtected,
                                   QValue[] defaultValues, boolean isMultiple,
                                   int requiredType,
                                   QValueConstraint[] valueConstraints,
                                   String[] availableQueryOperators,
                                   boolean isFullTextSearchable,
                                   boolean isQueryOrderable) {
        super(name, declaringNodeType, isAutoCreated, isMandatory,
                onParentVersion, isProtected);
        if (valueConstraints == null) {
            throw new NullPointerException("valueConstraints");
        }
        if (availableQueryOperators == null) {
            throw new NullPointerException("availableQueryOperators");
        }
        this.defaultValues = defaultValues;
        this.multiple = isMultiple;
        this.requiredType = requiredType;
        this.valueConstraints = valueConstraints;
        this.availableQueryOperators = availableQueryOperators;
        this.fullTextSearchable = isFullTextSearchable;
        this.queryOrderable = isQueryOrderable;
    }

    /**
     * Creates a new property definition based on <code>propDef</code>.
     *
     * @param propDef       the JCR property definition.
     * @param resolver      the name/path resolver of the session that provided
     *                      the property definition.
     * @param qValueFactory the QValue factory.
     * @throws RepositoryException if an error occurs while reading from
     *                             <code>propDef</code>.
     */
    public QPropertyDefinitionImpl(PropertyDefinition propDef,
                                   NamePathResolver resolver,
                                   QValueFactory qValueFactory)
            throws RepositoryException {
        this(propDef.getName().equals(NameConstants.ANY_NAME.getLocalName()) ? NameConstants.ANY_NAME : resolver.getQName(propDef.getName()),
                resolver.getQName(propDef.getDeclaringNodeType().getName()),
                propDef.isAutoCreated(), propDef.isMandatory(),
                propDef.getOnParentVersion(), propDef.isProtected(),
                convertValues(propDef.getDefaultValues(), resolver, qValueFactory),
                propDef.isMultiple(), propDef.getRequiredType(),
                ValueConstraint.create(propDef.getRequiredType(), propDef.getValueConstraints(), resolver),
                propDef.getAvailableQueryOperators(),
                propDef.isFullTextSearchable(), propDef.isQueryOrderable());
    }

    //------------------------------------------------< QPropertyDefinition >---
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
    public QValue[] getDefaultValues() {
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
    public String[] getAvailableQueryOperators() {
        return availableQueryOperators;
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

    /**
     * {@inheritDoc}
     *
     * @return always <code>false</code>
     */
    public boolean definesNode() {
        return false;
    }

    //-------------------------------------------------------------< Object >---
    /**
     * Compares two property definitions for equality. Returns <code>true</code>
     * if the given object is a property definition and has the same attributes
     * as this property definition.
     *
     * @param obj the object to compare this property definition with
     * @return <code>true</code> if the object is equal to this property definition,
     *         <code>false</code> otherwise
     * @see Object#equals(Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof QPropertyDefinition) {
            QPropertyDefinition other = (QPropertyDefinition) obj;

            return super.equals(obj)
                    && requiredType == other.getRequiredType()
                    && multiple == other.isMultiple()
                    && fullTextSearchable == other.isFullTextSearchable()
                    && queryOrderable == other.isQueryOrderable()
                    && ((valueConstraints == null || other.getValueConstraints() == null) ? (valueConstraints == other.getValueConstraints())
                        : new HashSet(Arrays.asList(valueConstraints)).equals(new HashSet(Arrays.asList(other.getValueConstraints()))))
                    && ((defaultValues == null || other.getDefaultValues() == null) ? (defaultValues == other.getDefaultValues())
                        : new HashSet(Arrays.asList(defaultValues)).equals(new HashSet(Arrays.asList(other.getDefaultValues()))))
                    && new HashSet(Arrays.asList(availableQueryOperators)).equals(new HashSet(Arrays.asList(other.getAvailableQueryOperators())));
        }
        return false;
    }

    /**
     * Overrides {@link QItemDefinitionImpl#hashCode()}.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        if (hashCode == 0) {
            int h = super.hashCode();
            h = 37 * h + requiredType;
            h = 37 * h + (multiple ? 11 : 43);
            h = 37 * h + (queryOrderable ? 11 : 43);
            h = 37 * h + (fullTextSearchable ? 11 : 43);
            h = 37 * h + ((valueConstraints != null) ? new HashSet(Arrays.asList(valueConstraints)).hashCode() : 0);
            h = 37 * h + ((defaultValues != null) ? new HashSet(Arrays.asList(defaultValues)).hashCode() : 0);
            h = 37 * h + new HashSet(Arrays.asList(availableQueryOperators)).hashCode();
            hashCode = h;
        }
        return hashCode;
    }

    //-----------------------------------------------------------< internal >---

    /**
     * Converts JCR {@link Value}s to {@link QValue}s.
     *
     * @param values   the JCR values.
     * @param resolver the name/path resolver of the session that provided the
     *                 values.
     * @param factory  the QValue factory.
     * @return the converted values.
     * @throws RepositoryException if an error occurs while converting the
     *                             values.
     */
    private static QValue[] convertValues(Value[] values,
                                          NamePathResolver resolver,
                                          QValueFactory factory)
            throws RepositoryException {
        if (values != null) {
            QValue[] defaultValues = new QValue[values.length];
            for (int i = 0; i < values.length; i++) {
                defaultValues[i] = ValueFormat.getQValue(values[i], resolver, factory);
            }
            return defaultValues;
        }  else {
            return null;
        }
    }
}
