/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

import org.apache.log4j.Logger;
import org.apache.jackrabbit.webdav.property.DavPropertyName;
import org.apache.jackrabbit.webdav.property.AbstractDavProperty;
import org.apache.jackrabbit.webdav.property.DavProperty;
import org.apache.jackrabbit.webdav.jcr.ItemResourceConstants;
import org.apache.jackrabbit.webdav.xml.DomUtil;
import org.apache.jackrabbit.value.ValueHelper;
import org.w3c.dom.Element;
import org.w3c.dom.Document;

import javax.jcr.Value;
import javax.jcr.RepositoryException;
import javax.jcr.PropertyType;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * <code>SearchResultProperty</code>...
 */
// todo: find proper solution for transporting search results...
public class SearchResultProperty extends AbstractDavProperty implements ItemResourceConstants {

    private static Logger log = Logger.getLogger(SearchResultProperty.class);

    public static final DavPropertyName SEARCH_RESULT_PROPERTY = DavPropertyName.create("search-result-property", ItemResourceConstants.NAMESPACE);

    private final String[] columnNames;
    private final Value[] values;

    /**
     * Creates a new <code>SearchResultProperty</code>.
     *
     * @param columnNames the column names of the search row represented by this
     * dav property.
     * @param values the values present in the columns
     */
    public SearchResultProperty(String[] columnNames, Value[] values) {
        super(SEARCH_RESULT_PROPERTY, true);
        this.columnNames = columnNames;
        this.values = values;
    }

    /**
     * Wrap the specified <code>DavProperty</code> in a new <code>SearchResultProperty</code>.
     *
     * @param property
     * @throws RepositoryException if an error occurs while build the property value
     * @throws IllegalArgumentException if the specified property does have the
     * required form.
     */
    public SearchResultProperty(DavProperty property) throws RepositoryException {
        super(SEARCH_RESULT_PROPERTY, true);
        if (!SEARCH_RESULT_PROPERTY.equals(property.getName())) {
	    throw new IllegalArgumentException("SearchResultProperty may only be created with a property that has name="+SEARCH_RESULT_PROPERTY.getName());
	}

        List colList = new ArrayList();
        List valList = new ArrayList();

        if (property.getValue() instanceof List) {
            List l = (List) property.getValue();

            String name = null;
            String value = null;
            int i = 0;
            Iterator elemIt = l.iterator();
            while (elemIt.hasNext()) {
                Object el = elemIt.next();
                if (el instanceof Element) {
                    String txt = DomUtil.getText((Element)el);
                    if (JCR_NAME.getName().equals(((Element)el).getLocalName())) {
                        name = txt;
                    } else if (JCR_VALUE.getName().equals(((Element)el).getLocalName())) {
                        value = txt;
                    } else if (JCR_TYPE.getName().equals(((Element)el).getLocalName())) {
                        int type = PropertyType.valueFromName(txt);
                        if (name == null) {
                            throw new IllegalArgumentException("SearchResultProperty requires a set of 'dcr:name','dcr:value' and 'dcr:type' xml elements.");
                        }
                        colList.add(name);
                        valList.add((value == null) ? null : ValueHelper.deserialize(value, type, false));
                        // reset...
                        name = null;
                        value = null;
                        i++;
                    }
                }
            }
        } else {
            new IllegalArgumentException("SearchResultProperty requires a list of 'dcr:name','dcr:value' and 'dcr:type' xml elements.");
        }

        columnNames = (String[]) colList.toArray(new String[colList.size()]);
        values = (Value[]) valList.toArray(new Value[valList.size()]);
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
    public Object getValue() {
        return values;
    }

    /**
     * Return the xml representation of this webdav property. For every value in
     * the query result row a dcr:name, dcr:value and dcr:type element is created.
     * Example:
     * <pre>
     * -----------------------------------------------------------
     *   col-name  |   bla   |   bli   |  dcr:path  |  dcr:score
     * -----------------------------------------------------------
     *   value     |   xxx   |   111   |  /aNode    |    1
     *   type      |    1    |    3    |     8      |    3
     * -----------------------------------------------------------
     * </pre>
     * results in:
     * <pre>
     * &lt;dcr:name&gt;bla&lt;dcr:name/&gt;
     * &lt;dcr:value&gt;xxx&lt;dcr:value/&gt;
     * &lt;dcr:type&gt;String&lt;dcr:value/&gt;
     * &lt;dcr:name&gt;bli&lt;dcr:name/&gt;
     * &lt;dcr:value&gt;111&lt;dcr:value/&gt;
     * &lt;dcr:type&gt;Long&lt;dcr:value/&gt;
     * &lt;dcr:name&gt;jcr:path&lt;dcr:name/&gt;
     * &lt;dcr:value&gt;/aNode&lt;dcr:value/&gt;
     * &lt;dcr:type&gt;Path&lt;dcr:value/&gt;
     * &lt;dcr:name&gt;jcr:score&lt;dcr:name/&gt;
     * &lt;dcr:value&gt;1&lt;dcr:value/&gt;
     * &lt;dcr:type&gt;Long&lt;dcr:value/&gt;
     * </pre>
     *
     * @see org.apache.jackrabbit.webdav.xml.XmlSerializable#toXml(org.w3c.dom.Document)
     */
    public Element toXml(Document document) {
        Element elem = getName().toXml(document);
        for (int i = 0; i < columnNames.length; i++) {
            String propertyName = columnNames[i];
            Value propertyValue = values[i];
            String valueStr = null;
            if (propertyValue != null) {
                try {
                    valueStr = propertyValue.getString();
                } catch (RepositoryException e) {
                    log.error(e.getMessage());
                }
            }
            String type = (propertyValue == null) ? PropertyType.TYPENAME_STRING : PropertyType.nameFromValue(propertyValue.getType());

            Element child = JCR_NAME.toXml(document);
            DomUtil.setText(child, propertyName);
            elem.appendChild(child);

            child = JCR_VALUE.toXml(document);
            DomUtil.setText(child, valueStr);
            elem.appendChild(child);

            child = JCR_TYPE.toXml(document);
            DomUtil.setText(child, type);
            elem.appendChild(child);
        }
        return elem;
    }
}