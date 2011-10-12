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
package org.apache.jackrabbit.commons.webdav;

import org.apache.jackrabbit.util.XMLUtil;
import org.apache.jackrabbit.value.ValueHelper;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.ValueFormatException;
import java.util.ArrayList;
import java.util.List;

/**
 * <code>ValuesUtil</code>...
 */
public class ValueUtil {

    public static Element valueToXml(Value jcrValue, Document document) throws RepositoryException {

        String type = PropertyType.nameFromValue(jcrValue.getType());
        String serializedValue = ValueHelper.serialize(jcrValue, true);

        Element xmlValue = document.createElementNS(JcrRemotingConstants.NS_URI, JcrRemotingConstants.NS_PREFIX + ":" + JcrRemotingConstants.XML_VALUE);

        Text txt = document.createTextNode(serializedValue);
        xmlValue.appendChild(txt);

        Attr attr = document.createAttributeNS(JcrRemotingConstants.NS_URI, JcrRemotingConstants.NS_PREFIX + ":" + JcrRemotingConstants.ATTR_VALUE_TYPE);
        attr.setValue(type);
        xmlValue.setAttributeNodeNS(attr);

        return xmlValue;
    }

    public static Value[] valuesFromXml(Object propValue, int defaultType, ValueFactory valueFactory) throws RepositoryException {
        Value[] jcrValues;
        // retrieve jcr-values from child 'value'-element(s)
        List<Element> valueElements = new ArrayList<Element>();
        if (propValue == null) {
            jcrValues = new Value[0];
        } else { /* not null propValue */
            if (isValueElement(propValue)) {
                valueElements.add((Element) propValue);
            } else if (propValue instanceof List) {
                for (Object el : ((List<?>) propValue)) {
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
        return jcrValues;
    }

    private static boolean isValueElement(Object obj) {
        return obj instanceof Element && JcrRemotingConstants.XML_VALUE.equals(((Element)obj).getLocalName());
    }

    /**
     *
     * @param valueElement
     * @param defaultType
     * @return
     * @throws javax.jcr.ValueFormatException
     * @throws javax.jcr.RepositoryException
     */
    private static Value getJcrValue(Element valueElement, int defaultType,
                                     ValueFactory valueFactory)
        throws ValueFormatException, RepositoryException {
        if (valueElement == null) {
            return null;
        }
        // make sure the value is never 'null'
        String value = XMLUtil.getText(valueElement, "");
        String typeStr = XMLUtil.getAttribute(valueElement, JcrRemotingConstants.ATTR_VALUE_TYPE, JcrRemotingConstants.NS_URI);
        int type = (typeStr == null) ? defaultType : PropertyType.valueFromName(typeStr);
        // deserialize value ->> see #valueToXml where values are serialized
        return ValueHelper.deserialize(value, type, true, valueFactory);
    }

    public static long[] lengthsFromXml(Object propValue) throws RepositoryException {
        long[] lengths;
        // retrieve jcr-values from child 'value'-element(s)
        List<Element> lengthElements = new ArrayList<Element>();
        if (propValue == null) {
            lengths = new long[0];
        } else { /* not null propValue */
            if (isLengthElement(propValue)) {
                lengthElements.add((Element) propValue);
            } else if (propValue instanceof List) {
                for (Object el : ((List<?>) propValue)) {
                    /* make sure, only Elements with name 'value' are used for
                    * the 'value' field. any other content (other elements, text,
                    * comment etc.) is ignored. NO bad-request/conflict error is
                    * thrown.
                    */
                    if (isLengthElement(el)) {
                        lengthElements.add((Element) el);
                    }
                }
            }
            /* fill the 'value' with the valid 'value' elements found before */
            lengths = new long[lengthElements.size()];
            int i = 0;
            for (Element element : lengthElements) {
                lengths[i] = Long.parseLong(XMLUtil.getText(element, "0"));
                i++;
            }
        }
        return lengths;
    }

    private static boolean isLengthElement(Object obj) {
        return obj instanceof Element && JcrRemotingConstants.XML_LENGTH.equals(((Element)obj).getLocalName());
    }
}