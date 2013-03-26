/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
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
package org.apache.jackrabbit.webdav.simple;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Property;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.webdav.DavCompliance;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavLocatorFactory;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.DavResourceFactory;
import org.apache.jackrabbit.webdav.DavResourceLocator;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.DavSession;
import org.apache.jackrabbit.webdav.property.DavProperty;
import org.apache.jackrabbit.webdav.property.DavPropertyName;
import org.apache.jackrabbit.webdav.property.HrefProperty;
import org.apache.jackrabbit.webdav.property.DefaultDavProperty;
import org.apache.jackrabbit.webdav.version.DeltaVConstants;
import org.apache.jackrabbit.webdav.version.DeltaVResource;
import org.apache.jackrabbit.webdav.version.OptionsInfo;
import org.apache.jackrabbit.webdav.version.OptionsResponse;
import org.apache.jackrabbit.webdav.version.report.Report;
import org.apache.jackrabbit.webdav.version.report.ReportInfo;
import org.apache.jackrabbit.webdav.version.report.ReportType;
import org.apache.jackrabbit.webdav.version.report.SupportedReportSetProperty;
import org.apache.jackrabbit.webdav.xml.DomUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

/**
 * The <code>DeltaVResourceImpl</code> encapsulates the functionality common to all
 * DeltaV compliant resources.
 */
public class DeltaVResourceImpl extends DavResourceImpl implements DeltaVResource {

    protected SupportedReportSetProperty supportedReports = new SupportedReportSetProperty();
    private static final Logger log = LoggerFactory.getLogger(DeltaVResourceImpl.class);

    private static final String DELTAV_COMPLIANCE_CLASSES = DavCompliance.concatComplianceClasses(
        new String[] {
            DavResourceImpl.COMPLIANCE_CLASSES,
            DavCompliance.BIND,
        }
    );

    public DeltaVResourceImpl(DavResourceLocator locator, DavResourceFactory factory, DavSession session, ResourceConfig config, Item item) throws DavException {
        super(locator, factory, session, config, (Node)item);
        initSupportedReports();
    }

    public DeltaVResourceImpl(DavResourceLocator locator, DavResourceFactory factory, DavSession session, ResourceConfig config, boolean isCollection) throws DavException {
        super(locator, factory, session, config, isCollection);
        initSupportedReports();
    }

    //---------------------------------------------------------< DavResource>---
    /**
     * @see org.apache.jackrabbit.webdav.DavResource#getComplianceClass()
     */
    @Override
    public String getComplianceClass() {
        return DELTAV_COMPLIANCE_CLASSES;
    }

    //------------------------------------------------------< DeltaVResource>---
    /**
     * @param optionsInfo
     * @return object to be used in the OPTIONS response body or <code>null</code>
     * @see DeltaVResource#getOptionResponse(org.apache.jackrabbit.webdav.version.OptionsInfo)
     */
    public OptionsResponse getOptionResponse(OptionsInfo optionsInfo) {
        OptionsResponse oR = null;
        if (optionsInfo != null) {
            oR = new OptionsResponse();
            // currently only DAV:version-history-collection-set is supported
            if (optionsInfo.containsElement(DeltaVConstants.XML_VH_COLLECTION_SET, DeltaVConstants.NAMESPACE)) {
                String[] hrefs = new String[] {
                    getLocatorFromNodePath("/"+JcrConstants.JCR_SYSTEM+"/"+JcrConstants.JCR_VERSIONSTORAGE).getHref(true)
                };
                oR.addEntry(DeltaVConstants.XML_VH_COLLECTION_SET, DeltaVConstants.NAMESPACE, hrefs);
            }
        }
        return oR;
    }

    /**
     * @param reportInfo
     * @return the requested report
     * @throws DavException
     * @see DeltaVResource#getReport(org.apache.jackrabbit.webdav.version.report.ReportInfo)
     */
    public Report getReport(ReportInfo reportInfo) throws DavException {
        if (reportInfo == null) {
            throw new DavException(DavServletResponse.SC_BAD_REQUEST, "A REPORT request must provide a valid XML request body.");
        }
        if (!exists()) {
            throw new DavException(DavServletResponse.SC_NOT_FOUND);
        }

        if (!supportedReports.isSupportedReport(reportInfo)) {
            Element condition = null;
            try {
                condition = DomUtil.createDocument().createElementNS("DAV:", "supported-report");
            } catch (ParserConfigurationException ex) {
                // we don't care THAT much
            }
            throw new DavException(DavServletResponse.SC_CONFLICT,
                    "Unknown report '" + reportInfo.getReportName() + "' requested.", null, condition);
        }
        return ReportType.getType(reportInfo).createReport(this, reportInfo);
    }

    /**
     * The JCR api does not provide methods to create new workspaces. Calling
     * <code>addWorkspace</code> on this resource will always fail.
     *
     * @param workspace
     * @throws DavException Always throws.
     * @see DeltaVResource#addWorkspace(org.apache.jackrabbit.webdav.DavResource)
     */
    public void addWorkspace(DavResource workspace) throws DavException {
        throw new DavException(DavServletResponse.SC_FORBIDDEN);
    }

    /**
     * Return an array of <code>DavResource</code> objects that are referenced
     * by the property with the specified name.
     *
     * @param hrefPropertyName
     * @return array of <code>DavResource</code>s
     * @throws DavException
     * @see DeltaVResource#getReferenceResources(org.apache.jackrabbit.webdav.property.DavPropertyName)
     */
    public DavResource[] getReferenceResources(DavPropertyName hrefPropertyName) throws DavException {
        DavProperty<?> prop = getProperty(hrefPropertyName);
        List<DavResource> resources = new ArrayList<DavResource>();
        if (prop != null && prop instanceof HrefProperty) {
            HrefProperty hp = (HrefProperty)prop;
            // process list of hrefs
            for (String href : hp.getHrefs()) {
                DavResourceLocator locator = getLocator().getFactory().createResourceLocator(getLocator().getPrefix(), href);
                resources.add(createResourceFromLocator(locator));
            }
        } else {
            throw new DavException(DavServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
        return resources.toArray(new DavResource[resources.size()]);
    }

    /**
     * Build a <code>DavResourceLocator</code> from the given nodePath path.
     *
     * @param nodePath
     * @return a new <code>DavResourceLocator</code>
     * @see DavLocatorFactory#createResourceLocator(String, String, String)
     */
    protected DavResourceLocator getLocatorFromNodePath(String nodePath) {
        DavResourceLocator loc = getLocator().getFactory().createResourceLocator(getLocator().getPrefix(), getLocator().getWorkspacePath(), nodePath, false);
        return loc;
    }


    /**
     * Build a new {@link DavResourceLocator} from the given repository node.
     *
     * @param repositoryNode
     * @return a new locator for the specified node.
     * @see #getLocatorFromNodePath(String)
     */
    protected DavResourceLocator getLocatorFromNode(Node repositoryNode) {
        String nodePath = null;
        try {
            if (repositoryNode != null) {
                nodePath = repositoryNode.getPath();
            }
        } catch (RepositoryException e) {
            // ignore: should not occur
            log.warn(e.getMessage());
        }
        return getLocatorFromNodePath(nodePath);
    }

    /**
     * Create a new <code>DavResource</code> from the given locator.
     * @param loc
     * @return new <code>DavResource</code>
     */
    protected DavResource createResourceFromLocator(DavResourceLocator loc)
            throws DavException {
        DavResource res = getFactory().createResource(loc, getSession());
        return res;
    }

    /**
     * Returns a {@link org.apache.jackrabbit.webdav.property.HrefProperty} with the
     * specified property name and values. Each node present in the specified
     * array is referenced in the resulting property.
     *
     * @param name
     * @param values
     * @param isProtected
     * @return HrefProperty
     */
    protected HrefProperty getHrefProperty(DavPropertyName name, Node[] values,
                                           boolean isProtected, boolean isCollection) {
        if (values == null) {
            return null;
        }
        String[] pHref = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            pHref[i] = getLocatorFromNode(values[i]).getHref(isCollection);
        }
        return new HrefProperty(name, pHref, isProtected);
    }

    /**
     * Initialize the supported reports field
     */
    protected void initSupportedReports() {
        if (exists()) {
            supportedReports.addReportType(ReportType.EXPAND_PROPERTY);
            if (isCollection()) {
                supportedReports.addReportType(ReportType.LOCATE_BY_HISTORY);
            }
        }
    }

    /**
     * Fill the property set for this resource.
     */
    @Override
    protected void initProperties() {
        if (!propsInitialized) {
            super.initProperties();
            if (exists()) {
                properties.add(supportedReports);

                // DAV:creator-displayname -> use jcr:createBy if present.
                Node n = getNode();
                try {
                    if (n.hasProperty(Property.JCR_CREATED_BY)) {
                        String createdBy = n.getProperty(Property.JCR_CREATED_BY).getString();
                        properties.add(new DefaultDavProperty<String>(DeltaVConstants.CREATOR_DISPLAYNAME, createdBy, true));
                    }
                } catch (RepositoryException e) {
                    log.debug("Error while accessing jcr:createdBy property");
                }
            }
        }
    }
}
