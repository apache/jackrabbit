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
package org.apache.jackrabbit.core.query;

import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.NoSuchItemStateException;
import org.apache.jackrabbit.core.state.ChildNodeEntry;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.conversion.DefaultNamePathResolver;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import javax.jcr.RepositoryException;
import java.util.Map;
import java.util.HashMap;

/**
 * <code>OnWorkspaceInconsistency</code> defines an interface to handle
 * workspace inconsistencies.
 */
public abstract class OnWorkspaceInconsistency {

    /**
     * Logger instance for this class.
     */
    private static final Logger log = LoggerFactory.getLogger(OnWorkspaceInconsistency.class);

    /**
     * An handler that simply logs the path of the parent node and the name
     * of the missing child node and then re-throws the exception.
     */
    public static final OnWorkspaceInconsistency FAIL = new OnWorkspaceInconsistency("fail") {

        public void handleMissingChildNode(NoSuchItemStateException exception,
                                           QueryHandler handler,
                                           Path path,
                                           NodeState node,
                                           ChildNodeEntry child)
                throws RepositoryException, ItemStateException {
            NamePathResolver resolver = new DefaultNamePathResolver(
                    handler.getContext().getNamespaceRegistry());
            log.error("Node {} ({}) has missing child '{}' ({})",
                    new Object[]{
                        resolver.getJCRPath(path),
                        node.getNodeId(),
                        resolver.getJCRName(child.getName()),
                        child.getId()
                    });
            throw exception;
        }
    };

    /**
     * An handler that simply logs the path of the parent node and the name
     * of the missing child node
     */
    public static final OnWorkspaceInconsistency LOG = new OnWorkspaceInconsistency("log") {

        public void handleMissingChildNode(NoSuchItemStateException exception,
                                           QueryHandler handler,
                                           Path path,
                                           NodeState node,
                                           ChildNodeEntry child)
                throws RepositoryException, ItemStateException {
            NamePathResolver resolver = new DefaultNamePathResolver(
                    handler.getContext().getNamespaceRegistry());
            log.error("Node {} ({}) has missing child '{}' ({}). Please run a consistency check on this workspace!",
                    new Object[]{
                        resolver.getJCRPath(path),
                        node.getNodeId(),
                        resolver.getJCRName(child.getName()),
                        child.getId()
                    });
        }
    };

    protected static final Map<String, OnWorkspaceInconsistency> INSTANCES
            = new HashMap<String, OnWorkspaceInconsistency>();

    static {
        INSTANCES.put(FAIL.name, FAIL);
        INSTANCES.put(LOG.name, LOG);
    }

    /**
     * The name of the {@link OnWorkspaceInconsistency} handler.
     */
    private final String name;

    /**
     * Protected constructor.
     *
     * @param name a unique name for this handler.
     */
    protected OnWorkspaceInconsistency(String name) {
        this.name = name;
    }

    /**
     * @return the name of this {@link OnWorkspaceInconsistency}.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the {@link OnWorkspaceInconsistency} with the given
     * <code>name</code>.
     *
     * @param name the name of a {@link OnWorkspaceInconsistency}.
     * @return the {@link OnWorkspaceInconsistency} with the given
     *         <code>name</code>.
     * @throws IllegalArgumentException if <code>name</code> is not a well-known
     *                                  {@link OnWorkspaceInconsistency} name.
     */
    public static OnWorkspaceInconsistency fromString(String name)
            throws IllegalArgumentException {
        OnWorkspaceInconsistency handler = INSTANCES.get(name.toLowerCase());
        if (handler == null) {
            throw new IllegalArgumentException("Unknown name: " + name);
        } else {
            return handler;
        }
    }

    /**
     * Handle a missing child node state.
     *
     * @param exception the exception that was thrown when the query handler
     *                  tried to load the child node state.
     * @param handler   the query handler.
     * @param path      the path of the parent node.
     * @param node      the parent node state.
     * @param child     the child node entry, for which no node state could be
     *                  found.
     * @throws ItemStateException  if an error occurs while handling the missing
     *                             child node state. This may also be the passed
     *                             <code>exception</code> instance.
     * @throws RepositoryException if another error occurs not related to item
     *                             state reading.
     */
    public abstract void handleMissingChildNode(NoSuchItemStateException exception,
                                                QueryHandler handler,
                                                Path path,
                                                NodeState node,
                                                ChildNodeEntry child)
            throws ItemStateException, RepositoryException;

    /**
     * Logs a generic workspace inconsistency error.
     *
     * @param exception the exception that was thrown when working on the workspace
     * @param handler   the query handler.
     * @param path      the path of the parent node.
     * @param node      the parent node state.
     * @param child     the child node entry, for which no node state could be
     *                  found.
     * @throws RepositoryException if another error occurs not related to item
     *                             state reading.
     */
    public void logError(ItemStateException exception, QueryHandler handler,
            Path path, NodeState node, ChildNodeEntry child)
            throws RepositoryException {
        if (log.isErrorEnabled()) {
            NamePathResolver resolver = new DefaultNamePathResolver(handler
                    .getContext().getNamespaceRegistry());
            StringBuilder err = new StringBuilder();
            err.append("Workspace inconsistency error on node ");
            err.append(resolver.getJCRPath(path));
            err.append(" (");
            err.append(node.getNodeId());
            err.append(") with child ");
            err.append(resolver.getJCRName(child.getName()));
            err.append(" (");
            err.append(child.getId());
            err.append(").");
            log.error(err.toString(), exception);
        }
    }
}
