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

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;

import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.DavResourceFactory;
import org.apache.jackrabbit.webdav.DavResourceLocator;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.DavSession;
import org.apache.jackrabbit.webdav.MultiStatus;
import org.apache.jackrabbit.webdav.DavMethods;
import org.apache.jackrabbit.webdav.jcr.JcrDavException;
import org.apache.jackrabbit.webdav.property.DefaultDavProperty;
import org.apache.jackrabbit.webdav.property.HrefProperty;
import org.apache.jackrabbit.webdav.version.LabelInfo;
import org.apache.jackrabbit.webdav.version.MergeInfo;
import org.apache.jackrabbit.webdav.version.UpdateInfo;
import org.apache.jackrabbit.webdav.version.VersionControlledResource;
import org.apache.jackrabbit.webdav.version.VersionHistoryResource;
import org.apache.jackrabbit.webdav.version.VersionResource;
import org.apache.jackrabbit.webdav.version.VersionableResource;
import org.apache.jackrabbit.webdav.version.report.ReportType;
import org.apache.jackrabbit.webdav.version.report.SupportedReportSetProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/**
 * <code>VersionControlledResourceImpl</code> represents a JCR node item and
 * covers all functionality related to versioning of {@link Node}s.
 *
 * @see Node
 */
public class VersionControlledResourceImpl extends DeltaVResourceImpl
        implements VersionControlledResource {

    private static final Logger log = LoggerFactory.getLogger(VersionControlledResourceImpl.class);

    /**
     * Create a new {@link org.apache.jackrabbit.webdav.DavResource}.
     *
     * @param locator
     * @param factory
     * @param session
     * @param config
     * @param item
     * @throws DavException
     */
    public VersionControlledResourceImpl(DavResourceLocator locator, DavResourceFactory factory, DavSession session, ResourceConfig config, Item item) throws DavException {
        super(locator, factory, session, config, item);
        initSupportedReports();
    }

    /**
     * Create a new {@link org.apache.jackrabbit.webdav.DavResource}.
     *
     * @param locator
     * @param factory
     * @param session
     * @param config
     * @param isCollection
     * @throws DavException
     */
    public VersionControlledResourceImpl(DavResourceLocator locator, DavResourceFactory factory, DavSession session, ResourceConfig config, boolean isCollection) throws DavException {
        super(locator, factory, session, config, isCollection);
        initSupportedReports();
    }

    //--------------------------------------------------------< DavResource >---
    /**
     * Return a comma separated string listing the supported method names.
     *
     * @return the supported method names.
     * @see org.apache.jackrabbit.webdav.DavResource#getSupportedMethods()
     */
    @Override
    public String getSupportedMethods() {
        StringBuffer sb = new StringBuffer(super.getSupportedMethods());
        // Versioning support
        sb.append(", ").append(VersionableResource.METHODS);
        if (isVersionControlled()) {
            try {
                if (getNode().isCheckedOut()) {
                    sb.append(", ").append(DavMethods.METHOD_CHECKIN);
                } else {
                    sb.append(", ").append(DavMethods.METHOD_CHECKOUT);
                    sb.append(", ").append(DavMethods.METHOD_LABEL);
                }
            } catch (RepositoryException e) {
                // should not occur.
                log.error(e.getMessage());
            }
        }
        return sb.toString();
    }

    //------------------------------------------< VersionControlledResource >---
    /**
     * Adds version control to this resource. If the resource is already under
     * version control, this method has no effect. If this resource is a Collection
     * resource this method fails with {@link DavServletResponse#SC_METHOD_NOT_ALLOWED}.
     *
     * @throws org.apache.jackrabbit.webdav.DavException if this resource does not
     * exist yet, is a collection or if an error occurs while making the
     * underlying node versionable.
     * @see org.apache.jackrabbit.webdav.version.VersionableResource#addVersionControl()
     */
    public void addVersionControl() throws DavException {
        if (!exists()) {
            throw new DavException(DavServletResponse.SC_NOT_FOUND);
        }
        if (isCollection()) {
            // since the version-controlled-collection feature is not supported
            // collections may not be put under dav version control even if
            // the underlying node was / could be made jcr versionable.
            throw new DavException(DavServletResponse.SC_METHOD_NOT_ALLOWED);
        }
        if (!isVersionControlled()) {
            Node item = getNode();
            try {
                item.addMixin(JcrConstants.MIX_VERSIONABLE);
                item.save();
            } catch (RepositoryException e) {
                throw new JcrDavException(e);
            }
        } // else: is already version controlled -> ignore
    }

    /**
     * Calls {@link javax.jcr.Node#checkin()} on the underlying repository node.
     *
     * @throws org.apache.jackrabbit.webdav.DavException
     * @see org.apache.jackrabbit.webdav.version.VersionControlledResource#checkin()
     */
    public String checkin() throws DavException {
        if (!exists()) {
            throw new DavException(DavServletResponse.SC_NOT_FOUND);
        }
        if (!isVersionControlled()) {
            throw new DavException(DavServletResponse.SC_METHOD_NOT_ALLOWED);
        }
        try {
            Version v = getNode().checkin();
            String versionHref = getLocatorFromNode(v).getHref(false);
            return versionHref;
        } catch (RepositoryException e) {
            // UnsupportedRepositoryException should not occur
            throw new JcrDavException(e);
        }
    }

    /**
     * Calls {@link javax.jcr.Node#checkout()} on the underlying repository node.
     *
     * @throws org.apache.jackrabbit.webdav.DavException
     * @see org.apache.jackrabbit.webdav.version.VersionControlledResource#checkout()
     */
    public void checkout() throws DavException {
        if (!exists()) {
            throw new DavException(DavServletResponse.SC_NOT_FOUND);
        }
        if (!isVersionControlled()) {
            throw new DavException(DavServletResponse.SC_METHOD_NOT_ALLOWED);
        }
        try {
            getNode().checkout();
        } catch (RepositoryException e) {
            // UnsupportedRepositoryException should not occur
            throw new JcrDavException(e);
        }
    }


    /**
     * UNCHECKOUT cannot be implemented on top of JSR 170 repository.
     * Therefore this methods always throws a <code>DavException</code> with error code
     * {@link org.apache.jackrabbit.webdav.DavServletResponse#SC_NOT_IMPLEMENTED}.
     *
     * @throws org.apache.jackrabbit.webdav.DavException
     * @see org.apache.jackrabbit.webdav.version.VersionControlledResource#uncheckout()
     */
    public void uncheckout() throws DavException {
        throw new DavException(DavServletResponse.SC_NOT_IMPLEMENTED);
    }

    /**
     * UPDATE feature is not (yet) supported. This method allows fails with
     * {@link DavServletResponse#SC_NOT_IMPLEMENTED}.
     *
     * @param updateInfo
     * @return
     * @throws DavException
     * @see VersionControlledResource#update(UpdateInfo)
     */
    public MultiStatus update(UpdateInfo updateInfo) throws DavException {
        throw new DavException(DavServletResponse.SC_NOT_IMPLEMENTED);
    }

    /**
     * MERGE feature is not (yet) supported. This method allows fails with
     * {@link DavServletResponse#SC_NOT_IMPLEMENTED}.
     *
     * @param mergeInfo
     * @return
     * @throws DavException
     * @see VersionControlledResource#merge(MergeInfo)
     */
    public MultiStatus merge(MergeInfo mergeInfo) throws DavException {
        throw new DavException(DavServletResponse.SC_NOT_IMPLEMENTED);
    }

    /**
     * Modify the labels present with the versions of this resource.
     *
     * @param labelInfo
     * @throws DavException
     * @see VersionControlledResource#label(LabelInfo)
     * @see javax.jcr.version.VersionHistory#addVersionLabel(String, String, boolean)
     * @see javax.jcr.version.VersionHistory#removeVersionLabel(String)
     */
    public void label(LabelInfo labelInfo) throws DavException {
        if (labelInfo == null) {
            throw new DavException(DavServletResponse.SC_BAD_REQUEST, "Valid label request body required.");
        }
        if (!exists()) {
            throw new DavException(DavServletResponse.SC_NOT_FOUND);
        }

        try {
            if (!isVersionControlled() || getNode().isCheckedOut()) {
                throw new DavException(DavServletResponse.SC_PRECONDITION_FAILED, "A LABEL request may only be applied to a version-controlled, checked-in resource.");
            }
            DavResource[] resArr = this.getReferenceResources(CHECKED_IN);
            if (resArr.length == 1 && resArr[0] instanceof VersionResource) {
                ((VersionResource)resArr[0]).label(labelInfo);
            } else {
                throw new DavException(DavServletResponse.SC_INTERNAL_SERVER_ERROR, "DAV:checked-in property on '" + getHref() + "' did not point to a single VersionResource.");
            }
        } catch (RepositoryException e) {
            throw new JcrDavException(e);
        }
    }

    /**
     * Returns the {@link javax.jcr.version.VersionHistory} associated with the repository node.
     * If the node is not versionable an exception is thrown.
     *
     * @return the {@link VersionHistoryResource} associated with this resource.
     * @throws org.apache.jackrabbit.webdav.DavException
     * @see org.apache.jackrabbit.webdav.version.VersionControlledResource#getVersionHistory()
     * @see javax.jcr.Node#getVersionHistory()
     */
    public VersionHistoryResource getVersionHistory() throws DavException {
        if (!exists()) {
            throw new DavException(DavServletResponse.SC_NOT_FOUND);
        }
        if (!isVersionControlled()) {
            throw new DavException(DavServletResponse.SC_FORBIDDEN);
        }
        try {
            VersionHistory vh = getNode().getVersionHistory();
            DavResourceLocator loc = getLocatorFromNode(vh);
            DavResource vhr =  createResourceFromLocator(loc);
            if (vhr instanceof VersionHistoryResource) {
                return (VersionHistoryResource)vhr;
            } else {
                // severe error since resource factory doesn't behave correctly.
                throw new DavException(DavServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        } catch (RepositoryException e) {
            throw new JcrDavException(e);
        }
    }

    //--------------------------------------------------------------------------
    /**
     * Define the set of reports supported by this resource.
     *
     * @see SupportedReportSetProperty
     * @see DeltaVResourceImpl#initSupportedReports()
     */
    @Override
    protected void initSupportedReports() {
        super.initSupportedReports();
        if (exists()) {
            supportedReports.addReportType(ReportType.LOCATE_BY_HISTORY);
            if (isVersionControlled()) {
                supportedReports.addReportType(ReportType.VERSION_TREE);
            }
        }
    }

    /**
     * Fill the property set for this resource.
     * @see DavResourceImpl#initProperties()
     */
    @Override
    protected void initProperties() {
        if (!propsInitialized) {
            super.initProperties();
            if (isVersionControlled()) {
                Node n = getNode();
                // properties defined by RFC 3253 for version-controlled resources
                try {
                    // DAV:version-history (computed)
                    String vhHref = getLocatorFromNode(n.getVersionHistory()).getHref(true);
                    properties.add(new HrefProperty(VERSION_HISTORY, vhHref, true));

                    // DAV:auto-version property: there is no auto version, explicit CHECKOUT is required.
                    properties.add(new DefaultDavProperty(AUTO_VERSION, null, true));

                    // baseVersion -> used for DAV:checked-out or DAV:checked-in
                    String baseVHref = getLocatorFromNode(n.getBaseVersion()).getHref(false);
                    if (n.isCheckedOut()) {
                        // DAV:predecessors property
                        if (n.hasProperty(JcrConstants.JCR_PREDECESSORS)) {
                            Value[] pv = n.getProperty(JcrConstants.JCR_PREDECESSORS).getValues();
                            Node[] predecessors = new Node[pv.length];
                            for (int i = 0; i < pv.length; i++) {
                                predecessors[i] = n.getSession().getNodeByIdentifier(pv[i].getString());
                            }
                            properties.add(getHrefProperty(VersionResource.PREDECESSOR_SET, predecessors, true, false));
                        }
                        // DAV:checked-out property (protected)
                        properties.add(new HrefProperty(CHECKED_OUT, baseVHref, true));
                    } else {
                        // DAV:checked-in property (protected)
                        properties.add(new HrefProperty(CHECKED_IN, baseVHref, true));
                    }
                } catch (RepositoryException e) {
                    log.error(e.getMessage());
                }
            }
        }
    }

    /**
     * @return true, if this resource is a non-collection resource and represents
     * an existing repository node that has the mixin nodetype 'mix:versionable' set.
     */
    private boolean isVersionControlled() {
        boolean vc = false;
        // since the version-controlled-collection feature is not supported
        // all collection are excluded from version-control even if the
        // underlying node was JCR versionable.
        if (exists() && !isCollection()) {
            Node item = getNode();
            try {
                vc = item.isNodeType(JcrConstants.MIX_VERSIONABLE);
            } catch (RepositoryException e) {
                log.warn(e.getMessage());
            }
        }
        return vc;
    }
}
