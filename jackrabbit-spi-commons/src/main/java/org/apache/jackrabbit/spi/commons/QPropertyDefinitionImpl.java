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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * <code>QPropertyDefinitionImpl</code> implements a qualified property
 * definition.
 */
public class QPropertyDefinitionImpl
        extends QItemDefinitionImpl
        implements QPropertyDefinition {

    /**
     * The required type.
     */
    private final int requiredType;

    /**
     * The value constraints.
     */
    private final String[] valueConstraints;

    /**
     * The default values.
     */
    private final QValue[] defaultValues;

    /**
     * The 'multiple' flag
     */
    private final boolean multiple;

    /**
     * The 'multiple' flag
     */
    private final Name[] availableQueryOperators;

    /**
     * The 'fullTextSearcheable' flag
     */
    private final boolean fullTextSearcheable;

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
                propDef.isFullTextSearcheable(),
                propDef.isQueryOrderable());
    }

    /**
     * Creates a new serializable qualified property definition.
     *
     * @param name              the name of the child item.
     * @param declaringNodeType the delaring node type
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
     * @throws NullPointerException if <code>valueConstraints</code> is
     *                              <code>null</code>.
     * @deprecated Use {@link #QPropertyDefinitionImpl(Name, Name,
                                   boolean, boolean, int, boolean, QValue[], boolean,
                                   int, String[], Name[], boolean, boolean)} instead.
     */
    public QPropertyDefinitionImpl(Name name, Name declaringNodeType,
                                   boolean isAutoCreated, boolean isMandatory,
                                   int onParentVersion, boolean isProtected,
                                   QValue[] defaultValues, boolean isMultiple,
                                   int requiredType, String[] valueConstraints) {
        this(name, declaringNodeType, isAutoCreated, isMandatory,
                onParentVersion, isProtected, defaultValues, isMultiple,
                requiredType, valueConstraints, null, false, false);
    }

    /**
     * Creates a new serializable qualified property definition.
     *
     * @param name              the name of the child item.
     * @param declaringNodeType the delaring node type
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
     * @param availableQueryOperators
     * @param isFullTextSearcheable
     * @param isQueryOrderable
     * @throws NullPointerException if <code>valueConstraints</code> is
     *                              <code>null</code>.
     * @since JCR 2.0
     */
    public QPropertyDefinitionImpl(Name name, Name declaringNodeType,
                                   boolean isAutoCreated, boolean isMandatory,
                                   int onParentVersion, boolean isProtected,
                                   QValue[] defaultValues, boolean isMultiple,
                                   int requiredType, String[] valueConstraints,
                                   Name[] availableQueryOperators,
                                   boolean isFullTextSearcheable,
                                   boolean isQueryOrderable) {
        super(name, declaringNodeType, isAutoCreated, isMandatory,
                onParentVersion, isProtected);
        if (valueConstraints == null) {
            throw new NullPointerException("valueConstraints");
        }
        this.defaultValues = defaultValues;
        this.multiple = isMultiple;
        this.requiredType = requiredType;
        this.valueConstraints = valueConstraints;
        this.availableQueryOperators = availableQueryOperators;
        this.fullTextSearcheable = isFullTextSearcheable;
        this.queryOrderable = isQueryOrderable;
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
    public String[] getValueConstraints() {
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
    public Name[] getAvailableQueryOperators() {
        return availableQueryOperators;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isFullTextSearcheable() {
        return fullTextSearcheable;
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
        if (obj instanceof QPropertyDefinition) {
            QPropertyDefinition other = (QPropertyDefinition) obj;
            return super.equals(obj)
                    && requiredType == other.getRequiredType()
                    && multiple == other.isMultiple()
                    && fullTextSearcheable == other.isFullTextSearcheable()
                    && queryOrderable == other.isQueryOrderable()
                    && Arrays.equals(valueConstraints, other.getValueConstraints())
                    && Arrays.equals(defaultValues, other.getDefaultValues())
                    && Arrays.equals(availableQueryOperators, other.getAvailableQueryOperators());
        }
        return false;
    }

    /**
     * Overwrites {@link QItemDefinitionImpl#hashCode()}.
     *
     * @return
     */
    public int hashCode() {
        if (hashCode == 0) {
            // build hashCode (format: <declaringNodeType>/<name>/<requiredType>/<multiple>)
            StringBuffer sb = new StringBuffer();

            sb.append(getDeclaringNodeType().toString());
            sb.append('/');
            if (definesResidual()) {
                sb.append('*');
            } else {
                sb.append(getName().toString());
            }
            sb.append('/');
            sb.append(requiredType);
            sb.append('/');
            sb.append(multiple ? 1 : 0);
            sb.append('/');
            sb.append(fullTextSearcheable ? 1 : 0);
            sb.append('/');
            sb.append(queryOrderable ? 1 : 0);
            sb.append('/');
            Set<Name> s = new HashSet<Name>();
            Name[] names = getAvailableQueryOperators();
            for (int i = 0; i < names.length; i++) {
                s.add(names[i]);
            }
            sb.append(s.toString());

            hashCode = sb.toString().hashCode();
        }
        return hashCode;
    }
}
