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
package org.apache.jackrabbit.webdav.jcr.version.report;

import org.apache.jackrabbit.commons.webdav.JcrRemotingConstants;
import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.DavResourceLocator;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.MultiStatus;
import org.apache.jackrabbit.webdav.jcr.ItemResourceConstants;
import org.apache.jackrabbit.webdav.jcr.JcrDavException;
import org.apache.jackrabbit.webdav.version.report.Report;
import org.apache.jackrabbit.webdav.version.report.ReportInfo;
import org.apache.jackrabbit.webdav.version.report.ReportType;
import org.apache.jackrabbit.webdav.xml.DomUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 * <code>LocateByUuidReport</code> handles REPORT requests for the 'locate-by-uuid'
 * report.
 * <p>
 * The request body must be a 'dcr:locate-by-uuid' XML element:
 * <pre>
 * &lt;!ELEMENT locate-by-uuid ( href , prop? ) &gt;
 * </pre>
 * The response to a successful report request will be a Multi-Status response.
 */
public class LocateByUuidReport extends AbstractJcrReport {

    private static Logger log = LoggerFactory.getLogger(LocateByUuidReport.class);

    /**
     * The exportview report type
     */
    public static final ReportType LOCATE_BY_UUID_REPORT = ReportType.register(JcrRemotingConstants.REPORT_LOCATE_BY_UUID, ItemResourceConstants.NAMESPACE, LocateByUuidReport.class);

    private MultiStatus ms;

    /**
     * Returns {@link #LOCATE_BY_UUID_REPORT} report type.
     *
     * @return {@link #LOCATE_BY_UUID_REPORT}
     * @see org.apache.jackrabbit.webdav.version.report.Report#getType()
     */
    public ReportType getType() {
        return LOCATE_BY_UUID_REPORT;
    }

    /**
     * Always returns <code>true</code>.
     *
     * @return true
     * @see org.apache.jackrabbit.webdav.version.report.Report#isMultiStatusReport()
     */
    public boolean isMultiStatusReport() {
        return true;
    }

    /**
     * @see Report#init(DavResource, ReportInfo)
     */
    @Override
    public void init(DavResource resource, ReportInfo info) throws DavException {
        // delegate basic validation to super class
        super.init(resource, info);
        // make also sure, the info contains a DAV:href child element
        if (!info.containsContentElement(DavConstants.XML_HREF, DavConstants.NAMESPACE)) {
            throw new DavException(DavServletResponse.SC_BAD_REQUEST, "dcr:locate-by-uuid element must at least contain a single DAV:href child.");
        }
        // immediately build the final multistatus element
        try {
            Element hrefElem = info.getContentElement(DavConstants.XML_HREF, DavConstants.NAMESPACE);
            String uuid = DomUtil.getTextTrim(hrefElem);
            DavResourceLocator resourceLoc = resource.getLocator();
            Node n = getRepositorySession().getNodeByUUID(uuid);
            DavResourceLocator loc = resourceLoc.getFactory().createResourceLocator(resourceLoc.getPrefix(), resourceLoc.getWorkspacePath(), n.getPath(), false);
            DavResource locatedResource = resource.getFactory().createResource(loc, resource.getSession());
            ms = new MultiStatus();
            ms.addResourceProperties(locatedResource, info.getPropertyNameSet(), info.getDepth());
        } catch (RepositoryException e) {
            throw new JcrDavException(e);
        }
    }

    /**
     * Creates a Xml document from the generated view.
     *
     * @param document
     * @return Xml element representing the output of the specified view.
     * @see org.apache.jackrabbit.webdav.xml.XmlSerializable#toXml(Document)
     */
    public Element toXml(Document document) {
        return ms.toXml(document);
    }
}
