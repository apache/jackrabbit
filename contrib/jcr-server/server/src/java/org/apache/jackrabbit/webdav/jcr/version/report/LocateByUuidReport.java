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
package org.apache.jackrabbit.webdav.jcr.version.report;

import org.apache.log4j.Logger;
import org.apache.jackrabbit.webdav.version.report.*;
import org.apache.jackrabbit.webdav.version.DeltaVResource;
import org.apache.jackrabbit.webdav.jcr.ItemResourceConstants;
import org.apache.jackrabbit.webdav.jcr.JcrDavException;
import org.apache.jackrabbit.webdav.*;
import org.apache.jackrabbit.webdav.property.DavPropertyNameSet;
import org.jdom.Document;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 * <code>LocateByUuidReport</code> handles REPORT requests for the 'locate-by-uuid'
 * report.
 * <p/>
 * The request body must be a 'dcr:locate-by-uuid' XML element:
 * <pre>
 * &lt;!ELEMENT locate-by-uuid ( href , prop? ) &gt;
 * </pre>
 * The response to a successful report request will be a Multi-Status response.
 */
public class LocateByUuidReport implements Report {

    private static Logger log = Logger.getLogger(LocateByUuidReport.class);

    private static final String REPORT_NAME = "locate-by-uuid";

    /**
     * The exportview report type
     */
    public static final ReportType LOCATE_BY_UUID_REPORT = ReportType.register(REPORT_NAME, ItemResourceConstants.NAMESPACE, LocateByUuidReport.class);

    private DeltaVResource resource;
    private ReportInfo info;

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
     * @param resource
     * @throws IllegalArgumentException if the resource is <code>null</code> or
     * if the session object provided with the resource is <code>null</code>.
     * @see Report#setResource(org.apache.jackrabbit.webdav.version.DeltaVResource)
     */
    public void setResource(DeltaVResource resource) {
        if (resource == null) {
            throw new IllegalArgumentException("Resource must not be null.");
        }
        DavSession davSession = resource.getSession();
        if (davSession == null || davSession.getRepositorySession() == null) {
            throw new IllegalArgumentException("The resource must provide a non-null session object in order to create the locate-by-uuid report.");
        }
        this.resource = resource;
    }

    /**
     * @param info
     * @throws IllegalArgumentException if the specified {@link ReportInfo info}
     * object does not contain a {@link ItemResourceConstants#NAMESPACE dcr}:locate-by-uuid element.
     * @see Report#setInfo(org.apache.jackrabbit.webdav.version.report.ReportInfo)
     */
    public void setInfo(ReportInfo info) {
        if (info == null || !REPORT_NAME.equals(info.getReportElement().getName())) {
            throw new IllegalArgumentException("dcr:locate-by-uuid element expected.");
        }
        this.info = info;
    }

    /**
     * Creates a Xml document from the generated view.
     *
     * @return Xml document representing the output of the specified view.
     * @throws DavException if the report document could not be created.
     * @see org.apache.jackrabbit.webdav.version.report.Report#toXml()
     */
    public Document toXml() throws DavException {
        String uuid = info.getReportElement().getChildText(DavConstants.XML_HREF, DavConstants.NAMESPACE);
        DavPropertyNameSet propNameSet = info.getPropertyNameSet();

        try {
            DavSession session = resource.getSession();
            DavResourceLocator resourceLoc = resource.getLocator();

            Node n = session.getRepositorySession().getNodeByUUID(uuid);
            DavResourceLocator loc = resourceLoc.getFactory().createResourceLocator(resourceLoc.getPrefix(), resourceLoc.getWorkspacePath(), n.getPath(), false);
            DavResource res = resource.getFactory().createResource(loc, session);

            MultiStatus ms = new MultiStatus();
            ms.addResourceProperties(res, propNameSet, info.getDepth());
            return ms.toXml();

        } catch (RepositoryException e) {
            throw new JcrDavException(e);
        }
    }
}