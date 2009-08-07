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
import org.apache.jackrabbit.spi.commons.value.ValueFactoryQImpl;
import org.apache.jackrabbit.spi.commons.value.ValueFormat;
import org.apache.jackrabbit.webdav.xml.DomUtil;
import org.apache.jackrabbit.webdav.xml.ElementIterator;
import org.w3c.dom.Element;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
            valueConstraints = new String[0];
        } else {
            List vc = new ArrayList();
            ElementIterator it = DomUtil.getChildren(child, VALUECONSTRAINT_ELEMENT, null);
            while (it.hasNext()) {
                int constType = (requiredType == PropertyType.REFERENCE) ?  PropertyType.NAME : requiredType;
                String qValue = DomUtil.getText(it.nextElement());
                // in case of name and path constraint, the value must be
                // converted to be in qualified format
                if (constType == PropertyType.NAME || constType == PropertyType.PATH) {
                   qValue = ValueFormat.getQValue(qValue, constType, resolver, qValueFactory).getString();
                }
                vc.add(qValue);
            }
            valueConstraints = (String[]) vc.toArray(new String[vc.size()]);
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
                sb.append(getName().toString());
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
