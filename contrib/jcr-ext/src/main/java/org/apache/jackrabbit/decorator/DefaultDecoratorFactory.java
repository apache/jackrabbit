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
package org.apache.jackrabbit.decorator;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.ValueFactory;
import javax.jcr.ItemVisitor;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.jcr.query.QueryManager;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.lock.Lock;

/**
 * Default implementation of a {@link DecoratorFactory}. All decorator instances
 * simply wrap the original instance and forward the call to it.
 */
public class DefaultDecoratorFactory implements DecoratorFactory {

    /**
     * {@inheritDoc}
     */
    public Repository getRepositoryDecorator(Repository repository) {
        return new RepositoryDecorator(this, repository);
    }

    /**
     * {@inheritDoc}
     */
    public Session getSessionDecorator(Repository repository, Session session) {
        return new SessionDecorator(this, repository, session);
    }

    /**
     * {@inheritDoc}
     */
    public Workspace getWorkspaceDecorator(Session session, Workspace workspace) {
        return new WorkspaceDecorator(this, session, workspace);
    }

    /**
     * {@inheritDoc}
     */
    public Node getNodeDecorator(Session session, Node node) {
        if (node instanceof Version) {
            return getVersionDecorator(session, (Version) node);
        } else if (node instanceof VersionHistory) {
            return getVersionHistoryDecorator(session, (VersionHistory) node);
        } else {
            return new NodeDecorator(this, session, node);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Property getPropertyDecorator(Session session, Property property) {
        return new PropertyDecorator(this, session, property);
    }

    /**
     * {@inheritDoc}
     */
    public Lock getLockDecorator(Session session, Lock lock) {
        return new LockDecorator(this, session, lock);
    }

    /**
     * {@inheritDoc}
     */
    public Version getVersionDecorator(Session session, Version version) {
        return new VersionDecorator(this, session, version);
    }

    /**
     * {@inheritDoc}
     */
    public VersionHistory getVersionHistoryDecorator(Session session,
                                                     VersionHistory versionHistory) {
        return new VersionHistoryDecorator(this, session, versionHistory);
    }

    /**
     * {@inheritDoc}
     */
    public Item getItemDecorator(Session session, Item item) {
        if (item instanceof Version) {
            return getVersionDecorator(session, (Version) item);
        } else if (item instanceof VersionHistory) {
            return getVersionHistoryDecorator(session, (VersionHistory) item);
        } else if (item instanceof Node) {
            return getNodeDecorator(session, (Node) item);
        } else if (item instanceof Property) {
            return getPropertyDecorator(session, (Property) item);
        } else {
            return new ItemDecorator(this, session, item);
        }
    }

    /**
     * {@inheritDoc}
     */
    public QueryManager getQueryManagerDecorator(Session session,
                                                 QueryManager queryManager) {
        return new QueryManagerDecorator(this, session, queryManager);
    }

    /**
     * {@inheritDoc}
     */
    public Query getQueryDecorator(Session session, Query query) {
        return new QueryDecorator(this, session, query);
    }

    /**
     * {@inheritDoc}
     */
    public QueryResult getQueryResultDecorator(Session session,
                                               QueryResult result) {
        return new QueryResultDecorator(this, session, result);
    }

    /**
     * {@inheritDoc}
     */
    public ValueFactory getValueFactoryDecorator(Session session,
                                                 ValueFactory valueFactory) {
        return new ValueFactoryDecorator(this, session, valueFactory);
    }

    /**
     * {@inheritDoc}
     */
    public ItemVisitor getItemVisitorDecorator(Session session,
                                               ItemVisitor visitor) {
        return new ItemVisitorDecorator(this, session, visitor);
    }
}
