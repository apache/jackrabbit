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
package org.apache.jackrabbit.spi2dav;

import javax.jcr.NamespaceException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.webdav.xml.DomUtil;
import org.apache.jackrabbit.webdav.xml.Namespace;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

final class BatchUtils {

    /**
     * The XML elements and attributes used in serialization
     */
    private static final Namespace SV_NAMESPACE = Namespace.getNamespace(Name.NS_SV_PREFIX, Name.NS_SV_URI);
    private static final String NODE_ELEMENT = "node";
    private static final String PROPERTY_ELEMENT = "property";
    private static final String VALUE_ELEMENT = "value";
    private static final String NAME_ATTRIBUTE = "name";
    private static final String TYPE_ATTRIBUTE = "type";

    private BatchUtils() {};

    static Element createNodeElement(Node parent, Name nodeName, Name primaryTypeName, String uniqueId, NamePathResolver resolver) throws NamespaceException {
        Element nodeElement = DomUtil.addChildElement(parent, NODE_ELEMENT, SV_NAMESPACE);
        String nameAttr = resolver.getJCRName(nodeName);
        DomUtil.setAttribute(nodeElement, NAME_ATTRIBUTE, SV_NAMESPACE, nameAttr);

        // nodetype must never be null
        Element propElement = DomUtil.addChildElement(nodeElement, PROPERTY_ELEMENT, SV_NAMESPACE);
        String name = resolver.getJCRName(NameConstants.JCR_PRIMARYTYPE);
        DomUtil.setAttribute(propElement, NAME_ATTRIBUTE, SV_NAMESPACE, name);
        DomUtil.setAttribute(propElement, TYPE_ATTRIBUTE, SV_NAMESPACE, PropertyType.nameFromValue(PropertyType.NAME));
        name = resolver.getJCRName(primaryTypeName);
        DomUtil.addChildElement(propElement, VALUE_ELEMENT, SV_NAMESPACE, name);
        // optional uuid
        if (uniqueId != null) {
            propElement = DomUtil.addChildElement(nodeElement, PROPERTY_ELEMENT, SV_NAMESPACE);
            name = resolver.getJCRName(NameConstants.JCR_UUID);
            DomUtil.setAttribute(propElement, NAME_ATTRIBUTE, SV_NAMESPACE, name);
            DomUtil.setAttribute(propElement, TYPE_ATTRIBUTE, SV_NAMESPACE, PropertyType.nameFromValue(PropertyType.STRING));
            DomUtil.addChildElement(propElement, VALUE_ELEMENT, SV_NAMESPACE, uniqueId);
        }
        return nodeElement;
    }

    static void importProperty(Element nodeElement, Name propertyName, int type, QValue[] values, NamePathResolver resolver) throws RepositoryException {
        Element propElement = DomUtil.addChildElement(nodeElement, PROPERTY_ELEMENT, SV_NAMESPACE);
        DomUtil.setAttribute(propElement, NAME_ATTRIBUTE, SV_NAMESPACE, resolver.getJCRName(propertyName));
        DomUtil.setAttribute(propElement, TYPE_ATTRIBUTE, SV_NAMESPACE, PropertyType.nameFromValue(type));

        // build all the values.
        for (QValue value : values) {
            DomUtil.addChildElement(propElement, VALUE_ELEMENT, SV_NAMESPACE, value.getString());
        }
    }
}