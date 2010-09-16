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
package org.apache.jackrabbit.webdav.simple;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.jackrabbit.server.io.DefaultIOManager;
import org.apache.jackrabbit.server.io.IOHandler;
import org.apache.jackrabbit.server.io.IOManager;
import org.apache.jackrabbit.server.io.PropertyHandler;
import org.apache.jackrabbit.server.io.PropertyManager;
import org.apache.jackrabbit.server.io.PropertyManagerImpl;
import org.apache.jackrabbit.webdav.xml.DomUtil;
import org.apache.jackrabbit.webdav.xml.ElementIterator;
import org.apache.tika.detect.Detector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * <code>ResourceConfig</code>...
 */
public class ResourceConfig {

    private static Logger log = LoggerFactory.getLogger(ResourceConfig.class);

    /**
     * Content type detector.
     */
    private final Detector detector;

    private ItemFilter itemFilter;
    private IOManager ioManager;
    private PropertyManager propManager;
    private String[] nodetypeNames = new String[0];
    private boolean collectionNames = false;

    public ResourceConfig(Detector detector) {
        this.detector = detector;
    }

    /**
     * Tries to parse the given xml configuration file.
     * The xml must match the following structure:<br>
     * <pre>
     * &lt;!ELEMENT config (iomanager, propertymanager, (collection | noncollection)?, filter?, mimetypeproperties?) &gt;
     * &lt;!ELEMENT iomanager (class, iohandler*) &gt;
     * &lt;!ELEMENT iohandler (class) &gt;
     * &lt;!ELEMENT propertymanager (class, propertyhandler*) &gt;
     * &lt;!ELEMENT propertyhandler (class) &gt;
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
     * &lt;!ELEMENT mimetypeproperties (mimemapping*, defaultmimetype) &gt;
     * &lt;!ELEMENT mimemapping &gt;
     *    &lt;!ATTLIST mimemapping
     *      extension  CDATA #REQUIRED
     *      mimetype  CDATA #REQUIRED
     *    &gt;
     * &lt;!ELEMENT defaultmimetype (CDATA) &gt;
     * </pre>
     * <p>
     * The &lt;mimetypeproperties/&gt; settings have been deprecated and will
     * be ignored with a warning. Instead you can use the
     * {@link SimpleWebdavServlet#INIT_PARAM_MIME_INFO mime-info}
     * servlet initialization parameter to customize the media type settings.
     *
     * @param configURL
     */
    public void parse(URL configURL) {
        try {
            InputStream in = configURL.openStream();
            Element config = DomUtil.parseDocument(in).getDocumentElement();

            if (config == null) {
                log.warn("Resource configuration: mandatory 'config' element is missing.");
                return;
            }

            // iomanager config entry
            Element el = DomUtil.getChildElement(config, "iomanager", null);
            if (el != null) {
                Object inst = buildClassFromConfig(el);
                if (inst != null && inst instanceof IOManager) {
                    ioManager = (IOManager)inst;
                    ioManager.setDetector(detector);
                    // get optional 'iohandler' child elements and populate the
                    // ioManager with the instances
                    ElementIterator iohElements = DomUtil.getChildren(el, "iohandler", null);
                    while (iohElements.hasNext()) {
                        Element iohEl = iohElements.nextElement();
                        inst = buildClassFromConfig(iohEl);
                        if (inst != null && inst instanceof IOHandler) {
                            ioManager.addIOHandler((IOHandler) inst);
                        } else {
                            log.warn("Resource configuration: the handler is not a valid IOHandler.");
                        }
                    }
                } else {
                    log.warn("Resource configuration: 'iomanager' does not define a valid IOManager.");
                }
            } else {
                log.warn("Resource configuration: 'iomanager' element is missing.");
            }

            // propertymanager config entry
            el = DomUtil.getChildElement(config, "propertymanager", null);
            if (el != null) {
                Object inst = buildClassFromConfig(el);
                if (inst != null && inst instanceof PropertyManager) {
                    propManager = (PropertyManager)inst;
                    // get optional 'iohandler' child elements and populate the
                    // ioManager with the instances
                    ElementIterator iohElements = DomUtil.getChildren(el, "propertyhandler", null);
                    while (iohElements.hasNext()) {
                        Element iohEl = iohElements.nextElement();
                        inst = buildClassFromConfig(iohEl);
                        if (inst != null && inst instanceof PropertyHandler) {
                            propManager.addPropertyHandler((PropertyHandler) inst);
                        } else {
                            log.warn("Resource configuration: the handler is not a valid PropertyHandler.");
                        }
                    }
                } else {
                    log.warn("Resource configuration: 'propertymanager' does not define a valid PropertyManager.");
                }
            } else {
                log.debug("Resource configuration: 'propertymanager' element is missing.");
            }

            // collection/non-collection config entry
            el = DomUtil.getChildElement(config, "collection", null);
            if (el != null) {
                nodetypeNames = parseNodeTypesEntry(el);
                collectionNames = true;
            } else if ((el = DomUtil.getChildElement(config, "noncollection", null)) != null) {
                nodetypeNames = parseNodeTypesEntry(el);
                collectionNames = false;
            }
            // todo: should check if both 'noncollection' and 'collection' are present and write a warning

            // filter config entry
            el = DomUtil.getChildElement(config, "filter", null);
            if (el != null) {
                Object inst = buildClassFromConfig(el);
                if (inst != null && inst instanceof ItemFilter) {
                    itemFilter = (ItemFilter)inst;
                }
                if (itemFilter != null) {
                    itemFilter.setFilteredNodetypes(parseNodeTypesEntry(el));
                    parseNamespacesEntry(el);
                }
            } else {
                log.debug("Resource configuration: no 'filter' element specified.");
            }

            el = DomUtil.getChildElement(config, "mimetypeproperties", null);
            if (el != null) {
                log.warn("Ignoring deprecated mimetypeproperties settings: {}",
                        configURL);
            }
        } catch (IOException e) {
            log.debug("Invalid resource configuration: " + e.getMessage());
        } catch (ParserConfigurationException e) {
            log.warn("Failed to parse resource configuration: " + e.getMessage());
        } catch (SAXException e) {
            log.warn("Failed to parse resource configuration: " + e.getMessage());
        }
    }

    private Object buildClassFromConfig(Element parent) {
        Object instance = null;
        Element classElem = DomUtil.getChildElement(parent, "class", null);
        if (classElem != null) {
            // contains a 'class' child node
            try {
                String className = DomUtil.getAttribute(classElem, "name", null);
                if (className != null) {
                    Class<?> c = Class.forName(className);
                    instance = c.newInstance();
                } else {
                    log.error("Invalid configuration: missing 'class' element");
                }
            } catch (Exception e) {
                log.error("Error while create class instance: " + e.getMessage());
            }
        }
        return instance;
    }

    private void parseNamespacesEntry(Element parent) {
        Element namespaces = DomUtil.getChildElement(parent, "namespaces", null);
        if (namespaces != null) {
            List<String> l = new ArrayList<String>();
            // retrieve prefix child elements
            ElementIterator it = DomUtil.getChildren(namespaces, "prefix", null);
            while (it.hasNext()) {
                Element e = it.nextElement();
                l.add(DomUtil.getText(e));
            }
            String[] prefixes = l.toArray(new String[l.size()]);
            l.clear();
            // retrieve uri child elements
            it = DomUtil.getChildren(namespaces, "uri", null);
            while (it.hasNext()) {
                Element e = it.nextElement();
                l.add(DomUtil.getText(e));
            }
            String[] uris = l.toArray(new String[l.size()]);
            itemFilter.setFilteredPrefixes(prefixes);
            itemFilter.setFilteredURIs(uris);
        }
    }

    private String[] parseNodeTypesEntry(Element parent) {
        String[] ntNames;
        Element nodetypes = DomUtil.getChildElement(parent, "nodetypes", null);
        if (nodetypes != null) {
            List<String> l = new ArrayList<String>();
            ElementIterator it = DomUtil.getChildren(nodetypes, "nodetype", null);
            while (it.hasNext()) {
                Element e = it.nextElement();
                l.add(DomUtil.getText(e));
            }
            ntNames = l.toArray(new String[l.size()]);
        } else {
            ntNames = new String[0];
        }
        return ntNames;
    }

    /**
     *
     * @return
     */
    public IOManager getIOManager() {
        if (ioManager == null) {
            log.debug("ResourceConfig: missing io-manager > building DefaultIOManager ");
            ioManager = new DefaultIOManager();
            ioManager.setDetector(detector);
        }
        return ioManager;
    }

    /**
     *
     * @return
     */
    public PropertyManager getPropertyManager() {
        if (propManager == null) {
            log.debug("ResourceConfig: missing property-manager > building default.");
            propManager = PropertyManagerImpl.getDefaultManager();
        }
        return propManager;
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
     * Returns the item filter specified with the configuration or {@link DefaultItemFilter}
     * if the configuration was missing the corresponding entry or the parser failed
     * to build a <code>ItemFilter</code> instance from the configuration.
     *
     * @return item filter as defined by the config or {@link DefaultItemFilter}
     */
    public ItemFilter getItemFilter() {
        if (itemFilter == null) {
            log.debug("ResourceConfig: missing resource filter > building DefaultItemFilter ");
            itemFilter = new DefaultItemFilter();
        }
        return itemFilter;
    }

    /**
     * Returns the configured content type detector.
     *
     * @return content type detector
     */
    public Detector getDetector() {
        return detector;
    }

}
