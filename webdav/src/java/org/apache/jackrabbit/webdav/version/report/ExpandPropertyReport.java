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
package org.apache.jackrabbit.webdav.version.report;

import org.apache.log4j.Logger;
import org.apache.jackrabbit.webdav.*;
import org.apache.jackrabbit.webdav.property.DavProperty;
import org.apache.jackrabbit.webdav.property.DavPropertyName;
import org.apache.jackrabbit.webdav.property.HrefProperty;
import org.apache.jackrabbit.webdav.property.AbstractDavProperty;
import org.apache.jackrabbit.webdav.version.DeltaVConstants;
import org.apache.jackrabbit.webdav.version.DeltaVResource;
import org.jdom.Element;
import org.jdom.Attribute;
import org.jdom.Namespace;
import org.jdom.Document;

import java.util.*;

/**
 * <code>ExpandPropertyReport</code> encapsulates the DAV:expand-property report,
 * that provides a mechanism for retrieving in one request the properties from
 * the resources identified by those DAV:href elements. It should be supported by
 * all resources that support the REPORT method.
 * <p/>
 * RFC 3253 specifies the following required format for the request body:
 * <pre>
 * &lt;!ELEMENT expand-property (property*)&gt;
 * &lt;!ELEMENT property (property*)&gt;
 * &lt;!ATTLIST property name NMTOKEN #REQUIRED&gt;
 * name value: a property element type
 * &lt;!ATTLIST property namespace NMTOKEN "DAV:"&gt;
 * namespace value: an XML namespace
 * </pre>
 * NOTE: any DAV:property elements defined in the request body, that does not
 * represent {@link HrefProperty} is treated as in a common PROPFIND request.
 *
 * @see DeltaVConstants#XML_EXPAND_PROPERTY
 * @see DeltaVConstants#XML_PROPERTY
 */
public class ExpandPropertyReport implements Report, DeltaVConstants {

    private static Logger log = Logger.getLogger(ExpandPropertyReport.class);

    private DeltaVResource resource;
    private ReportInfo info;
    private List properties;

    /**
     * Returns {@link ReportType#EXPAND_PROPERTY}.
     *
     * @return
     * @see Report#getType()
     */
    public ReportType getType() {
        return ReportType.EXPAND_PROPERTY;
    }

    /**
     * Set the target resource.
     *
     * @param resource
     * @throws IllegalArgumentException if the specified resource is <code>null</code>
     * @see Report#setResource(org.apache.jackrabbit.webdav.version.DeltaVResource)
     */
    public void setResource(DeltaVResource resource) throws IllegalArgumentException {
        if (resource == null) {
            throw new IllegalArgumentException("The resource specified must not be null.");
        }
        this.resource = resource;
    }

    /**
     * Set the <code>ReportInfo</code>.
     *
     * @param info
     * @throws IllegalArgumentException if the given <code>ReportInfo</code>
     * does not contain a DAV:expand-property element.
     * @see Report#setInfo(ReportInfo)
     */
    public void setInfo(ReportInfo info) throws IllegalArgumentException {
        if (info == null || !XML_EXPAND_PROPERTY.equals(info.getReportElement().getName())) {
            throw new IllegalArgumentException("DAV:expand-property element expected.");
        }
        this.info = info;
        properties = info.getReportElement().getChildren(XML_PROPERTY, NAMESPACE);
    }

    /**
     * Run the report
     *
     * @return Xml <code>Document</code> as defined by
     * <a href="http://www.ietf.org/rfc/rfc2518.txt">RFC 2518</a>
     * @throws DavException
     * @see Report#toXml()
     */
    public Document toXml() throws DavException {
        if (info == null || resource == null) {
            throw new DavException(DavServletResponse.SC_INTERNAL_SERVER_ERROR, "Error while running DAV:version-tree report");
        }

        MultiStatus ms = new MultiStatus();
        buildMultiStatus(resource, info.getDepth(), ms);
        return ms.toXml();
    }

    /**
     * Fills the specified <code>MultiStatus</code> object by generating a
     * <code>MultiStatusResponse</code> for the given resource (and
     * its member according to the depth value).
     *
     * @param res
     * @param depth
     * @param ms
     * @throws DavException
     * @see #getResponse(DavResource, List)
     */
    private void buildMultiStatus(DavResource res, int depth, MultiStatus ms)
            throws DavException {
        MultiStatusResponse response = getResponse(res, properties);
        ms.addResponse(response);
        if (depth > 0) {
            DavResourceIterator it = res.getMembers();
            while (it.hasNext()) {
                buildMultiStatus(it.nextResource(), depth-1, ms);
            }
        }
    }

    /**
     * Builds a <code>MultiStatusResponse</code> for the given resource respecting
     * the properties specified. Any property that represents a {@link HrefProperty}
     * is expanded: It's name equals the name of a valid {@link HrefProperty}.
     * However the value of that given property (consisting of one or multiple DAV:href elements)
     * is replaced by the Xml representation of a separate
     * {@link MultiStatusResponse multistatus responses} for the
     * resource referenced by the given DAV:href elements. The responses may
     * themselves have properties, which are defined by the separate list.
     *
     * @param res
     * @param propertyList
     * @return <code>MultiStatusResponse</code> for the given resource.
     * @see ExpandProperty
     */
    private MultiStatusResponse getResponse(DavResource res, List propertyList) {
        MultiStatusResponse resp = new MultiStatusResponse(res.getHref());
        Iterator propIter = propertyList.iterator();
        while (propIter.hasNext()) {
            Element propertyElem = (Element) propIter.next();
            Attribute nameAttr = propertyElem.getAttribute(ATTR_NAME);
            if (nameAttr == null) {
                // NOTE: this is not valid according to the DTD
                continue;
            }
            Attribute namespaceAttr = propertyElem.getAttribute(ATTR_NAMESPACE);

            String name = nameAttr.getValue();
            Namespace namespace = (namespaceAttr != null) ? Namespace.getNamespace(namespaceAttr.getValue()) : NAMESPACE;

            DavPropertyName propName = DavPropertyName.create(name, namespace);
            DavProperty p = res.getProperty(propName);
            if (p != null) {
                if (p instanceof HrefProperty && res instanceof DeltaVResource) {
                    resp.add(new ExpandProperty((DeltaVResource)res, (HrefProperty)p, propertyElem.getChildren(XML_PROPERTY, NAMESPACE)));
                } else {
                    resp.add(p);
                }
            } else {
                resp.add(propName, DavServletResponse.SC_NOT_FOUND);
            }
        }
        return resp;
    }

    //--------------------------------------------------------< inner class >---
    /**
     * <code>ExpandProperty</code> extends <code>DavProperty</code>. It's name
     * equals the name of a valid {@link HrefProperty}. However the value of
     * that given property (consisting of one or multiple DAV:href elements)
     * is replaced by the Xml representation of a separate
     * {@link MultiStatusResponse multistatus responses} for the
     * resource referenced to by the given DAV.:href elements. The responses may
     * themselves have properties, which are defined by the separate list.
     */
    private class ExpandProperty extends AbstractDavProperty {

        private List valueList = new ArrayList();

        /**
         * Create a new <code>ExpandProperty</code>.
         *
         * @param hrefProperty
         * @param propertyList
         */
        private ExpandProperty(DeltaVResource deltaVResource, HrefProperty hrefProperty, List propertyList) {
            super(hrefProperty.getName(), hrefProperty.isProtected());
            try {
                DavResource[] refResource = deltaVResource.getReferenceResources(hrefProperty.getName());
                for (int i = 0; i < refResource.length; i++) {
                    MultiStatusResponse resp = getResponse(refResource[i], propertyList);
                    valueList.add(resp.toXml());
                }
            } catch (DavException e) {
                // invalid references or unknown property
                log.error(e.getMessage());
            }
        }

        /**
         * Returns
         * @return
         */
        public Object getValue() {
            return valueList;
        }
    }
}