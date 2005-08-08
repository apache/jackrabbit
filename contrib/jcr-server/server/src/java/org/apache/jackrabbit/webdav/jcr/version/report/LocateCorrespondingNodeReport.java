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
import org.apache.jackrabbit.webdav.version.report.Report;
import org.apache.jackrabbit.webdav.version.report.ReportType;
import org.apache.jackrabbit.webdav.version.report.ReportInfo;
import org.apache.jackrabbit.webdav.version.DeltaVResource;
import org.apache.jackrabbit.webdav.version.DeltaVConstants;
import org.apache.jackrabbit.webdav.*;
import org.apache.jackrabbit.webdav.util.XmlUtil;
import org.apache.jackrabbit.webdav.jcr.ItemResourceConstants;
import org.apache.jackrabbit.webdav.jcr.JcrDavException;
import org.jdom.Document;
import org.jdom.Element;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 * <code>LocateCorrespondingNodeReport</code> is used to identify the resource that
 * represents the corresponding node in another workspace.
 *
 * <p/>
 * The request body must be a 'dcr:locate-corresponding-node' XML element, that
 * contains the href of the source workspace, where the corresponding node should
 * be searched:
 * <pre>
 * &lt;!ELEMENT locate-corresponding-node ( workspace ) &gt;
 * &lt;!ELEMENT workspace ( href ) &gt;  (as defined by <a href="http://www.webdav.org/specs/rfc3253.html#PROPERTY_workspace">RFC 3253</a>)
 * </pre>
 * The response to a successful report request must be a 'dcr:locate-corresponding-node-report'
 * element that contains the href of the corresponding node in the given source
 * workspace:
 *
 * <pre>
 * &lt;!ELEMENT locate-corresponding-node-report ( href ) &gt;
 * </pre>
 *
 * @see javax.jcr.Node#getCorrespondingNodePath(String)
 */
public class LocateCorrespondingNodeReport implements Report {

    private static Logger log = Logger.getLogger(LocateCorrespondingNodeReport.class);

    private static final String REPORT_NAME = "locate-corresponding-node";

    private DeltaVResource resource;
    private String workspaceHref;

    /**
     * The corresponding-node report type
     */
    public static final ReportType LOCATE_CORRESPONDING_NODE_REPORT = ReportType.register(REPORT_NAME, ItemResourceConstants.NAMESPACE, LocateByUuidReport.class);

    /**
     * Returns {@link #LOCATE_CORRESPONDING_NODE_REPORT}
     *
     * @return always returns {@link #LOCATE_CORRESPONDING_NODE_REPORT}
     * @see org.apache.jackrabbit.webdav.version.report.Report#getType() 
     */
    public ReportType getType() {
        return LOCATE_CORRESPONDING_NODE_REPORT;
    }

    /**
     * @see Report#setResource(org.apache.jackrabbit.webdav.version.DeltaVResource)
     */
    public void setResource(DeltaVResource resource) {
        if (resource == null) {
            throw new IllegalArgumentException("Resource must not be null.");
        }
        DavSession davSession = resource.getSession();
        if (davSession == null || davSession.getRepositorySession() == null) {
            throw new IllegalArgumentException("The resource must provide a non-null session object in order to create the dcr:locate-corresponding-node report.");
        }
        this.resource = resource;
    }

    /**
     * @see Report#setInfo(org.apache.jackrabbit.webdav.version.report.ReportInfo)
     */
    public void setInfo(ReportInfo info) {
        if (info == null || !REPORT_NAME.equals(info.getReportElement().getName())) {
            throw new IllegalArgumentException("dcr:locate-corresponding-node expected.");
        }
        Element workspace = info.getReportElement().getChild(DeltaVConstants.WORKSPACE.getName(), DeltaVConstants.WORKSPACE.getNamespace());
        if (workspace != null) {
            workspaceHref = workspace.getChildText(DavConstants.XML_HREF, DavConstants.NAMESPACE);
        }
    }

    /**
     * @see org.apache.jackrabbit.webdav.version.report.Report#toXml()
     */
    public Document toXml() throws DavException {
        if (resource == null || workspaceHref == null) {
            throw new DavException(DavServletResponse.SC_INTERNAL_SERVER_ERROR, "Error while running dcr:locate-corresponding-node report: resource must not be null and request body must define the href of a source workspace");
        }
        try {
            Item item = resource.getSession().getRepositorySession().getItem(resource.getResourcePath());
            if (item.isNode()) {
                DavResourceLocator rLoc = resource.getLocator();
                String workspaceName = rLoc.getFactory().createResourceLocator(rLoc.getPrefix(), workspaceHref).getWorkspaceName();

                String corrPath = ((Node)item).getCorrespondingNodePath(workspaceName);
                DavResourceLocator corrLoc = rLoc.getFactory().createResourceLocator(rLoc.getPrefix(), workspaceName + "/", corrPath);

                Element e = new Element("locate-corresponding-node-report", ItemResourceConstants.NAMESPACE);
                e.addContent(XmlUtil.hrefToXml(corrLoc.getHref(true)));
                return new Document(e);
            } else {
                throw new DavException(DavServletResponse.SC_INTERNAL_SERVER_ERROR, "Error while running dcr:locate-corresponding-node report: resource must represent a jcr node.");
            }
        } catch (RepositoryException e) {
            throw new JcrDavException(e);
        }
    }
}