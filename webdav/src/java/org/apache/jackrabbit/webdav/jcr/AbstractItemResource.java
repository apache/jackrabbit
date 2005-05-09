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
package org.apache.jackrabbit.webdav.jcr;

import org.apache.log4j.Logger;
import org.apache.jackrabbit.webdav.property.*;
import org.apache.jackrabbit.webdav.*;
import org.apache.jackrabbit.webdav.jcr.version.report.*;
import org.apache.jackrabbit.webdav.transaction.TxLockEntry;
import org.apache.jackrabbit.webdav.version.report.SupportedReportSetProperty;
import org.apache.jackrabbit.webdav.version.report.ReportType;
import org.apache.jackrabbit.webdav.util.Text;

import javax.jcr.*;

/**
 * <code>AbstractItemResource</code> covers common functionality for the various
 * resources, that represent a repository item.
 */
abstract class AbstractItemResource extends AbstractResource implements
    ItemResourceConstants {

    private static Logger log = Logger.getLogger(AbstractItemResource.class);

    protected final Item item;

    /**
     * Create a new <code>AbstractItemResource</code>.
     *
     * @param locator
     * @param session
     */
    AbstractItemResource(DavResourceLocator locator, DavSession session, DavResourceFactory factory, Item item) {
        super(locator, session, factory);
        this.item = item;
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
            throw new DavException(DavServletResponse.SC_CONFLICT, e.getMessage());
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

    //--------------------------------------------------------------------------
    /**
     * Initialize the {@link org.apache.jackrabbit.webdav.lock.SupportedLock} property
     * with entries that are valid for any type item resources.
     *
     * @see org.apache.jackrabbit.webdav.lock.SupportedLock
     * @see org.apache.jackrabbit.webdav.transaction.TxLockEntry
     * @see AbstractResource#initLockSupport()
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
     * @see org.apache.jackrabbit.webdav.version.report.SupportedReportSetProperty
     * @see AbstractResource#initSupportedReports()
     */
    protected void initSupportedReports() {
        if (exists()) {
            supportedReports = new SupportedReportSetProperty(new ReportType[] {
                ReportType.EXPAND_PROPERTY,
                NodeTypesReport.NODETYPES_REPORT,
                ExportViewReport.EXPORTVIEW_REPORT,
                LocateByUuidReport.LOCATE_BY_UUID_REPORT,
                RegisteredNamespacesReport.REGISTERED_NAMESPACES_REPORT,
                RepositoryDescriptorsReport.REPOSITORY_DESCRIPTORS_REPORT
            });
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
        }
    }

    /**
     * @return href of the workspace or <code>null</code> if this resource
     * does not represent a repository item.
     *
     * @see AbstractResource#getWorkspaceHref()
     */
    protected String getWorkspaceHref() {
        String workspaceHref = null;
	DavResourceLocator locator = getLocator();
        if (locator != null && locator.getWorkspaceName() != null) {
            workspaceHref = locator.getHref(isCollection());
            if (locator.getResourcePath() != null) {
                workspaceHref = workspaceHref.substring(workspaceHref.indexOf(locator.getResourcePath()));
            }
        }
	log.info(workspaceHref);
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
}