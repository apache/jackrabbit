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

import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.DavResourceFactory;
import org.apache.jackrabbit.webdav.DavResourceLocator;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.DavSession;
import org.apache.jackrabbit.webdav.DavResourceIterator;
import org.apache.jackrabbit.webdav.DavResourceIteratorImpl;
import org.apache.jackrabbit.webdav.MultiStatusResponse;
import org.apache.jackrabbit.webdav.util.HttpDateFormat;
import org.apache.jackrabbit.webdav.io.InputContext;
import org.apache.jackrabbit.webdav.jcr.JcrDavException;
import org.apache.jackrabbit.webdav.property.DavPropertyName;
import org.apache.jackrabbit.webdav.property.DefaultDavProperty;
import org.apache.jackrabbit.webdav.property.HrefProperty;
import org.apache.jackrabbit.webdav.property.DavProperty;
import org.apache.jackrabbit.webdav.property.PropEntry;
import org.apache.jackrabbit.webdav.version.LabelInfo;
import org.apache.jackrabbit.webdav.version.LabelSetProperty;
import org.apache.jackrabbit.webdav.version.VersionHistoryResource;
import org.apache.jackrabbit.webdav.version.VersionResource;
import org.apache.jackrabbit.webdav.version.report.ReportType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;

import java.util.ArrayList;
import java.util.List;

/**
 * <code>VersionResourceImpl</code> represents a JCR version.
 *
 * @see Version
 */
public class VersionResourceImpl extends DeltaVResourceImpl implements VersionResource {

    private static final Logger log = LoggerFactory.getLogger(VersionResourceImpl.class);

    /**
     * Create a new {@link org.apache.jackrabbit.webdav.DavResource}.
     * @param locator
     * @param factory
     * @param session
     * @param config
     * @param item
     * @throws DavException
     *
     */
    public VersionResourceImpl(DavResourceLocator locator, DavResourceFactory factory, DavSession session, ResourceConfig config, Item item) throws DavException {
        super(locator, factory, session, config, item);
        if (getNode() == null || !(getNode() instanceof Version)) {
            throw new IllegalArgumentException("Version item expected.");
        }
    }

    //--------------------------------------------------------< DavResource >---
    /**
     * Since this implementation of <code>VersionResource</code> never is a
     * version belonging to a version controlled collection, this method always
     * returns <code>false</code> not respecting the configuration.
     *
     * @return always false
     */
    @Override
    public boolean isCollection() {
        return false;
    }

    /**
     * @return An empty <code>DavResourceIterator</code>
     */
    @Override
    public DavResourceIterator getMembers() {
        return DavResourceIteratorImpl.EMPTY;
    }

    /**
     * The version storage is read-only -&gt; fails with 403.
     *
     * @see DavResource#addMember(DavResource, InputContext)
     */
    @Override
    public void addMember(DavResource member, InputContext inputContext) throws DavException {
        throw new DavException(DavServletResponse.SC_FORBIDDEN);
    }

    /**
     * The version storage is read-only -&gt; fails with 403.
     *
     * @see DavResource#removeMember(DavResource)
     */
    @Override
    public void removeMember(DavResource member) throws DavException {
        throw new DavException(DavServletResponse.SC_FORBIDDEN);
    }

    /**
     * Version storage is read-only -&gt; fails with 403.
     *
     * @see DavResource#setProperty(DavProperty)
     */
    @Override
    public void setProperty(DavProperty<?> property) throws DavException {
        throw new DavException(DavServletResponse.SC_FORBIDDEN);
    }

    /**
     * Version storage is read-only -&gt; fails with 403.
     *
     * @see DavResource#removeProperty(DavPropertyName)
     */
    @Override
    public void removeProperty(DavPropertyName propertyName) throws DavException {
        throw new DavException(DavServletResponse.SC_FORBIDDEN);
    }

    /**
     * Version storage is read-only -&gt; fails with 403.
     *
     * @see DavResource#alterProperties(List)
     */
    @Override
    public MultiStatusResponse alterProperties(List<? extends PropEntry> changeList) throws DavException {
        throw new DavException(DavServletResponse.SC_FORBIDDEN);
    }

    //----------------------------------------------------< VersionResource >---
    /**
     * Modify the labels defined for the underlying repository version.
     *
     * @param labelInfo
     * @throws org.apache.jackrabbit.webdav.DavException
     * @see VersionResource#label(org.apache.jackrabbit.webdav.version.LabelInfo)
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
            VersionHistory vh = getVersionHistoryItem();
            if (labelInfo.getType() == LabelInfo.TYPE_REMOVE) {
                vh.removeVersionLabel(labelInfo.getLabelName());
            } else if (labelInfo.getType() == LabelInfo.TYPE_ADD) {
                // ADD: only add if not yet existing
                vh.addVersionLabel(getNode().getName(), labelInfo.getLabelName(), false);
            } else {
                // SET: move label if already existing
                vh.addVersionLabel(getNode().getName(), labelInfo.getLabelName(), true);
            }
        } catch (RepositoryException e) {
            throw new JcrDavException(e);
        }
    }

    /**
     * Returns the {@link VersionHistory} associated with the repository version.
     * Note: in contrast to a versionable node, the version history of a version
     * item is always represented by its nearest ancestor.
     *
     * @return the {@link org.apache.jackrabbit.webdav.version.VersionHistoryResource} associated with this resource.
     * @throws org.apache.jackrabbit.webdav.DavException
     * @see org.apache.jackrabbit.webdav.version.VersionResource#getVersionHistory()
     * @see javax.jcr.Item#getParent()
     */
    public VersionHistoryResource getVersionHistory() throws DavException {
        if (!exists()) {
            throw new DavException(DavServletResponse.SC_NOT_FOUND);
        }

        try {
            VersionHistory vh = getVersionHistoryItem();
            DavResourceLocator loc = getLocatorFromNode(vh);
            DavResource vhr = createResourceFromLocator(loc);
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

    /**
     * Return versionhistory that contains this version item
     *
     * @return versionhistory that contains this version item
     * @throws RepositoryException
     * @see javax.jcr.version.Version#getContainingHistory()
     */
    private VersionHistory getVersionHistoryItem() throws RepositoryException {
        return ((Version)getNode()).getContainingHistory();
    }

    //--------------------------------------------------------------------------
    /**
     * Define the set of reports supported by this resource.
     *
     * @see org.apache.jackrabbit.webdav.version.report.SupportedReportSetProperty
     */
    @Override
    protected void initSupportedReports() {
        super.initSupportedReports();
        if (exists()) {
            supportedReports.addReportType(ReportType.VERSION_TREE);
        }
    }

    /**
     * Fill the property set for this resource.
     */
    @Override
    protected void initProperties() {
        if (!propsInitialized) {
            super.initProperties();
            Version v = (Version) getNode();
            try {
                String creationDate = HttpDateFormat.creationDateFormat().format(v.getCreated().getTime());
                // replace dummy creation date from default collection
                properties.add(new DefaultDavProperty<String>(DavPropertyName.CREATIONDATE, creationDate));

                // required, protected DAV:version-name property
                properties.add(new DefaultDavProperty<String>(VERSION_NAME, v.getName(), true));

                // required, protected DAV:label-name-set property
                String[] labels = getVersionHistoryItem().getVersionLabels(v);
                properties.add(new LabelSetProperty(labels));

                // required DAV:predecessor-set (protected) and DAV:successor-set (computed) properties
                properties.add(getHrefProperty(VersionResource.PREDECESSOR_SET, v.getPredecessors(), true, false));
                properties.add(getHrefProperty(SUCCESSOR_SET, v.getSuccessors(), true, false));

                // required DAV:version-history (computed) property
                String vhHref = getLocatorFromNode(getVersionHistoryItem()).getHref(true);
                properties.add(new HrefProperty(VersionResource.VERSION_HISTORY, vhHref, true));

                // required DAV:checkout-set (computed) property
                PropertyIterator it = v.getReferences();
                List<Node> nodeList = new ArrayList<Node>();
                while (it.hasNext()) {
                    Property p = it.nextProperty();
                    if (JcrConstants.JCR_BASEVERSION.equals(p.getName())) {
                        Node n = p.getParent();
                        if (n.isCheckedOut()) {
                            nodeList.add(n);
                        }
                    }
                }
                properties.add(getHrefProperty(CHECKOUT_SET, nodeList.toArray(new Node[nodeList.size()]), true, false));

            } catch (RepositoryException e) {
                log.error(e.getMessage());
            }
        }
    }
}
