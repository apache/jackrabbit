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
package org.apache.jackrabbit.webdav.spi.version.report;

import org.apache.log4j.Logger;
import org.apache.jackrabbit.webdav.version.report.*;
import org.apache.jackrabbit.webdav.version.DeltaVResource;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.DavSession;
import org.apache.jackrabbit.webdav.spi.nodetype.NodeTypeConstants;
import org.apache.jackrabbit.webdav.spi.nodetype.PropertyDefImpl;
import org.apache.jackrabbit.webdav.spi.nodetype.NodeDefImpl;
import org.apache.jackrabbit.webdav.spi.JcrDavException;
import org.apache.jackrabbit.core.util.IteratorHelper;
import org.jdom.Document;
import org.jdom.Element;

import javax.jcr.nodetype.*;
import javax.jcr.*;
import java.util.*;

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
 *
 * @todo currently the nodetype report is not consistent with the general way of representing nodetype names (with NodetypeElement) in order to be compatible with the jackrabbit nodetype registry...
 * @todo for the same reason, not the complete nodetype-definition, but only the nodetype def as stored is represented.
 * @todo no namespace definition with response (> jackrabbit)... and nodetype element has same name as the one used with dav-properties
 */
public class NodeTypesReport implements Report, NodeTypeConstants {

    private static Logger log = Logger.getLogger(NodeTypesReport.class);

    /**
     * The registered type of this report.
     */
    public static final ReportType NODETYPES_REPORT = ReportType.register("nodetypes", NodeTypeConstants.NAMESPACE, NodeTypesReport.class);

    private NodeTypeManager ntMgr;
    private ReportInfo info;

    /**
     * Returns {@link #NODETYPES_REPORT} type.
     * @return {@link #NODETYPES_REPORT}
     * @see org.apache.jackrabbit.webdav.version.report.Report#getType()
     */
    public ReportType getType() {
        return NODETYPES_REPORT;
    }

    /**
     * @param resource
     * @throws IllegalArgumentException if the resource or the session retrieved
     * from the specified resource is <code>null</code>
     * @see Report#setResource(org.apache.jackrabbit.webdav.version.DeltaVResource)
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
            ntMgr = session.getRepositorySession().getWorkspace().getNodeTypeManager();
        } catch (RepositoryException e) {
            log.error(e.getMessage());
        }
    }

    /**
     * @param info
     * @throws IllegalArgumentException if the specified info does not contain
     * a jcr:nodetypes element.
     * @see Report#setInfo(org.apache.jackrabbit.webdav.version.report.ReportInfo)
     */
    public void setInfo(ReportInfo info) {
        if (info == null || !"nodetypes".equals(info.getReportElement().getName())) {
            throw new IllegalArgumentException("jcr:nodetypes element expected.");
        }
        this.info = info;
    }

    /**
     * Returns a Xml representation of the node type definition(s) according
     * to the info object.
     *
     * @return Xml representation of the node type definition(s)
     * @throws DavException if the specified nodetypes are not known or if another
     * error occurs while retrieving the nodetype definitions.
     * @see org.apache.jackrabbit.webdav.version.report.Report#toXml()
     */
    public Document toXml() throws DavException {
        if (info == null || ntMgr == null) {
            throw new DavException(DavServletResponse.SC_INTERNAL_SERVER_ERROR, "Error while running jcr:nodetypes report");
        }
        try {
            Element report = new Element(XML_NODETYPES);
            NodeTypeIterator ntIter = getNodeTypes();
            while (ntIter.hasNext()) {
                NodeType nt = ntIter.nextNodeType();
                Element ntDef = new Element(XML_NODETYPE);
                ntDef.setAttribute(ATTR_NAME, nt.getName());
                ntDef.setAttribute(ATTR_ISMIXIN, Boolean.toString(nt.isMixin()));
                ntDef.setAttribute(ATTR_HASORDERABLECHILDNODES, Boolean.toString(nt.hasOrderableChildNodes()));

		// declared supertypes
		NodeType[] snts = nt.getDeclaredSupertypes();
                Element supertypes = new Element(XML_SUPERTYPES);
		for (int i = 0; i < snts.length; i++) {
		    supertypes.addContent(new Element(XML_SUPERTYPE).setText(snts[i].getName()));
		}
		ntDef.addContent(supertypes);

		// declared childnode defs
		NodeDef[] cnd = nt.getChildNodeDefs();
		for (int i = 0; i < cnd.length; i++) {
		    if (cnd[i].getDeclaringNodeType().getName().equals(nt.getName())) {
			ntDef.addContent(NodeDefImpl.create(cnd[i]).toXml());
		    }
		}

		// declared propertyDefs
		PropertyDef[] pd = nt.getPropertyDefs();
		for (int i = 0; i < pd.length; i++) {
		    if (pd[i].getDeclaringNodeType().getName().equals(nt.getName())) {
			ntDef.addContent(PropertyDefImpl.create(pd[i]).toXml());
		    }
		}

                String primaryItemName = nt.getPrimaryItemName();
                if (primaryItemName != null) {
                    ntDef.setAttribute(ATTR_PRIMARYITEMNAME, primaryItemName);
                }
                report.addContent(ntDef);
            }

            Document reportDoc = new Document(report);
            return reportDoc;
        } catch (RepositoryException e) {
            throw new JcrDavException(e);
        }
    }

    /**
     * Parse the Xml element in the info object an return an interator over
     * the specified node types.
     *
     * @return
     * @throws RepositoryException
     * @throws DavException
     */
    private NodeTypeIterator getNodeTypes() throws RepositoryException, DavException {
        NodeTypeIterator ntIter = null;
        Iterator it = info.getReportElement().getChildren().iterator();
        while (it.hasNext() && ntIter == null) {
            Element elem = (Element) it.next();
            if (elem.getNamespace().equals(NAMESPACE)) {
                String name = elem.getName();
                if (XML_REPORT_ALLNODETYPES.equals(name)) {
                    ntIter = ntMgr.getAllNodeTypes();
                } else if (XML_REPORT_MIXINNODETYPES.equals(name)) {
                    ntIter = ntMgr.getMixinNodeTypes();
                } else if (XML_REPORT_PRIMARYNODETYPES.equals(name)) {
                    ntIter = ntMgr.getPrimaryNodeTypes();
                }
            }
        }
        // None of the simple types. test if a report for individual nodetypes
        // was request. If not, the request body is not valid.
        if (ntIter == null) {
            List ntList = new ArrayList();
            List elemList = info.getReportElement().getChildren(XML_NODETYPE, NAMESPACE);
            if (elemList.isEmpty()) {
                // throw exception if the request body does not contain a single jcr:nodetype element
                throw new DavException(DavServletResponse.SC_BAD_REQUEST, "NodeTypes report: request body has invalid format.");
            }
            Iterator elemIter = elemList.iterator();
            while (elemIter.hasNext()) {
                String nodetypeName = ((Element)elemIter.next()).getChildText(XML_NODETYPENAME, NAMESPACE);
                if (nodetypeName != null) {
                    ntList.add(ntMgr.getNodeType(nodetypeName));
                }
            }
            ntIter = new IteratorHelper(Collections.unmodifiableCollection(ntList));
        }

        return ntIter;
    }
}