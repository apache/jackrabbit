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

import java.util.Iterator;

/**
 * <code>LabelInfo</code> encapsulates the request body of a LABEL request
 * used to add, set or remove a label from the requested version resource or
 * from that version specified with the Label header in case the requested resource
 * is a version-controlled resource.<br><br>
 * The request body (thus the 'labelElement' passed to the constructore must be
 * a DAV:label element:
 * <pre>
 * &lt;!ELEMENT label ANY&gt;
 * ANY value: A sequence of elements with at most one DAV:add,
 * DAV:set, or DAV:remove element.
 * &lt;!ELEMENT add (label-name)&gt;
 * &lt;!ELEMENT set (label-name)&gt;
 * &lt;!ELEMENT remove (label-name)&gt;
 * &lt;!ELEMENT label-name (#PCDATA)&gt;
 * PCDATA value: string
 * </pre>
 */
public class LabelInfo implements DeltaVConstants {

    private static Logger log = Logger.getLogger(LabelInfo.class);

    public static final int TYPE_SET = 0;
    public static final int TYPE_REMOVE = 1;
    public static final int TYPE_ADD = 2;

    private final Element labelElement;
    private final int depth;

    private int type;
    private String labelName;

    /**
     * Create a new <code>LabelInfo</code> from the given element and depth
     * integer. If the specified Xml element does have a {@link DeltaVConstants#XML_LABEL}
     * root element or no label name is specified with the action to perform
     * the creation will fail.
     *
     * @param labelElement
     * @param depth
     * @throws IllegalArgumentException if the specified element does not
     * start with a {@link DeltaVConstants#XML_LABEL} element or if the DAV:label
     * element contains illegal instructions e.g. contains multiple DAV:add, DAV:set
     * or DAV:remove elements.
     */
    public LabelInfo(Element labelElement, int depth) {
        if (labelElement == null || !labelElement.getName().equals(DeltaVConstants.XML_LABEL)) {
            throw new IllegalArgumentException("label element expected");
        }

        this.labelElement = (Element) labelElement.detach();

        Iterator childrenIter = labelElement.getChildren().iterator();
        while (childrenIter.hasNext()) {
            Element child = (Element) childrenIter.next();
            if (!NAMESPACE.equals(child.getNamespace())) {
                continue;
            }
            String name = child.getName();
            if (XML_LABEL_ADD.equals(name)) {
                type = TYPE_ADD;
                setLabelName(child);
            } else if (XML_LABEL_REMOVE.equals(name)) {
                type = TYPE_REMOVE;
                setLabelName(child);
            } else if (XML_LABEL_SET.equals(name)) {
                type = TYPE_SET;
                setLabelName(child);
            }
        }
        this.depth = depth;
    }

    /**
     * Create a new <code>LabelInfo</code> from the given element. As depth
     * the default value 0 is assumed.
     *
     * @param labelElement
     * @throws IllegalArgumentException
     * @see #LabelInfo(org.jdom.Element, int)
     */
    public LabelInfo(Element labelElement) {
        this(labelElement, 0);
    }

    /**
     * Return the 'label-name' or <code>null</code>
     *
     * @return 'label-name' or <code>null</code>
     */
    public String getLabelName() {
        return labelName;
    }

    /**
     * Retrieve the text of the 'label-name' child element of the specified
     * parent element.
     *
     * @param parent the is intended to contain a valid 'label-name' child.
     * @throws IllegalArgumentException if the labelName has been set before.
     */
    private void setLabelName(Element parent) {
        // test if any label name is present
        if (labelName != null) {
            throw new IllegalArgumentException("The DAV:label element may contain at most one DAV:add, DAV:set, or DAV:remove element");
        }
        labelName = parent.getChildText(XML_LABEL_NAME, NAMESPACE);
    }

    /**
     * Return the type of the LABEL request. This might either be {@link #TYPE_SET},
     * {@link #TYPE_ADD} or {@link #TYPE_REMOVE}.
     *
     * @return type
     */
    public int getType() {
        return type;
    }

    /**
     * Return the depth
     *
     * @return depth
     */
    public int getDepth() {
        return depth;
    }

    /**
     * Return the DAV:label element
     *
     * @return the DAV:label element
     */
    public Element getLabelElement() {
        return labelElement;
    }
}