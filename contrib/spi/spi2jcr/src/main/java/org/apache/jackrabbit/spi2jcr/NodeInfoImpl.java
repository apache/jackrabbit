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

import org.apache.jackrabbit.spi.NodeInfo;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.PropertyId;
import org.apache.jackrabbit.spi.IdIterator;
import org.apache.jackrabbit.name.NamespaceResolver;
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.name.NameFormat;
import org.apache.jackrabbit.name.NameException;

import javax.jcr.RepositoryException;
import javax.jcr.Node;
import javax.jcr.PropertyIterator;
import javax.jcr.nodetype.NodeType;
import java.util.List;
import java.util.ArrayList;

/**
 * <code>NodeInfoImpl</code> implements a <code>NodeInfo</code> on top of a JCR
 * repository.
 */
class NodeInfoImpl extends ItemInfoImpl implements NodeInfo {

    /**
     * The node id of the underlying node.
     */
    private final NodeId id;

    /**
     * 1-based index of the underlying node.
     */
    private final int index;

    /**
     * The name of the primary node type.
     */
    private final QName primaryTypeName;

    /**
     * The names of assigned mixins.
     */
    private final QName[] mixinNames;

    /**
     * The list of {@link PropertyId}s that reference this node info.
     */
    private final List references;

    /**
     * The list of {@link PropertyId}s of this node info.
     */
    private final List propertyIds;

    /**
     * Creates a new node info for the given <code>node</code>.
     *
     * @param node       the JCR node.
     * @param idFactory  the id factory.
     * @param nsResolver the namespace resolver in use.
     * @throws RepositoryException if an error occurs while reading from
     *                             <code>node</code>.
     */
    public NodeInfoImpl(Node node,
                        IdFactoryImpl idFactory,
                        NamespaceResolver nsResolver)
            throws RepositoryException {
        super(node, idFactory, nsResolver);
        try {
            this.id = idFactory.createNodeId(node, nsResolver);
            this.index = node.getIndex();
            this.primaryTypeName = NameFormat.parse(node.getPrimaryNodeType().getName(), nsResolver);
            NodeType[] mixins = node.getMixinNodeTypes();
            this.mixinNames = new QName[mixins.length];
            for (int i = 0; i < mixins.length; i++) {
                mixinNames[i] = NameFormat.parse(mixins[i].getName(), nsResolver);
            }
            this.references = new ArrayList();
            for (PropertyIterator it = node.getReferences(); it.hasNext(); ) {
                references.add(idFactory.createPropertyId(it.nextProperty(), nsResolver));
            }
            this.propertyIds = new ArrayList();
            for (PropertyIterator it = node.getProperties(); it.hasNext(); ) {
                propertyIds.add(idFactory.createPropertyId(it.nextProperty(), nsResolver));
            }
        } catch (NameException e) {
            throw new RepositoryException(e.getMessage(), e);
        }
    }

    //-------------------------------< NodeInfo >-------------------------------

    /**
     * {@inheritDoc}
     */
    public NodeId getId() {
        return id;
    }

    /**
     * {@inheritDoc}
     */
    public int getIndex() {
        return index;
    }

    /**
     * {@inheritDoc}
     */
    public QName getNodetype() {
        return primaryTypeName;
    }

    /**
     * {@inheritDoc}
     */
    public QName[] getMixins() {
        QName[] ret = new QName[mixinNames.length];
        System.arraycopy(mixinNames, 0, ret, 0, mixinNames.length);
        return ret;
    }

    /**
     * {@inheritDoc}
     */
    public PropertyId[] getReferences() {
        return (PropertyId[]) references.toArray(new PropertyId[references.size()]);
    }

    /**
     * {@inheritDoc}
     */
    public IdIterator getPropertyIds() {
        return new IteratorHelper(propertyIds);
    }
}
