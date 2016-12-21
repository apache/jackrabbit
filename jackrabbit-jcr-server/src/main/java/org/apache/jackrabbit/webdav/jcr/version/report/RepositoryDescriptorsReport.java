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

import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.jcr.ItemResourceConstants;
import org.apache.jackrabbit.webdav.version.report.Report;
import org.apache.jackrabbit.webdav.version.report.ReportInfo;
import org.apache.jackrabbit.webdav.version.report.ReportType;
import org.apache.jackrabbit.webdav.xml.DomUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.jcr.Repository;
import javax.jcr.Value;
import javax.jcr.RepositoryException;
import javax.jcr.PropertyType;

/**
 * <code>RepositoryDescriptorsReport</code> allows to retrieve the repository
 * descriptors. The request body must be an empty 'dcr:repositorydescriptors' element:
 * <pre>
 * &lt;!ELEMENT repositorydescriptors EMPTY &gt;
 * </pre>
 * <br>
 * The response body must match the following format
 *
 * <pre>
 * &lt;!ELEMENT repositorydescriptors-report ( descriptor )* &gt;
 * &lt;!ELEMENT descriptor ( descriptorkey, descriptorvalue+ ) &gt;
 * &lt;!ELEMENT descriptorkey (#PCDATA) &gt;
 * &lt;!ELEMENT descriptorvalue (#PCDATA) &gt;
 * &lt;!ATTLIST descriptorvalue
 *      type ( Reference | Path | Name | Boolean | String | Date | Double |
               Long | Binary | WeakReference | URI | Decimal )
   &gt;
 * </pre>
 *
 * @see javax.jcr.Repository#getDescriptorKeys()
 * @see javax.jcr.Repository#getDescriptor(String)
 * @see javax.jcr.Repository#getDescriptorValue(String)
 * @see javax.jcr.Repository#getDescriptorValues(String) 
 */
public class RepositoryDescriptorsReport extends AbstractJcrReport implements ItemResourceConstants {

    private static Logger log = LoggerFactory.getLogger(RepositoryDescriptorsReport.class);

    /**
     * The registered type of this report.
     */
    public static final ReportType REPOSITORY_DESCRIPTORS_REPORT = ReportType.register(REPORT_REPOSITORY_DESCRIPTORS, ItemResourceConstants.NAMESPACE, RepositoryDescriptorsReport.class);

    /**
     * Returns {@link #REPOSITORY_DESCRIPTORS_REPORT} type.
     * @return {@link #REPOSITORY_DESCRIPTORS_REPORT}
     * @see org.apache.jackrabbit.webdav.version.report.Report#getType()
     */
    public ReportType getType() {
        return REPOSITORY_DESCRIPTORS_REPORT;
    }

    /**
     * Always returns <code>false</code>.
     *
     * @return false
     * @see org.apache.jackrabbit.webdav.version.report.Report#isMultiStatusReport()
     */
    public boolean isMultiStatusReport() {
        return false;
    }

    /**
     * @see Report#init(DavResource, ReportInfo)
     */
    @Override
    public void init(DavResource resource, ReportInfo info) throws DavException {
        // delegate validation to abstract super class
        super.init(resource, info);
    }

    /**
     * Returns a Xml representation of the repository descriptors according
     * to the info object.
     *
     * @return Xml representation of the repository descriptors
     * @see org.apache.jackrabbit.webdav.xml.XmlSerializable#toXml(Document)
     * @param document
     */
    public Element toXml(Document document) {
        Repository repository = getRepositorySession().getRepository();
        Element report = DomUtil.createElement(document, "repositorydescriptors-report", NAMESPACE);
        for (String key : repository.getDescriptorKeys()) {
            Element elem = DomUtil.addChildElement(report, XML_DESCRIPTOR, NAMESPACE);
            DomUtil.addChildElement(elem, XML_DESCRIPTORKEY, NAMESPACE, key);
            for (Value v : repository.getDescriptorValues(key)) {
                String value;
                try {
                    value = v.getString();
                } catch (RepositoryException e) {
                    log.error("Internal error while reading descriptor value: ", e);
                    value = repository.getDescriptor(key);
                }
                Element child = DomUtil.addChildElement(elem, XML_DESCRIPTORVALUE, NAMESPACE, value);
                if (PropertyType.STRING != v.getType()) {
                    child.setAttribute(ATTR_VALUE_TYPE, PropertyType.nameFromValue(v.getType()));
                }
            }
        }
        return report;
    }
}
