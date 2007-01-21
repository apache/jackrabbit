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
package org.apache.jackrabbit.spi.xml;

import org.apache.jackrabbit.name.Path;
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.spi.IdFactory;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.PropertyId;

public class XMLIdFactory implements IdFactory {

    private final XMLNodeId root;

    public XMLIdFactory(XMLNodeId root) {
        this.root = root;
    }

    //------------------------------------------------------------< IdFactory>

    public NodeId createNodeId(String id) {
        return null;
    }

    public NodeId createNodeId(NodeId id, Path path) {
        try {
            return ((XMLNodeId) id).getNodeId(path);
        } catch (ClassCastException e) {
            throw new IllegalStateException("Invalid node identifier: " + id);
        }
    }

    public NodeId createNodeId(String id, Path path) {
        if (id == null) {
            return createNodeId(root, path);
        } else {
            return null;
        }
    }

    public PropertyId createPropertyId(NodeId id, QName name) {
        try {
            if (QName.JCR_PRIMARYTYPE.equals(name)) {
                return new XMLPrimaryTypeId(id);
            } else {
                return ((XMLNodeId) id).getPropertyId(name);
            }
        } catch (ClassCastException e) {
            throw new IllegalStateException("Invalid node identifier: " + id);
        }
    }

}
