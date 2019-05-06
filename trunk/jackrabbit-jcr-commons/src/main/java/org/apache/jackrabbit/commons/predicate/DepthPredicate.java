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
import javax.jcr.RepositoryException;

/**
 * Implements a filter that filters item according to their (passed) depth.
 *
 */
public class DepthPredicate implements Predicate {

    /**
     * The minimal depth
     */
    protected final int minDepth;

    /**
     * The maximal depth
     */
    protected final int maxDepth;

    /**
     * Creates a new depth filter for the given depths.
     * @param minDepth the minimal depth
     * @param maxDepth the maximal depth
     */
    public DepthPredicate(int minDepth, int maxDepth) {
        this.minDepth = minDepth;
        this.maxDepth = maxDepth;
    }

    /**
     * Matches if the given depth is greater or equal the minimum depth and
     * less or equal the maximum depth and if the call to {@link #matches(Item)}
     * returns <code>true</code>.
     * @see Predicate#evaluate(java.lang.Object)
     */
    public boolean evaluate(Object item) {
        if ( item instanceof Item ) {
            try {
                final int depth = ((Item)item).getDepth();
                return depth >= minDepth && depth <= maxDepth && matches((Item)item);
            } catch (RepositoryException re) {
                return false;
            }
        }
        return false;
    }

    /**
     * Returns <code>true</code>. Subclasses can override to implement something
     * useful that is dependant of the depth.
     *
     * @param item the item to match
     * @return <code>true</code> if the item matches; <code>false</code> otherwise.
     * @throws RepositoryException if an error occurs.
     */
    protected boolean matches(Item item) throws RepositoryException {
        return true;
    }
}