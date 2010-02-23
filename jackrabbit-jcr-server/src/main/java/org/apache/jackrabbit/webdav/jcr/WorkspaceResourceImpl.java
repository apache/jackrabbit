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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.jackrabbit.webdav.version.WorkspaceResource;
import org.apache.jackrabbit.webdav.version.DeltaVResource;
import org.apache.jackrabbit.webdav.version.UpdateInfo;
import org.apache.jackrabbit.webdav.version.VersionControlledResource;
import org.apache.jackrabbit.webdav.version.MergeInfo;
import org.apache.jackrabbit.webdav.version.LabelInfo;
import org.apache.jackrabbit.webdav.version.VersionHistoryResource;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavResourceIterator;
import org.apache.jackrabbit.webdav.DavResourceLocator;
import org.apache.jackrabbit.webdav.MultiStatusResponse;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.DavResourceIteratorImpl;
import org.apache.jackrabbit.webdav.DavResourceFactory;
import org.apache.jackrabbit.webdav.MultiStatus;
import org.apache.jackrabbit.webdav.DavMethods;
import org.apache.jackrabbit.webdav.xml.DomUtil;
import org.apache.jackrabbit.webdav.search.SearchResource;
import org.apache.jackrabbit.webdav.jcr.property.NamespacesProperty;
import org.apache.jackrabbit.webdav.jcr.version.report.JcrPrivilegeReport;
import org.apache.jackrabbit.webdav.property.DavProperty;
import org.apache.jackrabbit.webdav.property.PropEntry;
import org.apache.jackrabbit.webdav.io.InputContext;
import org.apache.jackrabbit.webdav.io.OutputContext;
import org.w3c.dom.Element;

import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;
import javax.jcr.Workspace;
import javax.jcr.Item;
import javax.jcr.Session;
import javax.jcr.Repository;
import javax.jcr.version.Version;
import javax.jcr.observation.EventListener;
import java.util.List;
import java.util.Date;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.OutputStreamWriter;

/**
 * <code>WorkspaceResourceImpl</code>...
 */
public class WorkspaceResourceImpl extends AbstractResource
    implements WorkspaceResource, VersionControlledResource {

    private static Logger log = LoggerFactory.getLogger(WorkspaceResourceImpl.class);

    /**
     * Create a new <code>WorkspaceResourceImpl</code>
     *
     * @param locator
     * @param session
     */
    WorkspaceResourceImpl(DavResourceLocator locator, JcrDavSession session, DavResourceFactory factory) {
        super(locator, session, factory);

        // initialize the supported locks and reports
        initLockSupport();
        initSupportedReports();
    }

    //--------------------------------------------------------< DavResource >---

    public String getSupportedMethods() {
        StringBuffer sb = new StringBuffer(DavResource.METHODS);
        sb.append(", ");
        sb.append(DeltaVResource.METHODS_INCL_MKWORKSPACE);
        sb.append(", ");
        sb.append(SearchResource.METHODS);
        // from vc-resource methods only UPDATE is supported
        sb.append(", ");
        sb.append(DavMethods.METHOD_UPDATE);
        return sb.toString();
    }

    /**
     * @return true
     */
    public boolean exists() {
        return true;
    }

    /**
     * @return true
     */
    public boolean isCollection() {
        return true;
    }

    /**
     * Returns the name of the workspace.
     *
     * @return The workspace name
     * @see org.apache.jackrabbit.webdav.DavResource#getDisplayName()
     * @see javax.jcr.Workspace#getName()
     */
    public String getDisplayName() {
        return getLocator().getWorkspaceName();
    }

    /**
     * Always returns 'now'
     *
     * @return
     */
    public long getModificationTime() {
        return new Date().getTime();
    }

    /**
     * Sets content lengths to '0' and retrieves the modification time.
     *
     * @param outputContext
     * @throws IOException
     */
    public void spool(OutputContext outputContext) throws IOException {
        if (outputContext.hasStream()) {
            Session session = getRepositorySession();
            Repository rep = session.getRepository();
            String repName = rep.getDescriptor(Repository.REP_NAME_DESC);
            String repURL = rep.getDescriptor(Repository.REP_VENDOR_URL_DESC);
            String repVersion = rep.getDescriptor(Repository.REP_VERSION_DESC);
            String repostr = repName + " " + repVersion;

            StringBuffer sb = new StringBuffer();
            sb.append("<html><head><title>");
            sb.append(repostr);
            sb.append("</title></head>");
            sb.append("<body><h2>").append(repostr).append("</h2><ul>");
            sb.append("<li><a href=\"..\">..</a></li>");
            DavResourceIterator it = getMembers();
            while (it.hasNext()) {
                DavResource res = it.nextResource();
                sb.append("<li><a href=\"");
                sb.append(res.getHref());
                sb.append("\">");
                sb.append(res.getDisplayName());
                sb.append("</a></li>");
            }
            sb.append("</ul><hr size=\"1\"><em>Powered by <a href=\"");
            sb.append(repURL).append("\">").append(repName);
            sb.append("</a> ").append(repVersion);
            sb.append("</em></body></html>");

            outputContext.setContentLength(sb.length());
            outputContext.setModificationTime(getModificationTime());
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(outputContext.getOutputStream(), "utf8"));
            writer.print(sb.toString());
            writer.close();
        } else {
            outputContext.setContentLength(0);
            outputContext.setModificationTime(getModificationTime());
        }
    }

    /**
     * Retrieve the collection that has all workspace collections
     * as internal members.
     *
     * @see org.apache.jackrabbit.webdav.DavResource#getCollection()
     */
    public DavResource getCollection() {
        DavResource collection = null;
        // create location with 'null' values for workspace-path and resource-path
        DavResourceLocator parentLoc = getLocator().getFactory().createResourceLocator(getLocator().getPrefix(), null, null);
        try {
            collection = createResourceFromLocator(parentLoc);
        } catch (DavException e) {
            log.error("Unexpected error while retrieving collection: " + e.getMessage());
        }
        return collection;
    }

    /**
     * Throws 403 exception (Forbidden)
     *
     * @param resource
     * @param inputContext
     * @throws DavException
     */
    public void addMember(DavResource resource, InputContext inputContext) throws DavException {
        log.error("Cannot add a new member to the workspace resource.");
        throw new DavException(DavServletResponse.SC_FORBIDDEN);
    }

    /**
     * Returns the resource representing the JCR root node.
     *
     * @return
     */
    public DavResourceIterator getMembers() {
        try {
            DavResourceLocator loc = getLocatorFromItem(getRepositorySession().getRootNode());
            List<DavResource> list = Collections.singletonList(createResourceFromLocator(loc));
            return new DavResourceIteratorImpl(list);
        } catch (DavException e) {
            log.error("Internal error while building resource for the root node.", e);
            return DavResourceIteratorImpl.EMPTY;
        } catch (RepositoryException e) {
            log.error("Internal error while building resource for the root node.", e);
            return DavResourceIteratorImpl.EMPTY;
        }       
    }

    /**
     * Throws 403 exception (Forbidden)
     *
     * @param member
     * @throws DavException
     */
    public void removeMember(DavResource member) throws DavException {
        log.error("Cannot add a remove the root node.");
        throw new DavException(DavServletResponse.SC_FORBIDDEN);
    }

    /**
     * Allows to alter the registered namespaces ({@link ItemResourceConstants#JCR_NAMESPACES}) and
     * forwards any other property to the super class.<p/>
     * Note that again no property status is set. Any failure while setting
     * a property results in an exception (violating RFC 2518).
     *
     * @param property
     * @throws DavException
     * @see DavResource#setProperty(org.apache.jackrabbit.webdav.property.DavProperty)
     */
    @Override
    public void setProperty(DavProperty<?> property) throws DavException {
        if (ItemResourceConstants.JCR_NAMESPACES.equals(property.getName())) {
            NamespacesProperty nsp = new NamespacesProperty(property);
            try {
                Map<String, String> changes = new HashMap<String, String>(nsp.getNamespaces());
                NamespaceRegistry nsReg = getRepositorySession().getWorkspace().getNamespaceRegistry();
                for (String prefix : nsReg.getPrefixes()) {
                    if (!changes.containsKey(prefix)) {
                        // prefix not present amongst the new values any more > unregister
                        nsReg.unregisterNamespace(prefix);
                    } else if (changes.get(prefix).equals(nsReg.getURI(prefix))) {
                        // present with same uri-value >> no action required
                        changes.remove(prefix);
                    }
                }

                // try to register any prefix/uri pair that has a changed uri or
                // it has not been present before.
                for (String prefix : changes.keySet()) {
                    String uri = changes.get(prefix);
                    nsReg.registerNamespace(prefix, uri);
                }
            } catch (RepositoryException e) {
                throw new JcrDavException(e);
            }
        } else {
            // only jcr:namespace can be modified
            throw new DavException(DavServletResponse.SC_CONFLICT);
        }
    }

    /**
     * Handles an attempt to set {@link ItemResourceConstants#JCR_NAMESPACES}
     * and forwards any other set or remove requests to the super class.
     *
     * @see #setProperty(DavProperty)
     * @see DefaultItemCollection#alterProperties(List)
     */
    @Override
    public MultiStatusResponse alterProperties(List<? extends PropEntry> changeList) throws DavException {
        if (changeList.size() == 1) {
           PropEntry propEntry = changeList.get(0);
            // only modification of prop is allowed. removal is not possible
            if (propEntry instanceof DavProperty
                && ItemResourceConstants.JCR_NAMESPACES.equals(((DavProperty<?>)propEntry).getName())) {
                DavProperty<?> namespaceProp = (DavProperty<?>) propEntry;
                setProperty(namespaceProp);
            } else {
                // attempt to remove the namespace property
                throw new DavException(DavServletResponse.SC_CONFLICT);
            }
        } else {
            // change list contains more than the jcr:namespaces property
            // TODO: build multistatus instead
            throw new DavException(DavServletResponse.SC_CONFLICT);
        }
        return new MultiStatusResponse(getHref(), DavServletResponse.SC_OK);
    }

    //------------------------------------------------< VersionableResource >---
    /**
     * @throws DavException (403) since workspace is not versionable. implementing
     * <code>VersionControlledResource</code> only for 'update'.
     */
    public void addVersionControl() throws DavException {
        throw new DavException(DavServletResponse.SC_FORBIDDEN);
    }

    //------------------------------------------< VersionControlledResource >---
    /**
     * @throws DavException (403) since workspace is not versionable. implementing
     * <code>VersionControlledResource</code> only for 'update'.
     */
    public String checkin() throws DavException {
        throw new DavException(DavServletResponse.SC_FORBIDDEN);
    }

    /**
     * @throws DavException (403) since workspace is not versionable. implementing
     * <code>VersionControlledResource</code> only for 'update'.
     */
    public void checkout() throws DavException {
        throw new DavException(DavServletResponse.SC_FORBIDDEN);
    }

    /**
     * @throws DavException (403) since workspace is not versionable. implementing
     * <code>VersionControlledResource</code> only for 'update'.
     */
    public void uncheckout() throws DavException {
        throw new DavException(DavServletResponse.SC_FORBIDDEN);
    }

    /**
     * While RFC 3253 does not define any version-related operations for the
     * workspace resource, this implementation uses {@link VersionControlledResource#update(UpdateInfo)}
     * to map {@link Workspace#restore(javax.jcr.version.Version[], boolean)} to
     * a WebDAV call.
     * </p>
     * Limitation: note that the <code>MultiStatus</code> returned by this method
     * will not list any nodes that have been removed due to an Uuid conflict.
     *
     * @param updateInfo
     * @return
     * @throws org.apache.jackrabbit.webdav.DavException
     * @see org.apache.jackrabbit.webdav.version.VersionControlledResource#update(org.apache.jackrabbit.webdav.version.UpdateInfo)
     */
    public MultiStatus update(UpdateInfo updateInfo) throws DavException {
        if (updateInfo == null) {
            throw new DavException(DavServletResponse.SC_BAD_REQUEST, "Valid update request body required.");
        }
        if (!exists()) {
            throw new DavException(DavServletResponse.SC_NOT_FOUND);
        }

        Session session = getRepositorySession();
        MultiStatus ms = new MultiStatus();
        try {
            Element udElem = updateInfo.getUpdateElement();
            boolean removeExisting = DomUtil.hasChildElement(udElem, ItemResourceConstants.XML_REMOVEEXISTING, ItemResourceConstants.NAMESPACE);

            // register eventListener in order to be able to report the modified resources.
            EventListener el = new EListener(updateInfo.getPropertyNameSet(), ms);
            registerEventListener(el, session.getRootNode().getPath());

            String[] hrefs = updateInfo.getVersionHref();
            if (hrefs == null || hrefs.length < 1) {
                throw new DavException(DavServletResponse.SC_BAD_REQUEST, "Invalid update request body: at least a single version href must be specified.");
            }
            // perform the update/restore according to the update info
            Version[] versions = new Version[hrefs.length];
            for (int i = 0; i < hrefs.length; i++) {
                DavResourceLocator vLoc = getLocator().getFactory().createResourceLocator(getLocator().getPrefix(), hrefs[i]);
                String versionPath = vLoc.getRepositoryPath();
                Item item = getRepositorySession().getItem(versionPath);
                if (item instanceof Version) {
                    versions[i] = (Version) item;
                } else {
                    throw new DavException(DavServletResponse.SC_BAD_REQUEST, "Invalid update request body: href does not identify a version " + hrefs[i]);
                }
            }
            session.getWorkspace().restore(versions, removeExisting);

            // unregister the event listener again
            unregisterEventListener(el);

        } catch (RepositoryException e) {
            throw new JcrDavException(e);
        }
        return ms;
    }

    /**
     * @throws DavException (403) since workspace is not versionable. implementing
     * <code>VersionControlledResource</code> only for 'update'.
     */
    public MultiStatus merge(MergeInfo mergeInfo) throws DavException {
        throw new DavException(DavServletResponse.SC_FORBIDDEN);
    }

    /**
     * @throws DavException (403) since workspace is not versionable. implementing
     * <code>VersionControlledResource</code> only for 'update'.
     */
    public void label(LabelInfo labelInfo) throws DavException {
        throw new DavException(DavServletResponse.SC_FORBIDDEN);
    }

    /**
     * @throws DavException (403) since workspace is not versionable. implementing
     * <code>VersionControlledResource</code> only for 'update'.
     */
    public VersionHistoryResource getVersionHistory() throws DavException {
        throw new DavException(DavServletResponse.SC_FORBIDDEN);
    }

    //---------------------------------------------------< AbstractResource >---
    @Override
    protected void initLockSupport() {
        // lock not allowed
    }

    @Override
    protected void initSupportedReports() {
        super.initSupportedReports();
        supportedReports.addReportType(JcrPrivilegeReport.PRIVILEGES_REPORT);
    }

    @Override
    protected String getWorkspaceHref() {
        return getHref();
    }

    @Override
    protected void initProperties() {
        super.initProperties();
        try {
            // init workspace specific properties
            NamespaceRegistry nsReg = getRepositorySession().getWorkspace().getNamespaceRegistry();
            DavProperty<?> namespacesProp = new NamespacesProperty(nsReg);
            properties.add(namespacesProp);
        } catch (RepositoryException e) {
            log.error("Failed to access NamespaceRegistry: " + e.getMessage());
        }

        // TODO: required property DAV:workspace-checkout-set (computed)
    }
}
