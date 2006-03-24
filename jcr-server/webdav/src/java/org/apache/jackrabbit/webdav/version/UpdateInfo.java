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
package org.apache.jackrabbit.webdav.version;

import org.apache.log4j.Logger;
import org.apache.jackrabbit.webdav.property.DavPropertyNameSet;
import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.xml.XmlSerializable;
import org.apache.jackrabbit.webdav.xml.DomUtil;
import org.apache.jackrabbit.webdav.xml.ElementIterator;
import org.w3c.dom.Element;
import org.w3c.dom.Document;

import java.util.List;
import java.util.ArrayList;

/**
 * <code>UpdateInfo</code> encapsulates the request body of an UPDATE request.
 * RFC 3253 defines the request body as follows:
 * <pre>
 * &lt;!ELEMENT update ANY&gt;
 * ANY value: A sequence of elements with at most one DAV:version element and at
 * most one DAV:prop element.
 * &lt;!ELEMENT version (href)&gt;
 * prop: see RFC 2518, Section 12.11
 * </pre>
 *
 * In order to reflect the complete range of version restoring and updating
 * of nodes defined by JSR170 the definition has been extended:
 * <pre>
 * &lt;!ELEMENT update ( (version+ | label-name | workspace ) , (prop)?, (removeExisting)? ) &gt;
 * &lt;!ELEMENT version (href) &gt;
 * &lt;!ELEMENT label-name (#PCDATA) &gt;
 * &lt;!ELEMENT workspace (href) &gt;
 * &lt;!ELEMENT prop ANY &gt;
 * &lt;!ELEMENT removeExisting EMPTY &gt;
 * </pre>
 */
public class UpdateInfo implements DeltaVConstants, XmlSerializable {

    private static Logger log = Logger.getLogger(UpdateInfo.class);

    private final Element updateElement;
    private final DavPropertyNameSet propertyNameSet;
    private String[] versionHref;
    private String[] labelName;
    private String workspaceHref;

    /**
     * Create a new <code>UpdateInfo</code> object.
     *
     * @param updateElement
     * @throws DavException if the updateElement is <code>null</code>
     * or not a DAV:update element or if the element does not match the required
     * structure.
     */
    public UpdateInfo(Element updateElement) throws DavException {
        if (!DomUtil.matches(updateElement, XML_UPDATE, NAMESPACE)) {
            log.warn("DAV:update element expected");
            throw new DavException(DavServletResponse.SC_BAD_REQUEST);
        }

        boolean done = false;
        ElementIterator it = DomUtil.getChildren(updateElement, XML_VERSION, NAMESPACE);
            while (it.hasNext()) {
            List hrefList = new ArrayList();
            Element el = it.nextElement();
            hrefList.add(DomUtil.getChildText(el, DavConstants.XML_HREF, DavConstants.NAMESPACE));
            versionHref = (String[])hrefList.toArray(new String[hrefList.size()]);
            done = true;
        }

        // alternatively 'DAV:label-name' elements may be present.
        if (!done) {
            it = DomUtil.getChildren(updateElement, XML_LABEL_NAME, NAMESPACE);
            while (it.hasNext()) {
                List labelList = new ArrayList();
                Element el = it.nextElement();
                labelList.add(DomUtil.getText(el));
                labelName = (String[])labelList.toArray(new String[labelList.size()]);
                done = true;
            }
        }

        // last possibility: a DAV:workspace element
        if (!done) {
            Element wspElem = DomUtil.getChildElement(updateElement, XML_WORKSPACE, NAMESPACE);
            if (wspElem != null) {
                workspaceHref = DomUtil.getChildTextTrim(wspElem, DavConstants.XML_HREF, DavConstants.NAMESPACE);
        } else {
                log.warn("DAV:update element must contain either DAV:version, DAV:label-name or DAV:workspace child element.");
                throw new DavException(DavServletResponse.SC_BAD_REQUEST);
        }
        }

        // if property name set if present
        if (DomUtil.hasChildElement(updateElement, DavConstants.XML_PROP, DavConstants.NAMESPACE)) {
            Element propEl = DomUtil.getChildElement(updateElement, DavConstants.XML_PROP, DavConstants.NAMESPACE);
            propertyNameSet = new DavPropertyNameSet(propEl);
            updateElement.removeChild(propEl);
        } else {
            propertyNameSet = new DavPropertyNameSet();
        }
        this.updateElement = updateElement;
    }

    /**
     *
     * @return
     */
    public String[] getVersionHref() {
       return versionHref;
    }

    /**
     *
     * @return
     */
    public String[] getLabelName() {
       return labelName;
    }

    /**
     *
     * @return
     */
    public String getWorkspaceHref() {
       return workspaceHref;
    }

    /**
     * Returns a {@link DavPropertyNameSet}. If the DAV:update element contains
     * a DAV:prop child element the properties specified therein are included
     * in the set. Otherwise an empty set is returned.<p/>
     *
     * <b>WARNING:</b> modifying the DavPropertyNameSet returned by this method does
     * not modify this <code>UpdateInfo</code>.
     *
     * @return set listing the properties specified in the DAV:prop element indicating
     * those properties that must be reported in the response body.
     */
    public DavPropertyNameSet getPropertyNameSet() {
        return propertyNameSet;
    }

    /**
     *
     * @return
     */
    public Element getUpdateElement() {
        return updateElement;
    }

    /**
     * @see org.apache.jackrabbit.webdav.xml.XmlSerializable#toXml(Document)
     * @param document
     */
    public Element toXml(Document document) {
        Element elem = (Element)document.importNode(updateElement, true);
        if (!propertyNameSet.isEmpty()) {
            elem.appendChild(propertyNameSet.toXml(document));
        }
        return elem;
    }

}