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
import org.apache.jackrabbit.webdav.spi.nodetype.NodeTypeElement;
import org.apache.jackrabbit.webdav.spi.JcrDavException;
import org.apache.jackrabbit.core.util.IteratorHelper;
import org.jdom.Document;
import org.jdom.Element;

import javax.jcr.nodetype.*;
import javax.jcr.*;
import javax.jcr.version.OnParentVersionAction;
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
            Element report = new Element("nodetypes-report", NAMESPACE);
            NodeTypeIterator ntIter = getNodeTypes();
            while (ntIter.hasNext()) {
                NodeType nt = ntIter.nextNodeType();
                Element ntDef = new Element(XML_NODETYPEDEFINITION, NAMESPACE);
                ntDef.addContent(new Element(XML_NODETYPENAME, NAMESPACE).setText(nt.getName()));

                if (nt.isMixin()) {
                    ntDef.addContent(new Element(XML_MIXIN, NAMESPACE));
                }
                if (nt.hasOrderableChildNodes()) {
                    ntDef.addContent(new Element(XML_ORDERABLECHILDNODES, NAMESPACE));
                }

                Element supertypes = new Element(XML_SUPERTYPES, NAMESPACE).addContent(Arrays.asList(NodeTypeElement.create(nt.getSupertypes())));
                ntDef.addContent(supertypes);
                Element declSupertypes = new Element(XML_DECLARED_SUPERTYPES, NAMESPACE).addContent(Arrays.asList(NodeTypeElement.create(nt.getDeclaredSupertypes())));
                ntDef.addContent(declSupertypes);

                NodeDef[] cnd = nt.getChildNodeDefs();
                for (int i = 0; i < cnd.length; i++) {
                    ntDef.addContent(getDefinitionElement(cnd[i]));
                }

                PropertyDef[] pd = nt.getPropertyDefs();
                for (int i = 0; i < pd.length; i++) {
                    ntDef.addContent(getDefinitionElement(pd[i]));
                }

                String primaryItemName = nt.getPrimaryItemName();
                if (primaryItemName != null) {
                    ntDef.addContent(new Element(XML_PRIMARYITEMNAME, NAMESPACE).setText(primaryItemName));
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
                if ("allnodetypes".equals(name)) {
                    ntIter = ntMgr.getAllNodeTypes();
                } else if ("mixinnodetypes".equals(name)) {
                    ntIter = ntMgr.getMixinNodeTypes();
                } else if ("primarynodetypes".equals(name)) {
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

    /**
     * Return the Xml representation of a {@link NodeDef}.
     *
     * @param def
     * @return Xml representation of the specified {@link NodeDef def}.
     */
    private Element getDefinitionElement(NodeDef def) {
        Element elem = getDefinitionElement(XML_CHILDNODEDEF, def);

        elem.setAttribute(ATTR_SAMENAMESIBS, Boolean.toString(def.allowSameNameSibs()), NAMESPACE);

        // defaultPrimaryType can be 'null'
        NodeType defaultPrimaryType = def.getDefaultPrimaryType();
        if (defaultPrimaryType != null) {
            Element ntElem = new Element(XML_DEFAULTPRIMARYTYPE, NAMESPACE);
            ntElem.addContent(new NodeTypeElement(defaultPrimaryType));
            elem.addContent(ntElem);
        }
        // reqPrimaryTypes: minimal set is nt:base.
        NodeType[] nts = def.getRequiredPrimaryTypes();
        Element reqPrimaryTypes = new Element(XML_REQUIREDPRIMARYTYPES, NAMESPACE);
        reqPrimaryTypes.addContent(Arrays.asList(NodeTypeElement.create(nts)));
        elem.addContent(reqPrimaryTypes);

        return elem;
    }

    /**
     * Returns the Xml representation of a {@link PropertyDef}.
     *
     * @param def
     * @return Xml representation of the specified {@link PropertyDef def}.
     */
    private Element getDefinitionElement(PropertyDef def) {
        Element elem = getDefinitionElement(XML_PROPERTYDEF, def);

        elem.setAttribute(ATTR_MULTIPLE, Boolean.toString(def.isMultiple()), NAMESPACE);
        elem.setAttribute(ATTR_TYPE, PropertyType.nameFromValue(def.getRequiredType()), NAMESPACE);

        // default values may be 'null'
        Value[] values = def.getDefaultValues();
        if (values != null) {
            Element dvElement = new Element(XML_DEFAULTVALUES, NAMESPACE);
            for (int i = 0; i < values.length; i++) {
                try {
                    Element valElem = new Element(XML_DEFAULTVALUE, NAMESPACE).setText(values[i].getString());
                    dvElement.addContent(valElem);
                } catch (RepositoryException e) {
                    // should not occur
                    log.error(e.getMessage());
                }
            }
            elem.addContent(dvElement);
        }
        // value constraints array is never null.
        Element constrElem = new Element(XML_VALUECONSTRAINTS, NAMESPACE);
        String[] constraints = def.getValueConstraints();
        for (int i = 0; i < constraints.length; i++) {
            constrElem.addContent(new Element(XML_VALUECONSTRAINT, NAMESPACE).setText(constraints[i]));
        }
        elem.addContent(constrElem);

        return elem;
    }

    /**
     * Returns the Xml representation of a {@link ItemDef} object.
     *
     * @param elementName
     * @param def
     * @return Xml representation of the specified {@link ItemDef def}.
     */
    private Element getDefinitionElement(String elementName, ItemDef def) {
        Element elem = new Element(elementName, NAMESPACE);
        elem.setAttribute(ATTR_NAME, def.getName(), NAMESPACE);
        elem.setAttribute(ATTR_AUTOCREATE, Boolean.toString(def.isAutoCreate()), NAMESPACE);
        elem.setAttribute(ATTR_MANDATORY, Boolean.toString(def.isMandatory()), NAMESPACE);
        elem.setAttribute(ATTR_ONPARENTVERSION, OnParentVersionAction.nameFromValue(def.getOnParentVersion()), NAMESPACE);
        elem.setAttribute(ATTR_PROTECTED, Boolean.toString(def.isProtected()), NAMESPACE);

        Element ntElem = new Element(XML_DECLARINGNODETYPE, NAMESPACE);
        ntElem.addContent(new NodeTypeElement(def.getDeclaringNodeType()));
        elem.addContent(ntElem);

        return elem;
    }
}