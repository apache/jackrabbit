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
package org.apache.jackrabbit.webdav.jcr.version;

import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavResourceFactory;
import org.apache.jackrabbit.webdav.DavResourceLocator;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.jcr.DefaultItemCollection;
import org.apache.jackrabbit.webdav.jcr.ItemResourceConstants;
import org.apache.jackrabbit.webdav.jcr.JcrDavException;
import org.apache.jackrabbit.webdav.jcr.JcrDavSession;
import org.apache.jackrabbit.webdav.property.DavPropertyName;
import org.apache.jackrabbit.webdav.property.DefaultDavProperty;
import org.apache.jackrabbit.webdav.property.HrefProperty;
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
 * <code>VersionItemCollection</code> represents a JCR version.
 *
 * @see Version
 */
public class VersionItemCollection extends DefaultItemCollection
        implements VersionResource {

    private static Logger log = LoggerFactory.getLogger(VersionItemCollection.class);

    /**
     * Create a new <code>VersionItemCollection</code>.
     *
     * @param locator
     * @param session
     * @param factory
     */
    public VersionItemCollection(DavResourceLocator locator,
                                 JcrDavSession session,
                                 DavResourceFactory factory, Item item) {
        super(locator, session, factory, item);
        if (item == null || !(item instanceof Version)) {
            throw new IllegalArgumentException("Version item expected.");
        }
    }

    //----------------------------------------------< DavResource interface >---
    /**
     * @see org.apache.jackrabbit.webdav.DavResource#getSupportedMethods()
     */
    public String getSupportedMethods() {
        StringBuffer sb = new StringBuffer(ItemResourceConstants.METHODS);
        sb.append(", ").append(VersionResource.METHODS);
        return sb.toString();
    }

    //------------------------------------------< VersionResource interface >---
    /**
     * Modify the labels defined for the underlying repository version.
     *
     * @param labelInfo
     * @throws org.apache.jackrabbit.webdav.DavException
     * @see VersionResource#label(org.apache.jackrabbit.webdav.version.LabelInfo)
     * @see VersionHistory#addVersionLabel(String, String, boolean)
     * @see VersionHistory#removeVersionLabel(String)
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
                vh.addVersionLabel(item.getName(), labelInfo.getLabelName(), false);
            } else {
                // SET: move label if already existing
                vh.addVersionLabel(item.getName(), labelInfo.getLabelName(), true);
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
     * @return the {@link VersionHistoryResource} associated with this resource.
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
            DavResourceLocator loc = getLocatorFromItem(vh);
            return (VersionHistoryResource) createResourceFromLocator(loc);
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
        return ((Version)item).getContainingHistory();
    }

    //--------------------------------------------------------------------------
    /**
     * Define the set of reports supported by this resource.
     *
     * @see org.apache.jackrabbit.webdav.version.report.SupportedReportSetProperty
     */
    protected void initSupportedReports() {
        super.initSupportedReports();
        if (exists()) {
            supportedReports.addReportType(ReportType.VERSION_TREE);
        }
    }

    /**
     * Fill the property set for this resource.
     */
    protected void initProperties() {
        super.initProperties();

        if (exists()) {
            Version v = (Version)item;
            // created and creationDate properties
            try {
                String creationDate = DavConstants.creationDateFormat.format(v.getCreated().getTime());
                // replace dummy creation date from default collection
                properties.add(new DefaultDavProperty(DavPropertyName.CREATIONDATE, creationDate));

                // required, protected DAV:version-name property
                properties.add(new DefaultDavProperty(VERSION_NAME, v.getName(), true));

                // required, protected DAV:label-name-set property
                String[] labels = getVersionHistoryItem().getVersionLabels(v);
                properties.add(new LabelSetProperty(labels));

                // required DAV:predecessor-set (protected) and DAV:successor-set (computed) properties
                addHrefProperty(VersionResource.PREDECESSOR_SET, v.getPredecessors(), true);
                addHrefProperty(SUCCESSOR_SET, v.getSuccessors(), true);

                // required DAV:version-history (computed) property
                String vhHref = getLocatorFromItem(getVersionHistoryItem()).getHref(true);
                properties.add(new HrefProperty(VersionResource.VERSION_HISTORY, vhHref, true));

                // required DAV:checkout-set (computed) property
                PropertyIterator it = v.getReferences();
                List nodeList = new ArrayList();
                while (it.hasNext()) {
                    Property p = it.nextProperty();
                    if (JcrConstants.JCR_BASEVERSION.equals(p.getName())) {
                        Node n = p.getParent();
                        if (n.isCheckedOut()) {
                           nodeList.add(n);
                        }
                    }
                }
                addHrefProperty(CHECKOUT_SET, (Node[]) nodeList.toArray(new Node[nodeList.size()]), true);

            } catch (RepositoryException e) {
                log.error(e.getMessage());
            }
        }
    }
}