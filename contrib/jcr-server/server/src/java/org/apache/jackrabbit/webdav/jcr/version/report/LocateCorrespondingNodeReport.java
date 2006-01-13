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
import org.apache.jackrabbit.webdav.xml.DomUtil;
import org.apache.jackrabbit.webdav.jcr.ItemResourceConstants;
import org.apache.jackrabbit.webdav.jcr.JcrDavException;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.DavSession;
import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.DavResourceLocator;
import org.w3c.dom.Element;
import org.w3c.dom.Document;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;

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

    private String correspHref;

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
     * Always returns <code>false</code>.
     *
     * @return false
     * @see org.apache.jackrabbit.webdav.version.report.Report#isMultiStatusReport()
     */
    public boolean isMultiStatusReport() {
        return false;
    }

    /**
     * @see Report#init(org.apache.jackrabbit.webdav.version.DeltaVResource, org.apache.jackrabbit.webdav.version.report.ReportInfo)
     */
    public void init(DeltaVResource resource, ReportInfo info) throws DavException {
        if (resource == null) {
            throw new DavException(DavServletResponse.SC_BAD_REQUEST, "Resource must not be null.");
        }
        DavSession davSession = resource.getSession();
        if (davSession == null || davSession.getRepositorySession() == null) {
            throw new DavException(DavServletResponse.SC_BAD_REQUEST, "The resource must provide a non-null session object in order to create the dcr:locate-corresponding-node report.");
        }
        if (!getType().isRequestedReportType(info)) {
            throw new DavException(DavServletResponse.SC_BAD_REQUEST, "dcr:locate-corresponding-node expected.");
    }

        Element workspace = info.getContentElement(DeltaVConstants.WORKSPACE.getName(), DeltaVConstants.WORKSPACE.getNamespace());
        String workspaceHref = DomUtil.getChildTextTrim(workspace, DavConstants.XML_HREF, DavConstants.NAMESPACE);
        if (workspaceHref == null || "".equals(workspaceHref)) {
            throw new DavException(DavServletResponse.SC_BAD_REQUEST, "Request body must define the href of a source workspace");
        }
        try {
            this.correspHref = getCorrespondingResourceHref(resource, workspaceHref);
        } catch (RepositoryException e) {
            throw new JcrDavException(e);
        }
    }

    /**
     * @see org.apache.jackrabbit.webdav.xml.XmlSerializable#toXml(Document)
     */
    public Element toXml(Document document) {
        Element elem = DomUtil.createElement(document, "locate-corresponding-node-report", ItemResourceConstants.NAMESPACE);
        if (correspHref != null) {
            elem.appendChild(DomUtil.hrefToXml(correspHref, document));
        }
        return elem;
        }

    private static String getCorrespondingResourceHref(DeltaVResource resource, String workspaceHref) throws RepositoryException {
            DavResourceLocator rLoc = resource.getLocator();
            String itemPath = rLoc.getJcrPath();
            Session s = resource.getSession().getRepositorySession();
            Item item = s.getItem(itemPath);
            if (item.isNode()) {
                String workspaceName = rLoc.getFactory().createResourceLocator(rLoc.getPrefix(), workspaceHref).getWorkspaceName();
                String corrPath = ((Node)item).getCorrespondingNodePath(workspaceName);
                DavResourceLocator corrLoc = rLoc.getFactory().createResourceLocator(rLoc.getPrefix(), "/" + workspaceName, corrPath, false);
            return corrLoc.getHref(true);
            } else {
            throw new PathNotFoundException("Node with path " + itemPath + " does not exist.");
        }
    }
}