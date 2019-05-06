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
 * Filters items according to their names.
 *
 */
public class NamePredicate extends DepthPredicate {

    /**
     * The name to filter on
     */
    protected final String name;

    /**
     * Creates a new name filter with the given name and depths
     * @param name the name to filter on
     * @param minDepth the minimal depth
     * @param maxDepth the maximal depth
     */
    public NamePredicate(String name, int minDepth, int maxDepth) {
        super(minDepth, maxDepth);
        this.name = name;
    }

    /**
     * Creates a new name filter with the given name.
     * @param name the name to filter on
     */
    public NamePredicate(String name) {
        this(name, 0, Integer.MAX_VALUE);
    }

    /**
     * Returns <code>true</code> if the name of the given item is equal to
     * the configured name.
     * @see org.apache.jackrabbit.commons.predicate.DepthPredicate#matches(javax.jcr.Item)
     */
    @Override
    protected boolean matches(Item item) throws RepositoryException {
        return item.getName().equals(name);
    }
}