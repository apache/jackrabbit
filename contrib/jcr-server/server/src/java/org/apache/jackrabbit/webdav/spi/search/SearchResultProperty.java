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
package org.apache.jackrabbit.webdav.spi.search;

import org.apache.log4j.Logger;
import org.apache.jackrabbit.webdav.property.DavPropertyName;
import org.apache.jackrabbit.webdav.property.DefaultDavProperty;
import org.apache.jackrabbit.webdav.spi.ItemResourceConstants;
import org.jdom.Element;

import javax.jcr.Value;
import javax.jcr.RepositoryException;

/**
 * <code>SearchResultProperty</code>...
 */
public class SearchResultProperty extends DefaultDavProperty {

    private static Logger log = Logger.getLogger(SearchResultProperty.class);

    public static final DavPropertyName SEARCH_RESULT_PROPERTY = DavPropertyName.create("search-result-property", ItemResourceConstants.NAMESPACE);

    private final String propertyName;

    /**
     * Creates a new WebDAV property with the given namespace, name and value.
     * If the property is intended to be protected the isProtected flag must
     * be set to true.
     *
     * @param propertyName JCR property name
     * @param value String representation of the property
     */
    public SearchResultProperty(String propertyName, Value value) {
        super(SEARCH_RESULT_PROPERTY, value, true);
        this.propertyName = propertyName;
    }

    /**
     * Return the Xml representation.
     * <pre>
     *
     * new SearchResultProperty("bli", new StringValue("blivalue")).toXml()
     *
     * returns an element with the following form:
     *
     * &lt;jcr:search-result-property&gt;
     *    &lt;jcr:name&gt;bli&lt;jcr:name/&gt;
     *    &lt;jcr:value&gt;blivalue&lt;jcr:value/&gt;
     * &lt;/jcr:search-result-property&gt;
     * </pre>
     *
     * @return the Xml representation
     * @see org.apache.jackrabbit.webdav.property.DavProperty#toXml()
     */
    public Element toXml() {
	Element elem = getName().toXml();
        elem.addContent(ItemResourceConstants.JCR_NAME.toXml().setText(propertyName));
        String valueStr = "";
        if (getValue() != null) {
            Value value = (Value) getValue();
            try {
                valueStr = value.getString();
            } catch (RepositoryException e) {
                log.error(e.getMessage());
            }
        }
        elem.addContent(ItemResourceConstants.JCR_VALUE.toXml().setText(valueStr));
	return elem;
    }
}