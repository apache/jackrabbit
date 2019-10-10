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
package org.apache.jackrabbit.webdav.jcr.version.report;

import javax.jcr.Session;

import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.DavSession;
import org.apache.jackrabbit.webdav.jcr.JcrDavSession;
import org.apache.jackrabbit.webdav.version.report.AbstractReport;
import org.apache.jackrabbit.webdav.version.report.Report;
import org.apache.jackrabbit.webdav.version.report.ReportInfo;

/**
 * <code>AbstractJcrReport</code>...
 */
public abstract class AbstractJcrReport extends AbstractReport {

    private Session session;
    private ReportInfo reportInfo;

    /**
     * Performs basic validation checks common to all JCR specific reports.
     *
     * @param resource
     * @param info
     * @throws DavException
     * @see Report#init(DavResource, ReportInfo)
     */
    public void init(DavResource resource, ReportInfo info) throws DavException {
        if (resource == null || info == null) {
            throw new DavException(DavServletResponse.SC_BAD_REQUEST, "Unable to run report: WebDAV Resource and ReportInfo must not be null.");
        }
        if (!getType().isRequestedReportType(info)) {
            throw new DavException(DavServletResponse.SC_BAD_REQUEST, "Expected report type: '" + getType().getReportName() + "', found: '" + info.getReportName() + ";" + "'.");
        }
        if (info.getDepth() > DavConstants.DEPTH_0) {
            throw new DavException(DavServletResponse.SC_BAD_REQUEST, "Invalid Depth header: " + info.getDepth());
        }

        DavSession davSession = resource.getSession();
        if (davSession == null) {
            throw new DavException(DavServletResponse.SC_BAD_REQUEST, "The resource must provide a non-null session object in order to create '" + getType().getReportName()+ "' report.");
        }
        session = JcrDavSession.getRepositorySession(resource.getSession());
        if (session == null) {
            throw new DavException(DavServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal error: Unable to access repository session.");
        }
        reportInfo = info;
    }

    //-----------------------------------------------------< implementation >---
    /**
     * @return session Session object as obtained from the {@link DavSession}.
     */
    Session getRepositorySession() {
        return session;
    }

    /**
     * @return reportInfo the <code>ReportInfo</code> specifying the requested
     * report details.
     */
    ReportInfo getReportInfo() {
        return reportInfo;
    }
}
