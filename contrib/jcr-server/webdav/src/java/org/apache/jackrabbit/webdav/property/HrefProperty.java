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
package org.apache.jackrabbit.webdav.property;

import org.apache.log4j.Logger;
import org.apache.jackrabbit.webdav.util.XmlUtil;
import org.jdom.Element;

import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * <code>HrefProperty</code> is an extension to the common {@link DavProperty}.
 * The String representation of the property value is always displayed as text
 * inside an extra 'href' element. If the value is a String array each array
 * element is added as text to a separate 'href' element.
 *
 * @see org.apache.jackrabbit.webdav.DavConstants#XML_HREF
 * @see org.apache.jackrabbit.webdav.property.DavProperty#getValue()
 */
public class HrefProperty extends AbstractDavProperty {

    private static Logger log = Logger.getLogger(HrefProperty.class);

    private final String[] value;

    /**
     * Creates a new WebDAV property with the given <code>DavPropertyName</code>
     *
     * @param name the name of the property
     * @param value the value of the property
     * @param isProtected A value of true, defines this property to be protected.
     * It will not be returned in a {@link org.apache.jackrabbit.webdav.DavConstants#PROPFIND_ALL_PROP DAV:allprop}
     * PROPFIND request and cannot be set/removed with a PROPPATCH request.
     */
    public HrefProperty(DavPropertyName name, String value, boolean isProtected) {
        super(name, isProtected);
        this.value = new String[]{value};
    }

    /**
     * Creates a new WebDAV property with the given <code>DavPropertyName</code>
     *
     * @param name the name of the property
     * @param value the value of the property
     * @param isProtected A value of true, defines this property to be protected.
     * It will not be returned in a {@link org.apache.jackrabbit.webdav.DavConstants#PROPFIND_ALL_PROP DAV:allprop}
     * PROPFIND request and cannot be set/removed with a PROPPATCH request.
     */
    public HrefProperty(DavPropertyName name, String[] value, boolean isProtected) {
        super(name, isProtected);
        this.value = value;
    }

    /**
     * Create a new <code>HrefProperty</code> from the specified property.
     * Please note, that the property must have a <code>List</code> value
     * object, consisting of {@link #XML_HREF href} <code>Element</code> entries.
     *
     * @param prop
     * @throws IllegalArgumentException if the property {@link DavProperty#getValue() value}
     * is not a <code>List</code>.
     */
    public HrefProperty(DavProperty prop) {
        super(prop.getName(), prop.isProtected());
        if (! (prop.getValue() instanceof List)) {
            throw new IllegalArgumentException("Expected a property with a List value object.");
        }
        Iterator it = ((List)prop.getValue()).iterator();
        ArrayList hrefList = new ArrayList();
        while (it.hasNext()) {
            Object o = it.next();
            if (o instanceof Element) {
                String href = ((Element)o).getChildText(XML_HREF, NAMESPACE);
                if (href != null) {
                    hrefList.add(href);
                } else {
                    log.warn("Valid DAV:href element expected instead of " + o.toString());
                }
            } else {
                log.warn("DAV: href element expected in the content of " + getName().toString());
            }
        }
        value = (String[]) hrefList.toArray(new String[hrefList.size()]);
    }

    /**
     * Returns an Xml element with the following form:
     * <pre>
     * &lt;Z:name&gt;
     *    &lt;DAV:href&gt;value&lt;/DAV:href/&gt;
     * &lt;/Z:name&gt;
     * </pre>
     * where Z: represents the prefix of the namespace defined with the initial
     * webdav property name.
     *
     * @return Xml representation
     * @see XmlUtil#hrefToXml(String)
     */
    public Element toXml() {
        Element elem = getName().toXml();
        Object value = getValue();
        if (value != null) {
            if (value instanceof String[]) {
                String[] hrefs = (String[]) value;
                for (int i = 0; i < hrefs.length; i++) {
                    elem.addContent(XmlUtil.hrefToXml(hrefs[i]));
                }
            } else {
                elem.addContent(XmlUtil.hrefToXml(value.toString()));
            }
        }
        return elem;
    }

    /**
     * Returns an array of String.
     *
     * @return an array of String.
     * @see DavProperty#getValue() 
     */
    public Object getValue() {
        return value;
    }

    /**
     * Return an array of String containg the text of those DAV:href elements
     * that would be returned as child elements of this property on {@link #toXml()}
     *
     * @return
     */
    public List getHrefs() {
        return Arrays.asList(value);
    }
}