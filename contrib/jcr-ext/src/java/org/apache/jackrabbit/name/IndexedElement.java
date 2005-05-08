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

import java.util.NoSuchElementException;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

/**
 * Indexed path element.
 */
final class IndexedElement implements PathElement {

    /** Path element name */
    private final Name name;

    /** Path element index */
    private final int index;

    /**
     * Creates an indexed path element instance.
     *
     * @param name  path element name
     * @param index path element index
     */
    public IndexedElement(Name name, int index) {
        this.name = name;
        this.index = index;
    }

    /**
     * Resolves the given item to the named child node with the
     * specified index.
     *
     * @param item context item
     * @return indexed child node
     * @throws PathNotFoundException if the path resolution fails
     * @throws RepositoryException   if another error occurs
     * @see PathElement#resolve(Item)
     */
    public Item resolve(Item item)
            throws PathNotFoundException, RepositoryException {
        if (item.isNode()) {
            String pattern = name.toJCRName(item.getSession());
            NodeIterator nodes = ((Node) item).getNodes(pattern);
            try {
                nodes.skip(index - 1);
                return nodes.nextNode();
            } catch (NoSuchElementException e) {
                // fall through
            }
        }
        throw new PathNotFoundException(
                "Path name or index not found: " + this);
    }

    /**
     * Returns the string representation of this path element.
     *
     * @return string representation of the path element name and index
     * @see Object#toString()
     * @see Name#toString()
     */
    public String toString() {
        return name + "[" + index + "]";
    }
}
