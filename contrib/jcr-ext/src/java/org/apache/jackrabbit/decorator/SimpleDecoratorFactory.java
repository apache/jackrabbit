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

/**
 * TODO
 */
public class SimpleDecoratorFactory implements DecoratorFactory {

    /** {@inheritDoc} */
    public Repository getRepositoryDecorator(Repository repository) {
        // TODO Auto-generated method stub
        return null;
    }

    /** {@inheritDoc} */
    public Session getSessionDecorator(Repository repository, Session session) {
        // TODO Auto-generated method stub
        return null;
    }

    /** {@inheritDoc} */
    public Workspace getWorkspaceDecorator(Session session, Workspace workspace) {
        // TODO Auto-generated method stub
        return null;
    }

    /** {@inheritDoc} */
    public Node getNodeDecorator(Session session, Node node) {
        // TODO Auto-generated method stub
        return null;
    }

    /** {@inheritDoc} */
    public Property getPropertyDecorator(Session session, Property property) {
        // TODO Auto-generated method stub
        return null;
    }

    /** {@inheritDoc} */
    public Item getItemDecorator(Session session, Item item) {
        // TODO Auto-generated method stub
        return null;
    }

}
