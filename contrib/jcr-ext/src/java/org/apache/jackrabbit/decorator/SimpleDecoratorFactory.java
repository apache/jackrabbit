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
package org.apache.jackrabbit.decorator;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.lock.Lock;

/**
 * TODO
 */
public class SimpleDecoratorFactory implements DecoratorFactory {

    /** {@inheritDoc} */
    public Repository getRepositoryDecorator(Repository repository) {
        return new RepositoryDecorator(this, repository);
    }

    /** {@inheritDoc} */
    public Session getSessionDecorator(Repository repository, Session session) {
        return new SessionDecorator(this, repository, session);
    }

    /** {@inheritDoc} */
    public Workspace getWorkspaceDecorator(Session session, Workspace workspace) {
        return new WorkspaceDecorator(this, session, workspace);
    }

    /** {@inheritDoc} */
    public Node getNodeDecorator(Session session, Node node) {
        return null;
    }

    /** {@inheritDoc} */
    public Property getPropertyDecorator(Session session, Property property) {
        // TODO Auto-generated method stub
        return null;
    }

    /** {@inheritDoc} */
    public Item getItemDecorator(Session session, Item item) {
        if (item instanceof Node) {
            return getNodeDecorator(session, (Node) item);
        } else if (item instanceof Property) {
            return getPropertyDecorator(session, (Property) item);
        } else {
            return new ItemDecorator(this, session, item);
        }
    }

    public Lock getLockDecorator(Node node, Lock lock) {
        return new LockDecorator(node, lock);
    }

}
