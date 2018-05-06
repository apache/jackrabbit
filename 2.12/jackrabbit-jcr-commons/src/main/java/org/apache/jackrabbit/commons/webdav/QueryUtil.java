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
import org.w3c.dom.Element;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.ValueFormatException;
import java.util.List;

/**
 * <code>QueryUtil</code>...
 */
public class QueryUtil implements JcrRemotingConstants {

    public static void parseResultPropertyValue(Object propValue,
                                                List<String> columnNames,
                                                List<String> selectorNames,
                                                List<Value> values,
                                                ValueFactory valueFactory)
            throws ValueFormatException, RepositoryException {
        if (propValue instanceof List) {
            for (Object o : ((List<?>) propValue)) {
                if (o instanceof Element) {
                    parseColumnElement((Element) o, columnNames, selectorNames, values, valueFactory);
                }
            }
        } else if (propValue instanceof Element) {
            parseColumnElement((Element) propValue, columnNames, selectorNames, values, valueFactory);
        } else {
            throw new IllegalArgumentException("SearchResultProperty requires a list of 'dcr:column' xml elements.");
        }
    }

    private static void parseColumnElement(Element columnElement,
                                           List<String> columnNames,
                                           List<String> selectorNames,
                                           List<Value> values,
                                           ValueFactory valueFactory)
            throws ValueFormatException, RepositoryException {
        if (!XML_QUERY_RESULT_COLUMN.equals(columnElement.getLocalName()) && NS_URI.equals(columnElement.getNamespaceURI())) {
            // dcr:column element expected within search result -> can't parse
            return;
        }

        columnNames.add(XMLUtil.getChildText(columnElement, JCR_NAME_LN, NS_URI));
        selectorNames.add(XMLUtil.getChildText(columnElement, JCR_SELECTOR_NAME_LN, NS_URI));

        Value jcrValue = null;
        Element valueElement = XMLUtil.getChildElement(columnElement, JCR_VALUE_LN, NS_URI);
        if (valueElement != null) {
            String text = XMLUtil.getText(valueElement);
            if (text != null) {
                String typeStr = XMLUtil.getAttribute(valueElement, ATTR_VALUE_TYPE, NS_URI);
                jcrValue = ValueHelper.deserialize(
                        text, PropertyType.valueFromName(typeStr), true, valueFactory);
            }
        }
        values.add(jcrValue);
    }
}