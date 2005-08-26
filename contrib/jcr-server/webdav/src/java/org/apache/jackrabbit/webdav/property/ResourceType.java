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

import java.util.HashSet;
import java.util.Set;
import java.util.ArrayList;
import java.util.List;

import org.jdom.Element;
import org.jdom.Namespace;
import org.apache.jackrabbit.webdav.version.DeltaVConstants;

/**
 * The <code>ResourceType</code> class represents the webdav resource
 * type property. The property may contain multiple resource type
 * values. Predefined resource types are those defined by RFC2518 and RFC3253:
 * <ul>
 * <li>{@link #DEFAULT_RESOURCE the empty default resource type},</li>
 * <li>'{@link #COLLECTION DAV:collection}',</li>
 * <li>'{@link #VERSION_HISTORY DAV:version-history}',</li>
 * <li>'{@link #ACTIVITY DAV:activity}',</li>
 * <li>'{@link #BASELINE DAV:baseline}',</li>
 * </ul>
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
    private static final List NAMES = new ArrayList();
    static {
        NAMES.add(null);
        NAMES.add(new TypeName(XML_COLLECTION, NAMESPACE));
        NAMES.add(new TypeName(DeltaVConstants.XML_VERSION_HISTORY, DeltaVConstants.NAMESPACE));
        NAMES.add(new TypeName(DeltaVConstants.XML_ACTIVITY, DeltaVConstants.NAMESPACE));
        NAMES.add(new TypeName(DeltaVConstants.XML_BASELINE, DeltaVConstants.NAMESPACE));
    };

    private final int[] resourceTypes;

    /**
     * Create a single-valued resource type property
     */
    public ResourceType(int resourceType) {
        this(new int[] { resourceType });
    }

    /**
     * Create a multi-valued resource type property
     */
    public ResourceType(int[] resourceTypes) {
        super(DavPropertyName.RESOURCETYPE, false);
        for (int i=0; i<resourceTypes.length; i++) {
            if (!isValidResourceType(resourceTypes[i])) {
                throw new IllegalArgumentException("Invalid resource type '"+ resourceTypes[i] +"'.");
            }
        }
	this.resourceTypes = resourceTypes;
    }

    /**
     * Return the JDOM element representation of this property
     *
     * @return a JDOM element
     */
    public Element toXml() {
        Element elem = getName().toXml();
        elem.addContent((Set)getValue());
        return elem;
    }

    /**
     * Returns the Xml representation of this property as a
     * <code>Set</code> of <code>Element</code>s.
     *
     * @return a <code>Set</code> of <code>Element</code>s
     * representing this property.
     * @see DavProperty#getValue()
     */
    public Object getValue() {
        Set elements = new HashSet();
        for (int i=0; i<resourceTypes.length; i++) {
            Element elem = resourceTypeToXml(resourceTypes[i]);
            if (elem != null) {
                elements.add(elem);
            }
        }
        return elements;
    }

    /**
     * Returns the resource types specified with the constructor.
     *
     * @return resourceTypes
     */
    public int[] getResourceTypes() {
        return resourceTypes;
    }

    /**
     * Returns the Xml representation of an individual resource type,
     * or <code>null</code> if the resource type has no Xml
     * representation (e.g. {@link #DEFAULT_RESOURCE}).<p/>{@link #getValue()} uses
     * this method to build the full set of Xml elements for the property's resource
     * types. Subclasses should override this method to add support for resource
     * types they define.
     *
     * @return Xml element representing the internal type or <code>null</code>
     * if the resource has no element name assigned (default resource type).
     */
    private static Element resourceTypeToXml(int resourceType) {
        TypeName name = (TypeName) NAMES.get(resourceType);
        return (name != null) ? new Element(name.localName, name.namespace) : null;
    }

    /**
     * Returns true if the given integer defines a valid resource type.
     *
     * @param resourceType to be validated.
     * @return true if this is one of the predefined resource types
     */
    private static boolean isValidResourceType(int resourceType) {
        if (resourceType < DEFAULT_RESOURCE || resourceType > NAMES.size()-1) {
            return false;
        }
        return true;
    }

    /**
     * Register an additional resource type
     *
     * @param name
     * @param namespace
     * @return int to be used for creation of a new <code>ResourceType</code> property
     * that contains this type.
     * @throws IllegalArgumentException if the given element is <code>null</code> or
     * if the registration fails for another reason.
     */
    public static int registerResourceType(String name, Namespace namespace) {
        if (name == null || namespace == null) {
            throw new IllegalArgumentException("Cannot register a <null> resourcetype");
        }
        TypeName tn = new TypeName(name, namespace);
        // avoid duplicate registrations
        if (NAMES.contains(tn)) {
            return NAMES.indexOf(tn);
        }
        // register new type
        if (NAMES.add(tn)) {
            return NAMES.size() - 1;
        } else {
            throw new IllegalArgumentException("Could not register resourcetype " +  namespace.getPrefix() + name);
        }
    }

    //--------------------------------------------------------< inner class >---
    /**
     * Private inner class used to register predefined and user defined resource
     * types.
     */
    private static class TypeName {

        private final String localName;
        private final Namespace namespace;
        private final int hashCode;

        private TypeName(String localName, Namespace namespace) {
            this.localName = localName;
            this.namespace = namespace;
            hashCode = ("{" + namespace.getURI() + "}" + localName).hashCode();
        }

        public int hashCode() {
            return hashCode;
        }

        public boolean equals(Object o) {
            if (o instanceof TypeName) {
                return hashCode == ((TypeName)o).hashCode;
            }
            return false;
        }
    }
}
