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
package org.apache.jackrabbit.webdav.version;

import org.apache.log4j.Logger;
import org.apache.jackrabbit.webdav.util.XmlUtil;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;

/**
 * <code>OptionsResponse</code> encapsulates the DAV:options-response element
 * present in the response body of a successful OPTIONS request (with body).
 * <br>
 * The DAV:options-response element is defined to have the following format.
 *
 * <pre>
 * &lt;!ELEMENT options-response ANY&gt;
 * ANY value: A sequence of elements
 * </pre>
 */
public class OptionsResponse implements DeltaVConstants {

    private static Logger log = Logger.getLogger(OptionsResponse.class);

    private final Element optionsResponse = new Element(XML_OPTIONS_RESPONSE, NAMESPACE);

    /**
     * Add a new entry to this <code>OptionsResponse</code>
     *
     * @param elem
     */
    public void addEntry(Element elem) {
        optionsResponse.addContent(elem.detach());
    }

    /**
     * Add a new entry to this <code>OptionsResponse</code> and make each
     * href present in the String array being a separate {@link org.apache.jackrabbit.webdav.DavConstants#XML_HREF DAV:href}
     * element within the entry.
     *
     * @param name
     * @param namespace
     * @param hrefs
     */
    public void addEntry(String name, Namespace namespace, String[] hrefs) {
        Element elem = new Element(name, namespace);
        for (int i = 0; i < hrefs.length; i++) {
            elem.addContent(XmlUtil.hrefToXml(hrefs[i]));
        }
        optionsResponse.addContent(elem);
    }

    /**
     * Return the Xml representation.
     *
     * @return Xml representation.
     */
    public Document toXml() {
        return new Document(optionsResponse);
    }
}