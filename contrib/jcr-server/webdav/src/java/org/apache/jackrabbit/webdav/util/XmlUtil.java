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
package org.apache.jackrabbit.webdav.util;

import org.apache.log4j.Logger;
import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.util.Text;
import org.jdom.Element;

/**
 * <code>XmlUtil</code> provides utility METHODS for building Xml representation.
 */
public class XmlUtil implements DavConstants {

    private static Logger log = Logger.getLogger(XmlUtil.class);

    /**
     * Converts the given timeout (long value defining the number of milli-
     * second until timeout is reached) to its Xml representation as defined
     * by RTF 2518.
     *
     * @param timeout number of milli-seconds until timeout is reached.
     * @return 'timeout' JDOM element
     */
    public static Element timeoutToXml(long timeout) {
        // TODO: check if 'infinite' would be better to return for infinite timeout.
        String expString = "Second-"+ timeout/1000;
        Element exp = new Element(XML_TIMEOUT, NAMESPACE);
        exp.setText(expString);
        return exp;
    }

    /**
     * Returns the Xml representation of a boolean isDeep, where false
     * presents a depth value of '0', true a depth value of 'infinity'.
     *
     * @param isDeep
     * @return Xml representation
     */
    public static Element depthToXml(boolean isDeep) {
        return depthToXml(isDeep? "infinity" : "0");
    }

    /**
     * Returns the Xml representation of a depth String. Webdav defines the
     * following valid values for depths: 0, 1, infinity
     *
     * @param depth
     * @return 'deep' JDOM element
     */
    public static Element depthToXml(String depth) {
        Element dElem = new Element(XML_DEPTH, NAMESPACE);
        dElem.setText(depth);
        return dElem;
    }

    /**
     * Builds a 'DAV:href' Xml element from the given href. Please note, that
     * the path present in the given String should be properly
     * {@link Text#escapePath(String) escaped} in order to prevent problems with
     * WebDAV clients.
     *
     * @param href String representing the text of the 'href' Xml element
     * @return Xml representation of a 'href' according to RFC 2518.
     */
    public static Element hrefToXml(String href) {
        return new Element(XML_HREF, NAMESPACE).setText(href);
    }

    /**
     * Verifies that the given element is a DAV:href element, retrieves the
     * element text.
     *
     * @param hrefElement a DAV:href element
     * @return the URL decoded element text or empty String if the given element is empty.
     */
    public static String hrefFromXml(Element hrefElement) {
        if (hrefElement == null || !XML_HREF.equals(hrefElement.getName()) || !NAMESPACE.equals(hrefElement.getNamespace())) {
            throw new IllegalArgumentException("DAV:href element expected.");
        }
        return hrefElement.getText();
    }
}