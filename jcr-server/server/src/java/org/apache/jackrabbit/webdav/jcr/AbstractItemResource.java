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

import org.apache.log4j.Logger;
import org.apache.jackrabbit.webdav.transaction.TxLockEntry;
import org.apache.jackrabbit.webdav.version.report.SupportedReportSetProperty;
import org.apache.jackrabbit.webdav.version.report.ReportType;
import org.apache.jackrabbit.webdav.DavResourceLocator;
import org.apache.jackrabbit.webdav.DavResourceFactory;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.property.DefaultDavProperty;
import org.apache.jackrabbit.webdav.property.HrefProperty;
import org.apache.jackrabbit.webdav.jcr.version.report.NodeTypesReport;
import org.apache.jackrabbit.webdav.jcr.version.report.LocateByUuidReport;
import org.apache.jackrabbit.webdav.jcr.version.report.RegisteredNamespacesReport;
import org.apache.jackrabbit.webdav.jcr.version.report.RepositoryDescriptorsReport;
import org.apache.jackrabbit.webdav.jcr.nodetype.ItemDefinitionImpl;
import org.apache.jackrabbit.webdav.jcr.nodetype.NodeDefinitionImpl;
import org.apache.jackrabbit.webdav.jcr.nodetype.PropertyDefinitionImpl;
import org.apache.jackrabbit.util.Text;

import javax.jcr.Item;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Workspace;
import javax.jcr.Node;
import javax.jcr.Property;

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
    AbstractItemResource(DavResourceLocator locator, JcrDavSession session,
                         DavResourceFactory factory, Item item) {
        super(locator, session, factory);
        this.item = item;

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
     * @see org.apache.jackrabbit.webdav.DavResource#exists()
     */
    public boolean exists() {
        return item != null;
    }

    /**
     * Retrieves the last segment of the item path (or the resource path if
     * this resource does not exist). An item path is in addition first translated
     * to the corresponding resource path.<br>
     * NOTE: the displayname is not equivalent to {@link Item#getName() item name}
     * which is exposed with the {@link #JCR_NAME &#123;http://www.day.com/jcr/webdav/1.0&#125;name}
     * property.
     *
     * @see org.apache.jackrabbit.webdav.DavResource#getDisplayName() )
     */
    public String getDisplayName() {
        String resPath = getResourcePath();
        return (resPath != null) ? Text.getName(resPath) : resPath;
    }

    /**
     * Returns the resource representing the parent item of the repository item
     * represented by this resource. If this resoure represents the root item
     * a {@link RootCollection} is returned.
     *
     * @return the collection this resource is internal member of. Except for the
     * repository root, the returned collection always represent the parent
     * repository node.
     * @see org.apache.jackrabbit.webdav.DavResource#getCollection()
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
     * Moves the underlying repository item to the indicated destination.
     *
     * @param destination
     * @throws DavException
     * @see DavResource#move(DavResource)
     * @see javax.jcr.Session#move(String, String)
     */
    public void move(DavResource destination) throws DavException {
        if (!exists()) {
            throw new DavException(DavServletResponse.SC_NOT_FOUND);
        }
        DavResourceLocator destLocator = destination.getLocator();
        if (!getLocator().isSameWorkspace(destLocator)) {
            throw new DavException(DavServletResponse.SC_FORBIDDEN);
        }

        try {
            String itemPath = getLocator().getRepositoryPath();
            String destItemPath = destination.getLocator().getRepositoryPath();
            if (getTransactionId() == null) {
                // if not part of a transaction directely import on workspace
                getRepositorySession().getWorkspace().move(itemPath, destItemPath);
            } else {
                // changes will not be persisted unless the tx is completed.
                getRepositorySession().move(itemPath, destItemPath);
            }
            // no use in calling 'complete' that would fail for a moved item anyway.
        } catch (PathNotFoundException e) {
            // according to rfc 2518
            throw new DavException(DavServletResponse.SC_CONFLICT, e.getMessage());
        } catch (RepositoryException e) {
            throw new JcrDavException(e);
        }
    }

    /**
     * Copies the underlying repository item to the indicated destination. If
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

        try {
            String itemPath = getLocator().getRepositoryPath();
            String destItemPath = destination.getLocator().getRepositoryPath();
            Workspace workspace = getRepositorySession().getWorkspace();
            if (getLocator().isSameWorkspace(destination.getLocator())) {
                workspace.copy(itemPath, destItemPath);
            } else {
                log.error("Copy between workspaces is not yet implemented (src: '" + getHref() + "', dest: '" + destination.getHref() + "')");
                throw new DavException(DavServletResponse.SC_NOT_IMPLEMENTED);
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
                // add href-property for the items parent unless its the root item
                if (item.getDepth() > 0) {
                    String parentHref = getLocatorFromItem(item.getParent()).getHref(true);
                    properties.add(new HrefProperty(JCR_PARENT, parentHref, false));
                }
                // protected 'definition' property revealing the item definition
                ItemDefinitionImpl val;
                if (item.isNode()) {
                    val = NodeDefinitionImpl.create(((Node)item).getDefinition());
                } else {
                    val = PropertyDefinitionImpl.create(((Property)item).getDefinition());
                }
                properties.add(new DefaultDavProperty(JCR_DEFINITION, val, true));
            } catch (RepositoryException e) {
                // should not get here
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
            DavResourceLocator wspLocator = locator.getFactory().createResourceLocator(locator.getPrefix(), locator.getWorkspacePath(), ItemResourceConstants.ROOT_ITEM_PATH);
            workspaceHref = wspLocator.getHref(true);
        }
	log.info(workspaceHref);
        return workspaceHref;
    }

    /**
     * If this resource exists but does not contain a transaction id, complete
     * will try to persist any modifications present on the underlying
     * repository item.
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
     * Retrieves the last segment of the given path and removes the index if
     * present.
     *
     * @param itemPath
     * @return valid jcr item name
     */
    protected static String getItemName(String itemPath) {
        if (itemPath == null) {
            throw new IllegalArgumentException("Cannot retrieve name from a 'null' item path.");
        }
        // retrieve the last part of the path
        String name = Text.getName(itemPath);
        // remove index
        if (name.endsWith("]")) {
            name = name.substring(0, name.lastIndexOf('['));
        }
        return name;
    }
}