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
package org.apache.jackrabbit.webdav.jcr.search;

import org.apache.jackrabbit.commons.webdav.QueryUtil;
import org.apache.jackrabbit.value.ValueHelper;
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
import javax.jcr.ValueFactory;
import java.util.ArrayList;
import java.util.List;

/**
 * <code>SearchResultProperty</code>...
 */
// todo: find proper solution for transporting search results...
public class SearchResultProperty extends AbstractDavProperty<Value[]> implements ItemResourceConstants {

    private static Logger log = LoggerFactory.getLogger(SearchResultProperty.class);

    private static final DavPropertyName SEARCH_RESULT_PROPERTY = DavPropertyName.create(JCR_QUERY_RESULT_LN, ItemResourceConstants.NAMESPACE);

    private final String[] columnNames;
    private final String[] selectorNames;
    private final Value[] values;

    /**
     * Creates a new <code>SearchResultProperty</code>.
     *
     * @param columnNames   the column names of the search row represented by
     *                      this dav property.
     * @param selectorNames the selector names of the row represented by this
     *                      dav property.
     * @param values        the values present in the columns
     */
    public SearchResultProperty(String[] columnNames,
                                String[] selectorNames,
                                Value[] values) {
        super(SEARCH_RESULT_PROPERTY, true);
        this.columnNames = columnNames;
        this.selectorNames = selectorNames;
        this.values = values;
    }

    /**
     * Wrap the specified <code>DavProperty</code> in a new <code>SearchResultProperty</code>.
     *
     * @param property
     * @param valueFactory factory used to deserialize the xml value to a JCR value.
     * @throws RepositoryException if an error occurs while build the property value
     * @throws IllegalArgumentException if the specified property does have the
     * required form.
     * @see #getValues()
     */
    public SearchResultProperty(DavProperty<?> property, ValueFactory valueFactory) throws RepositoryException {
        super(property.getName(), true);
        if (!SEARCH_RESULT_PROPERTY.equals(getName())) {
            throw new IllegalArgumentException("SearchResultProperty may only be created from a property named " + SEARCH_RESULT_PROPERTY.toString());
        }

        List<String> colList = new ArrayList<String>();
        List<String> selList = new ArrayList<String>();
        List<Value> valList = new ArrayList<Value>();

        QueryUtil.parseResultPropertyValue(property.getValue(), colList, selList, valList, valueFactory);

        columnNames = colList.toArray(new String[colList.size()]);
        selectorNames = selList.toArray(new String[selList.size()]);
        values = valList.toArray(new Value[valList.size()]);
    }

    /**
     * Return the column names representing the names of the properties present
     * in the {@link #getValues() values}.
     *
     * @return columnNames
     */
    public String[] getColumnNames() {
        return columnNames;
    }

    /**
     * @return the selector name for each of the columns in the result property.
     */
    public String[] getSelectorNames() {
        return selectorNames;
    }

    /**
     * Return the values representing the values of that row in the search
     * result table.
     *
     * @return values
     * @see javax.jcr.query.Row#getValues()
     */
    public Value[] getValues() {
        return values;
    }


    /**
     * Same as {@link #getValues()}
     *
     * @return Array of JCR Value object
     */
    public Value[] getValue() {
        return values;
    }

    /**
     * Return the xml representation of this webdav property. For every value in
     * the query result row a dcr:name, dcr:value, dcr:type and an optional
     * dcr:selectorName element is created.
     * Example:
     * <pre>
     * -----------------------------------------------------------
     *   col-name  |   bla   |   bli   |  jcr:path  |  jcr:score
     * -----------------------------------------------------------
     *   value     |   xxx   |   111   |  /aNode    |    1
     *   type      |    1    |    3    |     8      |    3
     *   sel-name  |         |         |     S      |    S
     * -----------------------------------------------------------
     * </pre>
     * results in:
     * <pre>
     * &lt;dcr:search-result-property xmlns:dcr="http://www.day.com/jcr/webdav/1.0"&gt;
     *    &lt;dcr:column&gt;
     *       &lt;dcr:name&gt;bla&lt;dcr:name/&gt;
     *       &lt;dcr:value dcr:type="String"&gt;xxx&lt;dcr:value/&gt;
     *    &lt;/dcr:column&gt;
     *    &lt;dcr:column&gt;
     *       &lt;dcr:name&gt;bli&lt;dcr:name/&gt;
     *       &lt;dcr:value dcr:type="Long"&gt;111&lt;dcr:value/&gt;
     *    &lt;/dcr:column&gt;
     *    &lt;dcr:column&gt;
     *       &lt;dcr:name&gt;jcr:path&lt;dcr:name/&gt;
     *       &lt;dcr:value dcr:type="Path"&gt;/aNode&lt;dcr:value/&gt;
     *       &lt;dcr:selectorName&gt;S&lt;dcr:selectorName/&gt;
     *    &lt;/dcr:column&gt;
     *    &lt;dcr:column&gt;
     *       &lt;dcr:name&gt;jcr:score&lt;dcr:name/&gt;
     *       &lt;dcr:value dcr:type="Long"&gt;1&lt;dcr:value/&gt;
     *       &lt;dcr:selectorName&gt;S&lt;dcr:selectorName/&gt;
     *    &lt;/dcr:column&gt;
     * &lt;/dcr:search-result-property&gt;
     * </pre>
     *
     * @see org.apache.jackrabbit.webdav.xml.XmlSerializable#toXml(org.w3c.dom.Document)
     */
    @Override
    public Element toXml(Document document) {
        Element elem = getName().toXml(document);
        for (int i = 0; i < columnNames.length; i++) {
            String propertyName = columnNames[i];
            String selectorName = selectorNames[i];
            Value propertyValue = values[i];

            Element columnEl = DomUtil.addChildElement(elem, XML_QUERY_RESULT_COLUMN, ItemResourceConstants.NAMESPACE);
            DomUtil.addChildElement(columnEl, JCR_NAME.getName(), JCR_NAME.getNamespace(), propertyName);
            if (propertyValue != null) {
                try {
                    String serializedValue = ValueHelper.serialize(propertyValue, true);
                    Element xmlValue = DomUtil.addChildElement(columnEl, XML_VALUE, ItemResourceConstants.NAMESPACE, serializedValue);
                    String type = PropertyType.nameFromValue(propertyValue.getType());
                    DomUtil.setAttribute(xmlValue, ATTR_VALUE_TYPE, ItemResourceConstants.NAMESPACE, type);
                } catch (RepositoryException e) {
                    log.error(e.toString());
                }
            }
            if (selectorName != null) {
                DomUtil.addChildElement(columnEl, JCR_SELECTOR_NAME.getName(), JCR_SELECTOR_NAME.getNamespace(), selectorName);
            }
        }
        return elem;
    }
}
