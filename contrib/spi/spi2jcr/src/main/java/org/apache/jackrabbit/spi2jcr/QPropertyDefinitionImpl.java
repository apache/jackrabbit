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
package org.apache.jackrabbit.spi2jcr;

import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.spi.QValueFactory;
import org.apache.jackrabbit.name.NamespaceResolver;
import org.apache.jackrabbit.value.ValueFormat;

import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.PropertyType;
import java.util.Arrays;

/**
 * <code>QPropertyDefinitionImpl</code> implements a qualified property
 * definition based on a JCR {@link javax.jcr.nodetype.PropertyDefinition}.
 * TODO: mostly copied from spi2dav, move common parts to spi-commons.
 */
class QPropertyDefinitionImpl extends QItemDefinitionImpl implements QPropertyDefinition {

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
     * Creates a new qualified property definition based on
     * <code>propDef</code>.
     *
     * @param propDef       the JCR property definition.
     * @param nsResolver    the namespace resolver in use.
     * @param qValueFactory the QValue factory.
     * @throws RepositoryException if an error occurs while reading from
     *                             <code>propDef</code>.
     */
    QPropertyDefinitionImpl(PropertyDefinition propDef,
                            NamespaceResolver nsResolver,
                            QValueFactory qValueFactory) throws RepositoryException {
        super(propDef, nsResolver);
        Value[] defValues = propDef.getDefaultValues();
        if (defValues != null) {
            defaultValues = new QValue[defValues.length];
            for (int i = 0; i < defValues.length; i++) {
                defaultValues[i] = ValueFormat.getQValue(defValues[i], nsResolver, qValueFactory);
            }
        } else {
            defaultValues = null;
        }
        this.multiple = propDef.isMultiple();
        this.requiredType = propDef.getRequiredType();
        this.valueConstraints = propDef.getValueConstraints();
        if (requiredType == PropertyType.REFERENCE
                || requiredType == PropertyType.NAME
                || requiredType == PropertyType.PATH) {
            for (int i = 0; i < valueConstraints.length; i++) {
                int type = requiredType == PropertyType.REFERENCE ? PropertyType.NAME : requiredType;
                this.valueConstraints[i] = ValueFormat.getQValue(
                        valueConstraints[i], type, nsResolver, qValueFactory).getString();
            }
        }
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
                    && Arrays.equals(valueConstraints, other.getValueConstraints())
                    && Arrays.equals(defaultValues, other.getDefaultValues())
                    && multiple == other.isMultiple();
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
                sb.append(getQName().toString());
            }
            sb.append('/');
            sb.append(getRequiredType());
            sb.append('/');
            sb.append(isMultiple() ? 1 : 0);

            hashCode = sb.toString().hashCode();
        }
        return hashCode;
    }
}
