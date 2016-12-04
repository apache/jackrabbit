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
package org.apache.jackrabbit.spi2jcr;

import java.util.Map;
import java.util.HashMap;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.PropertyType;
import javax.jcr.Session;
import javax.jcr.NamespaceException;
import javax.jcr.nodetype.NodeType;

import org.apache.jackrabbit.spi.Event;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.ItemId;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.spi.IdFactory;
import org.apache.jackrabbit.spi.QValueFactory;
import org.apache.jackrabbit.spi.commons.value.ValueFormat;
import org.apache.jackrabbit.spi.commons.EventImpl;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.conversion.NameException;
import org.apache.jackrabbit.spi.commons.conversion.NameResolver;

/**
 * <code>EventFactory</code> implements a factory for SPI Event instances.
 */
class EventFactory {

    private final Session session;

    private final NamePathResolver resolver;

    private final IdFactory idFactory;

    private final QValueFactory qValueFactory;

    public EventFactory(Session session,
                        NamePathResolver resolver,
                        IdFactory idFactory,
                        QValueFactory qValueFactory) {
        this.session = session;
        this.resolver = resolver;
        this.idFactory = idFactory;
        this.qValueFactory = qValueFactory;
    }

    public Event fromJCREvent(javax.jcr.observation.Event e)
            throws RepositoryException {
        Path p = e.getPath() != null ? resolver.getQPath(e.getPath()) : null;
        Path parent = p != null ? p.getAncestor(1) : null;
        int type = e.getType();

        NodeId parentId = parent != null ?idFactory.createNodeId((String) null, parent) : null;
        String identifier = e.getIdentifier();
        ItemId itemId;
        Node node = null;
        if (identifier != null) {
            itemId = idFactory.fromJcrIdentifier(e.getIdentifier());
            try {
                node = session.getItem(e.getPath()).getParent();
            } catch (RepositoryException re) {
                // ignore. TODO improve
            }
        } else {
            switch (type) {
                case Event.NODE_ADDED:
                case Event.NODE_MOVED:
                    node = session.getItem(e.getPath()).getParent();
                case Event.NODE_REMOVED:
                    itemId = idFactory.createNodeId((String) null, p);
                    break;
                case Event.PROPERTY_ADDED:
                case Event.PROPERTY_CHANGED:
                    node = session.getItem(e.getPath()).getParent();
                case Event.PROPERTY_REMOVED:
                    itemId = idFactory.createPropertyId(parentId, p.getName());
                    break;
                case Event.PERSIST:
                default:
                    itemId = null;
            }
        }

        Name nodeTypeName = null;
        Name[] mixinTypes = Name.EMPTY_ARRAY;
        if (node != null) {
            try {
                parentId = idFactory.createNodeId(node.getUUID(), null);
            } catch (UnsupportedRepositoryOperationException ex) {
                // not referenceable
            }
            nodeTypeName = resolver.getQName(node.getPrimaryNodeType().getName());
            mixinTypes = getNodeTypeNames(node.getMixinNodeTypes(), resolver);
        }
        Map<Name, QValue> info = new HashMap<Name, QValue>();
        Map<String, Object> jcrInfo = e.getInfo();
        for (Map.Entry<String, Object> entry : jcrInfo.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            Name name = resolver.getQName(key);
            if (value != null) {
                // event information is generated for NODE_MOVED only in which
                // case all values are of type PATH.
                QValue v = ValueFormat.getQValue(value.toString(), PropertyType.PATH, resolver, qValueFactory);
                info.put(name, v);
            } else {
                info.put(name, null);
            }
        }
        return new EventImpl(e.getType(), p, itemId, parentId, nodeTypeName,
                mixinTypes, e.getUserID(), e.getUserData(), e.getDate(), info);
    }

    /**
     * Returns the names of the passed node types using the namespace resolver
     * to parse the names.
     *
     * @param nt       the node types
     * @param resolver the name resolver.
     * @return the names of the node types.
     * @throws NameException      if a node type returns an illegal name.
     * @throws NamespaceException if the name of a node type contains a prefix
     *                            that is not known to <code>resolver</code>.
     */
    private static Name[] getNodeTypeNames(NodeType[] nt, NameResolver resolver)
            throws NameException, NamespaceException {
        Name[] names = new Name[nt.length];
        for (int i = 0; i < nt.length; i++) {
            Name ntName = resolver.getQName(nt[i].getName());
            names[i] = ntName;
        }
        return names;
    }
}
