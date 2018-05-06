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
 * Implements a query node that defines an exact match of a property and a
 * value.
 */
public class ExactQueryNode extends QueryNode {

    /**
     * The name of the property to match
     */
    private final Name property;

    /**
     * The value of the property to match
     */
    private final Name value;

    /**
     * Creates a new <code>ExactQueryNode</code> instance.
     *
     * @param parent   the parent node for this <code>ExactQueryNode</code>.
     * @param property the name of the property to match.
     * @param value    the value of the property to match.
     */
    public ExactQueryNode(QueryNode parent, Name property, Name value) {
        super(parent);
        if (parent == null) {
            throw new NullPointerException("parent");
        }
        this.property = property;
        this.value = value;
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
    public int getType() {
        return QueryNode.TYPE_EXACT;
    }

    /**
     * Returns the name of the property to match.
     *
     * @return the name of the property to match.
     */
    public Name getPropertyName() {
        return property;
    }

    /**
     * Returns the value of the property to match.
     *
     * @return the value of the property to match.
     */
    public Name getValue() {
        return value;
    }

    /**
     * {@inheritDoc}
     */
    public boolean equals(Object obj) {
        if (obj instanceof ExactQueryNode) {
            ExactQueryNode other = (ExactQueryNode) obj;
            return (value == null ? other.value == null : value.equals(other.value))
                    && (property == null ? other.property == null : property.equals(other.property));
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean needsSystemTree() {
        return false;
    }
}
