/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.name;

import javax.jcr.Item;
import javax.jcr.RepositoryException;

/**
 * The ".." path element.
 */
class ParentElement implements PathElement {

    /**
     * Resolves the given item to its parent.
     *
     * @param item context item
     * @return parent node
     * @throws RepositoryException if the parent node can not be retrieved
     * @see PathElement#resolve(Item)
     * @see Item#getParent()
     */
    public Item resolve(Item item) throws RepositoryException {
        return item.getParent();
    }

    /**
     * Returns the string representation of this path element.
     *
     * @return the string ".."
     * @see Object#toString()
     */
    public String toString() {
        return "..";
    }

}
