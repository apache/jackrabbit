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
package org.apache.jackrabbit.webdav.jcr.version.report;

import org.apache.log4j.Logger;
import org.apache.jackrabbit.webdav.jcr.ItemResourceConstants;
import org.apache.jackrabbit.webdav.jcr.JcrDavException;
import org.apache.jackrabbit.webdav.version.report.*;
import org.apache.jackrabbit.webdav.version.DeltaVResource;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavSession;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.util.Text;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import javax.jcr.Session;
import javax.jcr.RepositoryException;
import javax.jcr.PathNotFoundException;
import java.io.*;

/**
 * <code>ExportViewReport</code> handles REPORT requests for the 'exportview'
 * report. The 'exportview' report is used to export
 * {@link Session#exportDocumentView(String, java.io.OutputStream, boolean, boolean) DocView}
 * and {@link Session#exportSystemView(String, java.io.OutputStream, boolean, boolean) SysView}
 * of the {@link javax.jcr.Item item} represented by the requested resource.
 * <p/>
 * The request body must contain a jcr:exportview element:
 * <pre>
 * &lt;!ELEMENT exportview  ( (sysview | docview)?, skipbinary?, norecurse ) &gt;
 * &lt;!ELEMENT sysview EMPTY &gt;
 * &lt;!ELEMENT docview EMPTY &gt;
 * &lt;!ELEMENT skipbinary EMPTY &gt;
 * &lt;!ELEMENT norecurse EMPTY &gt;
 * </pre>
 * If no view type is specified the DocView is generated.
 */
public class ExportViewReport implements Report {

    private static Logger log = Logger.getLogger(ExportViewReport.class);

    private static final String REPORT_NAME = "exportview";

    /**
     * The exportview report type
     */
    public static final ReportType EXPORTVIEW_REPORT = ReportType.register(REPORT_NAME, ItemResourceConstants.NAMESPACE, ExportViewReport.class);

    private String absPath;
    private Session session;
    private ReportInfo info;

    /**
     * Returns {@link #EXPORTVIEW_REPORT} report type.
     *
     * @return {@link #EXPORTVIEW_REPORT}
     * @see org.apache.jackrabbit.webdav.version.report.Report#getType()
     */
    public ReportType getType() {
        return EXPORTVIEW_REPORT;
    }

    /**
     * @param resource The resource this report is generated from. NOTE: the
     * {@link org.apache.jackrabbit.webdav.DavResource#getResourcePath() resource path}
     * of the resource is used as 'absPath' argument for exporting the specified
     * view.
     * @throws IllegalArgumentException if the resource is <code>null</code> or
     * if the session object provided with the resource is <code>null</code>.
     * @see Report#setResource(org.apache.jackrabbit.webdav.version.DeltaVResource)
     */
    public void setResource(DeltaVResource resource) {
        if (resource == null) {
            throw new IllegalArgumentException("Resource must not be null.");
        }
        DavSession davSession = resource.getSession();
        if (davSession == null || davSession.getRepositorySession() == null) {
            throw new IllegalArgumentException("The resource must provide a non-null session object in order to create the jcr:nodetypes report.");
        }
        session = davSession.getRepositorySession();
        absPath = resource.getResourcePath();
    }

    /**
     * @param info
     * @throws IllegalArgumentException if the specified {@link ReportInfo info}
     * object does not contain a jcr:exportview element.
     * @see Report#setInfo(org.apache.jackrabbit.webdav.version.report.ReportInfo)
     */
    public void setInfo(ReportInfo info) {
        if (info == null || !REPORT_NAME.equals(info.getReportElement().getName())) {
            throw new IllegalArgumentException("jcr:exportview element expected.");
        }
        this.info = info;
    }

    /**
     * Creates a Xml document from the generated view.
     *
     * @return Xml document representing the output of the specified view.
     * @throws DavException if the report document could not be created.
     * @see org.apache.jackrabbit.webdav.version.report.Report#toXml()
     */
    public Document toXml() throws DavException {
        Element reportElem = info.getReportElement();
        boolean skipBinary = reportElem.getChild("skipbinary", ItemResourceConstants.NAMESPACE) != null;
        boolean noRecurse = reportElem.getChild("norecurse", ItemResourceConstants.NAMESPACE) != null;

        try {
            // create tmpFile in default system-tmp directory
            String prefix = "_tmp_" + Text.getName(absPath);
            File tmpfile = File.createTempFile(prefix, null, null);
            tmpfile.deleteOnExit();
            FileOutputStream out = new FileOutputStream(tmpfile);

            if (reportElem.getChild("sysview", ItemResourceConstants.NAMESPACE) != null) {
                session.exportSystemView(absPath, out, skipBinary, noRecurse);
            } else {
                // default is docview
                session.exportDocumentView(absPath, out, skipBinary, noRecurse);
            }
            out.close();

            SAXBuilder builder = new SAXBuilder(false);
            InputStream in = new FileInputStream(tmpfile);
            return builder.build(in);

        } catch (FileNotFoundException e) {
            throw new DavException(DavServletResponse.SC_INTERNAL_SERVER_ERROR);
        } catch (IOException e) {
            throw new DavException(DavServletResponse.SC_INTERNAL_SERVER_ERROR);
        } catch (PathNotFoundException e) {
            throw new DavException(DavServletResponse.SC_NOT_FOUND);
        } catch (RepositoryException e) {
            throw new JcrDavException(e);
        } catch (JDOMException e) {
            throw new DavException(DavServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}