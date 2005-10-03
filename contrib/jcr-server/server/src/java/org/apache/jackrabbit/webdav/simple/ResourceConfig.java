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

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.net.URL;
import java.util.List;
import java.util.Iterator;

/**
 * <code>ResourceConfig</code>...
 */
public class ResourceConfig {

    private static Logger log = Logger.getLogger(ResourceConfig.class);

    private ResourceFilter resourceFilter;
    private String[] nodetypeNames = new String[0];
    private boolean collectionNames = false;

    /**
     * Tries to parse the given xml configuration file.
     * The xml must match the following structure:<br>
     * <pre>
     * &lt;!ELEMENT config ((collection | noncollection)?, filter?) &gt;
     * &lt;!ELEMENT collection (nodetypes) &gt;
     * &lt;!ELEMENT noncollection (nodetypes) &gt;
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
     * @param configURL
     */
    public void parse(URL configURL) {
        try {
            Document doc = new SAXBuilder().build(configURL);
            Element root = doc.getRootElement();

            Element collection = root.getChild("collection");
            Element noncollection = root.getChild("noncollection");
            if (collection != null && noncollection != null) {
                log.warn("Resource configuration may only contain a collection OR a noncollection element -> entries are ignored");
            } else {
                if (collection != null) {
                    Element nts = collection.getChild("nodetypes");
                    nodetypeNames = parseNodeTypesEntry(nts);
                    collectionNames = true;
                } else if (noncollection != null) {
                    Element nts = noncollection.getChild("nodetypes");
                    nodetypeNames = parseNodeTypesEntry(nts);
                    collectionNames = false;
                }
            }

            Element filter = root.getChild("filter");
            if (filter != null) {
                resourceFilter = buildResourceFilter(filter.getChild("class"));
                if (resourceFilter != null) {
                    Element nts = filter.getChild("nodetypes");
                    resourceFilter.setFilteredNodetypes(parseNodeTypesEntry(nts));
                    parseNamespacesEntry(filter.getChild("namespaces"), resourceFilter);
                }
            } else {
                log.debug("Resource configuration: no 'filter' element specified.");
            }
        } catch (Exception e) {
            log.warn("Error while reading resource configuration: " + e.getMessage());
        }
    }

    private ResourceFilter buildResourceFilter(Element classElement) {
        if (classElement == null) {
            return null;
        }
        try {
            String className = classElement.getAttributeValue("name");
            if (className != null) {
                Class cl = Class.forName(className);
                Class[] interfaces = cl.getInterfaces();
                boolean isfilterClass = false;
                for (int i = 0; i < interfaces.length && !isfilterClass; i++) {
                    isfilterClass = (interfaces[i].equals(ResourceFilter.class));
                }
                if (isfilterClass) {
                    return (ResourceFilter) cl.newInstance();
                } else {
                    log.warn("Class '" + className + "' specified does not represent a resource filter > using default.");
                }
            } else {
                log.warn("Invalid filter configuration: missing 'class' element");
            }
        } catch (Exception e) {
            log.warn("Error while reading filter configuration. Using empty filter instead.");
        }
        return null;
    }

    private void parseNamespacesEntry(Element child, ResourceFilter filter) {
        if (child == null) {
            return;
        }
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

    private String[] parseNodeTypesEntry(Element child) {
        if (child == null) {
            return new String[0];
        }
        List l = child.getChildren("nodetype");
        Iterator it = l.iterator();
        String[] ntNames = new String[l.size()];
        int i = 0;
        while(it.hasNext()) {
            ntNames[i++] = ((Element) it.next()).getText();
        }
        return ntNames;
    }

    /**
     * Returns true, if the given item represents a {@link Node node} that is
     * either any of the nodetypes specified to represent a collection or
     * none of the nodetypes specified to represent a non-collection, respectively.
     * If no valid configuration entry is present, this method returns true
     * for node items. For items which are not a node, this method always
     * returns false.
     *
     * @param item
     * @return true if the given item is a node that represents a webdav
     * collection, false otherwise.
     */
    public boolean isCollectionResource(Item item) {
        if (item.isNode()) {
            boolean isCollection = true;
            Node n = (Node)item;
            try {
                for (int i = 0; i < nodetypeNames.length && isCollection; i++) {
                    isCollection = collectionNames ? n.isNodeType(nodetypeNames[i]) : !n.isNodeType(nodetypeNames[i]);
                }
            } catch (RepositoryException e) {
                log.warn(e.getMessage());
            }
            return isCollection;
        } else {
            return false;
        }
    }

    /**
     * Returns the resource filter specified with the configuration or {@link DefaultResourceFilter}
     * if the configuration was missing the corresponding entry or the parser failed
     * to build a <code>ResourceFilter</code> instance from the configuration.
     *
     * @return resource filter as defined by the config or {@link DefaultResourceFilter}
     */
    public ResourceFilter getResourceFilter() {
        if (resourceFilter == null) {
            log.debug("ResourceConfig: missing resource filter > building DefaultResourceFilter ");
            resourceFilter = new DefaultResourceFilter();
        }
        return resourceFilter;
    }
}