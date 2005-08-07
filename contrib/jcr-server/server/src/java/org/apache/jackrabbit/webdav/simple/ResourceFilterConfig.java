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
package org.apache.jackrabbit.webdav.simple;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

import java.net.URL;
import java.util.List;
import java.util.Iterator;

/**
 * <code>ResourceFilterConfig</code>...
 */
public class ResourceFilterConfig {

    private static Logger log = Logger.getLogger(ResourceFilterConfig.class);

    /**
     * Tries to parse the given xml configuration file. If the parser fails,
     * <code>null</code> is returned.<p/>
     * The xml must match the following structure:<br>
     * <pre>
     * &lt;!ELEMENT filter (class, namespaces?, nodetypes?) &gt;
     * &lt;!ELEMENT class &gt;
     *    &lt;!ATTLIST class
     *      name  CDATA #REQUIRED
     *    &gt;
     * &lt;!ELEMENT namespaces (prefix|uri)* &gt;
     * &lt;!ELEMENT prefix (CDATA) &gt;
     * &lt;!ELEMENT uri (CDATA) &gt;
     * &lt;!ELEMENT nodetypes (nodetype)* &gt;
     * &lt;!ELEMENT nodetype (CDATA) &gt;
     * </pre>
     *
     * @param filterURL
     * @return resource filter retrieved from xml configuration or <code>null</code>
     * if the parser fails.
     */
    public ResourceFilter parse(URL filterURL) {
        ResourceFilter filter = null;
        try {
            String className = null;
            Element nts = null;
            Element ns = null;

            Document doc = new SAXBuilder().build(filterURL);
            Element root = doc.getRootElement();
            Iterator elemIter = root.getChildren().iterator();
            while (elemIter.hasNext()) {
                Element child = (Element) elemIter.next();
                String childName = child.getName();
                if ("class".equals(childName)) {
                    className = child.getAttributeValue("name");
                } else if ("nodetypes".equals(childName)) {
                    nts = child;
                } else if ("namespaces".equals(childName)) {
                    ns = child;
                }
            }

            if (className != null) {
                Class cl = Class.forName(className);
                Class[] interfaces = cl.getInterfaces();
                boolean isfilterClass = false;
                for (int i = 0; i < interfaces.length && !isfilterClass; i++) {
                    isfilterClass = (interfaces[i].equals(ResourceFilter.class));
                }
                if (isfilterClass) {
                    filter = (ResourceFilter) cl.newInstance();
                    if (ns != null) {
                        parseNamespacesEntry(ns, filter);
                    }
                    if (nts != null) {
                        parseNodeTypesEntry(nts, filter);
                    }
                } else {
                    log.warn("Class '" + className + "' specified does not represent a resource filter > using default.");
                }
            } else {
                log.warn("Invalid filter configuration: missing 'class' element");
            }
        } catch (Exception e) {
            log.warn("Error while reading filter configuration. Using empty filter instead.");
        }
        return filter;
    }

    private void parseNamespacesEntry(Element child, ResourceFilter filter) {
        List l = child.getChildren("prefix");
        Iterator it = l.iterator();
        String[] prefixes = new String[l.size()];
        int i = 0;
        while(it.hasNext()) {
            prefixes[i++] = ((Element) it.next()).getText();
        }

        l = child.getChildren("uri");
        it = l.iterator();
        String[] uris = new String[l.size()];
        i = 0;
        while(it.hasNext()) {
            uris[i++] = ((Element) it.next()).getText();
        }

        filter.setFilteredPrefixes(prefixes);
        filter.setFilteredURIs(uris);
    }

    private void parseNodeTypesEntry(Element child, ResourceFilter filter) {
        List l = child.getChildren("nodetype");
        Iterator it = l.iterator();
        String[] ntNames = new String[l.size()];
        int i = 0;
        while(it.hasNext()) {
            ntNames[i++] = ((Element) it.next()).getText();
        }
        filter.setFilteredNodetypes(ntNames);
    }
}