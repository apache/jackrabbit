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
import org.apache.jackrabbit.webdav.property.DavPropertyNameSet;
import org.apache.jackrabbit.webdav.DavConstants;
import org.jdom.Element;

import java.util.List;
import java.util.Iterator;

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
public class UpdateInfo implements DeltaVConstants {

    private static Logger log = Logger.getLogger(UpdateInfo.class);

    private final Element updateElement;
    private String[] versionHref;
    private String[] labelName;
    private String workspaceHref;

    /**
     * Create a new <code>UpdateInfo</code> object.
     *
     * @param updateElement
     * @throws IllegalArgumentException if the updateElement is <code>null</code>
     * or not a DAV:update element or if the element does not match the required
     * structure.
     */
    public UpdateInfo(Element updateElement) {
         if (updateElement == null || !updateElement.getName().equals(DeltaVConstants.XML_UPDATE)) {
            throw new IllegalArgumentException("DAV:update element expected");
        }

        List targetList;
        if (!(targetList = updateElement.getChildren(XML_VERSION, NAMESPACE)).isEmpty()) {
            Iterator it = targetList.iterator();
            versionHref = new String[targetList.size()];
            int i = 0;
            while (it.hasNext()) {
                Element versionElem = (Element) it.next();
                versionHref[i] = versionElem.getChildText(DavConstants.XML_HREF, NAMESPACE);
                i++;
            }
        } else if (!(targetList = updateElement.getChildren(XML_LABEL_NAME, NAMESPACE)).isEmpty()) {
            Iterator it = targetList.iterator();
            labelName = new String[targetList.size()];
            int i = 0;
            while (it.hasNext()) {
                Element labelNameElem = (Element) it.next();
                labelName[i] = labelNameElem.getText();
                i++;
            }
        } else if (updateElement.getChild(XML_WORKSPACE, NAMESPACE) != null) {
            workspaceHref = updateElement.getChild(XML_WORKSPACE, NAMESPACE).getChildText(DavConstants.XML_HREF, NAMESPACE);
        } else {
            throw new IllegalArgumentException("DAV:update element must contain either DAV:version, DAV:label-name or DAV:workspace child element.");
        }

        this.updateElement = (Element) updateElement.detach();
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
     * in the set. Otherwise an empty set is returned.
     *
     * @return set listing the properties specified in the DAV:prop element indicating
     * those properties that must be reported in the response body.
     */
    public DavPropertyNameSet getPropertyNameSet() {
        Element propElement = updateElement.getChild(DavConstants.XML_PROP, DavConstants.NAMESPACE);
        if (propElement != null) {
            return new DavPropertyNameSet(propElement);
        } else {
            return new DavPropertyNameSet();
        }
    }

    /**
     *
     * @return
     */
    public Element getUpdateElement() {
        return updateElement;
    }
}