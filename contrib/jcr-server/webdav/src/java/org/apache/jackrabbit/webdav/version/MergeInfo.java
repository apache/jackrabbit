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
import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.property.DavPropertyNameSet;
import org.jdom.Element;

/**
 * <code>MergeInfo</code> encapsulates the information present in the DAV:merge
 * element, that forms the mandatory request body of a MERGE request.<br>
 * The DAV:merge element is specified to have the following form.
 * <pre>
 * &lt;!ELEMENT merge ANY&gt;
 * ANY value: A sequence of elements with one DAV:source element, at most one
 * DAV:no-auto-merge element, at most one DAV:no-checkout element, at most one
 * DAV:prop element, and any legal set of elements that can occur in a DAV:checkout
 * element.
 * &lt;!ELEMENT source (href+)&gt;
 * &lt;!ELEMENT no-auto-merge EMPTY&gt;
 * &lt;!ELEMENT no-checkout EMPTY&gt;
 * prop: see <a href="http://www.ietf.org/rfc/rfc2518.txt">RFC 2518, Section 12.11</a>
 * </pre>
 */
public class MergeInfo implements DeltaVConstants {

    private static Logger log = Logger.getLogger(MergeInfo.class);

    private Element mergeElement;

    /**
     * Create a new <code>MergeInfo</code>
     *
     * @param mergeElement
     * @throws IllegalArgumentException if the mergeElement is <code>null</code>
     * or not a DAV:merge element.
     */
    public MergeInfo(Element mergeElement) {
        if (mergeElement == null || !mergeElement.getName().equals(XML_MERGE)) {
            throw new IllegalArgumentException("'DAV:merge' element expected");
        }
        this.mergeElement = (Element) mergeElement.detach();
    }

    /**
     * Returns the URL specified with the DAV:source element or <code>null</code>
     * if no such child element is present in the DAV:merge element.
     *
     * @return href present in the DAV:source child element or <code>null</code>.
     */
    public String getSourceHref() {
        Element source = mergeElement.getChild(DavConstants.XML_SOURCE, DavConstants.NAMESPACE);
        if (source != null) {
            return source.getChildText(DavConstants.XML_HREF, DavConstants.NAMESPACE);
        }
        return null;
    }

    /**
     * Returns true if the DAV:merge element contains a DAV:no-auto-merge child element.
     *
     * @return true if the DAV:merge element contains a DAV:no-auto-merge child.
     */
    public boolean isNoAutoMerge() {
        return mergeElement.getChild(XML_N0_AUTO_MERGE, NAMESPACE) != null;
    }

    /**
     * Returns true if the DAV:merge element contains a DAV:no-checkout child element.
     *
     * @return true if the DAV:merge element contains a DAV:no-checkout child
     */
    public boolean isNoCheckout() {
        return mergeElement.getChild(XML_N0_CHECKOUT, NAMESPACE) != null;
    }

    /**
     * Returns a {@link DavPropertyNameSet}. If the DAV:merge element contains
     * a DAV:prop child element the properties specified therein are included
     * in the set. Otherwise an empty set is returned.
     *
     * @return set listing the properties specified in the DAV:prop element indicating
     * those properties that must be reported in the response body.
     */
    public DavPropertyNameSet getPropertyNameSet() {
        Element propElement = mergeElement.getChild(DavConstants.XML_PROP, DavConstants.NAMESPACE);
        if (propElement != null) {
            return new DavPropertyNameSet(propElement);
        } else {
            return new DavPropertyNameSet();
        }
    }

    /**
     * Returns the DAV:merge element used to create this <code>MergeInfo</code>
     * object.
     *
     * @return DAV:merge element
     */
    public Element getMergeElement() {
        return mergeElement;
    }
}