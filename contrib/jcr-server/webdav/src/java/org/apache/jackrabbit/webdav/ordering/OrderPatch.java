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
import org.apache.jackrabbit.webdav.DavConstants;
import org.jdom.Element;

import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

/**
 * <code>OrderPatch</code> represents the mandatory request body of an
 * ORDERPATCH request. RFC 3648 defines the following structure for it:<br>
 * <pre>
 * &lt;!ELEMENT orderpatch (ordering-type?, order-member*) &gt;
 * &lt;!ELEMENT order-member (segment, position) &gt;
 * &lt;!ELEMENT position (first | last | before | after) &gt;
 * &lt;!ELEMENT segment (#PCDATA) &gt;
 * &lt;!ELEMENT first EMPTY &gt;
 * &lt;!ELEMENT last EMPTY &gt;
 * &lt;!ELEMENT before segment &gt;
 * &lt;!ELEMENT after segment &gt;
 * </pre>
 */
public class OrderPatch implements OrderingConstants{

    private static Logger log = Logger.getLogger(OrderPatch.class);

    private Member[] instructions;
    private String orderingType;

    /**
     * Create a new <code>OrderPath</code> object.
     *
     * @param orderPatchElement
     * @throws IllegalArgumentException if the specified Xml element was not valid.
     */
    public OrderPatch(Element orderPatchElement) {
        if (!OrderingConstants.XML_ORDERPATCH.equals(orderPatchElement.getName()) ||
                orderPatchElement.getChild(OrderingConstants.XML_ORDERING_TYPE) == null) {
            throw new IllegalArgumentException("ORDERPATH request body must start with an 'orderpatch' element, which must contain an 'ordering-type' child element.");
        }
        // retrieve the orderingtype element
        orderingType = orderPatchElement.getChild(OrderingConstants.XML_ORDERING_TYPE).getChildText(DavConstants.XML_HREF);

        // set build the list of ordering instructions
        List oMembers = orderPatchElement.getChildren(OrderingConstants.XML_ORDER_MEMBER, DavConstants.NAMESPACE);
        Iterator it = oMembers.iterator();
        int cnt = 0;
        List tmpInst = new ArrayList();
        while (it.hasNext()) {
            Element member = (Element) it.next();
            try {
                String segment = member.getChildText(OrderingConstants.XML_SEGMENT);
                Position pos = new Position(member.getChild(OrderingConstants.XML_POSITION));
                Member om = new Member(segment, pos);
                tmpInst.add(om);
                cnt++;
            } catch (IllegalArgumentException e) {
                log.error("Invalid element in 'orderpatch' request body: " + e.getMessage());
            }
        }
        instructions = (Member[]) tmpInst.toArray(new Member[cnt]);
    }

    /**
     * Create a new <code>OrderPath</code> object.
     *
     * @param orderingType
     * @param instructions
     */
    public OrderPatch(String orderingType, Member[] instructions) {
        this.orderingType = orderingType;
        this.instructions = instructions;
    }

    /**
     * Return the ordering type.
     *
     * @return ordering type
     */
    public String getOrderingType() {
        return orderingType;
    }

    /**
     * Return an array of {@link Member} objects defining the re-ordering
     * instructions to be applied to the requested resource.
     *
     * @return ordering instructions.
     */
    public Member[] getOrderInstructions() {
        return instructions;
    }

    //--------------------------------------------------------------------------
    /**
     * Internal class <code>Member</code> represents the 'Order-Member' children
     * elements of an 'OrderPatch' request body present in the ORDERPATCH request.
     */
    public class Member {

        private String memberHandle;
        private Position position;

        /**
         * Create a new <code>Member</code> object.
         *
         * @param memberHandle
         * @param position
         */
        public Member(String memberHandle, Position position) {
            this.memberHandle = memberHandle;
            this.position = position;
        }

        /**
         * Return the handle of the internal member to be reordered.
         *
         * @return handle of the internal member.
         */
        public String getMemberHandle() {
            return memberHandle;
        }

        /**
         * Return the position where the internal member identified by the
         * member handle should be placed.
         *
         * @return position for the member after the request.
         * @see #getMemberHandle()
         */
        public Position getPosition() {
            return position;
        }
    }
}