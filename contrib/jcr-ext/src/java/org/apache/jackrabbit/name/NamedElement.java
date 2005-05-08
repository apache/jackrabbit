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
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;

/**
 * Named path element.
 */
final class NamedElement implements PathElement {

    /** Path element name */
    private final Name name;

    /**
     * Creates a named path element instance.
     *
     * @param name path element name
     */
    public NamedElement(Name name) {
        this.name = name;
    }

    /**
     * Resolves the given item to the named property or child node.
     *
     * @param item context item
     * @return named property or child node
     * @throws PathNotFoundException if the path resolution fails
     * @throws RepositoryException   if another error occurs
     * @see PathElement#resolve(Item)
     */
    public Item resolve(Item item)
            throws PathNotFoundException, RepositoryException {
        if (item.isNode()) {
            Node node = (Node) item;

            /* Note: JCR names can not contain special pattern characters */
            String pattern = name.toJCRName(item.getSession());

            PropertyIterator properties = node.getProperties(pattern);
            if (properties.hasNext()) {
                return properties.nextProperty();
            }

            NodeIterator nodes = node.getNodes(pattern);
            if (nodes.hasNext()) {
                return nodes.nextNode();
            }
        }
        throw new PathNotFoundException("Path name not found: " + this);
    }

    /**
     * Returns the string representation of this path element.
     *
     * @return string representation of the path element name
     * @see Object#toString()
     * @see Name#toString()
     */
    public String toString() {
        return name.toString();
    }

}
