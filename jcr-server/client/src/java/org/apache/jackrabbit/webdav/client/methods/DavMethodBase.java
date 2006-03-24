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
package org.apache.jackrabbit.webdav.client.methods;

import org.apache.log4j.Logger;
import org.apache.commons.httpclient.methods.EntityEnclosingMethod;
import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.MultiStatus;
import org.apache.jackrabbit.webdav.xml.XmlSerializable;
import org.apache.jackrabbit.webdav.header.Header;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.xml.sax.SAXException;
import org.w3c.dom.Element;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.InputStream;
import java.io.IOException;
import java.io.ByteArrayOutputStream;

/**
 * <code>DavMethodBase</code>...
 */
public abstract class DavMethodBase extends EntityEnclosingMethod implements DavConstants {

    private static Logger log = Logger.getLogger(DavMethodBase.class);
    static final DocumentBuilderFactory BUILDER_FACTORY = DocumentBuilderFactory.newInstance();
    static {
       BUILDER_FACTORY.setNamespaceAware(true);
    }

    public DavMethodBase(String uri) {
	super(uri);
    }

    /**
     *
     * @param header
     */
    public void setRequestHeader(Header header) {
        setRequestHeader(header.getHeaderName(), header.getHeaderValue());
    }

    /**
     *
     * @param requestBody
     * @throws IOException
     */
    public void setRequestBody(XmlSerializable requestBody) throws IOException {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document doc = BUILDER_FACTORY.newDocumentBuilder().newDocument();
            doc.appendChild(requestBody.toXml(doc));

            OutputFormat format = new OutputFormat("xml", "UTF-8", true);
            XMLSerializer serializer = new XMLSerializer(out, format);
            serializer.setNamespaces(true);
            serializer.asDOMSerializer().serialize(doc);
            setRequestBody(out.toString());
        } catch (ParserConfigurationException e) {
            throw new IOException(e.getMessage());
        }
    }

    /**
     * Return the response body as <code>MultiStatus</code> object.
     *
     * @return
     * @throws IOException if the response body could not be parsed
     * @throws DavException if the status code is other than MultiStatus
     */
    public MultiStatus getResponseBodyAsMultiStatus() throws IOException, DavException {
	checkUsed();
        if (getStatusCode() == DavServletResponse.SC_MULTI_STATUS) {
            return MultiStatus.createFromXml(getRootElement());
        } else {
            throw new DavException(getStatusCode(), getName() + " resulted with unexpected status code: " + getStatusCode());
        }
    }

    /**
     * Parse the response body into an Xml <code>Document</code>.
     *
     * @return Xml document or <code>null</code> if the response stream is
     * <code>null</code>.
     * @throws IOException if the parsing fails.
     */
    public Document getResponseBodyAsDocument() throws IOException {
        InputStream in = getResponseBodyAsStream();
        if (in == null) {
	    return null;
	}
	    try {
            DocumentBuilder docBuilder = BUILDER_FACTORY.newDocumentBuilder();
            Document document = docBuilder.parse(in);
            return document;
        } catch (ParserConfigurationException e) {
            throw new IOException(e.getMessage());
        } catch (SAXException e) {
            throw new IOException(e.getMessage());
	    }
	}

    /**
     * 
     * @return
     * @throws IOException
     */
    Element getRootElement() throws IOException {
        Document document = getResponseBodyAsDocument();
        if (document != null) {
            return document.getDocumentElement();
    }
        return null;
    }

    /**
     *
     * @return
     * @throws IOException
     */
    public DavException getResponseException() throws IOException {
        checkUsed();
	if (getStatusCode() < DavServletResponse.SC_BAD_REQUEST) {
	    log.warn("Cannot retrieve exception from successful response.");
                return null;
            }

        // todo: build exception from response body if present.

	// fallback: no or unparsable response body
	return new DavException(getStatusCode(), getStatusText());
    }
}