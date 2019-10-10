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
import org.apache.jackrabbit.webdav.jcr.ItemResourceConstants;
import org.apache.jackrabbit.webdav.jcr.JcrDavException;
import org.apache.jackrabbit.webdav.version.DeltaVConstants;
import org.apache.jackrabbit.webdav.version.report.Report;
import org.apache.jackrabbit.webdav.version.report.ReportInfo;
import org.apache.jackrabbit.webdav.version.report.ReportType;
import org.apache.jackrabbit.webdav.xml.DomUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * <code>LocateCorrespondingNodeReport</code> is used to identify the resource that
 * represents the corresponding node in another workspace.
 *
 * <p>
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
public class LocateCorrespondingNodeReport extends AbstractJcrReport {

    private static Logger log = LoggerFactory.getLogger(LocateCorrespondingNodeReport.class);

    private String correspHref;

    /**
     * The corresponding-node report type
     */
    public static final ReportType LOCATE_CORRESPONDING_NODE_REPORT = ReportType.register(JcrRemotingConstants.REPORT_LOCATE_CORRESPONDING_NODE, ItemResourceConstants.NAMESPACE, LocateByUuidReport.class);

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
     * @see Report#init(DavResource, ReportInfo)
     */
    @Override
    public void init(DavResource resource, ReportInfo info) throws DavException {
        // general validation checks
        super.init(resource, info);
        // specific for this report: a workspace href must be provided
        Element workspace = info.getContentElement(DeltaVConstants.WORKSPACE.getName(), DeltaVConstants.WORKSPACE.getNamespace());
        String workspaceHref = normalizeResourceHref(DomUtil.getChildTextTrim(workspace, DavConstants.XML_HREF, DavConstants.NAMESPACE));
        if (workspaceHref == null || "".equals(workspaceHref)) {
            throw new DavException(DavServletResponse.SC_BAD_REQUEST, "Request body must define the href of a source workspace");
        }
        // retrieve href of the corresponding resource in the other workspace
        try {
            this.correspHref = getCorrespondingResourceHref(resource, getRepositorySession(), workspaceHref);
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

    /**
     * Retrieve the href of the corresponding resource in the indicated workspace.
     *
     * @param resource
     * @param session Session object used to access the {@link Node} object
     * represented by the given resource.
     * @param workspaceHref
     * @return
     * @throws RepositoryException
     */
    private static String getCorrespondingResourceHref(DavResource resource, Session session, String workspaceHref) throws RepositoryException {
        DavResourceLocator rLoc = resource.getLocator();
        String itemPath = rLoc.getRepositoryPath();
        Item item = session.getItem(itemPath);
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
