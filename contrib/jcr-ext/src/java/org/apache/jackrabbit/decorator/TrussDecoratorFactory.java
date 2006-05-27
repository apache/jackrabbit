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

import org.apache.log4j.Logger;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.NodeIterator;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;

/**
 * Decorator implementation that logs all calls to:
 * <ul>
 * <li>{@link javax.jcr.Node#getNode(String)}</li>
 * <li>{@link javax.jcr.Node#getNodes()}</li>
 * <li>{@link javax.jcr.Node#getNodes(String)}</li>
 * </ul>
 */
public class TrussDecoratorFactory extends DefaultDecoratorFactory {

    private static final Logger log = Logger.getLogger(TrussDecoratorFactory.class);

    /**
     * @inheritDoc
     */
    public Node getNodeDecorator(Session session, Node node) {
        if (node instanceof Version) {
            return getVersionDecorator(session, (Version) node);
        } else if (node instanceof VersionHistory) {
            return getVersionHistoryDecorator(session, (VersionHistory) node);
        } else {
            return new TrussNodeDecorator(this, session, node);
        }
    }

    /**
     * Node decorator that logs calls to {@link #getNode(String)},
     * {@link #getNodes()} and {@link #getNodes(String)}.
     */
    private static final class TrussNodeDecorator extends NodeDecorator {

        public TrussNodeDecorator(DecoratorFactory factory, Session session, Node node) {
            super(factory, session, node);
        }

        /**
         * @inheritDoc
         */
        public Node getNode(String relPath)
                throws PathNotFoundException, RepositoryException {
            log.info("<" + node.getPath() + ">.getNode(" + relPath + ")");
            return super.getNode(relPath);
        }

        /**
         * @inheritDoc
         */
        public NodeIterator getNodes() throws RepositoryException {
            log.info("<" + node.getPath() + ">.getNodes()");
            return super.getNodes();
        }

        /**
         * @inheritDoc
         */
        public NodeIterator getNodes(String namePattern)
                throws RepositoryException {
            log.info("<" + node.getPath() + ">.getNodes(" + namePattern + ")");
            return super.getNodes(namePattern);
        }
    }
}
