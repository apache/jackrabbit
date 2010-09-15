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
package org.apache.jackrabbit.spi.commons.identifier;

import org.apache.jackrabbit.spi.IdFactory;
import org.apache.jackrabbit.spi.PropertyId;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.ItemId;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.PathFactory;
import org.apache.jackrabbit.util.Text;

import javax.jcr.RepositoryException;
import java.io.Serializable;

/**
 * <code>AbstractIdFactory</code>...
 */
public abstract class AbstractIdFactory implements IdFactory {

    private static final char DELIMITER = '@';

    //----------------------------------------------------------< IdFactory >---
    /**
     * {@inheritDoc}
     * @see IdFactory#createNodeId(NodeId, Path)
     */
    public NodeId createNodeId(NodeId parentId, Path path) {
        try {
            return new NodeIdImpl(parentId, path, getPathFactory());
        } catch (RepositoryException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     * @see IdFactory#createNodeId(String, Path)
     */
    public NodeId createNodeId(String uniqueID, Path path) {
        return new NodeIdImpl(uniqueID, path);
    }

    /**
     * {@inheritDoc}
     * @see IdFactory#createNodeId(String)
     */
    public NodeId createNodeId(String uniqueID) {
        return new NodeIdImpl(uniqueID);
    }

    /**
     * {@inheritDoc}
     * @see IdFactory#createPropertyId(NodeId,Name)
     */
    public PropertyId createPropertyId(NodeId parentId, Name propertyName) {
        try {
            return new PropertyIdImpl(parentId, propertyName, getPathFactory());
        } catch (RepositoryException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    /**
     * @see IdFactory#toJcrIdentifier(NodeId)
     */
    public String toJcrIdentifier(NodeId nodeId) {
        // TODO improve
        String uniqueId = nodeId.getUniqueID();
        Path path = nodeId.getPath();
        if (path == null) {
            return uniqueId;
        } else if (uniqueId == null) {
            return DELIMITER + path.toString();
        } else {
            StringBuffer bf = new StringBuffer();
            bf.append(Text.escape(uniqueId, DELIMITER));
            bf.append(DELIMITER);
            bf.append(path.toString());
            return bf.toString();
        }
    }

    /**
     * @see IdFactory#fromJcrIdentifier(String)
     */
    public NodeId fromJcrIdentifier(String jcrIdentifier) {
        // TODO improve
        int pos = jcrIdentifier.indexOf(DELIMITER);
        switch (pos) {
            case -1:
                return createNodeId(jcrIdentifier);
            case 0:
                return createNodeId((String) null, getPathFactory().create(jcrIdentifier.substring(1)));
            default:
                String uniqueId = Text.unescape(jcrIdentifier.substring(0, pos), DELIMITER);
                Path path = getPathFactory().create(jcrIdentifier.substring(pos+1));
                return createNodeId(uniqueId, path);
        }        
    }

    //--------------------------------------------------------------------------
    /**
     * Subclassed need to define a PathFactory used to create IDs
     *
     * @return a implementation of <code>PathFactory</code>.
     */
    protected abstract PathFactory getPathFactory();

    //------------------------------------------------------< Inner classes >---

    private static abstract class ItemIdImpl implements ItemId, Serializable {

        private final String uniqueID;
        private final Path path;

        private transient int hashCode = 0;

        private ItemIdImpl(String uniqueID, Path path) {
            if (uniqueID == null && path == null) {
                throw new IllegalArgumentException("Only uniqueID or relative path might be null.");
            }
            this.uniqueID = uniqueID;
            this.path = path;
        }

        private ItemIdImpl(NodeId parentId, Name name, PathFactory factory)
                throws RepositoryException {
            if (parentId == null || name == null) {
                throw new IllegalArgumentException("Invalid ItemIdImpl: parentId and name must not be null.");
            }
            this.uniqueID = parentId.getUniqueID();
            Path parentPath = parentId.getPath();
            if (parentPath != null) {
                this.path = factory.create(parentPath, name, true);
            } else {
                this.path = factory.create(name);
            }
        }

        public abstract boolean denotesNode();

        public String getUniqueID() {
            return uniqueID;
        }

        public Path getPath() {
            return path;
        }

        /**
         * ItemIdImpl objects are equal if the have the same uuid and relative path.
         *
         * @param obj
         * @return
         */
        @Override
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
            return (uniqueID == null ? other.getUniqueID() == null : uniqueID.equals(other.getUniqueID()))
                && (path == null ? other.getPath() == null : path.equals(other.getPath()));
        }

        /**
         * Returns the hash code of the uuid and the path. The computed hash code
         * is memorized for better performance.
         *
         * @return hash code
         * @see Object#hashCode()
         */
        @Override
        public int hashCode() {
            // since the ItemIdImpl is immutable, store the computed hash code value
            if (hashCode == 0) {
                int result = 17;
                result = 37 * result + (uniqueID != null ? uniqueID.hashCode() : 0);
                result = 37 * result + (path != null ? path.hashCode() : 0);
                hashCode = result;
            }
            return hashCode;
        }

        /**
         * Combination of uuid and relative path
         *
         * @return
         */
        @Override
        public String toString() {
            StringBuffer b = new StringBuffer();
            if (uniqueID != null) {
                b.append(uniqueID);
            }
            if (path != null) {
                b.append(path.toString());
            }
            return b.toString();
        }
    }

    private static class NodeIdImpl extends ItemIdImpl implements NodeId {

        private static final long serialVersionUID = -360276648861146631L;

        public NodeIdImpl(String uniqueID) {
            super(uniqueID, null);
        }

        public NodeIdImpl(String uniqueID, Path path) {
            super(uniqueID, path);
        }

        public NodeIdImpl(NodeId parentId, Path path, PathFactory factory)
                throws RepositoryException {
            super(parentId.getUniqueID(), (parentId.getPath() != null) ? factory.create(parentId.getPath(), path, true) : path);
        }

        @Override
        public boolean denotesNode() {
            return true;
        }

        @Override
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

    private static class PropertyIdImpl extends ItemIdImpl implements PropertyId, Serializable {

        private static final long serialVersionUID = -1953124047770776444L;

        private final NodeId parentId;

        private PropertyIdImpl(NodeId parentId, Name name, PathFactory factory)
                throws RepositoryException {
            super(parentId, name, factory);
            this.parentId = parentId;
        }

        @Override
        public boolean denotesNode() {
            return false;
        }

        public NodeId getParentId() {
            return parentId;
        }

        public Name getName() {
            return getPath().getName();
        }

        @Override
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
