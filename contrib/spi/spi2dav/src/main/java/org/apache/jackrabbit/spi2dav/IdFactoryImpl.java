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
package org.apache.jackrabbit.spi2dav;

import org.apache.jackrabbit.name.Path;
import org.apache.jackrabbit.spi.IdFactory;
import org.apache.jackrabbit.spi.ItemId;
import org.apache.jackrabbit.spi.PropertyId;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.name.MalformedPathException;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 * <code>IdFactoryImpl</code>...
 */
public class IdFactoryImpl implements IdFactory {

    private static Logger log = LoggerFactory.getLogger(IdFactoryImpl.class);

    private static final IdFactory idFactory = new IdFactoryImpl(); 

    private IdFactoryImpl() {};

    public static IdFactory getInstance() {
        return idFactory;
    }

    public PropertyId createPropertyId(NodeId parentId, QName propertyName) {
        try {
            return new PropertyIdImpl(parentId, propertyName);
        } catch (MalformedPathException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    public NodeId createNodeId(NodeId parentId, Path relativePath) {
        try {
            return new NodeIdImpl(parentId, relativePath);
        } catch (MalformedPathException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    public NodeId createNodeId(String uuid, Path relativePath) {
        return new NodeIdImpl(uuid, relativePath);
    }

    public NodeId createNodeId(String uuid) {
        return new NodeIdImpl(uuid);
    }

    //------------------------------------------------------< Inner classes >---
    private abstract class ItemIdImpl implements ItemId {

        private final String uuid;
        private final Path relativePath;

        private int hashCode = 0;

        private ItemIdImpl(String uuid, Path relativePath) {
            if (uuid == null && relativePath == null) {
                throw new IllegalArgumentException("Only uuid or relative path might be null.");
            }
            this.uuid = uuid;
            this.relativePath = relativePath;
        }

        private ItemIdImpl(NodeId parentId, QName name) throws MalformedPathException {
            if (parentId == null || name == null) {
                throw new IllegalArgumentException("Invalid ItemIdImpl: parentId and name must not be null.");
            }
            this.uuid = parentId.getUUID();
            Path parent = parentId.getRelativePath();
            if (parent != null) {
                this.relativePath = Path.create(parent, name, true);
            } else {
                this.relativePath = Path.create(name, Path.INDEX_UNDEFINED);
            }
        }

        public abstract boolean denotesNode();

        public String getUUID() {
            return uuid;
        }

        public Path getRelativePath() {
            return relativePath;
        }

        /**
         * ItemIdImpl objects are equal if the have the same uuid and relative path.
         *
         * @param obj
         * @return
         */
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj instanceof ItemId) {
                ItemId other = (ItemId) obj;
                return equals(other);
            }
            return false;
        }

        boolean equals(ItemId other) {
            return (uuid == null) ? other.getUUID() == null : uuid.equals(other.getUUID())
                && (relativePath == null) ? other.getRelativePath() == null : relativePath.equals(other.getRelativePath());
        }

        /**
         * Returns the hash code of the uuid and the relativePath. The computed hash code
         * is memorized for better performance.
         *
         * @return hash code
         * @see Object#hashCode()
         */
        public int hashCode() {
            // since the ItemIdImpl is immutable, store the computed hash code value
            if (hashCode == 0) {
                hashCode = toString().hashCode();
            }
            return hashCode;
        }

        /**
         * Combination of uuid and relative path
         *
         * @return
         */
        public String toString() {
            StringBuffer b = new StringBuffer();
            if (uuid != null) {
                b.append(uuid);
            }
            if (relativePath != null) {
                b.append(relativePath.toString());
            }
            return b.toString();
        }
    }

    public class NodeIdImpl extends ItemIdImpl implements NodeId {

        public NodeIdImpl(String uuid) {
            super(uuid, null);
        }

        public NodeIdImpl(String uuid, Path relativePath) {
            super(uuid, relativePath);
        }

        public NodeIdImpl(NodeId parentId, Path relativePath) throws MalformedPathException {
            super(parentId.getUUID(), (parentId.getRelativePath() != null) ? Path.create(parentId.getRelativePath(), relativePath, true) : relativePath);
        }

        public boolean denotesNode() {
            return true;
        }

        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj instanceof NodeId) {
                return super.equals((NodeId)obj);
            }
            return false;
        }
    }

    private class PropertyIdImpl extends ItemIdImpl implements PropertyId {

        private final NodeId parentId;

        private PropertyIdImpl(NodeId parentId, QName name) throws MalformedPathException {
            super(parentId, name);
            this.parentId = parentId;
        }

        public boolean denotesNode() {
            return false;
        }

        public NodeId getParentId() {
            return parentId;
        }

        public QName getQName() {
            return getRelativePath().getNameElement().getName();
        }

        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj instanceof PropertyId) {
                return super.equals((PropertyId)obj);
            }
            return false;
        }
    }
}