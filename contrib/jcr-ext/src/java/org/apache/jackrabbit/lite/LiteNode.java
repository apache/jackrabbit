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
package org.apache.jackrabbit.lite;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeDef;
import javax.jcr.nodetype.NodeType;

import org.apache.jackrabbit.base.BaseNode;
import org.apache.jackrabbit.iterator.ArrayNodeIterator;
import org.apache.jackrabbit.iterator.ArrayPropertyIterator;

/**
 * TODO
 */
public class LiteNode extends BaseNode {

    private final Session session;

    private final NodeDef definition;

    private final NodeType type;

    protected LiteNode(Session session, NodeDef definition, NodeType type) {
        this.session = session;
        this.definition = definition;
        this.type = type;
    }

    public Session getSession() {
        return session;
    }

    public int getIndex() throws RepositoryException {
        return 1;
    }
    
    public NodeIterator getNodes() throws RepositoryException {
        return new ArrayNodeIterator(new Node[0]);
    }

    public PropertyIterator getProperties() throws RepositoryException {
        return new ArrayPropertyIterator(new Property[0]);
    }

    public NodeDef getDefinition() throws RepositoryException {
        return definition;
    }

    public NodeType getPrimaryNodeType() throws RepositoryException {
        return type;
    }
}
