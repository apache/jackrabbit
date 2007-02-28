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
package org.apache.jackrabbit.commons;

import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.RepositoryException;

/**
 * Abstract base class for implementing the JCR {@link Item} interface.
 * <p>
 * {@link Item} methods <em>without</em> a default implementation:
 * <ul>
 *   <li>{@link Item#accept(javax.jcr.ItemVisitor)}</li>
 *   <li>{@link Item#getName()}</li>
 *   <li>{@link Item#getParent()}</li>
 *   <li>{@link Item#getPath()}</li>
 *   <li>{@link Item#getSession()}</li>
 *   <li>{@link Item#isModified()}</li>
 *   <li>{@link Item#isNew()}</li>
 *   <li>{@link Item#isNode()}</li>
 *   <li>{@link Item#isSame(Item)}</li>
 *   <li>{@link Item#refresh(boolean)}</li>
 *   <li>{@link Item#remove()}</li>
 *   <li>{@link Item#save()}</li>
 * </ul>
 */
public abstract class AbstractItem implements Item {

    /**
     * Returns the ancestor of this item at the given depth from the
     * root node.
     * <p>
     * The default implementation returns this item if the given depth
     * equals the return value of the {@link Item#getDepth()} method.
     * Otherwise calls the method recursively on the parent node.
     *
     * @param depth depth of the returned ancestor item
     * @return ancestor item
     * @throws RepositoryException if an error occurs
     */
    public Item getAncestor(int depth) throws RepositoryException {
        if (getDepth() == depth) {
            return this;
        } else {
            return getParent().getAncestor(depth);
        }
    }

    /**
     * Returns the depth of this item.
     * <p>
     * Recursively calls the method on the parent node and increments
     * the return value to get the depth of this item. Returns zero if
     * the parent node is not available (i.e. this is the root node).
     *
     * @return depth of this item
     * @throws RepositoryException if an error occurs
     */
    public int getDepth() throws RepositoryException {
        try {
            return getParent().getDepth() + 1;
        } catch (ItemNotFoundException e) {
            return 0;
        }
    }

}
