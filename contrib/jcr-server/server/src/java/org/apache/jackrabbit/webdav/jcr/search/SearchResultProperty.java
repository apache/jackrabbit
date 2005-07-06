/*
 * Copyright 2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
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
import org.apache.jackrabbit.value.ValueHelper;
import org.jdom.Element;

import javax.jcr.Value;
import javax.jcr.RepositoryException;
import javax.jcr.PropertyType;
import javax.jcr.ValueFactory;
import java.util.*;

/**
 * <code>SearchResultProperty</code>...
 */
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
    public SearchResultProperty(ValueFactory fac, DavProperty property) throws RepositoryException {
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
                    String txt = ((Element)el).getText();
                    if (JCR_NAME.getName().equals(((Element)el).getName())) {
                        name = txt;
                    } else if (JCR_VALUE.getName().equals(((Element)el).getName())) {
                        value = txt;
                    } else if (JCR_TYPE.getName().equals(((Element)el).getName())) {
                        int type = PropertyType.valueFromName(txt);
                        if (name == null) {
                            throw new IllegalArgumentException("SearchResultProperty requires a set of 'jcr:name','jcr:value' and 'jcr:type' xml elements.");
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
            new IllegalArgumentException("SearchResultProperty requires a set of 'jcr:name','jcr:value' and 'jcr:type' xml elements.");
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
     * Return the value of this webdav property i.e. an list of xml
     * {@link Element}s. For every value in the query result row represented by
     * this webdav property a jcr:name, jcr:value and jcr:type element is created.
     * Example:
     * <pre>
     * -----------------------------------------------------------
     *   col-name  |   bla   |   bli   |  jcr:path  |  jcr:score
     * -----------------------------------------------------------
     *   value     |   xxx   |   111   |  /aNode    |    1
     *   type      |    1    |    3    |     8      |    3
     * -----------------------------------------------------------
     * </pre>
     * results in:
     * <pre>
     * &lt;jcr:name&gt;bla&lt;jcr:name/&gt;
     * &lt;jcr:value&gt;xxx&lt;jcr:value/&gt;
     * &lt;jcr:type&gt;String&lt;jcr:value/&gt;
     * &lt;jcr:name&gt;bli&lt;jcr:name/&gt;
     * &lt;jcr:value&gt;111&lt;jcr:value/&gt;
     * &lt;jcr:type&gt;Long&lt;jcr:value/&gt;
     * &lt;jcr:name&gt;jcr:path&lt;jcr:name/&gt;
     * &lt;jcr:value&gt;/aNode&lt;jcr:value/&gt;
     * &lt;jcr:type&gt;Path&lt;jcr:value/&gt;
     * &lt;jcr:name&gt;jcr:score&lt;jcr:name/&gt;
     * &lt;jcr:value&gt;1&lt;jcr:value/&gt;
     * &lt;jcr:type&gt;Long&lt;jcr:value/&gt;
     * </pre>
     *
     * @return value of this webdav property consisting of an list of xml elements.
     * @see org.apache.jackrabbit.webdav.property.DavProperty#getValue()
     */
    public Object getValue() {
        List value = new ArrayList();
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

            value.add(JCR_NAME.toXml().setText(propertyName));
            value.add(JCR_VALUE.toXml().setText(valueStr));
            value.add(JCR_TYPE.toXml().setText(type));
        }
        return value;
    }
}