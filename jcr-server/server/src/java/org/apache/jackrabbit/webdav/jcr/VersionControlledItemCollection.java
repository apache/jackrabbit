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
package org.apache.jackrabbit.webdav.jcr;

import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavLocatorFactory;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.DavResourceFactory;
import org.apache.jackrabbit.webdav.DavResourceLocator;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.MultiStatus;
import org.apache.jackrabbit.webdav.MultiStatusResponse;
import org.apache.jackrabbit.webdav.property.DavPropertyName;
import org.apache.jackrabbit.webdav.property.DavPropertyNameSet;
import org.apache.jackrabbit.webdav.property.DavPropertySet;
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
import org.apache.jackrabbit.webdav.xml.DomUtil;
import org.apache.log4j.Logger;
import org.w3c.dom.Element;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.Workspace;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import java.util.List;

/**
 * <code>VersionControlledItemCollection</code> represents a JCR node item and
 * covers all functionality related to versioning of {@link Node}s.
 *
 * @see Node
 */
public class VersionControlledItemCollection extends DefaultItemCollection
        implements VersionControlledResource {

    private static Logger log = Logger.getLogger(VersionControlledItemCollection.class);

    /**
     * Create a new <code>VersionControlledItemCollection</code>.
     *
     * @param locator
     * @param session
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
    public String getSupportedMethods() {
        StringBuffer sb = new StringBuffer(super.getSupportedMethods());
        // Versioning support
        sb.append(", ").append(VersionableResource.METHODS);
        if (this.isVersionControlled()) {
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

    /**
     *
     * @param setProperties
     * @param removePropertyNames
     * @throws DavException
     * @see DefaultItemCollection#alterProperties(org.apache.jackrabbit.webdav.property.DavPropertySet, org.apache.jackrabbit.webdav.property.DavPropertyNameSet)
     * for additional description of non-compliant behaviour.
     */
    public MultiStatusResponse alterProperties(DavPropertySet setProperties, DavPropertyNameSet removePropertyNames) throws DavException {
        /* first resolve merge conflict since they cannot be handled by
           setting property values in jcr (and are persisted immediately).
           NOTE: this violates RFC 2518 that requires that proppatch
           is processed in the order entries are present in the xml and that
           required that no changes must be persisted if any set/remove fails.
        */
        // TODO: solve violation of RFC 2518
        resolveMergeConflict(setProperties, removePropertyNames);
        // alter other properties only if merge-conflicts could be handled
        return super.alterProperties(setProperties, removePropertyNames);
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
     * @param setProperties
     * @param removePropertyNames
     * @throws org.apache.jackrabbit.webdav.DavException
     * @see Node#doneMerge(Version)
     * @see Node#cancelMerge(Version)
     */
    private void resolveMergeConflict(DavPropertySet setProperties,
                                     DavPropertyNameSet removePropertyNames)
        throws DavException {

        if (!exists()) {
            throw new DavException(DavServletResponse.SC_NOT_FOUND);
        }

        try {
            Node n = (Node)item;
            if (removePropertyNames.contains(AUTO_MERGE_SET)) {
                // retrieve the current jcr:mergeFailed property values
                if (!n.hasProperty(JcrConstants.JCR_MERGEFAILED)) {
                    throw new DavException(DavServletResponse.SC_CONFLICT, "Attempt to resolve non-existing merge conflicts.");
                }
                Value[] mergeFailed = n.getProperty(JcrConstants.JCR_MERGEFAILED).getValues();

                // resolve all remaining merge conflicts with 'cancel'
                for (int i = 0; i < mergeFailed.length; i++) {
                    n.cancelMerge((Version)getRepositorySession().getNodeByUUID(mergeFailed[i].getString()));
                }
                // adjust removeProperty set
                removePropertyNames.remove(AUTO_MERGE_SET);

            } else if (setProperties.contains(AUTO_MERGE_SET) && setProperties.contains(PREDECESSOR_SET)){
                // retrieve the current jcr:mergeFailed property values
                if (!n.hasProperty(JcrConstants.JCR_MERGEFAILED)) {
                    throw new DavException(DavServletResponse.SC_CONFLICT, "Attempt to resolve non-existing merge conflicts.");
                }
                Value[] mergeFailed = n.getProperty(JcrConstants.JCR_MERGEFAILED).getValues();


                // check which mergeFailed entries have been removed from the
                // auto-merge-set (cancelMerge) and have been moved over to the
                // predecessor set (doneMerge)
                List mergeset = new HrefProperty(setProperties.get(AUTO_MERGE_SET)).getHrefs();
                List predecSet = new HrefProperty(setProperties.get(PREDECESSOR_SET)).getHrefs();

                Session session = getRepositorySession();
                for (int i = 0; i < mergeFailed.length; i++) {
                    // build version-href from each entry in the jcr:mergeFailed property
                    Version version = (Version) session.getNodeByUUID(mergeFailed[i].getString());
                    String href = getLocatorFromItem(version).getHref(true);

                    // Test if that version has been removed from the merge-set.
                    // thus indicating that the merge-conflict needs to be resolved.
                    if (!mergeset.contains(href)) {
                        // Test if the 'href' has been moved over to the
                        // predecessor-set (thus 'doneMerge' is appropriate) or
                        // if it is not present in the predecessor set and the
                        // the conflict is resolved by 'cancelMerge'.
                        if (predecSet.contains(href)) {
                            n.doneMerge(version);
                        } else {
                            n.cancelMerge(version);
                        }
                    }
                }
                // adjust setProperty set
                setProperties.remove(AUTO_MERGE_SET);
                setProperties.remove(PREDECESSOR_SET);
            }
            /* else: no (valid) attempt to resolve merge conflict > return silently */
        } catch (RepositoryException e) {
            throw new JcrDavException(e);
        }
    }

    //--------------------------------< VersionControlledResource interface >---
    /**
     * Adds version control to this resource. If the resource is already under
     * version control, this method has no effect.
     *
     * @throws org.apache.jackrabbit.webdav.DavException if this resource does not
     * exist yet or if an error occurs while making the underlying node versionable.
     * @see org.apache.jackrabbit.webdav.version.VersionableResource#addVersionControl()
     */
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
            Version v = ((Node) item).checkin();
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
    public void checkout() throws DavException {
        if (!exists()) {
            throw new DavException(DavServletResponse.SC_NOT_FOUND);
        }
        if (!isVersionControlled()) {
            throw new DavException(DavServletResponse.SC_METHOD_NOT_ALLOWED);
        }
        try {
            ((Node) item).checkout();
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
     * <li>{@link Workspace#restore(javax.jcr.version.Version[], boolean)}</li>
     * <li>{@link Node#update(String)}</li>
     * </ul>
     * </p>
     * Limitation: note that the <code>MultiStatus</code> returned by this method
     * will not list any nodes that have been removed due to an Uuid conflict.
     *
     * @param updateInfo
     * @return
     * @throws org.apache.jackrabbit.webdav.DavException
     * @see org.apache.jackrabbit.webdav.version.VersionControlledResource#update(org.apache.jackrabbit.webdav.version.UpdateInfo)
     */
    //TODO: with jcr the node must not be versionable in order to perform Node.update.
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
                VersionHistory vh = node.getVersionHistory();
                String[] hrefs = updateInfo.getVersionHref();
                Version[] versions = new Version[hrefs.length];
                for (int  i = 0; i < hrefs.length; i++) {
                    String itemPath = getLocatorFromHref(hrefs[i]).getRepositoryPath();
                    versions[i] = vh.getVersion(getItemName(itemPath));
                }
                if (versions.length == 1) {
                    String relPath = DomUtil.getChildText(udElem, XML_RELPATH, NAMESPACE);
                    if (relPath == null) {
                        node.restore(versions[0], removeExisting);
                    } else {
                        node.restore(versions[0], relPath, removeExisting);
                    }
                } else {
                    getRepositorySession().getWorkspace().restore(versions, removeExisting);
                }
            } else if (updateInfo.getLabelName() != null) {
                String[] labels = updateInfo.getLabelName();
                if (labels.length == 1) {
                    node.restoreByLabel(labels[0], removeExisting);
                } else {
                    Version[] vs = new Version[labels.length];
                    VersionHistory vh = node.getVersionHistory();
                    for (int  i = 0; i < labels.length; i++) {
                        vs[i] = vh.getVersionByLabel(labels[i]);
                    }
                    getRepositorySession().getWorkspace().restore(vs, removeExisting);
                }
            } else if (updateInfo.getWorkspaceHref() != null) {
                String workspaceName = getLocatorFromHref(updateInfo.getWorkspaceHref()).getWorkspaceName();
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
    public MultiStatus merge(MergeInfo mergeInfo) throws DavException {
        if (mergeInfo == null) {
            throw new DavException(DavServletResponse.SC_BAD_REQUEST);
        }
        if (!exists()) {
            throw new DavException(DavServletResponse.SC_NOT_FOUND);
        }

        MultiStatus ms = new MultiStatus();
        try {
            Node node = (Node)item;

            // register eventListener in order to be able to report all
            // modified resources.
            EventListener el = new EListener(mergeInfo.getPropertyNameSet(), ms);
            registerEventListener(el, node.getPath());

            // todo: RFC allows multiple href elements inside the DAV:source element
            String workspaceName = getLocatorFromHref(mergeInfo.getSourceHrefs()[0]).getWorkspaceName();
            NodeIterator failed = node.merge(workspaceName, !mergeInfo.isNoAutoMerge());

            // unregister the event listener again
            unregisterEventListener(el);

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
    protected void initSupportedReports() {
        super.initSupportedReports();
        if (exists()) {
	    supportedReports.addReportType(ReportType.LOCATE_BY_HISTORY);
            if (this.isVersionControlled()) {
            supportedReports.addReportType(ReportType.VERSION_TREE);
	    }            
        }
    }

    /**
     * Fill the property set for this resource.
     */
    protected void initProperties() {
        super.initProperties();
        if (exists()) {
            Node n = (Node)item;
            // properties defined by RFC 3253 for version-controlled resources
            if (isVersionControlled()) {
                // workspace property already set in AbstractResource.initProperties()
                try {
                    // DAV:version-history (computed)
                    String vhHref = getLocatorFromItem(n.getVersionHistory()).getHref(true);
                    properties.add(new HrefProperty(VERSION_HISTORY, vhHref, true));

                    // DAV:auto-version property: there is no auto version, explicit CHECKOUT is required.
                    properties.add(new DefaultDavProperty(AUTO_VERSION, null, false));

                    String baseVHref = getLocatorFromItem(n.getBaseVersion()).getHref(true);
                    if (n.isCheckedOut()) {
                        // DAV:checked-out property (protected)
                        properties.add(new HrefProperty(CHECKED_OUT, baseVHref, true));

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
            nodes[i] = getRepositorySession().getNodeByUUID(values[i].getString());
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

    /**
     * Register the specified event listener with the observation manager present
     * the repository session.
     *
     * @param listener
     * @param nodePath
     * @throws javax.jcr.RepositoryException
     */
    private void registerEventListener(EventListener listener, String nodePath) throws RepositoryException {
        getRepositorySession().getWorkspace().getObservationManager().addEventListener(listener, EListener.ALL_EVENTS, nodePath, true, null, null, false);
    }

    /**
     * Unregister the specified event listener with the observation manager present
     * the repository session.
     *
     * @param listener
     * @throws javax.jcr.RepositoryException
     */
    private void unregisterEventListener(EventListener listener) throws RepositoryException {
        getRepositorySession().getWorkspace().getObservationManager().removeEventListener(listener);
    }

    //------------------------------------------------------< inner classes >---
    /**
     * Simple EventListener that creates a new {@link org.apache.jackrabbit.webdav.MultiStatusResponse} object
     * for each event and adds it to the specified {@link org.apache.jackrabbit.webdav.MultiStatus}.
     */
    private class EListener implements EventListener {

        private static final int ALL_EVENTS = Event.NODE_ADDED | Event.NODE_REMOVED | Event.PROPERTY_ADDED | Event.PROPERTY_CHANGED | Event.PROPERTY_REMOVED;

        private final DavPropertyNameSet propNameSet;
        private MultiStatus ms;

        private EListener(DavPropertyNameSet propNameSet, MultiStatus ms) {
            this.propNameSet = propNameSet;
            this.ms = ms;
        }

        /**
         * @see EventListener#onEvent(javax.jcr.observation.EventIterator)
         */
        public void onEvent(EventIterator events) {
            while (events.hasNext()) {
                try {
                    Event e = events.nextEvent();
                    DavResourceLocator loc = getLocatorFromItemPath(e.getPath());
                    DavResource res = createResourceFromLocator(loc);
                    ms.addResponse(new MultiStatusResponse(res, propNameSet));

                } catch (DavException e) {
                    // should not occur
                    log.error("Error while building MultiStatusResponse from Event: " + e.getMessage());
                } catch (RepositoryException e) {
                    // should not occur
                    log.error("Error while building MultiStatusResponse from Event: " + e.getMessage());
                }
            }
        }
    }
}