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
import org.apache.jackrabbit.webdav.version.DeltaVResource;
import org.apache.jackrabbit.webdav.version.report.Report;
import org.apache.jackrabbit.webdav.version.report.ReportType;
import org.apache.jackrabbit.webdav.version.report.ReportInfo;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavSession;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.util.Text;
import org.xml.sax.SAXException;
import org.w3c.dom.Element;
import org.w3c.dom.Document;

import javax.jcr.Session;
import javax.jcr.RepositoryException;
import javax.jcr.PathNotFoundException;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * <code>ExportViewReport</code> handles REPORT requests for the 'exportview'
 * report. The 'exportview' report is used to export
 * {@link Session#exportDocumentView(String, java.io.OutputStream, boolean, boolean) DocView}
 * and {@link Session#exportSystemView(String, java.io.OutputStream, boolean, boolean) SysView}
 * of the {@link javax.jcr.Item item} represented by the requested resource.
 * <p/>
 * The request body must contain a {@link ItemResourceConstants#NAMESPACE dcr}:exportview
 * element:
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

    private String absItemPath;
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
     * Always returns <code>false</code>.
     *
     * @return false
     */
    public boolean isMultiStatusReport() {
        return false;
    }

    /**
     * @see Report#init(org.apache.jackrabbit.webdav.version.DeltaVResource, org.apache.jackrabbit.webdav.version.report.ReportInfo)
     */
    public void init(DeltaVResource resource, ReportInfo info) throws DavException {
        if (!getType().isRequestedReportType(info)) {
            throw new DavException(DavServletResponse.SC_BAD_REQUEST, "dcr:exportview element expected.");
        }
        if (resource == null) {
            throw new DavException(DavServletResponse.SC_BAD_REQUEST, "Resource must not be null.");
        }

        this.info = info;

        DavSession davSession = resource.getSession();
        if (davSession == null || davSession.getRepositorySession() == null) {
            throw new DavException(DavServletResponse.SC_BAD_REQUEST, "The resource must provide a non-null session object in order to create the exportview report.");
        }
        session = davSession.getRepositorySession();
        absItemPath = resource.getLocator().getJcrPath();
        try {
            if (!session.itemExists(absItemPath)) {
                throw new JcrDavException(new PathNotFoundException(absItemPath + " does not exist."));
    }
        } catch (RepositoryException e) {
            throw new JcrDavException(e);
        }
    }

    /**
     * Creates a Xml document from the generated view.
     *
     * @param document
     * @return Xml element representing the output of the specified view.
     * @see org.apache.jackrabbit.webdav.xml.XmlSerializable#toXml(Document)
     */
    public Element toXml(Document document) {
        boolean skipBinary = info.containsContentElement("skipbinary", ItemResourceConstants.NAMESPACE);
        boolean noRecurse = info.containsContentElement("norecurse", ItemResourceConstants.NAMESPACE);

        // todo improve...
        try {
            // create tmpFile in default system-tmp directory
            String prefix = "_tmp_" + Text.getName(absItemPath);
            File tmpfile = File.createTempFile(prefix, null, null);
            tmpfile.deleteOnExit();

            FileOutputStream out = new FileOutputStream(tmpfile);
            if (info.containsContentElement("sysview", ItemResourceConstants.NAMESPACE)) {
                session.exportSystemView(absItemPath, out, skipBinary, noRecurse);
            } else {
                // default is docview
                session.exportDocumentView(absItemPath, out, skipBinary, noRecurse);
            }
            out.close();

            InputStream in = new FileInputStream(tmpfile);
            Document tmpDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in);

            // import the root node of the generated xml to the given document.
            Element rootElem = (Element)document.importNode(tmpDoc.getDocumentElement(), true);
            return rootElem;

        } catch (RepositoryException e) {
            log.error(e.getMessage());
        } catch (FileNotFoundException e) {
            log.error(e.getMessage());
        } catch (IOException e) {
            log.error(e.getMessage());
        } catch (ParserConfigurationException e) {
            log.error(e.getMessage());
        } catch (SAXException e) {
            log.error(e.getMessage());
        }
        return null;
    }
}