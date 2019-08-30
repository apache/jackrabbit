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
package org.apache.jackrabbit.webdav.jcr;

import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavLocatorFactory;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.DavResourceFactory;
import org.apache.jackrabbit.webdav.DavResourceLocator;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.MultiStatus;
import org.apache.jackrabbit.webdav.MultiStatusResponse;
import org.apache.jackrabbit.webdav.jcr.property.JcrDavPropertyNameSet;
import org.apache.jackrabbit.webdav.property.DavPropertyName;
import org.apache.jackrabbit.webdav.property.DefaultDavProperty;
import org.apache.jackrabbit.webdav.property.HrefProperty;
import org.apache.jackrabbit.webdav.property.DavProperty;
import org.apache.jackrabbit.webdav.property.PropEntry;
import org.apache.jackrabbit.webdav.version.LabelInfo;
import org.apache.jackrabbit.webdav.version.MergeInfo;
import org.apache.jackrabbit.webdav.version.UpdateInfo;
import org.apache.jackrabbit.webdav.version.VersionControlledResource;
import org.apache.jackrabbit.webdav.version.VersionHistoryResource;
import org.apache.jackrabbit.webdav.version.VersionResource;
import org.apache.jackrabbit.webdav.version.VersionableResource;
import org.apache.jackrabbit.webdav.version.report.ReportType;
import org.apache.jackrabbit.webdav.version.report.SupportedReportSetProperty;
import org.apache.jackrabbit.webdav.xml.DomUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.observation.EventListener;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionManager;
import java.util.List;
import java.util.Collections;

/**
 * <code>VersionControlledItemCollection</code> represents a JCR node item and
 * covers all functionality related to versioning of {@link Node}s.
 *
 * @see Node
 */
public class VersionControlledItemCollection extends DefaultItemCollection
        implements VersionControlledResource {

    private static Logger log = LoggerFactory.getLogger(VersionControlledItemCollection.class);

    /**
     * Create a new <code>VersionControlledItemCollection</code>.
     *
     * @param locator
     * @param session
     * @param factory
     * @param item
     */
    public VersionControlledItemCollection(DavResourceLocator locator,
                                           JcrDavSession session,
                                           DavResourceFactory factory,
                                           Item item) {
        super(locator, session, factory, item);
        if (exists() && !(item instanceof Node)) {
            throw new IllegalArgumentException("A collection resource can not be constructed from a Property item.");
        }
    }

    //----------------------------------------------< DavResource interface >---
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
                if (((Node)item).isCheckedOut()) {
                    sb.append(", ").append(VersionControlledResource.methods_checkedOut);
                } else {
                    sb.append(", ").append(VersionControlledResource.methods_checkedIn);
                }
            } catch (RepositoryException e) {
                // should not occur.
                log.error(e.getMessage());
            }
        }
        return sb.toString();
    }

    @Override
    public DavProperty<?> getProperty(DavPropertyName name) {
        DavProperty prop = super.getProperty(name);
        if (prop == null && isVersionControlled()) {
            Node n = (Node) item;
            // properties defined by RFC 3253 for version-controlled resources
            // workspace property already set in AbstractResource.initProperties()
            try {
                if (VERSION_HISTORY.equals(name)) {
                    // DAV:version-history (computed)
                    String vhHref = getLocatorFromItem(n.getVersionHistory()).getHref(true);
                    prop  = new HrefProperty(VERSION_HISTORY, vhHref, true);
                } else if (CHECKED_OUT.equals(name) && n.isCheckedOut()) {
                    // DAV:checked-out property (protected)
                    String baseVHref = getLocatorFromItem(n.getBaseVersion()).getHref(true);
                    prop = new HrefProperty(CHECKED_OUT, baseVHref, true);
                } else if (CHECKED_IN.equals(name) && !n.isCheckedOut()) {
                    // DAV:checked-in property (protected)
                    String baseVHref = getLocatorFromItem(n.getBaseVersion()).getHref(true);
                    prop = new HrefProperty(CHECKED_IN, baseVHref, true);
                }
            } catch (RepositoryException e) {
                log.error(e.getMessage());
            }
        }
                
        return prop;
    }

    /**
     * @param changeList
     * @throws DavException
     * @see DefaultItemCollection#alterProperties(List)
     * for additional description of non-compliant behaviour.
     */
    @Override
    public MultiStatusResponse alterProperties(List<? extends PropEntry> changeList) throws DavException {
        /* first resolve merge conflict since they cannot be handled by
           setting property values in jcr (and are persisted immediately).
           NOTE: this violates RFC 2518 that requires that proppatch
           is processed in the order entries are present in the xml and that
           required that no changes must be persisted if any set/remove fails.
        */
        // TODO: solve violation of RFC 2518
        resolveMergeConflict(changeList);
        // alter other properties only if merge-conflicts could be handled
        return super.alterProperties(changeList);
    }

    /**
     * Resolve one or multiple merge conflicts present on this resource. Please
     * note that the 'setProperties' or 'removeProperties' set my contain additional
     * resource properties, that need to be changed. Those properties are left
     * untouched, whereas the {@link #AUTO_MERGE_SET DAV:auto-merge-set}, is
     * removed from the list upon successful resolution of a merge conflict.<br>
     * If the removeProperties or setProperties set do not contain the mentioned
     * merge conflict resource properties or if the value of those properties do
     * not allow for a resolution of an existing merge conflict, this method
     * returns silently.
     *
     * @param changeList
     * @throws org.apache.jackrabbit.webdav.DavException
     * @see Node#doneMerge(Version)
     * @see Node#cancelMerge(Version)
     */
    private void resolveMergeConflict(List<? extends PropEntry> changeList) throws DavException {
        if (!exists()) {
            throw new DavException(DavServletResponse.SC_NOT_FOUND);
        }
        try {
            Node n = (Node) item;
            VersionManager vMgr = getVersionManager();
            String path = item.getPath();

            DavProperty<?> autoMergeSet = null;
            DavProperty<?> predecessorSet = null;
            /* find DAV:auto-merge-set entries. If none exists no attempt is made
               to resolve merge conflict > return silently */
            for (int i = 0; i < changeList.size(); i++) {
                PropEntry propEntry = changeList.get(i);
                // If DAV:auto-merge-set is DavPropertyName all remaining merge
                // conflicts are resolved with 'cancel'
                if (propEntry instanceof DavPropertyName && AUTO_MERGE_SET.equals(propEntry)) {
                    // retrieve the current jcr:mergeFailed property values
                    if (!n.hasProperty(JcrConstants.JCR_MERGEFAILED)) {
                        throw new DavException(DavServletResponse.SC_CONFLICT, "Attempt to resolve non-existing merge conflicts.");
                    }
                    Value[] mergeFailed = n.getProperty(JcrConstants.JCR_MERGEFAILED).getValues();
                    for (Value value : mergeFailed) {
                        vMgr.cancelMerge(path, (Version) getRepositorySession().getNodeByIdentifier(value.getString()));
                    }
                    // remove this entry from the changeList
                    changeList.remove(propEntry);
                } else if (propEntry instanceof DavProperty) {
                    if (AUTO_MERGE_SET.equals(((DavProperty<?>)propEntry).getName())) {
                        autoMergeSet = (DavProperty<?>) propEntry;
                    } else if (PREDECESSOR_SET.equals(((DavProperty<?>)propEntry).getName())) {
                        predecessorSet = (DavProperty<?>) propEntry;
                    }
                }
            }

            // If DAV:auto-merge-set is a DavProperty merge conflicts need to be
            // resolved individually according to the DAV:predecessor-set property.
            if (autoMergeSet != null) {
                // retrieve the current jcr:mergeFailed property values
                if (!n.hasProperty(JcrConstants.JCR_MERGEFAILED)) {
                    throw new DavException(DavServletResponse.SC_CONFLICT, "Attempt to resolve non-existing merge conflicts.");
                }

                List<String> mergeset = new HrefProperty(autoMergeSet).getHrefs();
                List<String> predecL;
                if (predecessorSet == null) {
                    predecL = Collections.emptyList();
                } else {
                    predecL = new HrefProperty(predecessorSet).getHrefs();
                }

                Session session = getRepositorySession();
                // loop over the mergeFailed values (versions) and test whether they are
                // removed from the DAV:auto-merge-set thus indicating resolution.
                Value[] mergeFailed = n.getProperty(JcrConstants.JCR_MERGEFAILED).getValues();
                for (Value value : mergeFailed) {
                    // build version-href from each entry in the jcr:mergeFailed property
                    // in order to be able to compare to the entries in the HrefProperty.
                    Version version = (Version) session.getNodeByIdentifier(value.getString());
                    String href = getLocatorFromItem(version).getHref(true);

                    // Test if that version has been removed from the merge-set.
                    // thus indicating that this merge conflict needs to be resolved.
                    if (!mergeset.contains(href)) {
                        // If the conflict value has been moved over from DAV:auto-merge-set
                        // to the predecessor-set, resolution with 'doneMerge' is
                        // appropriate. If the value has been removed from the
                        // merge-set but not added to the predecessors 'cancelMerge'
                        // must be called.
                        if (predecL.contains(href)) {
                            vMgr.doneMerge(path, version);
                        } else {
                            vMgr.cancelMerge(path, version);
                        }
                    }
                }
                // after successful resolution of merge-conflicts according to
                // DAV:auto-merge-set and DAV:predecessor-set remove these entries
                // from the changeList.
                changeList.remove(autoMergeSet);
                if (predecessorSet != null) {
                    changeList.remove(predecessorSet);
                }
            }
        } catch (RepositoryException e) {
            throw new JcrDavException(e);
        }
    }

    //--------------------------------------< VersionableResource interface >---
    /**
     * Adds version control to this resource. If the resource is already under
     * version control, this method has no effect.
     *
     * @throws org.apache.jackrabbit.webdav.DavException if this resource does not
     * exist yet or if an error occurs while making the underlying node versionable.
     * @see org.apache.jackrabbit.webdav.version.VersionableResource#addVersionControl()
     */
    @Override
    public void addVersionControl() throws DavException {
        if (!exists()) {
            throw new DavException(DavServletResponse.SC_NOT_FOUND);
        }
        if (!isVersionControlled()) {
            try {
                ((Node)item).addMixin(JcrConstants.MIX_VERSIONABLE);
                item.save();
            } catch (RepositoryException e) {
                throw new JcrDavException(e);
            }
        } // else: is already version controlled -> ignore
    }

    //--------------------------------< VersionControlledResource interface >---
    /**
     * Calls {@link javax.jcr.Node#checkin()} on the underlying repository node.
     *
     * @throws org.apache.jackrabbit.webdav.DavException
     * @see org.apache.jackrabbit.webdav.version.VersionControlledResource#checkin()
     */
    @Override
    public String checkin() throws DavException {
        if (!exists()) {
            throw new DavException(DavServletResponse.SC_NOT_FOUND);
        }
        if (!isVersionControlled()) {
            throw new DavException(DavServletResponse.SC_METHOD_NOT_ALLOWED);
        }
        try {
            Version v = getVersionManager().checkin(item.getPath());
            String versionHref = getLocatorFromItem(v).getHref(true);
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
    @Override
    public void checkout() throws DavException {
        if (!exists()) {
            throw new DavException(DavServletResponse.SC_NOT_FOUND);
        }
        if (!isVersionControlled()) {
            throw new DavException(DavServletResponse.SC_METHOD_NOT_ALLOWED);
        }
        try {
            getVersionManager().checkout(item.getPath());
        } catch (RepositoryException e) {
            // UnsupportedRepositoryException should not occur
            throw new JcrDavException(e);
        }
    }

    /**
     * Not implemented. Always throws a <code>DavException</code> with error code
     * {@link org.apache.jackrabbit.webdav.DavServletResponse#SC_NOT_IMPLEMENTED}.
     *
     * @throws org.apache.jackrabbit.webdav.DavException
     * @see org.apache.jackrabbit.webdav.version.VersionControlledResource#uncheckout()
     */
    @Override
    public void uncheckout() throws DavException {
        throw new DavException(DavServletResponse.SC_NOT_IMPLEMENTED);
    }

    /**
     * Perform an update on this resource. Depending on the format of the <code>updateInfo</code>
     * this is translated to one of the following methods defined by the JCR API:
     * <ul>
     * <li>{@link Node#restore(javax.jcr.version.Version, boolean)}</li>
     * <li>{@link Node#restore(javax.jcr.version.Version, String, boolean)}</li>
     * <li>{@link Node#restoreByLabel(String, boolean)}</li>
     * <li>{@link Node#update(String)}</li>
     * </ul>
     * <p>
     * Limitation: note that the <code>MultiStatus</code> returned by this method
     * will not list any nodes that have been removed due to an Uuid conflict.
     *
     * @param updateInfo
     * @return
     * @throws org.apache.jackrabbit.webdav.DavException
     * @see org.apache.jackrabbit.webdav.version.VersionControlledResource#update(org.apache.jackrabbit.webdav.version.UpdateInfo)
     */
    //TODO: with jcr the node must not be versionable in order to perform Node.update.
    @Override
    public MultiStatus update(UpdateInfo updateInfo) throws DavException {
        if (updateInfo == null) {
            throw new DavException(DavServletResponse.SC_BAD_REQUEST, "Valid update request body required.");
        }
        if (!exists()) {
            throw new DavException(DavServletResponse.SC_NOT_FOUND);
        }

        MultiStatus ms = new MultiStatus();
        try {
            Node node = (Node)item;
            Element udElem = updateInfo.getUpdateElement();
            boolean removeExisting = DomUtil.hasChildElement(udElem, XML_REMOVEEXISTING, NAMESPACE);

            // register eventListener in order to be able to report the modified resources.
            EventListener el = new EListener(updateInfo.getPropertyNameSet(), ms);
            registerEventListener(el, node.getPath());

            // perform the update/restore according to the update info
            if (updateInfo.getVersionHref() != null) {
                String[] hrefs = updateInfo.getVersionHref();
                if (hrefs.length != 1) {
                    throw new DavException(DavServletResponse.SC_BAD_REQUEST, "Invalid update request body missing version href or containing multiple version hrefs.");
                }

                final String href = normalizeResourceHref(hrefs[0]);
                String versionPath = getLocatorFromHref(href).getRepositoryPath();
                String versionName = getItemName(versionPath);

                String relPath = DomUtil.getChildText(udElem, XML_RELPATH, NAMESPACE);
                if (relPath == null) {
                    // restore version by name
                    node.restore(versionName, removeExisting);
                } else if (node.hasNode(relPath)) {
                    Version v = node.getNode(relPath).getVersionHistory().getVersion(versionName);
                    node.restore(v, relPath, removeExisting);
                } else {
                    Version v = (Version) getRepositorySession().getNode(versionPath);
                    node.restore(v, relPath, removeExisting);
                }

            } else if (updateInfo.getLabelName() != null) {
                String[] labels = updateInfo.getLabelName();
                if (labels.length != 1) {
                    throw new DavException(DavServletResponse.SC_BAD_REQUEST, "Invalid update request body: Multiple labels specified.");
                }
                node.restoreByLabel(labels[0], removeExisting);

            } else if (updateInfo.getWorkspaceHref() != null) {
                String href = normalizeResourceHref(obtainAbsolutePathFromUri(updateInfo.getWorkspaceHref()));
                String workspaceName = getLocatorFromHref(href).getWorkspaceName();
                node.update(workspaceName);
            } else {
                throw new DavException(DavServletResponse.SC_BAD_REQUEST, "Invalid update request body.");
            }

            // unregister the event listener again
            unregisterEventListener(el);

        } catch (RepositoryException e) {
            throw new JcrDavException(e);
        }
        return ms;
    }

    /**
     * Merge the repository node represented by this resource according to the
     * information present in the given {@link MergeInfo} object.
     *
     * @param mergeInfo
     * @return <code>MultiStatus</code> recording all repository items modified
     * by this merge call as well as the resources that a client must modify to
     * complete the merge (see <a href="http://www.webdav.org/specs/rfc3253.html#METHOD_MERGE">RFC 3253</a>)
     * @throws org.apache.jackrabbit.webdav.DavException
     * @see org.apache.jackrabbit.webdav.version.VersionControlledResource#merge(org.apache.jackrabbit.webdav.version.MergeInfo)
     * @see Node#merge(String, boolean)
     */
    //TODO: with jcr the node must not be versionable in order to perform Node.merge
    @Override
    public MultiStatus merge(MergeInfo mergeInfo) throws DavException {
        if (mergeInfo == null) {
            throw new DavException(DavServletResponse.SC_BAD_REQUEST);
        }
        if (!exists()) {
            throw new DavException(DavServletResponse.SC_NOT_FOUND);
        }

        MultiStatus ms = new MultiStatus();
        try {
            // NOTE: RFC requires that all modified resources are reported in the
            // multistatus response. this doesn't work however with the remoting
            // there is no way to distinguish the 'failedId's from any other
            // resources that got modified by this merge operation -> omitted.
            
            // todo: RFC allows multiple href elements inside the DAV:source element
            final String href = normalizeResourceHref(mergeInfo.getSourceHrefs()[0]);
            String workspaceName = getLocatorFromHref(href).getWorkspaceName();

            String depth = DomUtil.getChildTextTrim(mergeInfo.getMergeElement(), DavConstants.XML_DEPTH, DavConstants.NAMESPACE);
            boolean isShallow = "0".equals(depth);

            NodeIterator failed = getVersionManager().merge(item.getPath(), workspaceName, !mergeInfo.isNoAutoMerge(), isShallow);

            // add resources to the multistatus, that failed to be merged
            while (failed.hasNext()) {
                Node failedNode = failed.nextNode();
                DavResourceLocator loc = getLocatorFromItem(failedNode);
                DavResource res = createResourceFromLocator(loc);
                ms.addResponse(new MultiStatusResponse(res, mergeInfo.getPropertyNameSet()));
            }

        } catch (RepositoryException e) {
            throw new JcrDavException(e);
        }

        return ms;
    }

    /**
     * Modify the labels present with the versions of this resource.
     *
     * @param labelInfo
     * @throws DavException
     * @see VersionHistory#addVersionLabel(String, String, boolean)
     * @see VersionHistory#removeVersionLabel(String)
     */
    @Override
    public void label(LabelInfo labelInfo) throws DavException {
        if (labelInfo == null) {
            throw new DavException(DavServletResponse.SC_BAD_REQUEST, "Valid label request body required.");
        }
        if (!exists()) {
            throw new DavException(DavServletResponse.SC_NOT_FOUND);
        }

        try {
            if (!isVersionControlled() || ((Node)item).isCheckedOut()) {
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
     * Returns the {@link VersionHistory} associated with the repository node.
     * If the node is not versionable an exception is thrown.
     *
     * @return the {@link VersionHistoryResource} associated with this resource.
     * @throws org.apache.jackrabbit.webdav.DavException
     * @see org.apache.jackrabbit.webdav.version.VersionControlledResource#getVersionHistory()
     * @see javax.jcr.Node#getVersionHistory()
     */
    @Override
    public VersionHistoryResource getVersionHistory() throws DavException {
        if (!exists()) {
            throw new DavException(DavServletResponse.SC_NOT_FOUND);
        }

        try {
            VersionHistory vh = ((Node)item).getVersionHistory();
            DavResourceLocator loc = getLocatorFromItem(vh);
            return (VersionHistoryResource) createResourceFromLocator(loc);
        } catch (RepositoryException e) {
            throw new JcrDavException(e);
        }
    }

    //--------------------------------------------------------------------------
    /**
     * Define the set of reports supported by this resource.
     *
     * @see SupportedReportSetProperty
     */
    @Override
    protected void initSupportedReports() {
        super.initSupportedReports();
        if (exists()) {
            supportedReports.addReportType(ReportType.LOCATE_BY_HISTORY);
            if (this.isVersionControlled()) {
                supportedReports.addReportType(ReportType.VERSION_TREE);
            }
        }
    }

    @Override
    protected void initPropertyNames() {
        super.initPropertyNames();

        if (isVersionControlled()) {
            names.addAll(JcrDavPropertyNameSet.VERSIONABLE_SET);

            Node n = (Node) item;
            try {
                if (n.isCheckedOut()) {
                    names.add(CHECKED_OUT);
                    if (n.hasProperty(JcrConstants.JCR_PREDECESSORS)) {
                        names.add(PREDECESSOR_SET);
                    }
                    if (n.hasProperty(JcrConstants.JCR_MERGEFAILED)) {
                        names.add(AUTO_MERGE_SET);
                    }
                    // todo: checkout-fork, checkin-fork
                } else {
                    names.add(CHECKED_IN);
                }
            } catch (RepositoryException e) {
                log.warn(e.getMessage());
            }
        }
    }

    /**
     * Fill the property set for this resource.
     */
    @Override
    protected void initProperties() {
        super.initProperties();
        if (isVersionControlled()) {
            Node n = (Node)item;
            // properties defined by RFC 3253 for version-controlled resources
            // workspace property already set in AbstractResource.initProperties()
            try {
                // DAV:version-history (computed)
                String vhHref = getLocatorFromItem(n.getVersionHistory()).getHref(true);
                properties.add(new HrefProperty(VERSION_HISTORY, vhHref, true));

                // DAV:auto-version property: there is no auto version, explicit CHECKOUT is required.
                properties.add(new DefaultDavProperty<String>(AUTO_VERSION, null, false));

                String baseVHref = getLocatorFromItem(n.getBaseVersion()).getHref(true);
                if (n.isCheckedOut()) {
                    // DAV:predecessors property
                    if (n.hasProperty(JcrConstants.JCR_PREDECESSORS)) {
                        Value[] predec = n.getProperty(JcrConstants.JCR_PREDECESSORS).getValues();
                        addHrefProperty(PREDECESSOR_SET, predec, false);
                    }
                    // DAV:auto-merge-set property. NOTE: the DAV:merge-set
                    // never occurs, because merging without bestEffort flag
                    // being set results in an exception on failure.
                    if (n.hasProperty(JcrConstants.JCR_MERGEFAILED)) {
                        Value[] mergeFailed = n.getProperty(JcrConstants.JCR_MERGEFAILED).getValues();
                        addHrefProperty(AUTO_MERGE_SET, mergeFailed, false);
                    }
                    // todo: checkout-fork, checkin-fork
                }
            } catch (RepositoryException e) {
                log.error(e.getMessage());
            }
        }
    }

    /**
     * Add a {@link org.apache.jackrabbit.webdav.property.HrefProperty} with the
     * specified property name and values.
     *
     * @param name
     * @param values Array of {@link Value}s.
     * @param isProtected
     * @throws javax.jcr.ValueFormatException
     * @throws IllegalStateException
     * @throws javax.jcr.RepositoryException
     */
    private void addHrefProperty(DavPropertyName name, Value[] values,
                                 boolean isProtected)
            throws ValueFormatException, IllegalStateException, RepositoryException {
        Node[] nodes = new Node[values.length];
        for (int i = 0; i < values.length; i++) {
            nodes[i] = getRepositorySession().getNodeByIdentifier(values[i].getString());
        }
        addHrefProperty(name, nodes, isProtected);
    }

    /**
     * @return true, if this resource represents an existing repository node
     * that has the mixin nodetype 'mix:versionable' set.
     */
    private boolean isVersionControlled() {
        boolean vc = false;
        if (exists()) {
            try {
                vc = ((Node) item).isNodeType(JcrConstants.MIX_VERSIONABLE);
            } catch (RepositoryException e) {
                log.warn(e.getMessage());
            }
        }
        return vc;
    }

    /**
     * Build a new locator for the given href.
     *
     * @param href
     * @return
     */
    private DavResourceLocator getLocatorFromHref(String href) {
        DavLocatorFactory f = getLocator().getFactory();
        String prefix = getLocator().getPrefix();
        return f.createResourceLocator(prefix, href);
    }

    private VersionManager getVersionManager() throws RepositoryException {
        return getRepositorySession().getWorkspace().getVersionManager();
    }

    private static String obtainAbsolutePathFromUri(String uri) {
        try {
            java.net.URI u = new java.net.URI(uri);
            StringBuilder sb = new StringBuilder();
            sb.append(u.getRawPath());
            if (u.getRawQuery() != null) {
                sb.append("?").append(u.getRawQuery());
            }
            return sb.toString();
        }
        catch (java.net.URISyntaxException ex) {
            log.warn("parsing " + uri, ex);
            return uri;
        }
    }
}
