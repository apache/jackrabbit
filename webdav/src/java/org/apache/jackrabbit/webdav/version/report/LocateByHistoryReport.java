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
import org.apache.jackrabbit.webdav.property.DavPropertyNameSet;
import org.apache.jackrabbit.webdav.version.DeltaVConstants;
import org.apache.jackrabbit.webdav.version.VersionControlledResource;
import org.apache.jackrabbit.webdav.version.VersionHistoryResource;
import org.apache.jackrabbit.webdav.version.DeltaVResource;
import org.jdom.Element;
import org.jdom.Document;

import java.util.List;
import java.util.HashSet;
import java.util.Iterator;

/**
 * <code>LocateByHistoryReport</code> encapsulates the DAV:locate-by-hisotry
 * report, that may be used to locate a version-controlled resource for that
 * version history. The DAV:locate-by-history report can be applied to a collection
 * to locate the collection member that is a version-controlled resource for a
 * specified version history resource.
 *
 * <pre>
 * &lt;!ELEMENT locate-by-history (version-history-set, prop)&gt;
 * &lt;!ELEMENT version-history-set (href+)&gt;
 * </pre>
 */
public class LocateByHistoryReport implements Report, DeltaVConstants {

    private static Logger log = Logger.getLogger(LocateByHistoryReport.class);

    private ReportInfo info;
    private HashSet vhHrefSet = new HashSet();
    private DeltaVResource resource;

    /**
     *
     * @return
     * @see Report#getType()
     */
    public ReportType getType() {
        return ReportType.LOCATE_BY_HISTORY;
    }

    /**
     * Set the DeltaVResource.
     *
     * @param resource
     * @throws IllegalArgumentException if the specified resource is not a {@link VersionControlledResource}.
     * @see Report#setResource(org.apache.jackrabbit.webdav.version.DeltaVResource)
     */
    public void setResource(DeltaVResource resource) throws IllegalArgumentException {
        if (resource instanceof VersionControlledResource) {
            this.resource = resource;
        } else {
            throw new IllegalArgumentException("DAV:version-tree report can only be created for version-controlled resources and version resources.");
        }
    }

    /**
     * Set the <code>ReportInfo</code>
     *
     * @param info
     * @throws IllegalArgumentException if the given <code>ReportInfo</code>
     * does not contain a DAV:version-tree element.
     * @see Report#setInfo(ReportInfo)
     */
    public void setInfo(ReportInfo info) throws IllegalArgumentException {
        if (info == null || !XML_LOCATE_BY_HISTORY.equals(info.getReportElement().getName())) {
            throw new IllegalArgumentException("DAV:locate-by-history element expected.");
        }
        Element versionHistorySet = info.getReportElement().getChild(XML_VERSION_HISTORY_SET, NAMESPACE);
        if (versionHistorySet == null) {
            throw new IllegalArgumentException("The DAV:locate-by-history element must contain a DAV:version-history-set child.");
        }

        List l = versionHistorySet.getChildren(DavConstants.XML_HREF, DavConstants.NAMESPACE);
        if (l != null && !l.isEmpty()) {
            Iterator it = l.iterator();
            while (it.hasNext()) {
                String href = ((Element)it.next()).getText();
                if (href != null) {
                    vhHrefSet.add(href);
                }
            }
        }
        this.info = info;
    }

    /**
     * Run the report.
     *
     * @return Xml <code>Document</code> representing the report in the required
     * format.
     * @throws DavException
     * @see Report#toXml()
     */
    public Document toXml() throws DavException {
        if (info == null || resource == null) {
            throw new DavException(DavServletResponse.SC_INTERNAL_SERVER_ERROR, "Error while running DAV:locate-by-history report");
        }

        MultiStatus ms = new MultiStatus();
        buildResponse(resource, info.getPropertyNameSet(), info.getDepth(), ms);
        return ms.toXml();
    }

    /**
     * Fill the <code>MultiStatus</code> with the <code>MultiStatusResponses</code>
     * generated for the specified resource and its members according to the
     * depth value.
     *
     * @param res
     * @param propNameSet
     * @param depth
     * @param ms
     * @throws DavException
     */
    private void buildResponse(DavResource res, DavPropertyNameSet propNameSet,
                               int depth, MultiStatus ms) throws DavException {
        // loop over members first, since this report only list members
        DavResourceIterator it = res.getMembers();
        while (!vhHrefSet.isEmpty() && it.hasNext()) {
            DavResource childRes = it.nextResource();
            if (childRes instanceof VersionControlledResource) {
                try {
                    VersionHistoryResource vhr = ((VersionControlledResource)childRes).getVersionHistory();
                    if (vhHrefSet.remove(vhr.getHref())) {
                        if (propNameSet.isEmpty()) {
                            ms.addResourceStatus(childRes, DavServletResponse.SC_OK, 0);
                        } else {
                            ms.addResourceProperties(childRes, propNameSet, 0);
                        }
                    }
                } catch (DavException e) {
                    log.info(e.getMessage());
                }
            }
            // traverse subtree
            if (depth > 0) {
                buildResponse(it.nextResource(), propNameSet, depth-1, ms);
            }
        }
    }
}