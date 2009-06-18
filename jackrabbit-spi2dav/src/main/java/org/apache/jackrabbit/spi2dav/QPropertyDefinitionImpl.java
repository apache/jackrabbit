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
package org.apache.jackrabbit.spi2dav;

import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.spi.QValueFactory;
import org.apache.jackrabbit.spi.QValueConstraint;
import org.apache.jackrabbit.spi.commons.value.ValueFactoryQImpl;
import org.apache.jackrabbit.spi.commons.value.ValueFormat;
import org.apache.jackrabbit.spi.commons.nodetype.constraint.ValueConstraint;
import org.apache.jackrabbit.webdav.xml.DomUtil;
import org.apache.jackrabbit.webdav.xml.ElementIterator;
import org.w3c.dom.Element;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

/**
 * This class implements the <code>QPropertyDefinition</code> interface and additionally
 * provides setter methods for the various property definition attributes.
 */
public class QPropertyDefinitionImpl extends QItemDefinitionImpl implements QPropertyDefinition {

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

    private final String[] availableQueryOperators;
    private final boolean fullTextSearcheable;
    private final boolean queryOrderable;

    /**
     * Default constructor.
     */
    QPropertyDefinitionImpl(Name declaringNodeType, Element pdefElement,
                            NamePathResolver resolver, QValueFactory qValueFactory)
        throws RepositoryException {
        // TODO: webdav server sends jcr names -> nsResolver required. improve this.
        // NOTE: the server should send the namespace-mappings as addition ns-defininitions
        super(declaringNodeType, pdefElement, resolver);

        if (pdefElement.hasAttribute(REQUIREDTYPE_ATTRIBUTE)) {
            requiredType = PropertyType.valueFromName(pdefElement.getAttribute(REQUIREDTYPE_ATTRIBUTE));
        } else {
            requiredType = PropertyType.UNDEFINED;
        }

        if (pdefElement.hasAttribute(MULTIPLE_ATTRIBUTE)) {
            multiple = Boolean.valueOf(pdefElement.getAttribute(MULTIPLE_ATTRIBUTE)).booleanValue();
        } else {
            multiple = false;
        }

        if (pdefElement.hasAttribute(FULL_TEXT_SEARCHABLE_ATTRIBUTE)) {
            fullTextSearcheable = Boolean.valueOf(pdefElement.getAttribute(FULL_TEXT_SEARCHABLE_ATTRIBUTE)).booleanValue();
        } else {
            fullTextSearcheable = false;
        }
        if (pdefElement.hasAttribute(QUERY_ORDERABLE_ATTRIBUTE)) {
            queryOrderable = Boolean.valueOf(pdefElement.getAttribute(QUERY_ORDERABLE_ATTRIBUTE)).booleanValue();
        } else {
            queryOrderable = false;
        }

        Element child = DomUtil.getChildElement(pdefElement, DEFAULTVALUES_ELEMENT, null);
        if (child == null) {
            // No default value defined at all.
            defaultValues = null;
        } else {
            List vs = new ArrayList();
            ElementIterator it = DomUtil.getChildren(child, DEFAULTVALUE_ELEMENT, null);
            while (it.hasNext()) {
                String jcrVal = DomUtil.getText(it.nextElement());
                if (jcrVal == null) {
                    jcrVal = "";
                }
                QValue qValue;
                if (requiredType == PropertyType.BINARY) {
                    // TODO: improve
                    Value v = new ValueFactoryQImpl(qValueFactory, resolver).createValue(jcrVal, requiredType);
                    qValue = ValueFormat.getQValue(v, resolver, qValueFactory);
                } else {
                    qValue = ValueFormat.getQValue(jcrVal, requiredType, resolver, qValueFactory);
                }
                vs.add(qValue);
            }
            defaultValues = (QValue[]) vs.toArray(new QValue[vs.size()]);
        }

        child = DomUtil.getChildElement(pdefElement, VALUECONSTRAINTS_ELEMENT, null);
        if (child == null) {
            valueConstraints = QValueConstraint.EMPTY_ARRAY;
        } else {
            List<QValueConstraint> vc = new ArrayList<QValueConstraint>();
            ElementIterator it = DomUtil.getChildren(child, VALUECONSTRAINT_ELEMENT, null);
            while (it.hasNext()) {
                String qValue = DomUtil.getText(it.nextElement());
                // in case of name and path constraint, the value must be
                // converted to SPI values
                // TODO: tobefixed. path-constraint may contain trailing *
                vc.add(ValueConstraint.create(requiredType, qValue, resolver));
            }
            valueConstraints = vc.toArray(new QValueConstraint[vc.size()]);
        }

        child = DomUtil.getChildElement(pdefElement, AVAILABLE_QUERY_OPERATORS_ELEMENT, null);
        if (child == null) {
            availableQueryOperators = new String[0];
        } else {
            List<String> names = new ArrayList<String>();
            ElementIterator it = DomUtil.getChildren(child, AVAILABLE_QUERY_OPERATOR_ELEMENT, null);
            while (it.hasNext()) {
                String str = DomUtil.getText(it.nextElement());
                names.add(str);
            }
            availableQueryOperators = names.toArray(new String[names.size()]);
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
                    && fullTextSearcheable == other.isFullTextSearchable()
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
            Set s = new HashSet(availableQueryOperators.length);
            for (int i = 0; i < availableQueryOperators.length; i++) {
                s.add(availableQueryOperators[i]);
            }
            sb.append(s.toString());

            hashCode = sb.toString().hashCode();
        }
        return hashCode;
    }
}
