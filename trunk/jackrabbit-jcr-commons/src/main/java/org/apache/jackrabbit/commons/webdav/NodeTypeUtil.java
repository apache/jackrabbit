/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.commons.webdav;

import org.apache.jackrabbit.util.XMLUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * <code>NodeTypeUtil</code>...
 */
public class NodeTypeUtil implements NodeTypeConstants {

    public static Element ntNameToXml(String nodeTypeName, Document document) {
        Element ntElem = document.createElementNS(JcrRemotingConstants.NS_URI, JcrRemotingConstants.NS_PREFIX + ":" + XML_NODETYPE);
        Element nameElem = document.createElementNS(JcrRemotingConstants.NS_URI, JcrRemotingConstants.NS_PREFIX + ":" + XML_NODETYPENAME);
        Text txt = document.createTextNode(nodeTypeName);
        nameElem.appendChild(txt);
        ntElem.appendChild(nameElem);
        return ntElem;
    }

    public static Collection<String> ntNamesFromXml(Object propValue) {
        // assume property has be built from xml
        if (propValue instanceof List) {
            return retrieveNodeTypeNames(((List<?>)propValue));
        } else if (propValue instanceof Element) {
            List<Element> l = Collections.singletonList((Element) propValue);
            return retrieveNodeTypeNames(l);
        } else {
            // Property value cannot be parsed into node type names.
            return Collections.emptySet();
        }
    }

    private static Set<String> retrieveNodeTypeNames(List<?> elementList) {
        Set<String> nodetypeNames = new HashSet<String>();
        for (Object content : elementList) {
            if (!(content instanceof Element)) {
                continue;
            }
            Element el = (Element) content;
            if (XML_NODETYPE.equals(el.getLocalName()) && JcrRemotingConstants.NS_URI.equals(el.getNamespaceURI())) {
                String nodetypeName = XMLUtil.getChildText(el, XML_NODETYPENAME, JcrRemotingConstants.NS_URI);
                if (nodetypeName != null && !"".equals(nodetypeName)) {
                    nodetypeNames.add(nodetypeName);
                }
            } // else: 'dcr:nodetype' element expected -> ignoring element
        }
        return nodetypeNames;
    }
}