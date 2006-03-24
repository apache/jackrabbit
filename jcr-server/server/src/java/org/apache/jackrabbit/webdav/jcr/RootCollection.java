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

import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.DavResourceFactory;
import org.apache.jackrabbit.webdav.DavResourceIterator;
import org.apache.jackrabbit.webdav.DavResourceIteratorImpl;
import org.apache.jackrabbit.webdav.DavResourceLocator;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.DavMethods;
import org.apache.jackrabbit.webdav.io.InputContext;
import org.apache.jackrabbit.webdav.jcr.version.report.NodeTypesReport;
import org.apache.jackrabbit.webdav.jcr.version.report.RegisteredNamespacesReport;
import org.apache.jackrabbit.webdav.jcr.version.report.RepositoryDescriptorsReport;
import org.apache.jackrabbit.webdav.version.report.ReportType;
import org.apache.jackrabbit.webdav.version.report.SupportedReportSetProperty;
import org.apache.log4j.Logger;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * <code>RootCollection</code> represent the WebDAV root resource that does not
 * represent any repository item. A call to getMembers() returns a
 * <code>DavResourceIterator</code> containing only <code>RootItemCollection</code>
 * resources, thus revealing the names of the accessable workspaces.
 */
public class RootCollection extends AbstractResource implements DavResource {

    private static Logger log = Logger.getLogger(RootCollection.class);

    /**
     * Create a new <code>RootCollection</code>.
     *
     * @param locator
     * @param session
     */
    protected RootCollection(DavResourceLocator locator, JcrDavSession session,
                             DavResourceFactory factory) {
        super(locator, session, factory);
        setModificationTime(new Date().getTime());

        // initialize the supported locks and reports
        initLockSupport();
        initSupportedReports();
    }

    /**
     * Returns a string listing the complieance classes for this resource as it
     * is required for the DAV response header.
     *
     * @return string listing the compliance classes.
     * @see org.apache.jackrabbit.webdav.DavResource#getComplianceClass()
     */
    public String getComplianceClass() {
        return DavResource.COMPLIANCE_CLASS;
    }

    /**
     * Returns a string listing the METHODS for this resource as it
     * is required for the "Allow" response header.
     *
     * @return string listing the METHODS allowed
     * @see org.apache.jackrabbit.webdav.DavResource#getSupportedMethods()
     */
    public String getSupportedMethods() {
        StringBuffer sb = new StringBuffer(DavResource.METHODS);
        return sb.toString();
    }

    /**
     * Returns true
     *
     * @return true
     * @see org.apache.jackrabbit.webdav.DavResource#exists()
     */
    public boolean exists() {
        return true;
    }

    /**
     * Returns true
     *
     * @return true
     * @see org.apache.jackrabbit.webdav.DavResource#isCollection()
     */
    public boolean isCollection() {
        return true;
    }

    /**
     * Returns an empty string.
     *
     * @return empty string
     * @see org.apache.jackrabbit.webdav.DavResource#getDisplayName()
     */
    public String getDisplayName() {
        return "";
    }

    /**
     * Always returns <code>null</code>
     *
     * @return <code>null</code> for the root resource is not internal member
     * of any resource.
     * @see org.apache.jackrabbit.webdav.DavResource#getCollection()
     */
    public DavResource getCollection() {
        return null;
    }

    /**
     * Throws exception: 403 Forbidden.
     * @see DavResource#addMember(DavResource, InputContext)
     */
    public void addMember(DavResource resource, InputContext inputContext) throws DavException {
        throw new DavException(DavServletResponse.SC_FORBIDDEN);
    }

    /**
     * Returns an iterator over the member resources, which are all
     * <code>RootItemCollection</code> resources, revealing
     * the names of all available workspaces.
     *
     * @return members of this collection
     * @see org.apache.jackrabbit.webdav.DavResource#getMembers()
     */
    public DavResourceIterator getMembers() {
        List memberList = new ArrayList();
        try {
            String[] wsNames = getRepositorySession().getWorkspace().getAccessibleWorkspaceNames();
            for (int i = 0; i < wsNames.length; i++) {
                DavResourceLocator childLoc = getLocator().getFactory().createResourceLocator(getLocator().getPrefix(), "/"+wsNames[i], ItemResourceConstants.ROOT_ITEM_PATH);
                memberList.add(createResourceFromLocator(childLoc));
            }
        } catch (RepositoryException e) {
            log.error(e.getMessage());
        } catch (DavException e) {
            // should never occur
            log.error(e.getMessage());
        }
        return new DavResourceIteratorImpl(memberList);
    }

    /**
     * Throws exception: 403 Forbidden.
     * @see DavResource#removeMember(org.apache.jackrabbit.webdav.DavResource)
     */
    public void removeMember(DavResource member) throws DavException {
        throw new DavException(DavServletResponse.SC_FORBIDDEN);
    }

    //--------------------------------------------------------------------------
    /**
     * @see AbstractResource#initLockSupport()
     */
    protected void initLockSupport() {
        // no locking supported
    }

    /**
     * @see AbstractResource#initSupportedReports()
     */
    protected void initSupportedReports() {
        supportedReports = new SupportedReportSetProperty(new ReportType[] {
            ReportType.EXPAND_PROPERTY,
            NodeTypesReport.NODETYPES_REPORT,
            RegisteredNamespacesReport.REGISTERED_NAMESPACES_REPORT,
            RepositoryDescriptorsReport.REPOSITORY_DESCRIPTORS_REPORT
        });
    }

    /**
     * Since the root resource does not represent a repository item and therefore
     * is not member of a workspace resource, the workspace href is calculated
     * from the workspace name retrieved from the underlying repository session.
     *
     * @return workspace href build from workspace name.
     * @see AbstractResource#getWorkspaceHref()
     */
    protected String getWorkspaceHref() {
        Session session = getRepositorySession();
        if (session != null) {
            String workspaceName = session.getWorkspace().getName();
            DavResourceLocator loc = getLocator().getFactory().createResourceLocator(getLocator().getPrefix(), "/"+workspaceName, ItemResourceConstants.ROOT_ITEM_PATH);
            return loc.getHref(true);
        }
        return null;
    }
}