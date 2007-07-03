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

import org.apache.jackrabbit.spi.ItemInfo;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.name.Path;
import org.apache.jackrabbit.name.NamespaceResolver;
import org.apache.jackrabbit.name.PathFormat;
import org.apache.jackrabbit.name.MalformedPathException;
import org.apache.jackrabbit.name.NameFormat;
import org.apache.jackrabbit.name.NameException;

import javax.jcr.Item;
import javax.jcr.RepositoryException;
import javax.jcr.AccessDeniedException;
import javax.jcr.Node;

/**
 * <code>ItemInfoImpl</code> is a base class for <code>ItemInfo</code>
 * implementations.
 */
abstract class ItemInfoImpl implements ItemInfo {

    /**
     * The parent node id of this item or <code>null</code> if this item
     * represents the root node info.
     */
    private final NodeId parentId;

    /**
     * The name of this item info.
     */
    private final QName name;

    /**
     * The path of this item info.
     */
    private final Path path;

    /**
     * Creates a new item info for the given JCR <code>item</code>.
     *
     * @param item       the JCR item.
     * @param idFactory  the id factory.
     * @param nsResolver the namespace resolver in use.
     * @throws RepositoryException if an error occurs while reading from
     *                             <code>item</code>.
     */
    public ItemInfoImpl(Item item,
                        IdFactoryImpl idFactory,
                        NamespaceResolver nsResolver)
            throws RepositoryException {
        try {
            if (item.getName().equals("")) {
                this.name = QName.ROOT;
            } else {
                this.name = NameFormat.parse(item.getName(), nsResolver);
            }
        } catch (NameException e) {
            throw new RepositoryException(e.getMessage(), e);
        }
        try {
            this.path = PathFormat.parse(item.getPath(), nsResolver);
        } catch (MalformedPathException e) {
            throw new RepositoryException(e.getMessage(), e);
        }
        if (item.getName().equals("")) {
            this.parentId = null;
        } else {
            Node parent = null;
            try {
                parent = item.getParent();
            } catch (AccessDeniedException e) {
                // cannot access parent
            }
            if (parent == null) {
                // use plain path for parent
                Path parentPath = path.getAncestor(1);
                this.parentId = idFactory.createNodeId((String) null, parentPath);
            } else {
                this.parentId = idFactory.createNodeId(item.getParent(), nsResolver);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public NodeId getParentId() {
        return parentId;
    }

    /**
     * {@inheritDoc}
     */
    public QName getQName() {
        return name;
    }

    /**
     * {@inheritDoc}
     */
    public Path getPath() {
        return path;
    }
}
