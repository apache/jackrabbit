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
package org.apache.jackrabbit.webdav.spi.version;

import org.apache.log4j.Logger;
import org.apache.jackrabbit.webdav.property.*;
import org.apache.jackrabbit.webdav.*;
import org.apache.jackrabbit.webdav.spi.JcrDavException;
import org.apache.jackrabbit.webdav.spi.DefaultItemCollection;
import org.apache.jackrabbit.webdav.version.*;
import org.apache.jackrabbit.webdav.version.report.*;

import javax.jcr.*;
import javax.jcr.observation.*;
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
    public VersionControlledItemCollection(DavResourceLocator locator, DavSession session,
                                           DavResourceFactory factory, Item item) {
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

    //--------------------------------< VersionControlledResource interface >---
    /**
     * Adds version control to this resource. If the resource is already under
     * version control, this method has no effect.
     *
     * @throws org.apache.jackrabbit.webdav.DavException if this resource does not
     * exist yet or if an error occurs while making the underlaying node versionable.
     * @see org.apache.jackrabbit.webdav.version.VersionableResource#addVersionControl()
     */
    public void addVersionControl() throws DavException {
        if (!exists()) {
            throw new DavException(DavServletResponse.SC_NOT_FOUND);
        }
        if (!isVersionControlled()) {
            try {
                ((Node)item).addMixin(MIX_VERSIONABLE);
                item.save();
            } catch (RepositoryException e) {
                throw new JcrDavException(e);
            }
        } // else: is already version controlled -> ignore
    }

    /**
     * Calls {@link javax.jcr.Node#checkin()} on the underlaying repository node.
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
            DavResourceLocator loc = getLocator();
            String versionHref = loc.getFactory().createResourceLocator(loc.getPrefix(), loc.getWorkspacePath(), v.getPath()).getHref(true);
            return versionHref;
        } catch (RepositoryException e) {
            // UnsupportedRepositoryException should not occur
            throw new JcrDavException(e);
        }
    }

    /**
     * Calls {@link javax.jcr.Node#checkout()} on the underlaying repository node.
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
     * @todo with jcr the node must not be versionable in order to perform Node.update.
     */
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
            boolean removeExisting = updateInfo.getUpdateElement().getChild(XML_REMOVEEXISTING, NAMESPACE) != null;

            // register eventListener in order to be able to report the modified resources.
            EventListener el = new EListener(updateInfo.getPropertyNameSet(), ms);
            registerEventListener(el, node.getPath());

            // perform the update/restore according to the update info
            if (updateInfo.getVersionHref() != null) {
                VersionHistory vh = node.getVersionHistory();
                String[] hrefs = updateInfo.getVersionHref();
                Version[] versions = new Version[hrefs.length];
                for (int  i = 0; i < hrefs.length; i++) {
                    versions[i] = vh.getVersion(getResourceName(hrefs[i], true));
                }
                if (versions.length == 1) {
                    String relPath = updateInfo.getUpdateElement().getChildText(XML_RELPATH, NAMESPACE);
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
                String workspaceName = getResourceName(updateInfo.getWorkspaceHref(), true);
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
     * @return <code>MultiStatus</code> reccording all repository items affected
     * by this merge call.
     * @throws org.apache.jackrabbit.webdav.DavException
     * @see org.apache.jackrabbit.webdav.version.VersionControlledResource#merge(org.apache.jackrabbit.webdav.version.MergeInfo)
     * @see Node#merge(String, boolean)
     * @todo with jcr the node must not be versionable in order to perform Node.merge
     */
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

            // register eventListener in order to be able to report the modifications.
            EventListener el = new EListener(mergeInfo.getPropertyNameSet(), ms);
            registerEventListener(el, node.getPath());

            String workspaceName = getResourceName(mergeInfo.getSourceHref(), true);
            node.merge(workspaceName, !mergeInfo.isNoAutoMerge());

            // unregister the event listener again
            unregisterEventListener(el);

        } catch (RepositoryException e) {
            throw new JcrDavException(e);
        }

        return ms;
    }

    /**
     * Resolve the merge conflicts according to the value of the {@link #AUTO_MERGE_SET DAV:auto-merge-set}
     * property present in the specified <code>DavPropertySet</code>s.
     *
     * @param setProperties
     * @param removePropertyNames
     * @throws org.apache.jackrabbit.webdav.DavException
     * @see VersionControlledResource#resolveMergeConflict(DavPropertySet, DavPropertyNameSet)
     * @see Node#doneMerge(Version)
     * @see Node#cancelMerge(Version)
     */
    public void resolveMergeConflict(DavPropertySet setProperties,
                                     DavPropertyNameSet removePropertyNames) throws DavException {

        if (!exists()) {
            throw new DavException(DavServletResponse.SC_NOT_FOUND);
        }
        if (!isVersionControlled()) {
            throw new DavException(DavServletResponse.SC_METHOD_NOT_ALLOWED);
        }

        try {
            Node n = (Node)item;
            if (removePropertyNames.contains(AUTO_MERGE_SET)) {
                // retrieve the current jcr:mergeFailed property values
                if (!((Node)item).hasProperty(PROP_MERGEFAILED)) {
                    throw new DavException(DavServletResponse.SC_CONFLICT, "Attempt to resolve non-existing merge conflicts.");
                }
                Value[] mergeFailed = ((Node)item).getProperty(PROP_MERGEFAILED).getValues();

                // resolve all remaining merge conflicts with 'cancel'
                for (int i = 0; i < mergeFailed.length; i++) {
                    n.cancelMerge((Version)getRepositorySession().getNodeByUUID(mergeFailed[i].getString()));
                }
                // adjust removeProperty set
                removePropertyNames.remove(AUTO_MERGE_SET);

            } else if (setProperties.contains(AUTO_MERGE_SET) && setProperties.contains(PREDECESSOR_SET)){
                // retrieve the current jcr:mergeFailed property values
                if (!((Node)item).hasProperty(PROP_MERGEFAILED)) {
                    throw new DavException(DavServletResponse.SC_CONFLICT, "Attempt to resolve non-existing merge conflicts.");
                }
                Value[] mergeFailed = ((Node)item).getProperty(PROP_MERGEFAILED).getValues();


                // check which mergeFailed entries have been removed from the
                // auto-merge-set (cancelMerge) and have been moved over to the
                // predecessor set (doneMerge)
                List mergeset = new HrefProperty(setProperties.get(AUTO_MERGE_SET)).getHrefs();
                List predecSet = new HrefProperty(setProperties.get(PREDECESSOR_SET)).getHrefs();

                Session session = getRepositorySession();
                for (int i = 0; i < mergeFailed.length; i++) {
                    // build version-href from each entry in the jcr:mergeFailed property
                    Version version = (Version) session.getNodeByUUID(mergeFailed[i].getString());
                    String href = this.getLocatorFromItem(version).getHref(true);

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
            } else {
                // setPropertySet and removePropertySet do not ask for resolving merge conflicts */
                log.debug("setProperties and removeProperties sets do not request for merge conflict resolution.");
            }
        } catch (RepositoryException e) {
            throw new JcrDavException(e);
        }
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
                    String vhHref = getLocatorFromResourcePath(n.getVersionHistory().getPath()).getHref(true);
                    properties.add(new HrefProperty(VERSION_HISTORY, vhHref, true));

                    // DAV:auto-version property: there is no auto version, explicit CHECKOUT is required.
                    properties.add(new DefaultDavProperty(AUTO_VERSION, null, false));

                    String baseVHref = getLocatorFromResourcePath(n.getBaseVersion().getPath()).getHref(true);
                    if (n.isCheckedOut()) {
                        // DAV:checked-out property (protected)
                        properties.add(new HrefProperty(CHECKED_OUT, baseVHref, true));

                        // DAV:predecessors property
                        if (n.hasProperty(PROP_PREDECESSORS)) {
                            Value[] predec = n.getProperty(PROP_PREDECESSORS).getValues();
                            addHrefProperty(PREDECESSOR_SET, predec, false);
                        }
                        // DAV:auto-merge-set property. NOTE: the DAV:merge-set
                        // never occurs, because merging without bestEffort flag
                        // being set results in an exception on failure.
                        if (n.hasProperty(PROP_MERGEFAILED)) {
                            ReferenceValue[] mergeFailed = (ReferenceValue[]) n.getProperty(PROP_MERGEFAILED).getValues();
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
     * Add a {@link org.apache.jackrabbit.webdav.property.HrefProperty} with the specified property name and values.
     *
     * @param name
     * @param values Array of {@link ReferenceValue}s.
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
                vc = ((Node) item).isNodeType("mix:versionable");
            } catch (RepositoryException e) {
                log.warn(e.getMessage());
            }
        }
        return vc;
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
                    String itemPath = e.getPath();
                    DavResourceLocator loc = getLocatorFromResourcePath(itemPath);
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