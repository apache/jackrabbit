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
package org.apache.jackrabbit.webdav.ordering;

import org.apache.log4j.Logger;
import org.jdom.Element;

import java.util.HashMap;

/**
 * <code>Position</code> encapsulates the position in ordering information
 * contained in a Webdav request. This includes both the
 * {@link OrderingConstants#HEADER_POSITION Position header} and the position
 * Xml element present in the request body of an ORDERPATCH request.
 *
 * @see OrderingConstants#HEADER_POSITION
 * @see OrderingConstants#XML_POSITION
 * @see OrderPatch
 */
public class Position implements OrderingConstants {

    private static Logger log = Logger.getLogger(Position.class);

    public static final int TYPE_FIRST = 1;
    public static final int TYPE_LAST = 2;
    public static final int TYPE_BEFORE = 4;
    public static final int TYPE_AFTER = 8;

    private static final HashMap xmlTypeMap = new HashMap(4);
    static {
        xmlTypeMap.put(XML_FIRST, new Integer(TYPE_FIRST));
        xmlTypeMap.put(XML_LAST, new Integer(TYPE_LAST));
        xmlTypeMap.put(XML_BEFORE, new Integer(TYPE_BEFORE));
        xmlTypeMap.put(XML_AFTER, new Integer(TYPE_AFTER));
    }

    private int type;
    private String segment;

    /**
     * Create a new <code>Position</code> object with the specified type.
     * Since any type except for {@link #XML_FIRST first} and {@link #XML_LAST last}
     * must be combined with a segment, only the mentioned types are valid
     * arguments.
     *
     * @param type {@link #XML_FIRST first} or {@link #XML_LAST last}
     * @throws IllegalArgumentException if the given type is other than {@link #XML_FIRST}
     * or {@link #XML_LAST}.
     */
    public Position(String type) {
        if (!(XML_FIRST.equals(type) || XML_LAST.equals(type))) {
            throw new IllegalArgumentException("If type is other than 'first' or 'last' a segment must be specified");
        }
        setType(type);
    }

    /**
     * Create a new <code>Position</code> object from the specified position
     * element. The element must fulfill the following structure:<br>
     * <pre>
     * &lt;!ELEMENT position (first | last | before | after) &gt;
     * &lt;!ELEMENT segment (#PCDATA) &gt;
     * &lt;!ELEMENT first EMPTY &gt;
     * &lt;!ELEMENT last EMPTY &gt;
     * &lt;!ELEMENT before segment &gt;
     * &lt;!ELEMENT after segment &gt;
     * </pre>
     *
     * @param position Xml element defining the position.
     * @throws IllegalArgumentException if the given Xml element is not valid.
     */
    public Position(Element position) {
        if (position.getChildren().size() != 1) {
            throw new IllegalArgumentException("The 'position' element must contain exactly a single child indicating the type.");
        }
        Element typeElem = (Element)position.getChildren().get(0);
        String type = typeElem.getName();
        String segmentText = null;
        if (typeElem.getChildren().size() > 0) {
            segmentText = typeElem.getChildText(XML_SEGMENT);
        }
        init(type, segmentText);
    }

    /**
     * Create a new <code>Position</code> object with the specified type and
     * segment.
     *
     * @param type
     * @param segment
     * @throws IllegalArgumentException if the specified type and segment do not
     * form a valid pair.
     */
    public Position(String type, String segment) {
        init(type, segment);
    }

    /**
     * Initialize the internal fields.
     *
     * @param type
     * @param segment
     */
    private void init(String type, String segment) {
        if ((XML_AFTER.equals(type) || XML_BEFORE.equals(type)) && (segment == null || "".equals(segment))) {
            throw new IllegalArgumentException("If type is other than 'first' or 'last' a segment must be specified");
        }
        setType(type);
        this.segment = segment;
    }

    /**
     * Return the type of this <code>Position</code> object, which may be any
     * of the following valid types: {@link #XML_FIRST first},
     * {@link #XML_LAST last}, {@link #XML_AFTER after}, {@link #XML_BEFORE before}
     *
     * @return type
     */
    public int getType() {
        return type;
    }

    /**
     * Set the type.
     *
     * @param xmlType
     */
    private void setType(String xmlType) {
        type = ((Integer)xmlTypeMap.get(xmlType)).intValue();
    }

    /**
     * Returns the segment used to create this <code>Position</code> object or
     * <code>null</code> if no segment is present with the type.
     *
     * @return segment or <code>null</code>
     * @see #getType()
     */
    public String getSegment() {
        return segment;
    }
}