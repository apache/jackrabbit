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
package org.apache.jackrabbit.webdav.version;

import org.apache.log4j.Logger;
import org.jdom.Element;

/**
 * The <code>ResourceType</code> extends the {@link org.apache.jackrabbit.webdav.property.ResourceType}
 * by DeltaV specific types.
 */
public class ResourceType extends org.apache.jackrabbit.webdav.property.ResourceType
        implements DeltaVConstants {

    private static Logger log = Logger.getLogger(ResourceType.class);

    /**
     * The version-history resource type
     */
    public static final int VERSION_HISTORY = COLLECTION + 1;

    /**
     * The activity resource type
     */
    public static final int ACTIVITY = VERSION_HISTORY + 1;

    /**
     * The baseline resource type
     */
    public static final int BASELINE = ACTIVITY + 1;

    /**
     * Array containing all possible resourcetype elements
     */
    private static final String[] ELEMENT_NAMES = {
        null,
        XML_COLLECTION,
        XML_VERSION_HISTORY,
        XML_ACTIVITY,
        XML_BASELINE
    };


    /**
     * Create a resource type property
     * 
     * @param resourceType
     */
    public ResourceType(int resourceType) {
        super(resourceType);
    }

    /**
     * Return the resource type as Xml element.
     *
     * @return Xml element representing the internal type or <code>null</code>
     * if the resource has no element name assigned (default resource type).
     */
    public Object getValue() {
        String name = ELEMENT_NAMES[getResourceType()];
        return (name != null) ? new Element(name, DeltaVConstants.NAMESPACE) : null;
    }

    /**
     * Returns true if the given integer defines a valid resource type.
     *
     * @param resourceType to be validated.
     * @return true if this is a known resource type.
     */
    public boolean isValidResourceType(int resourceType) {
        if (resourceType < DEFAULT_RESOURCE || resourceType > BASELINE) {
            return false;
        }
        return true;
    }
}