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
package org.apache.jackrabbit.decorator;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.lock.Lock;

import org.apache.jackrabbit.decorator.DecoratorFactory;

/**
 * Creates a chain of decorator factories. Decorated classes extend this class which provides
 * basic functinality for changing, especially the #chainedFactory which will be set automatically
 * by the DecoratedRepositoryFactoryBean.
 *
 * @author Costin Leau
 */
public class ChainedDecoratorFactory implements DecoratorFactory {

    protected DecoratorFactory chainedFactory;

    /**
     * @see DecoratorFactory#getRepositoryDecorator(javax.jcr.Repository)
     */
    public Repository getRepositoryDecorator(Repository repository) {
        return chainedFactory.getRepositoryDecorator(repository);
    }

    /**
     * @see DecoratorFactory#getSessionDecorator(javax.jcr.Repository,
     *      javax.jcr.Session)
     */
    public Session getSessionDecorator(Repository repository, Session session) {
        return chainedFactory.getSessionDecorator(repository, session);
    }

    /**
     * @see DecoratorFactory#getItemDecorator(javax.jcr.Session, javax.jcr.Item)
     */
    public Item getItemDecorator(Session session, Item item) {
        return chainedFactory.getItemDecorator(session, item);
    }

    /**
     * @see DecoratorFactory#getNodeDecorator(javax.jcr.Session, javax.jcr.Node)
     */
    public Node getNodeDecorator(Session session, Node node) {
        return chainedFactory.getNodeDecorator(session, node);
    }

    /**
     * @see DecoratorFactory#getPropertyDecorator(javax.jcr.Session, javax.jcr.Property)
     */
    public Property getPropertyDecorator(Session session, Property property) {
        return chainedFactory.getPropertyDecorator(session, property);
    }

    /**
     * @see DecoratorFactory#getWorkspaceDecorator(javax.jcr.Session, javax.jcr.Workspace)
     */
    public Workspace getWorkspaceDecorator(Session session, Workspace workspace) {
        return chainedFactory.getWorkspaceDecorator(session, workspace);
    }

    /**
     * @see DecoratorFactory#getLockDecorator(Node, Lock)
     */
    public Lock getLockDecorator(Node node, Lock lock) {
        return chainedFactory.getLockDecorator(node, lock);
    }

    /**
     * @return Returns the factory.
     */
    public DecoratorFactory getChainedFactory() {
        return chainedFactory;
    }

    /**
     * @param factory The factory to set.
     */
    public void setChainedFactory(DecoratorFactory factory) {
        this.chainedFactory = factory;
    }
}
