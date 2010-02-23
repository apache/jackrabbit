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

import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.DavResourceFactory;
import org.apache.jackrabbit.webdav.DavResourceIterator;
import org.apache.jackrabbit.webdav.DavResourceIteratorImpl;
import org.apache.jackrabbit.webdav.DavResourceLocator;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.search.SearchResource;
import org.apache.jackrabbit.webdav.io.InputContext;
import org.apache.jackrabbit.webdav.io.OutputContext;
import org.apache.jackrabbit.webdav.version.DeltaVResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Repository;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.OutputStreamWriter;

/**
 * <code>RootCollection</code> represent the WebDAV root resource that does not
 * represent any repository item. A call to getMembers() returns a
 * <code>DavResourceIterator</code> containing only workspace resources
 * resources, thus revealing the names of the accessable JCR workspaces.
 */
public class RootCollection extends AbstractResource {

    private static Logger log = LoggerFactory.getLogger(RootCollection.class);

    /**
     * Create a new <code>RootCollection</code>.
     *
     * @param locator
     * @param session
     */
    protected RootCollection(DavResourceLocator locator, JcrDavSession session,
                             DavResourceFactory factory) {
        super(locator, session, factory);

        // initialize the supported locks and reports
        initLockSupport();
        initSupportedReports();
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
        sb.append(", ");
        sb.append(DeltaVResource.METHODS_INCL_MKWORKSPACE);
        sb.append(", ");
        sb.append(SearchResource.METHODS);
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
            sb.append("<body><h2>").append(repostr).append("</h2>");
            sb.append("<h3>Available Workspace Resources:</h3><ul>");

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
     * workspace resources available.
     *
     * @return members of this collection
     * @see org.apache.jackrabbit.webdav.DavResource#getMembers()
     */
    public DavResourceIterator getMembers() {
        List<DavResource> memberList = new ArrayList();
        try {
            String[] wsNames = getRepositorySession().getWorkspace().getAccessibleWorkspaceNames();
            for (String wsName : wsNames) {
                String wspPath = "/" + wsName;
                DavResourceLocator childLoc = getLocator().getFactory().createResourceLocator(getLocator().getPrefix(), wspPath, wspPath);
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
    @Override
    protected void initLockSupport() {
        // no locking supported
    }

    /**
     * Since the root resource does not represent a repository item and therefore
     * is not member of a workspace resource, this method always returns
     * <code>null</code>.
     *
     * @return <code>null</code>
     * @see AbstractResource#getWorkspaceHref()
     */
    @Override
    protected String getWorkspaceHref() {
        return null;
    }
}
