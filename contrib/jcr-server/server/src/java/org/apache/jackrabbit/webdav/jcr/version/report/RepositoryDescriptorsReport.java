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
import org.apache.jackrabbit.webdav.version.report.*;
import org.apache.jackrabbit.webdav.version.DeltaVResource;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavSession;
import org.apache.jackrabbit.webdav.jcr.ItemResourceConstants;
import org.jdom.Document;
import org.jdom.Element;

import javax.jcr.*;
import java.util.ArrayList;
import java.util.List;

/**
 * <code>RepositoryDescriptorsReport</code> allows to retrieve the repository
 * descriptors. The request body must be an empty 'jcr:repositorydescriptors' element:
 * <pre>
 * &lt;!ELEMENT repositorydescriptors EMPTY &gt;
 * </pre>
 * <br>
 * The response body must match the following format
 *
 * <pre>
 * &lt;!ELEMENT repositorydescriptors-report ( descriptor )* &gt;
 * &lt;!ELEMENT descriptor ( descriptorkey, descriptorvalue ) &gt;
 * &lt;!ELEMENT descriptorkey (#PCDATA) &gt;
 * &lt;!ELEMENT descriptorvalue (#PCDATA) &gt;
 * </pre>
 *
 * @see javax.jcr.Repository#getDescriptorKeys()
 * @see Repository#getDescriptor(String)
 */
public class RepositoryDescriptorsReport implements Report, ItemResourceConstants {

    private static Logger log = Logger.getLogger(RepositoryDescriptorsReport.class);

    /**
     * The registered type of this report.
     */
    public static final ReportType REPOSITORY_DESCRIPTORS_REPORT = ReportType.register("repositorydescriptors", ItemResourceConstants.NAMESPACE, RepositoryDescriptorsReport.class);

    private Repository repository;

    /**
     * Returns {@link #REPOSITORY_DESCRIPTORS_REPORT} type.
     * @return {@link #REPOSITORY_DESCRIPTORS_REPORT}
     * @see org.apache.jackrabbit.webdav.version.report.Report#getType()
     */
    public ReportType getType() {
        return REPOSITORY_DESCRIPTORS_REPORT;
    }

    /**
     * @param resource
     * @throws IllegalArgumentException if the resource or the session retrieved
     * from the specified resource is <code>null</code>
     * @see org.apache.jackrabbit.webdav.version.report.Report#setResource(org.apache.jackrabbit.webdav.version.DeltaVResource)
     */
    public void setResource(DeltaVResource resource) {
        if (resource == null) {
            throw new IllegalArgumentException("Resource must not be null.");
        }
        DavSession session = resource.getSession();
        if (session == null || session.getRepositorySession() == null) {
            throw new IllegalArgumentException("The resource must provide a non-null session object in order to create the jcr:nodetypes report.");
        }
        repository = session.getRepositorySession().getRepository();
    }

    /**
     * @param info
     * @throws IllegalArgumentException if the specified info does not contain
     * a jcr:nodetypes element.
     * @see org.apache.jackrabbit.webdav.version.report.Report#setInfo(org.apache.jackrabbit.webdav.version.report.ReportInfo)
     */
    public void setInfo(ReportInfo info) {
        if (info == null || !"repositorydescriptors".equals(info.getReportElement().getName())) {
            throw new IllegalArgumentException("jcr:repositorydescriptors element expected.");
        }
    }

    /**
     * Returns a Xml representation of the node type definition(s) according
     * to the info object.
     *
     * @return Xml representation of the node type definition(s)
     * @throws org.apache.jackrabbit.webdav.DavException if the specified nodetypes are not known or if another
     * error occurs while retrieving the nodetype definitions.
     * @see org.apache.jackrabbit.webdav.version.report.Report#toXml()
     */
    public Document toXml() throws DavException {
        String[] keys = repository.getDescriptorKeys();
        List descList = new ArrayList();
        for (int i = 0; i < keys.length; i++) {
            Element elem = new Element(XML_DESCRIPTOR, NAMESPACE);
            elem.addContent(new Element(XML_DESCRIPTORKEY, NAMESPACE).setText(keys[i]));
            elem.addContent(new Element(XML_DESCRIPTORVALUE, NAMESPACE).setText(repository.getDescriptor(keys[i])));
            descList.add(elem);
        }
        Element report = new Element("repositorydescriptors-report", NAMESPACE).addContent(descList);
        Document reportDoc = new Document(report);
        return reportDoc;
    }
}