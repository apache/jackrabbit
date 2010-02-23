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
package org.apache.jackrabbit.webdav.jcr.property;

import org.apache.jackrabbit.value.ValueHelper;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.jcr.ItemResourceConstants;
import org.apache.jackrabbit.webdav.property.AbstractDavProperty;
import org.apache.jackrabbit.webdav.property.DavProperty;
import org.apache.jackrabbit.webdav.property.DavPropertyName;
import org.apache.jackrabbit.webdav.xml.DomUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.ValueFactory;
import java.util.ArrayList;
import java.util.List;

/**
 * <code>ValuesProperty</code> extends {@link org.apache.jackrabbit.webdav.property.DavProperty} providing
 * utilities to handle the multiple values of the property item represented
 * by this resource.
 */
public class ValuesProperty extends AbstractDavProperty<Value[]> implements ItemResourceConstants {

    private static Logger log = LoggerFactory.getLogger(ValuesProperty.class);

    private final Value[] jcrValues;

    /**
     * Create a new <code>ValuesProperty</code> from the given single {@link Value}.
     *
     * @param value Array of Value objects as obtained from the JCR property.
     */
    public ValuesProperty(Value value) {
        super(JCR_VALUE, false);
        // finally set the value to the DavProperty
        jcrValues = (value == null) ? new Value[0] : new Value[] {value};
    }

    /**
     * Create a new <code>ValuesProperty</code> from the given {@link javax.jcr.Value Value
     * array}.
     *
     * @param values Array of Value objects as obtained from the JCR property.
     */
    public ValuesProperty(Value[] values) {
        super(JCR_VALUES, false);
        // finally set the value to the DavProperty
        jcrValues = (values == null) ? new Value[0] : values;
    }

    /**
     * Wrap the specified <code>DavProperty</code> in a new <code>ValuesProperty</code>.
     *
     * @param property
     * @param defaultType default type of the values to be deserialized. If however
     * the {@link #XML_VALUE 'value'} elements provide a {@link #ATTR_VALUE_TYPE 'type'}
     * attribute, the default value is ignored.
     * @param valueFactory Factory used to retrieve JCR values from the value
     * of the given <code>DavProperty</code>.
     */
    public ValuesProperty(DavProperty<?> property, int defaultType,
                          ValueFactory valueFactory)
        throws RepositoryException, DavException {
        super(property.getName(), false);

        if (!(JCR_VALUES.equals(property.getName()) || JCR_VALUE.equals(getName()))) {
            throw new DavException(DavServletResponse.SC_BAD_REQUEST, "ValuesProperty may only be created with a property that has name="+JCR_VALUES.getName());
        }

        // retrieve jcr-values from child 'value'-element(s)
        List<Element> valueElements = new ArrayList<Element>();
        Object propValue = property.getValue();
        if (propValue == null) {
            jcrValues = new Value[0];
        } else { /* not null propValue */
            if (isValueElement(propValue)) {
                valueElements.add((Element) propValue);
            } else if (propValue instanceof List) {
                for (Object el : ((List<?>) property.getValue())) {
                    /* make sure, only Elements with name 'value' are used for
                    * the 'value' field. any other content (other elements, text,
                    * comment etc.) is ignored. NO bad-request/conflict error is
                    * thrown.
                    */
                    if (isValueElement(el)) {
                        valueElements.add((Element) el);
                    }
                }
            }
            /* fill the 'value' with the valid 'value' elements found before */
            jcrValues = new Value[valueElements.size()];
            int i = 0;
            for (Element element : valueElements) {
                jcrValues[i] = getJcrValue(element, defaultType, valueFactory);
                i++;
            }
        }
    }

    private static boolean isValueElement(Object obj) {
        return obj instanceof Element && XML_VALUE.equals(((Element)obj).getLocalName());
    }

    private void checkPropertyName(DavPropertyName reqName) throws ValueFormatException {
        if (!reqName.equals(getName())) {
            throw new ValueFormatException("Attempt to retrieve mulitple values from single property '" + getName() + "'.");
        }
    }

    /**
     *
     * @param valueElement
     * @param defaultType
     * @return
     * @throws ValueFormatException
     * @throws RepositoryException
     */
    private static Value getJcrValue(Element valueElement, int defaultType,
                                     ValueFactory valueFactory)
        throws ValueFormatException, RepositoryException {
        if (valueElement == null) {
            return null;
        }
        // make sure the value is never 'null'
        String value = DomUtil.getText(valueElement, "");
        String typeStr = DomUtil.getAttribute(valueElement, ATTR_VALUE_TYPE, ItemResourceConstants.NAMESPACE);
        int type = (typeStr == null) ? defaultType : PropertyType.valueFromName(typeStr);
        // deserialize value ->> see #toXml where values are serialized
        return ValueHelper.deserialize(value, type, true, valueFactory);
    }

    /**
     * Converts the value of this property to a {@link javax.jcr.Value value array}.
     *
     * @return Array of Value objects
     * @throws ValueFormatException if convertion of the internal jcr values to
     * the specified value type fails.
     */
    public Value[] getJcrValues(int propertyType, ValueFactory valueFactory) throws ValueFormatException {
        checkPropertyName(JCR_VALUES);
        Value[] vs = new Value[jcrValues.length];
        for (int i = 0; i < jcrValues.length; i++) {
            vs[i] = ValueHelper.convert(jcrValues[i], propertyType, valueFactory);
        }
        return jcrValues;
    }

    /**
     * Returns the internal property value as jcr <code>Value</code> array
     *
     * @return the internal property value as jcr <code>Value</code> array
     */
    public Value[] getJcrValues() throws ValueFormatException {
        checkPropertyName(JCR_VALUES);
        return jcrValues;
    }

    /**
     *
     * @param propertyType
     * @return
     * @throws ValueFormatException
     */
    public Value getJcrValue(int propertyType, ValueFactory valueFactory) throws ValueFormatException {
        checkPropertyName(JCR_VALUE);
        return (jcrValues.length == 0)
                ? null
                : ValueHelper.convert(jcrValues[0], propertyType, valueFactory);
    }

    /**
     *
     * @return
     * @throws ValueFormatException
     */
    public Value getJcrValue() throws ValueFormatException {
        checkPropertyName(JCR_VALUE);
        return (jcrValues.length == 0) ? null : jcrValues[0];
    }

    /**
     * Returns the type of the {@link Value value}s present in this property
     * or {@link PropertyType#UNDEFINED} if no values are available.
     *
     * @return type of values or {@link PropertyType#UNDEFINED}
     */
    public int getValueType() {
        // TODO: check if correct behaviour if values array is empty
        return (jcrValues.length > 0) ? jcrValues[0].getType() : PropertyType.UNDEFINED;
    }

    /**
     * Returns an array of {@link Value}s representing the value of this
     * property.
     *
     * @return an array of {@link Value}s
     * @see #getJcrValues()
     * @see #getJcrValue()
     */
    public Value[] getValue() {
        return jcrValues;
    }

    /**
     *
     * @param document
     * @return the xml element
     */
    @Override
    public Element toXml(Document document) {
        Element elem = getName().toXml(document);
        try {
            for (Value v : jcrValues) {
                String type = PropertyType.nameFromValue(v.getType());
                String serializedValue = ValueHelper.serialize(v, true);
                Element xmlValue = DomUtil.createElement(document, XML_VALUE, ItemResourceConstants.NAMESPACE, serializedValue);
                DomUtil.setAttribute(xmlValue, ATTR_VALUE_TYPE, ItemResourceConstants.NAMESPACE, type);
                elem.appendChild(xmlValue);
            }
        } catch (RepositoryException e) {
            log.error("Unexpected Error while converting jcr value to String: " + e.getMessage());
        }
        return elem;
    }
}
