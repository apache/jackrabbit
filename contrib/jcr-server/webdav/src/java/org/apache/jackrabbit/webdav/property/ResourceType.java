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

import org.jdom.Element;

/**
 * The <code>ResourceType</code> class represents the webdav resource
 * type property. Valid resource types are '{@link #COLLECTION collection}',
 * {@link #DEFAULT_RESOURCE}.
 */
public class ResourceType extends AbstractDavProperty {

    /**
     * The default resource type
     */
    public static final int DEFAULT_RESOURCE = 0;

    /**
     * The collection resource type
     */
    public static final int COLLECTION = DEFAULT_RESOURCE + 1;

    private int resourceType = DEFAULT_RESOURCE;

    /**
     * Create a resource type property
     */
    public ResourceType(int resourceType) {
        super(DavPropertyName.RESOURCETYPE, false);
	if (!isValidResourceType(resourceType)) {
           throw new IllegalArgumentException("Invalid resource type '"+ resourceType +"'.");
        }
	this.resourceType = resourceType;
    }

    /**
     * Return the JDOM element representation of this resource type
     *
     * @return a JDOM element
     */
    public Element toXml() {
        Element elem = getName().toXml();
        if (getValue() != null) {
            elem.addContent((Element)getValue());
        }
        return elem;
    }

    /**
     * Returns the Xml representation of this resource type.
     *
     * @return Xml representation of this resource type.
     * @see DavProperty#getValue()
     */
    public Object getValue() {
        return (resourceType == COLLECTION) ? new Element(XML_COLLECTION, NAMESPACE) : null;
    }

    /**
     * Returns the resource type specified with the constructor.
     *
     * @return resourceType
     */
    public int getResourceType() {
        return resourceType;
    }

    /**
     * Validates the specified resourceType.
     *
     * @param resourceType
     * @return true if the specified resourceType is valid.
     */
    public boolean isValidResourceType(int resourceType) {
        if (resourceType < DEFAULT_RESOURCE || resourceType > COLLECTION) {
	    return false;
	}
        return true;
    }
}
