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
 * <code>IsMandatoryFilter</code>...
 *
 */
public class IsMandatoryPredicate extends DepthPredicate {

    protected final boolean isMandatory;

    public IsMandatoryPredicate() {
        this(true);
    }

    public IsMandatoryPredicate(boolean isMandatory, int minDepth, int maxDepth) {
        super(minDepth, maxDepth);
        this.isMandatory = isMandatory;
    }

    public IsMandatoryPredicate(boolean isMandatory) {
        this(isMandatory, 0, Integer.MAX_VALUE);
    }

    /**
     * @see org.apache.jackrabbit.commons.predicate.DepthPredicate#matches(javax.jcr.Item)
     */
    @Override
    protected boolean matches(Item item) throws RepositoryException {
        if (item.isNode()) {
            return ((Node) item).getDefinition().isMandatory() == isMandatory;
        }
        return ((Property) item).getDefinition().isMandatory() == isMandatory;
    }
}