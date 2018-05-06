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

import javax.jcr.AccessDeniedException;
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
     * Returns the ancestor of this item at the given depth.
     * <p>
     * The default implementation handles the root node at depth zero and
     * this item at depth equal to the depth of this item as special cases,
     * and uses {@link javax.jcr.Session#getItem(String)} to retrieve other
     * ancestors based on the ancestor path calculated from the path of this
     * node as returned by {@link Item#getPath()}.
     *
     * @param depth depth of the returned ancestor item
     * @return ancestor item
     * @throws ItemNotFoundException if the given depth is negative or greater
     *                               than the depth of this item
     * @throws AccessDeniedException if access to the ancestor item is denied
     * @throws RepositoryException if an error occurs
     */
    public Item getAncestor(int depth)
            throws ItemNotFoundException, AccessDeniedException,
            RepositoryException {
        if (depth < 0) {
            throw new ItemNotFoundException(
                    this + ": Invalid ancestor depth (" + depth + ")");
        } else if (depth == 0) {
            return getSession().getRootNode();
        }

        String path = getPath();
        int slash = 0;
        for (int i = 0; i < depth - 1; i++) {
            slash = path.indexOf('/', slash + 1);
            if (slash == -1) {
                throw new ItemNotFoundException(
                        this + ": Invalid ancestor depth (" + depth + ")");
            }
        }
        slash = path.indexOf('/', slash + 1);
        if (slash == -1) {
            return this;
        }

        try {
            return getSession().getItem(path.substring(0, slash));
        } catch (ItemNotFoundException e) {
            throw new AccessDeniedException(
                    this + ": Ancestor access denied (" + depth + ")");
        }
    }

    /**
     * Returns the depth of this item.
     * <p>
     * The default implementation determines the depth by counting the
     * slashes in the path returned by {@link Item#getPath()}.
     *
     * @return depth of this item
     * @throws RepositoryException if an error occurs
     */
    public int getDepth() throws RepositoryException {
        String path = getPath();
        if (path.length() == 1) {
            return 0;
        } else {
            int depth = 1;
            int slash = path.indexOf('/', 1);
            while (slash != -1) {
                depth++;
                slash = path.indexOf('/', slash + 1);
            }
            return depth;
        }
    }

    //--------------------------------------------------------------< Object >

    /**
     * Returns a string representation of this item.
     * <p>
     * The default implementation returns the path of this item and falls
     * back to the {@link Object#toString()} implementation if the item path
     * can not be retrieved.
     *
     * @return string representation of this item
     */
    public String toString() {
        try {
            return getPath();
        } catch (RepositoryException e) {
            return super.toString();
        }
    }

}
