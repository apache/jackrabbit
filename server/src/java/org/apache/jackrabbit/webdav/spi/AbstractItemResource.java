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
package org.apache.jackrabbit.webdav.spi;

import org.apache.log4j.Logger;
import org.apache.jackrabbit.webdav.property.*;
import org.apache.jackrabbit.webdav.*;
import org.apache.jackrabbit.webdav.spi.search.SearchResourceImpl;
import org.apache.jackrabbit.webdav.spi.version.report.NodeTypesReport;
import org.apache.jackrabbit.webdav.spi.version.report.ExportViewReport;
import org.apache.jackrabbit.webdav.spi.version.report.LocateByUuidReport;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.transaction.TxLockEntry;
import org.apache.jackrabbit.webdav.version.report.*;
import org.apache.jackrabbit.webdav.version.DeltaVResource;
import org.apache.jackrabbit.webdav.version.DeltaVConstants;
import org.apache.jackrabbit.webdav.version.OptionsResponse;
import org.apache.jackrabbit.webdav.version.OptionsInfo;
import org.apache.jackrabbit.webdav.search.*;
import org.apache.jackrabbit.webdav.util.Text;

import javax.jcr.*;
import java.util.*;

/**
 * <code>AbstractItemResource</code> covers common functionality for the various
 * resources, that represent a repository item.
 */
abstract class AbstractItemResource extends AbstractResource implements
    SearchResource, DeltaVResource, ItemResourceConstants {

    private static Logger log = Logger.getLogger(AbstractItemResource.class);

    protected final Item item;
    protected SupportedReportSetProperty supportedReports;

    /**
     * Create a new <code>AbstractItemResource</code>.
     *
     * @param locator
     * @param session
     */
    AbstractItemResource(DavResourceLocator locator, DavSession session, DavResourceFactory factory) {
        super(locator, session, factory);
        Item repositoryItem = null;
        if (locator != null) {
            try {
                repositoryItem = getRepositorySession().getItem(locator.getResourcePath());
            } catch (RepositoryException e) {
                // ignore: exists field evaluates to false
                log.info(e.getMessage());
            }
        }
        item = repositoryItem;
        // initialize the supported locks and reports
        initLockSupport();
        initSupportedReports();
    }

    //----------------------------------------------< DavResource interface >---
    /**
     * @see org.apache.jackrabbit.webdav.DavResource#getComplianceClass()
     */
    public String getComplianceClass() {
        return ItemResourceConstants.COMPLIANCE_CLASS;
    }

    /**
     * @see org.apache.jackrabbit.webdav.DavResource#getSupportedMethods()
     */
    public String getSupportedMethods() {
        return ItemResourceConstants.METHODS;
    }

    /**
     * Returns true if there exists a {@link Item repository item} with the given
     * resource path, false otherwise.
     *
     * @see DavResource#exists()
     */
    public boolean exists() {
        return item != null;
    }

    /**
     * @see DavResource#getDisplayName() )
     */
    public String getDisplayName() {
        String name = null;
        if (exists()) {
            try {
                name = item.getName();
            } catch (RepositoryException e) {
                // ignore: should not occure
                log.warn(e.getMessage());
            }
        }
        String resPath = getResourcePath();
        if (name == null && resPath != null) {
            int pos = resPath.lastIndexOf('/');
            if (pos>=0) {
                name = resPath.substring(pos+1);
            } else {
                name = resPath;
            }
            // note: since index info is present only with existing resources
            // there is no need to check for any '[index]' suffix.
        }
        return name;
    }

    /**
     * Returns the resource representing the parent item of the repository item
     * represented by this resource. If this resoure represents the root item
     * a {@link RootCollection} is returned.
     *
     * @return the collection this resource is internal member of. Except for the
     * repository root, the returned collection always represent the parent
     * repository node.
     * @see DavResource#getCollection()
     */
    public DavResource getCollection() {
        DavResource collection = null;

        String resourcePath = getResourcePath();
        // No special treatment for the root-item needed, because this is
        // covered by the RootItemCollection itself.
        String parentResourcePath = Text.getRelativeParent(resourcePath, 1);
        String parentWorkspacePath = getLocator().getWorkspacePath();

        DavResourceLocator parentLoc = getLocator().getFactory().createResourceLocator(getLocator().getPrefix(), parentWorkspacePath, parentResourcePath);
        try {
            collection = createResourceFromLocator(parentLoc);
        } catch (DavException e) {
            log.error("Unexpected error while retrieving collection: " + e.getMessage());
        }

        return collection;
    }

    /**
     * Moves the underlaying repository item to the indicated destination.
     *
     * @param destination
     * @throws DavException
     * @see DavResource#move(DavResource)
     * @see Session#move(String, String)
     */
    public void move(DavResource destination) throws DavException {
        if (!exists()) {
            throw new DavException(DavServletResponse.SC_NOT_FOUND);
        }
        DavResourceLocator destPath = destination.getLocator();
        if (!getLocator().isSameWorkspace(destPath)) {
            throw new DavException(DavServletResponse.SC_FORBIDDEN);
        }

        try {
            getRepositorySession().move(getResourcePath(), destination.getResourcePath());
            complete();

        } catch (PathNotFoundException e) {
            // according to rfc 2518
            throw new DavException(DavServletResponse.SC_NOT_FOUND, e.getMessage());
        } catch (RepositoryException e) {
            throw new JcrDavException(e);
        }
    }

    /**
     * Copies the underlaying repository item to the indicated destination. If
     * the locator of the specified destination resource indicates a different
     * workspace, {@link Workspace#copy(String, String, String)} is used to perform
     * the copy operation, {@link Workspace#copy(String, String)} otherwise.
     * <p/>
     * Note, that this implementation does not support shallow copy.
     *
     * @param destination
     * @param shallow
     * @throws DavException
     * @see DavResource#copy(DavResource, boolean)
     * @see Workspace#copy(String, String)
     * @see Workspace#copy(String, String, String)
     */
    public void copy(DavResource destination, boolean shallow) throws DavException {
        if (!exists()) {
            throw new DavException(DavServletResponse.SC_NOT_FOUND);
        }
        // TODO: support shallow and deep copy is required by RFC 2518
        if (shallow) {
            throw new DavException(DavServletResponse.SC_FORBIDDEN, "Unable to perform shallow copy.");
        }

        if (!(destination instanceof AbstractItemResource)) {
            throw new DavException(DavServletResponse.SC_FORBIDDEN, "Cannot copy a resource that does not represent a repository item.");
        }

        try {
            AbstractItemResource destResource = (AbstractItemResource) destination;
            String destResourcePath = destResource.getResourcePath();
            Workspace workspace = getRepositorySession().getWorkspace();
            if (getLocator().isSameWorkspace(destination.getLocator())) {
                workspace.copy(getResourcePath(), destResourcePath);
            } else {
                Workspace destWorkspace = destResource.getRepositorySession().getWorkspace();
                destWorkspace.copy(workspace.getName(), getResourcePath(), destResourcePath);
            }
        } catch (PathNotFoundException e) {
            // according to RFC 2518, should not occur
            throw new DavException(DavServletResponse.SC_NOT_FOUND, e.getMessage());
        } catch (RepositoryException e) {
            throw new JcrDavException(e);
        }
    }

    //-------------------------------------------< SearchResource interface >---
    /**
     * @return
     * @see org.apache.jackrabbit.webdav.search.SearchResource#getQueryGrammerSet()
     */
    public QueryGrammerSet getQueryGrammerSet() {
        return new SearchResourceImpl(getLocator(), getSession()).getQueryGrammerSet();
    }

    /**
     * @param sRequest
     * @return
     * @throws DavException
     * @see SearchResource#search(org.apache.jackrabbit.webdav.search.SearchRequest)
     */
    public MultiStatus search(SearchRequest sRequest) throws DavException {
        return new SearchResourceImpl(getLocator(), getSession()).search(sRequest);
    }

    //-------------------------------------------< DeltaVResource interface >---
    /**
     * @param optionsInfo
     * @return object to be used in the OPTIONS response body or <code>null</code>
     * @see DeltaVResource#getOptionResponse(org.apache.jackrabbit.webdav.version.OptionsInfo)
     */
    public OptionsResponse getOptionResponse(OptionsInfo optionsInfo) {
        OptionsResponse oR = null;
        if (optionsInfo != null) {
            oR = new OptionsResponse();
            // currently on DAV:version-history-collection-set and
            // DAV:workspace-collection-set is supported.
            if (optionsInfo.containsElement(DeltaVConstants.XML_VH_COLLECTION_SET, DeltaVConstants.NAMESPACE)) {
                String[] hrefs = new String[] { getLocatorFromResourcePath(VERSIONSTORAGE_PATH).getHref(true)};
                oR.addEntry(DeltaVConstants.XML_VH_COLLECTION_SET, DeltaVConstants.NAMESPACE, hrefs);
            } else if (optionsInfo.containsElement(DeltaVConstants.XML_WSP_COLLECTION_SET, DeltaVConstants.NAMESPACE)) {
                // workspaces cannot be created anywhere.
                oR.addEntry(DeltaVConstants.XML_WSP_COLLECTION_SET, DeltaVConstants.NAMESPACE, new String[0]);
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

        if (supportedReports.isSupportedReport(reportInfo)) {
            try {
                Report report = ReportType.getType(reportInfo).createReport();
                report.setInfo(reportInfo);
                report.setResource(this);
                return report;
            } catch (IllegalArgumentException e) {
                // should never occur.
                throw new DavException(DavServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            }
        } else {
            throw new DavException(DavServletResponse.SC_UNPROCESSABLE_ENTITY, "Unkown report "+ reportInfo.getReportElement().getNamespacePrefix() + reportInfo.getReportElement().getName() +"requested.");
        }
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
        DavProperty prop = getProperty(hrefPropertyName);
        if (prop == null || !(prop instanceof HrefProperty)) {
            throw new DavException(DavServletResponse.SC_CONFLICT, "Unknown Href-Property '"+hrefPropertyName+"' on resource "+getResourcePath());
        }

        List hrefs = ((HrefProperty)prop).getHrefs();
        DavResource[] refResources = new DavResource[hrefs.size()];
        Iterator hrefIter = hrefs.iterator();
        int i = 0;
        while (hrefIter.hasNext()) {
            refResources[i] = getResourceFromHref((String)hrefIter.next());
            i++;
        }
        return refResources;
    }

    /**
     * Retrieve the <code>DavResource</code> object that is represented by
     * the given href String.
     *
     * @param href
     * @return <code>DavResource</code> object
     */
    private DavResource getResourceFromHref(String href) throws DavException {
        // build a new locator: remove trailing prefix
        DavResourceLocator locator = getLocator();
        String prefix = locator.getPrefix();
        if (href.startsWith(prefix)) {
            href = href.substring(prefix.length());
        }
        DavResourceLocator loc = locator.getFactory().createResourceLocator(prefix, href);

        // create a new resource object
        DavResource res;
        if (getRepositorySession().itemExists(loc.getResourcePath())) {
            res = createResourceFromLocator(loc);
        } else {
            throw new DavException(DavServletResponse.SC_NOT_FOUND);
        }
        return res;
    }

    //--------------------------------------------------------------------------
    /**
     * Initialize the {@link org.apache.jackrabbit.webdav.lock.SupportedLock} property
     * with entries that are valid for any type item resources.
     *
     * @see org.apache.jackrabbit.webdav.lock.SupportedLock
     * @see org.apache.jackrabbit.webdav.transaction.TxLockEntry
     */
    protected void initLockSupport() {
        if (exists()) {
            // add supportedlock entries for local and eventually for global transaction locks
            supportedLock.addEntry(new TxLockEntry(true));
            supportedLock.addEntry(new TxLockEntry(false));
        }
    }

    /**
     * Define the set of reports supported by this resource.
     *
     * @see SupportedReportSetProperty
     */
    protected void initSupportedReports() {
        if (exists()) {
            supportedReports = new SupportedReportSetProperty(new ReportType[] {
                ReportType.EXPAND_PROPERTY,
                NodeTypesReport.NODETYPES_REPORT,
                ExportViewReport.EXPORTVIEW_REPORT,
                LocateByUuidReport.LOCATE_BY_UUID_REPORT
            });
        } else {
            supportedReports = new SupportedReportSetProperty();
        }
    }

    /**
     * Fill the property set for this resource.
     */
    protected void initProperties() {
        super.initProperties();
        if (exists()) {
            try {
                properties.add(new DefaultDavProperty(JCR_NAME, item.getName()));
                properties.add(new DefaultDavProperty(JCR_PATH, item.getPath()));
                properties.add(new DefaultDavProperty(JCR_DEPTH, String.valueOf(item.getDepth())));
            } catch (RepositoryException e) {
                log.error("Error while accessing jcr properties: " + e.getMessage());
            }

            // transaction resource additional protected properties
            if (item.isNew()) {
                properties.add(new DefaultDavProperty(JCR_ISNEW, null, true));
            } else if (item.isModified()) {
                properties.add(new DefaultDavProperty(JCR_ISMODIFIED, null, true));
            }

            // DeltaV properties
            properties.add(supportedReports);
            // creator-displayname, comment: not value available from jcr
            properties.add(new DefaultDavProperty(DeltaVConstants.CREATOR_DISPLAYNAME, null, true));
            properties.add(new DefaultDavProperty(DeltaVConstants.COMMENT, null, true));


	    // 'workspace' property as defined by RFC 3253
	    String workspaceHref = getWorkspaceHref();
	    if (workspaceHref != null) {
		properties.add(new HrefProperty(DeltaVConstants.WORKSPACE, workspaceHref, true));
	    }
            // TODO: required supported-live-property-set
        }
    }

    /**
     * @return href of the workspace or <code>null</code> if this resource
     * does not represent a repository item.
     */
    private String getWorkspaceHref() {
        String workspaceHref = null;
	DavResourceLocator locator = getLocator();
        if (locator != null && locator.getWorkspaceName() != null) {
            workspaceHref = locator.getHref(isCollection());
            if (locator.getResourcePath() != null) {
                workspaceHref = workspaceHref.substring(workspaceHref.indexOf(locator.getResourcePath()));
            }
        }
        return workspaceHref;
    }

    /**
     * If this resource exists but does not contain a transaction id, complete
     * will try to persist any modifications prsent on the underlaying repository
     * item.
     *
     * @throws DavException if calling {@link Item#save()} fails
     */
    void complete() throws DavException {
        if (exists() && getTransactionId() == null) {
            try {
                if (item.isModified()) {
                    item.save();
                }
            } catch (RepositoryException e) {
                // this includes LockException, ConstraintViolationException etc. not detected before
                log.error("Error while completing request: " + e.getMessage() +" -> reverting changes.");
                try {
                    item.refresh(false);
                } catch (RepositoryException re) {
                    log.error("Error while reverting changes: " + re.getMessage());
                }
                throw new JcrDavException(e);
            }
        }
    }

    /**
     * Build a new {@link DavResourceLocator} from the given repository item.
     *
     * @param repositoryItem
     * @return a new locator for the specified item.
     * @see #getLocatorFromResourcePath(String)
     */
    protected DavResourceLocator getLocatorFromItem(Item repositoryItem) {
        String itemPath = null;
        try {
            if (repositoryItem != null) {
                itemPath = repositoryItem.getPath();
            }
        } catch (RepositoryException e) {
            // ignore: should not occur
            log.warn(e.getMessage());
        }
        return getLocatorFromResourcePath(itemPath);
    }

    /**
     * Shortcut for <code>getSession().getRepositorySession()</code>
     *
     * @return repository session present in the {@link #session}.
     */
    protected Session getRepositorySession() {
        return getSession().getRepositorySession();
    }
}