/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.name;

import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;

/**
 * TODO
 */
class NamedElement implements PathElement {

    private final Name name;

    public NamedElement(Name name) {
        this.name = name;
    }

    /** {@inheritDoc} */
    public Item step(Item item) throws ItemNotFoundException,
            RepositoryException {
        if (item.isNode()) {
            String pattern = name.toJCRName(item.getSession());

            PropertyIterator properties = ((Node) item).getProperties(pattern);
            if (properties.hasNext()) {
                return properties.nextProperty();
            }

            NodeIterator nodes = ((Node) item).getNodes(pattern);
            if (nodes.hasNext()) {
                return nodes.nextNode();
            }
        }

        throw new ItemNotFoundException("Invalid item path " + name);
    }

}
