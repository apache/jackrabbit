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
import org.apache.jackrabbit.webdav.DavConstants;
import org.jdom.Element;

import java.util.HashSet;
import java.util.Collection;
import java.util.List;

/**
 * <code>DavPropertyNameSet</code> represents a Set of {@link DavPropertyName}
 * objects.
 */
public class DavPropertyNameSet extends HashSet {

    private static Logger log = Logger.getLogger(DavPropertyNameSet.class);

    /**
     * Create a new empty set.
     * @see HashSet()
     */
    public DavPropertyNameSet() {
        super();
    }

    /**
     * Create a new set from the given collection.
     * @param c
     * @see HashSet(Collection)
     */
    public DavPropertyNameSet(Collection c) {
        super(c);
    }

    /**
     * Create a new <code>DavPropertyNameSet</code> from the given DAV:prop
     * element.
     *
     * @param propElement
     * @throws IllegalArgumentException if the specified element is <code>null</code>
     * or is not a DAV:prop element.
     */
    public DavPropertyNameSet(Element propElement) {
        super();
        if (propElement == null || !propElement.getName().equals(DavConstants.XML_PROP)) {
            throw new IllegalArgumentException("'DAV:prop' element expected.");
        }

        // fill the set
        List props = propElement.getChildren();
        for (int j = 0; j < props.size(); j++) {
            Element prop = (Element) props.get(j);
            String propName = prop.getName();
            if (propName != null && !"".equals(propName)) {
                add(DavPropertyName.create(propName, prop.getNamespace()));
            }
        }
    }

    /**
     * Adds the specified {@link DavPropertyName} object to this
     * set if it is not already present.
     *
     * @param propertyName element to be added to this set.
     * @return <tt>true</tt> if the set did not already contain the specified
     * element.
     */
    public boolean add(DavPropertyName propertyName) {
        return super.add(propertyName);
    }

    /**
     * Add the given object to this set. In case the object is not a {@link DavPropertyName}
     * this method returns false.
     *
     * @param o
     * @return true if adding the object was successful.
     * @see #add(DavPropertyName)
     */
    public boolean add(Object o) {
        if (o instanceof DavPropertyName) {
            return add((DavPropertyName) o);
        } else {
            return false;
        }
    }
}