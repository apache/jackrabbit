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
import org.jdom.Element;
import org.jdom.Namespace;

import java.util.List;

/**
 * <code>OptionsInfo</code> represents the Xml request body, that may be present
 * with a OPTIONS request.
 * <br>
 * The DAV:options element is specified to have the following form.
 *
 * <pre>
 * &lt;!ELEMENT options ANY&gt;
 * ANY value: A sequence of elements each at most onces.
 * </pre>
 *
 * @see DeltaVConstants#XML_VH_COLLECTION_SET
 * @see DeltaVConstants#XML_WSP_COLLECTION_SET
 * @see DeltaVConstants#XML_ACTIVITY_COLLECTION_SET
 */
public class OptionsInfo {

    private static Logger log = Logger.getLogger(OptionsInfo.class);

    private final Element optionsElement;

    /**
     * Create a new <code>UpdateInfo</code> object.
     *
     * @param optionsElement
     * @throws IllegalArgumentException if the updateElement is <code>null</code>
     * or not a DAV:update element or if the element does not match the required
     * structure.
     */
    public OptionsInfo(Element optionsElement) {
         if (optionsElement == null || !optionsElement.getName().equals(DeltaVConstants.XML_OPTIONS)) {
            throw new IllegalArgumentException("DAV:options element expected");
        }
        this.optionsElement = (Element) optionsElement.detach();
    }

    /**
     * Returns the set of elements present in the {@link DeltaVConstants#XML_OPTIONS DAV:options}
     * element. These elements define the information the client wishes to retrieve
     * the OPTIONS request.
     *
     * @return set of child elements
     */
    public List getElements() {
        return optionsElement.getChildren();
    }

    /**
     * Returns true if a child element with the given name and namespace is present.
     *
     * @param name
     * @param namespace
     * @return true if such a child element exists in the options element.
     */
    public boolean containsElement(String name, Namespace namespace) {
        return optionsElement.getChild(name, namespace) != null;
    }
}