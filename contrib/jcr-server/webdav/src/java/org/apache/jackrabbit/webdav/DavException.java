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
package org.apache.jackrabbit.webdav;

import org.apache.log4j.Logger;
import org.jdom.Element;

import java.util.Properties;
import java.io.IOException;

/**
 * <code>DavException</code> extends the {@link Exception} class in order
 * to simplify handling of exceptional situations occuring during processing
 * of WebDAV requests and provides possibility to retrieve an Xml representation
 * of the error.
 */
public class DavException extends Exception {

    private static Logger log = Logger.getLogger(DavException.class);
    private static Properties statusPhrases = new Properties();
    static {
        try {
            statusPhrases.load(DavException.class.getResourceAsStream("statuscode.properties"));
        } catch (IOException e) {
            log.error("Failed to load status properties: "+ e.getMessage());
        }
    }

    private static final String XML_ERROR = "error";

    private int errorCode = DavServletResponse.SC_INTERNAL_SERVER_ERROR;
    private Element conditionElement;

    /**
     * Create a new <code>DavException</code>.
     *
     * @param errorCode integer specifying any of the status codes defined by
     * {@link DavServletResponse}.
     * @param message Human readable error message.
     */
    public DavException(int errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        log.debug("DavException: (" + errorCode + ") " + message);
    }

    /**
     * Create a new <code>DavException</code>.
     *
     * @param errorCode integer specifying any of the status codes defined by
     * {@link DavServletResponse}.
     */
    public DavException(int errorCode) {
        this(errorCode, statusPhrases.getProperty(String.valueOf(errorCode)));
    }

    /**
     * Create a new <code>DavException</code>.
     *
     * @param errorCode integer specifying any of the status codes defined by
     * {@link DavServletResponse}.
     * @param message
     * @param conditionElement
     */
    public DavException(int errorCode, String message, Element conditionElement) {
        this(errorCode, message);
        this.conditionElement = conditionElement;
        log.debug("DavException: (" + errorCode + ") " + conditionElement.toString());
    }

    /**
     * Return the error code attached to this <code>DavException</code>.
     *
     * @return errorCode
     */
    public int getErrorCode() {
        return errorCode;
    }

    /**
     * Returns the Xml representation of this <code>DavException</code>. In case
     * no {@link Element} has been passed to the constructor, an empty DAV:error
     * element is returned.
     *
     * @return Xml representation of this exception.
     */
    public Element getError() {
        Element error = new Element(XML_ERROR, DavConstants.NAMESPACE);
        if (conditionElement != null) {
            error.addContent(conditionElement);
        }
        return error;
    }

    /**
     * Return the status phrase corresponding to the error code attached to
     * this <code>DavException</code>.
     *
     * @return status phrase corresponding to the error code.
     * @see #getErrorCode()
     */
    public String getStatusPhrase() {
        return getStatusPhrase(errorCode);
    }

    /**
     * Returns the status phrase for the given error code.
     *
     * @param errorCode
     * @return status phrase corresponding to the given error code.
     */
    public static String getStatusPhrase(int errorCode) {
        return statusPhrases.getProperty(errorCode+"");
    }
}