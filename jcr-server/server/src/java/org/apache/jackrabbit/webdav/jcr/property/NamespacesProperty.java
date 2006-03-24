/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
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
package org.apache.jackrabbit.webdav.jcr.property;

import org.apache.log4j.Logger;
import org.apache.jackrabbit.webdav.property.AbstractDavProperty;
import org.apache.jackrabbit.webdav.property.DavProperty;
import org.apache.jackrabbit.webdav.jcr.ItemResourceConstants;
import org.apache.jackrabbit.webdav.xml.DomUtil;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.w3c.dom.Element;
import org.w3c.dom.Document;

import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;
import java.util.Properties;
import java.util.List;
import java.util.Iterator;
import java.util.Enumeration;

/**
 * <code>NamespacesProperty</code>...
 */
public class NamespacesProperty extends AbstractDavProperty implements ItemResourceConstants {

    private static Logger log = Logger.getLogger(NamespacesProperty.class);

    private final Properties value;

    public NamespacesProperty(NamespaceRegistry nsReg) throws RepositoryException {
        super(JCR_NAMESPACES, false);
        String[] prefixes = nsReg.getPrefixes();
        value = new Properties();
        for (int i = 0; i < prefixes.length; i++) {
            value.setProperty(prefixes[i], nsReg.getURI(prefixes[i]));
        }
    }

    public NamespacesProperty(DavProperty property) throws DavException {
        super(JCR_NAMESPACES, false);
        Object v = property.getValue();
        if (!(v instanceof List)) {
            log.warn("Unexpected structure of dcr:namespace property.");
            throw new DavException(DavServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
        value = new Properties();
        // retrieve list of prefix/uri pairs that build the new values of
        // the ns-registry
        Iterator it = ((List)v).iterator();
        while (it.hasNext()) {
            Object listEntry = it.next();
            if (listEntry instanceof Element) {
                Element e = (Element)listEntry;
                if (XML_NAMESPACE.equals(e.getLocalName())) {
                    String prefix = DomUtil.getChildText(e, XML_PREFIX, ItemResourceConstants.NAMESPACE);
                    String uri = DomUtil.getChildText(e, XML_URI, ItemResourceConstants.NAMESPACE);
                    value.setProperty(prefix, uri);
                }
            }
        }
    }

    public Properties getNamespaces() {
        return value;
    }

    public Object getValue() {
        return value;
    }

    /**
     * @see org.apache.jackrabbit.webdav.xml.XmlSerializable#toXml(Document)
     */
    public Element toXml(Document document) {
        Element elem = getName().toXml(document);
        Enumeration prefixes = value.propertyNames();
        while (prefixes.hasMoreElements()) {
            String prefix = (String)prefixes.nextElement();
            String uri = value.getProperty(prefix);
            Element nsElem = DomUtil.addChildElement(elem, XML_NAMESPACE, ItemResourceConstants.NAMESPACE);
            DomUtil.addChildElement(nsElem, XML_PREFIX, ItemResourceConstants.NAMESPACE, prefix);
            DomUtil.addChildElement(nsElem, XML_URI, ItemResourceConstants.NAMESPACE, uri);
        }
        return elem;
    }

}