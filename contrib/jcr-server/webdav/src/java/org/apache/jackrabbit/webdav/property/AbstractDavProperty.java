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
import org.jdom.Element;

import java.util.Arrays;
import java.util.List;

/**
 * <code>AbstractDavProperty</code> provides generic METHODS used by various
 * implementations of the {@link DavProperty} interface.
 */
public abstract class AbstractDavProperty implements DavProperty {

    private static Logger log = Logger.getLogger(AbstractDavProperty.class);

    private final DavPropertyName name;
    private final boolean isProtected;

    /**
     * Create a new <code>AbstractDavProperty</code> with the given {@link DavPropertyName}
     * and a boolean flag indicating whether this property is protected.
     * 
     * @param name
     * @param isProtected
     */
    public AbstractDavProperty(DavPropertyName name, boolean isProtected) {
        this.name = name;
        this.isProtected = isProtected;
    }

    /**
     * Computes the hash code using this propertys name and value.
     *
     * @return the hash code
     */
    public int hashCode() {
        int hashCode = getName().hashCode();
        if (getValue() != null) {
            hashCode += getValue().hashCode();
        }
        return hashCode % Integer.MAX_VALUE;
    }

    /**
     * Checks if this property has the same {@link DavPropertyName name}
     * and value as the given one.
     *
     * @param obj the object to compare to
     * @return <code>true</code> if the 2 objects are equal;
     *         <code>false</code> otherwise
     */
    public boolean equals(Object obj) {
        if (obj instanceof DavProperty) {
            DavProperty prop = (DavProperty) obj;
            boolean equalName = getName().equals(prop.getName());
            boolean equalValue = (getValue() == null) ? prop.getValue() == null : getValue().equals(prop.getValue());
            return equalName && equalValue;
        }
        return false;
    }


    /**
     * Return a JDOM element representation of this property. The value of the
     * property will be added as text or as child element.
     * <pre>
     * new DavProperty("displayname", "WebDAV Directory").toXml()
     * gives a element like:
     * &lt;D:displayname&gt;WebDAV Directory&lt;/D:displayname&gt;
     *
     * new DavProperty("resourcetype", new Element("collection")).toXml()
     * gives a element like:
     * &lt;D:resourcetype&gt;&lt;D:collection/&gt;&lt;/D:resourcetype&gt;
     *
     * Element[] customVals = { new Element("bla", customNamespace), new Element("bli", customNamespace) };
     * new DavProperty("custom-property", customVals, customNamespace).toXml()
     * gives an element like
     * &lt;Z:custom-property&gt;
     *    &lt;Z:bla/&gt;
     *    &lt;Z:bli/&gt;
     * &lt;/Z:custom-property&gt;
     * </pre>
     *
     * @return a JDOM element of this property
     * @see DavProperty#toXml()
     */
    public Element toXml() {
	Element elem = getName().toXml();
        Object value = getValue();
	if (value != null) {
	    if (value instanceof Element) {
		elem.addContent((Element) value);
	    } else if (value instanceof Element[]) {
                elem.addContent(Arrays.asList((Element[])value));
            } else if (value instanceof List) {
                elem.addContent((List)value);
            } else {
                elem.setText(value.toString());
	    }
	}
	return elem;
    }

    /**
     * Returns the name of this property.
     *
     * @return name
     * @see DavProperty#getName()
     */
    public DavPropertyName getName() {
        return name;
    }

    /**
     * Returns true if this property is protected or computed.
     *
     * @return true if this is a protected (or computed) property.
     * @see org.apache.jackrabbit.webdav.property.DavProperty#isProtected()
     */
    public boolean isProtected() {
        return isProtected;
    }
}