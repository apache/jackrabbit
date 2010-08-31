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
package org.apache.jackrabbit.commons.predicate;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

/**
 * Filter that checks the declared type of an item
 *
 */
public class DeclaringTypePredicate extends DepthPredicate {

    /**
     * The nodetype to check
     */
    protected final String nodeType;

    /**
     * indicates if only props should be checked
     */
    protected final boolean propsOnly;

    /**
     * Creates a new filter for the given nodetype and flags.
     * @param nodeType the nodetype name to check
     * @param propsOnly if <code>true</code> only properties are checked
     * @param minDepth the minimal depth
     * @param maxDepth the maximal depth
     */
    public DeclaringTypePredicate(String nodeType, boolean propsOnly,
                                   int minDepth, int maxDepth) {
        super(minDepth, maxDepth);
        this.nodeType = nodeType;
        this.propsOnly = propsOnly;
    }

    /**
     * Creates a new filter for the given nodetype and flags
     * @param nodeType the nodetype name to check
     * @param propsOnly if <code>true</code> only properties are checked
     */
    public DeclaringTypePredicate(String nodeType, boolean propsOnly) {
        this(nodeType, propsOnly, 0, Integer.MAX_VALUE);
    }

    /**
     * Matches if the declaring nodetype of the item is equal to the one
     * specified in this filter. If the item is a node and <code>propsOnly</code>
     * flag is <code>true</code> it returns <code>false</code>.
     * @see org.apache.jackrabbit.commons.predicate.DepthPredicate#matches(javax.jcr.Item)
     */
    @Override
    protected boolean matches(Item item) throws RepositoryException {
        if (item.isNode()) {
            return !propsOnly && ((Node) item).getDefinition().getDeclaringNodeType().getName().equals(nodeType);
        }
        return ((Property) item).getDefinition().getDeclaringNodeType().getName().equals(nodeType);
    }
}