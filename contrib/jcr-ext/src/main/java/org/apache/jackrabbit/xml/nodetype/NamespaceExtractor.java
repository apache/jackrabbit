/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.xml.nodetype;

import java.util.HashMap;
import java.util.Map;

import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.state.nodetype.NodeDefinitionState;
import org.apache.jackrabbit.state.nodetype.NodeTypeManagerState;
import org.apache.jackrabbit.state.nodetype.NodeTypeState;
import org.apache.jackrabbit.state.nodetype.PropertyDefinitionState;

class NamespaceExtractor {

    public static Map extractNamespaces(
            NodeTypeManagerState nodeTypeManagerState) {
        NamespaceExtractor extractor = new NamespaceExtractor();
        NodeTypeState[] nodeTypeStates = nodeTypeManagerState.getNodeTypeStates();
        for (int i = 0; i < nodeTypeStates.length; i++) {
            extractor.extractNamespaces(nodeTypeStates[i]);
        }
        return extractor.namespaces;
    }

    private final Map namespaces = new HashMap();

    private void extractNamespaces(NodeTypeState nodeTypeState) {
        extractNamespace(nodeTypeState.getName());
        extractNamespace(nodeTypeState.getPrimaryItemName());
        extractNamespaces(nodeTypeState.getSupertypeNames());
        NodeDefinitionState[] childNodeDefinitionStates =
            nodeTypeState.getChildNodeDefinitionStates();
        for (int i = 0; i < childNodeDefinitionStates.length; i++) {
            extractNamespaces(childNodeDefinitionStates[i]);
        }
        PropertyDefinitionState[] propertyDefinitionStates =
            nodeTypeState.getPropertyDefinitionStates();
        for (int i = 0; i < propertyDefinitionStates.length; i++) {
            extractNamespace(propertyDefinitionStates[i].getName());
        }
    }

    private void extractNamespaces(NodeDefinitionState nodeDefinitionState) {
        extractNamespace(nodeDefinitionState.getName());
        extractNamespace(nodeDefinitionState.getDefaultPrimaryTypeName());
        extractNamespaces(nodeDefinitionState.getRequiredPrimaryTypeNames());
    }

    private void extractNamespaces(QName[] names) {
        for (int i = 0; i < names.length; i++) {
            extractNamespace(names[i]);
        }
    }

    private void extractNamespace(QName name) {
        if (name != null) {
            String uri = name.getNamespaceURI();
            if (!namespaces.containsKey(uri)) {
                if (uri.equals(QName.NS_JCR_URI)) {
                    namespaces.put(uri, QName.NS_JCR_PREFIX);
                } else if (uri.equals(QName.NS_MIX_URI)) {
                    namespaces.put(uri, QName.NS_MIX_PREFIX);
                } else if (uri.equals(QName.NS_NT_URI)) {
                    namespaces.put(uri, QName.NS_NT_PREFIX);
                } else {
                    namespaces.put(uri, "ns" + namespaces.size());
                }
            }
        }
    }
    
}
