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
package org.apache.jackrabbit.webdav.jcr.property;

import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.jcr.ItemResourceConstants;
import org.apache.jackrabbit.webdav.property.AbstractDavProperty;
import org.apache.jackrabbit.webdav.property.DavProperty;
import org.apache.jackrabbit.webdav.xml.DomUtil;
import org.apache.jackrabbit.webdav.xml.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

/**
 * <code>NamespacesProperty</code>...
 */
public class NamespacesProperty extends AbstractDavProperty<Map<String, String>> implements ItemResourceConstants {

    private static Logger log = LoggerFactory.getLogger(NamespacesProperty.class);

    private final Map<String, String> value = new HashMap<String, String>();

    public NamespacesProperty(NamespaceRegistry nsReg) throws RepositoryException {
        super(JCR_NAMESPACES, false);
        if (nsReg != null) {
            for (String prefix : nsReg.getPrefixes()) {
                value.put(prefix, nsReg.getURI(prefix));
            }
        }
    }

    public NamespacesProperty(Map<String, String> namespaces) {
        super(JCR_NAMESPACES, false);
        value.putAll(namespaces);
    }

    public NamespacesProperty(DavProperty<?> property) throws DavException {
        super(JCR_NAMESPACES, false);
        Object v = property.getValue();
        if (!(v instanceof List)) {
            log.warn("Unexpected structure of dcr:namespace property.");
            throw new DavException(DavServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
        // retrieve list of prefix/uri pairs that build the new values of
        // the ns-registry
        for (Object listEntry : (List<?>) v) {
            if (listEntry instanceof Element) {
                Element e = (Element)listEntry;
                if (XML_NAMESPACE.equals(e.getLocalName())) {
                    Element pElem = DomUtil.getChildElement(e, XML_PREFIX, ItemResourceConstants.NAMESPACE);
                    String prefix = DomUtil.getText(pElem, Namespace.EMPTY_NAMESPACE.getPrefix());
                    Element uElem = DomUtil.getChildElement(e, XML_URI, ItemResourceConstants.NAMESPACE);
                    String uri = DomUtil.getText(uElem, Namespace.EMPTY_NAMESPACE.getURI());
                    value.put(prefix, uri);
                }
            }
        }
    }

    public Map<String, String> getNamespaces() {
        return Collections.unmodifiableMap(value);
    }

    public Map<String, String> getValue() {
        return Collections.unmodifiableMap(value);
    }

    /**
     * @see org.apache.jackrabbit.webdav.xml.XmlSerializable#toXml(Document)
     */
    @Override
    public Element toXml(Document document) {
        Element elem = getName().toXml(document);
        for (String prefix : value.keySet()) {
            String uri = value.get(prefix);
            Element nsElem = DomUtil.addChildElement(elem, XML_NAMESPACE, ItemResourceConstants.NAMESPACE);
            DomUtil.addChildElement(nsElem, XML_PREFIX, ItemResourceConstants.NAMESPACE, prefix);
            DomUtil.addChildElement(nsElem, XML_URI, ItemResourceConstants.NAMESPACE, uri);
        }
        return elem;
    }

}
