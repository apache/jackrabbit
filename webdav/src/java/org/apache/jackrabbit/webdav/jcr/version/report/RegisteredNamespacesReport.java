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
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.DavSession;
import org.apache.jackrabbit.webdav.jcr.JcrDavException;
import org.apache.jackrabbit.webdav.jcr.ItemResourceConstants;
import org.jdom.Document;
import org.jdom.Element;

import javax.jcr.*;
import java.util.ArrayList;
import java.util.List;

/**
 * <code>NodeTypesReport</code> allows to retrieve the definition of a single
 * or multiple node types. The request body must be a 'jcr:nodetypes' element:
 * <pre>
 * &lt;!ELEMENT nodetypes ( nodetype+ | all-nodetypes | mixin-nodetypes | primary-nodetypes ) &gt;
 *
 * &lt;!ELEMENT nodetype ( nodetype-name ) &gt;
 * &lt;!ELEMENT nodetype-name (#PCDATA) &gt;
 *
 * &lt;!ELEMENT all-nodetypes EMPTY &gt;
 * &lt;!ELEMENT mixin-nodetypes EMPTY &gt;
 * &lt;!ELEMENT primary-nodetypes EMPTY &gt;
 * </pre>
 */
public class RegisteredNamespacesReport implements Report, ItemResourceConstants {

    private static Logger log = Logger.getLogger(RegisteredNamespacesReport.class);

    /**
     * The registered type of this report.
     */
    public static final ReportType REGISTERED_NAMESPACES_REPORT = ReportType.register("registerednamespaces", ItemResourceConstants.NAMESPACE, RegisteredNamespacesReport.class);

    private NamespaceRegistry nsReg;
    private ReportInfo info;

    /**
     * Returns {@link #REGISTERED_NAMESPACES_REPORT} type.
     * @return {@link #REGISTERED_NAMESPACES_REPORT}
     * @see org.apache.jackrabbit.webdav.version.report.Report#getType()
     */
    public ReportType getType() {
        return REGISTERED_NAMESPACES_REPORT;
    }

    /**
     * @param resource
     * @throws IllegalArgumentException if the resource or the session retrieved
     * from the specified resource is <code>null</code>
     * @see org.apache.jackrabbit.webdav.version.report.Report#setResource(org.apache.jackrabbit.webdav.version.DeltaVResource)
     */
    public void setResource(DeltaVResource resource) {
        if (resource == null) {
            throw new IllegalArgumentException("Resource must not be null.");
        }
        try {
            DavSession session = resource.getSession();
            if (session == null || session.getRepositorySession() == null) {
                throw new IllegalArgumentException("The resource must provide a non-null session object in order to create the jcr:nodetypes report.");
            }
            nsReg = session.getRepositorySession().getWorkspace().getNamespaceRegistry();
        } catch (RepositoryException e) {
            log.error(e.getMessage());
        }
    }

    /**
     * @param info
     * @throws IllegalArgumentException if the specified info does not contain
     * a jcr:nodetypes element.
     * @see org.apache.jackrabbit.webdav.version.report.Report#setInfo(org.apache.jackrabbit.webdav.version.report.ReportInfo)
     */
    public void setInfo(ReportInfo info) {
        if (info == null || !"registerednamespaces".equals(info.getReportElement().getName())) {
            throw new IllegalArgumentException("jcr:registerednamespaces element expected.");
        }
        this.info = info;
    }

    /**
     * Returns a Xml representation of the node type definition(s) according
     * to the info object.
     *
     * @return Xml representation of the node type definition(s)
     * @throws org.apache.jackrabbit.webdav.DavException if the specified nodetypes are not known or if another
     * error occurs while retrieving the nodetype definitions.
     * @see org.apache.jackrabbit.webdav.version.report.Report#toXml()
     */
    public Document toXml() throws DavException {
        if (info == null || nsReg == null) {
            throw new DavException(DavServletResponse.SC_INTERNAL_SERVER_ERROR, "Error while running jcr:registerednamespaces report");
        }
        try {
	    String[] prefixes = nsReg.getPrefixes();
	    List namespaceList = new ArrayList();
	    for (int i = 0; i < prefixes.length; i++) {
		Element elem = new Element(XML_NAMESPACE, NAMESPACE);
		elem.addContent(new Element(XML_NSPREFIX, NAMESPACE).setText(prefixes[i]));
		elem.addContent(new Element(XML_NSURI, NAMESPACE).setText(nsReg.getURI(prefixes[i])));
		namespaceList.add(elem);
	    }
	    Element report = new Element("registerednamespaces-report", NAMESPACE).addContent(namespaceList);
            Document reportDoc = new Document(report);
            return reportDoc;
        } catch (RepositoryException e) {
            throw new JcrDavException(e);
        }
    }
}