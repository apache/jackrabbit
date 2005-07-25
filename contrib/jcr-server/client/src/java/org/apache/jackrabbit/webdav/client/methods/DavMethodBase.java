/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
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
package org.apache.jackrabbit.webdav.client.methods;

import org.apache.log4j.Logger;
import org.apache.commons.httpclient.methods.EntityEnclosingMethod;
import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.MultiStatus;
import org.jdom.input.SAXBuilder;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;
import org.xml.sax.ContentHandler;

import java.io.InputStream;
import java.io.IOException;

/**
 * <code>DavMethodBase</code>...
 */
public abstract class DavMethodBase extends EntityEnclosingMethod implements DavConstants {

    private static Logger log = Logger.getLogger(DavMethodBase.class);

    public DavMethodBase(String uri) {
	super(uri);
    }

    public void setRequestBody(Document bodyDocument) {
	String reqBody = new XMLOutputter(Format.getRawFormat()).outputString(bodyDocument);
	setRequestBody(reqBody);
    }

    public Document getReponseBodyAsDocument() throws IOException, JDOMException {
	SAXBuilder builder = new SAXBuilder();
	return builder.build(getResponseBodyAsStream());
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
	InputStream in = this.getResponseBodyAsStream();
	if (in != null) {
	    try {
		SAXBuilder builder = new SAXBuilder(false);
		Document responseDocument = builder.build(in);
		return new DavException(getStatusCode(), getStatusText(), responseDocument.getRootElement());
	    } catch (JDOMException e) {
		log.error(e.getMessage());
	    }
	}
	// no or unparsable response body
	return new DavException(getStatusCode(), getStatusText());
    }

    public void parseResponse(ContentHandler contentHandler) throws IOException, DavException {
        // todo
    }

    public MultiStatus getResponseBodyAsMultiStatus() throws IOException, DavException {
        checkUsed();
        if (getStatusCode() == DavServletResponse.SC_MULTI_STATUS) {
            try {
                return MultiStatus.createFromXml(getReponseBodyAsDocument());
            } catch (JDOMException e) {
                log.error(e.getMessage());
                return null;
            }
        } else {
            throw new DavException(getStatusCode(), getName() + " resulted with unexpected status code: " + getStatusCode());
        }
    }

    public void parseMultiStatus(ContentHandler contentHandler) throws IOException, DavException {
        checkUsed();
        if (getStatusCode() == DavServletResponse.SC_MULTI_STATUS) {
            /// todo...
        } else {
            throw new DavException(getStatusCode(), getName() + " resulted with unexpected status code: " + getStatusCode());
        }
    }
}
