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
package org.apache.jackrabbit.spi.commons.query;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.spi.Name;

/**
 * Represents query node that dereferences a reference property into a node and
 * does an optional name test on the target node.
 */
public class DerefQueryNode extends LocationStepQueryNode {

    /** The name of the reference property */
    private Name refProperty;

    /**
     * Creates a new <code>DerefQueryNode</code> without a name set for the
     * reference property.
     * @param parent the parent query node.
     * @param nameTest the name test on the target node, or <code>null</code>
     *   if no name test should be performed on the target node.
     * @param descendants if <code>true</code> this location step uses the
     *   descendant-or-self axis; otherwise the child axis.
     */
    protected DerefQueryNode(QueryNode parent, Name nameTest, boolean descendants) {
        super(parent);
        setNameTest(nameTest);
        setIncludeDescendants(descendants);
    }

    /**
     * Sets a new name for the reference property.
     * @param propertyName the name of the reference property.
     */
    public void setRefProperty(Name propertyName) {
        refProperty = propertyName;
    }

    /**
     * Returns the name of the reference property or <code>null</code> if
     * none is set.
     * @return the name of the reference property or <code>null</code> if
     * none is set.
     */
    public Name getRefProperty() {
        return refProperty;
    }

    /**
     * {@inheritDoc}
     */
    public int getType() {
        return TYPE_DEREF;
    }

    /**
     * {@inheritDoc}
     * @throws RepositoryException
     */
    public Object accept(QueryNodeVisitor visitor, Object data) throws RepositoryException {
        return visitor.visit(this, data);
    }

    /**
     * {@inheritDoc}
     */
    public boolean equals(Object obj) {
        if (obj instanceof DerefQueryNode) {
            DerefQueryNode other = (DerefQueryNode) obj;
            return super.equals(obj)
                    && refProperty == null ? other.refProperty == null : refProperty.equals(other.refProperty);
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean needsSystemTree() {
        // Always return true since we don't know if the referenced nodes path
        // is a child of /jcr:system
        return true;
    }

}
