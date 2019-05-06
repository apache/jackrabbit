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
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.jackrabbit.server.io.CopyMoveHandler;
import org.apache.jackrabbit.server.io.CopyMoveManager;
import org.apache.jackrabbit.server.io.CopyMoveManagerImpl;
import org.apache.jackrabbit.server.io.DefaultIOManager;
import org.apache.jackrabbit.server.io.DeleteManager;
import org.apache.jackrabbit.server.io.DeleteManagerImpl;
import org.apache.jackrabbit.server.io.IOHandler;
import org.apache.jackrabbit.server.io.IOManager;
import org.apache.jackrabbit.server.io.PropertyHandler;
import org.apache.jackrabbit.server.io.PropertyManager;
import org.apache.jackrabbit.server.io.PropertyManagerImpl;
import org.apache.jackrabbit.webdav.xml.DomUtil;
import org.apache.jackrabbit.webdav.xml.ElementIterator;
import org.apache.jackrabbit.webdav.xml.Namespace;
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

    private static final String ELEMENT_IOMANAGER = "iomanager";
    private static final String ELEMENT_IOHANDLER = "iohandler";

    private static final String ELEMENT_PROPERTYMANAGER = "propertymanager";
    private static final String ELEMENT_PROPERTYHANDLER = "propertyhandler";

    private static final String ELEMENT_COPYMOVEMANAGER = "copymovemanager";
    private static final String ELEMENT_COPYMOVEHANDLER = "copymovehandler";

    private static final String ELEMENT_CLASS = "class";

    private static final String ELEMENT_PARAM = "param";
    private static final String ATTR_NAME = "name";
    private static final String ATTR_VALUE = "value";

    /**
     * Content type detector.
     */
    private final Detector detector;

    private ItemFilter itemFilter;
    private IOManager ioManager;
    private CopyMoveManager cmManager;
    private PropertyManager propManager;
    private DeleteManager deleteManager;
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
            parse(configURL.openStream());
        } catch (IOException e) {
            log.debug("Invalid resource configuration: " + e.getMessage());
        }
    }

    /**
     * Parses the given input stream into the xml configuration file.
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
     * @param stream
     */
    public void parse(InputStream stream) {
        try {
            Element config = DomUtil.parseDocument(stream).getDocumentElement();
            if (config == null) {
                log.warn("Mandatory 'config' element is missing.");
                return;
            }

            // iomanager config entry
            Element el = DomUtil.getChildElement(config, ELEMENT_IOMANAGER, null);
            if (el != null) {
                Object inst = buildClassFromConfig(el);
                if (inst != null && inst instanceof IOManager) {
                    ioManager = (IOManager)inst;
                    ioManager.setDetector(detector);
                    // get optional 'iohandler' child elements and populate the
                    // ioManager with the instances
                    ElementIterator iohElements = DomUtil.getChildren(el, ELEMENT_IOHANDLER, null);
                    while (iohElements.hasNext()) {
                        Element iohEl = iohElements.nextElement();
                        inst = buildClassFromConfig(iohEl);
                        if (inst != null && inst instanceof IOHandler) {
                            IOHandler handler = (IOHandler) inst;
                            setParameters(handler, iohEl);
                            ioManager.addIOHandler(handler);
                        } else {
                            log.warn("Not a valid IOHandler : " + getClassName(iohEl));
                        }
                    }
                } else {
                    log.warn("'iomanager' element does not define a valid IOManager.");
                }
            } else {
                log.warn("'iomanager' element is missing.");
            }

            // propertymanager config entry
            el = DomUtil.getChildElement(config, ELEMENT_PROPERTYMANAGER, null);
            if (el != null) {
                Object inst = buildClassFromConfig(el);
                if (inst != null && inst instanceof PropertyManager) {
                    propManager = (PropertyManager)inst;
                    // get optional 'iohandler' child elements and populate the
                    // ioManager with the instances
                    ElementIterator iohElements = DomUtil.getChildren(el, ELEMENT_PROPERTYHANDLER, null);
                    while (iohElements.hasNext()) {
                        Element iohEl = iohElements.nextElement();
                        inst = buildClassFromConfig(iohEl);
                        if (inst != null && inst instanceof PropertyHandler) {
                            PropertyHandler handler = (PropertyHandler) inst;
                            setParameters(handler, iohEl);
                            propManager.addPropertyHandler(handler);
                        } else {
                            log.warn("Not a valid PropertyHandler : " + getClassName(iohEl));
                        }
                    }
                } else {
                    log.warn("'propertymanager' element does not define a valid PropertyManager.");
                }
            } else {
                log.debug("'propertymanager' element is missing.");
            }

            // copymovemanager config entry
            el = DomUtil.getChildElement(config, ELEMENT_COPYMOVEMANAGER, null);
            if (el != null) {
                Object inst = buildClassFromConfig(el);
                if (inst != null && inst instanceof CopyMoveManager) {
                    cmManager = (CopyMoveManager) inst;
                    // get optional 'copymovehandler' child elements and populate
                    // the copy move manager with the instances
                    ElementIterator iohElements = DomUtil.getChildren(el, ELEMENT_COPYMOVEHANDLER, null);
                    while (iohElements.hasNext()) {
                        Element iohEl = iohElements.nextElement();
                        inst = buildClassFromConfig(iohEl);
                        if (inst != null && inst instanceof CopyMoveHandler) {
                            CopyMoveHandler handler = (CopyMoveHandler) inst;
                            setParameters(handler, iohEl);
                            cmManager.addCopyMoveHandler(handler);
                        } else {
                            log.warn("Not a valid CopyMoveHandler : " + getClassName(iohEl));
                        }
                    }
                } else {
                    log.warn("'copymovemanager' element does not define a valid CopyMoveManager.");
                }
            } else {
                log.debug("'copymovemanager' element is missing.");
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
                log.debug("No 'filter' element specified.");
            }

            el = DomUtil.getChildElement(config, "mimetypeproperties", null);
            if (el != null) {
                log.warn("Ignoring deprecated mimetypeproperties settings");
            }
        } catch (IOException e) {
            log.debug("Invalid resource configuration: " + e.getMessage());
        } catch (ParserConfigurationException e) {
            log.warn("Failed to parse resource configuration: " + e.getMessage());
        } catch (SAXException e) {
            log.warn("Failed to parse resource configuration: " + e.getMessage());
        }
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

    private static String[] parseNodeTypesEntry(Element parent) {
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
    
    private static Object buildClassFromConfig(Element parent) {
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

    private static String getClassName(Element parent) {
        String className = null;
        Element classElem = DomUtil.getChildElement(parent, "class", null);
        if (classElem != null) {
            className = DomUtil.getAttribute(classElem, "name", null);
        }
        return (className == null) ? "" : className;
    }

    /**
     * Retrieve 'param' elements for the specified <code>xmlElement</code> and
     * use the public setter methods of the given <code>instance</code> to set
     * the corresponding instance fields.
     *
     * @param instance
     * @param xmlElement
     */
    private static void setParameters(Object instance, Element xmlElement) {
        ElementIterator paramElems = DomUtil.getChildren(xmlElement, ELEMENT_PARAM, Namespace.EMPTY_NAMESPACE);
        if (paramElems.hasNext()) {
            Map<String, Method> setters = getSetters(instance.getClass());
            if (!setters.isEmpty()) {
                while (paramElems.hasNext()) {
                    Element parameter = paramElems.next();
                    String name = DomUtil.getAttribute(parameter, ATTR_NAME, null);
                    String value = DomUtil.getAttribute(parameter, ATTR_VALUE, null);
                    if (name == null || value == null) {
                        log.error("Parameter name or value missing -> ignore.");
                        continue;
                    }
                    Method setter = setters.get(name);
                    if (setter != null) {
                        Class<?> type = setter.getParameterTypes()[0];
                        try {
                            if (type.isAssignableFrom(String.class)
                                    || type.isAssignableFrom(Object.class)) {
                                setter.invoke(instance, value);
                            } else if (type.isAssignableFrom(Boolean.TYPE)
                                    || type.isAssignableFrom(Boolean.class)) {
                                setter.invoke(instance, Boolean.valueOf(value));
                            } else if (type.isAssignableFrom(Integer.TYPE)
                                    || type.isAssignableFrom(Integer.class)) {
                                setter.invoke(instance, Integer.valueOf(value));
                            } else if (type.isAssignableFrom(Long.TYPE)
                                    || type.isAssignableFrom(Long.class)) {
                                setter.invoke(instance, Long.valueOf(value));
                            } else if (type.isAssignableFrom(Double.TYPE)
                                    || type.isAssignableFrom(Double.class)) {
                                setter.invoke(instance, Double.valueOf(value));
                            } else {
                                log.error("Cannot set configuration property " + name);
                            }
                        } catch (Exception e) {
                            log.error("Invalid format (" + value + ") for property " + name + " of class " + instance.getClass().getName(), e);
                        }
                    }
                }
            }
        }
    }

    private static Map<String, Method> getSetters(Class<?> cl) {
        Map<String, Method> methods = new HashMap<String, Method>();
        for (Method method : cl.getMethods()) {
            String name = method.getName();
            if (name.startsWith("set") && name.length() > 3
                    && Modifier.isPublic(method.getModifiers())
                    && !Modifier.isStatic(method.getModifiers())
                    && Void.TYPE.equals(method.getReturnType())
                    && method.getParameterTypes().length == 1) {
                methods.put(name.substring(3, 4).toLowerCase() + name.substring(4), method);
            }
        }
        return methods;
    }

    /**
     *
     * @return
     */
    public IOManager getIOManager() {
        if (ioManager == null) {
            log.debug("Missing io-manager > building DefaultIOManager ");
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
            log.debug("Missing property-manager > building default.");
            propManager = PropertyManagerImpl.getDefaultManager();
        }
        return propManager;
    }

    /**
     *
     * @return
     */
    public CopyMoveManager getCopyMoveManager() {
        if (cmManager == null) {
            log.debug("Missing copymove-manager > building default.");
            cmManager = CopyMoveManagerImpl.getDefaultManager();
        }
        return cmManager;
    }

    /**
     * Returns the delete manager.
     * @return the delete manager
     */
    public DeleteManager getDeleteManager() {
        if (deleteManager == null) {
            log.debug("Missing delete-manager > building default.");
            deleteManager = DeleteManagerImpl.getDefaultManager();
        }
        return deleteManager;
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
            log.debug("Missing resource filter > building DefaultItemFilter ");
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
