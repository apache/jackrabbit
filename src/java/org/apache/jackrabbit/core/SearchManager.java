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
package org.apache.jackrabbit.core;

import org.apache.commons.collections.BeanMap;
import org.apache.jackrabbit.core.config.SearchConfig;
import org.apache.jackrabbit.core.fs.FileSystem;
import org.apache.jackrabbit.core.fs.FileSystemException;
import org.apache.jackrabbit.core.observation.EventImpl;
import org.apache.jackrabbit.core.observation.SynchronousEventListener;
import org.apache.jackrabbit.core.query.QueryHandler;
import org.apache.jackrabbit.core.query.QueryImpl;
import org.apache.jackrabbit.core.query.QueryHandlerContext;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.ItemStateManager;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.log4j.Logger;

import javax.jcr.NamespaceException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;
import javax.jcr.Node;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;

/**
 * Acts as a global entry point to execute queries and index nodes.
 */
public class SearchManager implements SynchronousEventListener {

    /**
     * Logger instance for this class
     */
    private static final Logger log = Logger.getLogger(SearchManager.class);

    /**
     * Namespace URI for xpath functions
     */
    // @todo this is not final! What should we use?
    private static final String NS_FN_PREFIX = "fn";
    public static final String NS_FN_URI = "http://www.w3.org/2004/10/xpath-functions";

    /**
     * Namespace URI for XML schema
     */
    private static final String NS_XS_PREFIX = "xs";
    public static final String NS_XS_URI = "http://www.w3.org/2001/XMLSchema";

    /**
     * The shared item state manager instance for the workspace.
     */
    private final ItemStateManager itemMgr;

    /**
     * Storage for search index
     */
    private final FileSystem fs;

    /**
     * QueryHandler where query execution is delegated to
     */
    private final QueryHandler handler;

    /**
     * Creates a new <code>SearchManager</code>.
     * @param session the system session.
     * @param config the search configuration.
     * @param ntReg the node type registry.
     * @param itemMgr the shared item state manager.
     * @throws RepositoryException
     */
    public SearchManager(SessionImpl session,
                         SearchConfig config,
                         NodeTypeRegistry ntReg,
                         ItemStateManager itemMgr) throws RepositoryException {
        this.fs = config.getFileSystem();
        this.itemMgr = itemMgr;
        // register namespaces
        NamespaceRegistry nsReg = session.getWorkspace().getNamespaceRegistry();
        try {
            nsReg.getPrefix(NS_XS_URI);
        } catch (NamespaceException e) {
            // not yet known
            nsReg.registerNamespace(NS_XS_PREFIX, NS_XS_URI);
        }
        try {
            nsReg.getPrefix(NS_FN_URI);
        } catch (RepositoryException e) {
            // not yet known
            nsReg.registerNamespace(NS_FN_PREFIX, NS_FN_URI);
        }

        // initialize query handler
        try {
            handler = (QueryHandler) config.newInstance();
            NodeId rootId = (NodeId) session.getHierarchyManager().resolvePath(Path.ROOT);
            QueryHandlerContext context
                    = new QueryHandlerContext(fs, itemMgr, rootId.getUUID(), ntReg);
            handler.init(context);
        } catch (Exception e) {
            throw new RepositoryException(e.getMessage(), e);
        }

        // set properties
        BeanMap bm = new BeanMap(handler);
        try {
            bm.putAll(config.getParameters());
        } catch (IllegalArgumentException e) {
            log.error("Invalid configuration: " + e.getMessage());
        }
    }

    /**
     * Adds a <code>Node</code> to the search index.
     *
     * @param node the NodeState to add.
     * @throws RepositoryException if an error occurs while indexing the node.
     * @throws IOException         if an error occurs while adding the node to the index.
     */
    public void addNode(NodeState node)
            throws RepositoryException, IOException {
        handler.addNode(node);
    }

    /**
     * Deletes the Node with <code>UUID</code> from the search index.
     *
     * @param uuid the <code>UUID</code> of the node to delete.
     * @throws IOException if an error occurs while deleting the node.
     */
    public void deleteNode(String uuid) throws IOException {
        handler.deleteNode(uuid);
    }

    /**
     * Closes this <code>SearchManager</code> and also closes the
     * {@link FileSystem} configured in {@link SearchConfig}.
     */
    public void close() {
        try {
            handler.close();
            fs.close();
        } catch (IOException e) {
            log.error("Exception closing QueryHandler.", e);
        } catch (FileSystemException e) {
            log.error("Exception closing FileSystem.", e);
        }
    }

    /**
     * Creates a query object that can be executed on the workspace.
     *
     * @param session   the session of the user executing the query.
     * @param itemMgr   the item manager of the user executing the query. Needed
     *                  to return <code>Node</code> instances in the result set.
     * @param statement the actual query statement.
     * @param language  the syntax of the query statement.
     * @return a <code>Query</code> instance to execute.
     * @throws InvalidQueryException if the query is malformed or the
     *                               <code>language</code> is unknown.
     * @throws RepositoryException   if any other error occurs.
     */
    public Query createQuery(SessionImpl session,
                             ItemManager itemMgr,
                             String statement,
                             String language)
            throws InvalidQueryException, RepositoryException {
        return new QueryImpl(session, itemMgr, handler, statement, language);
    }

    /**
     * Creates a query object from a node that can be executed on the workspace.
     *
     * @param session the session of the user executing the query.
     * @param itemMgr the item manager of the user executing the query. Needed
     *                to return <code>Node</code> instances in the result set.
     * @param node a node of type nt:query.
     * @return a <code>Query</code> instance to execute.
     * @throws InvalidQueryException if <code>absPath</code> is not a valid
     *                               persisted query (that is, a node of type nt:query)
     * @throws RepositoryException   if any other error occurs.
     */
    public Query createQuery(SessionImpl session,
                             ItemManager itemMgr,
                             Node node)
            throws InvalidQueryException, RepositoryException {
        return new QueryImpl(session, itemMgr, handler, node);
    }

    //---------------< EventListener interface >--------------------------------

    public void onEvent(EventIterator events) {
        log.debug("onEvent: indexing started");
        long time = System.currentTimeMillis();

        // nodes that need to be removed from the index.
        Set removedNodes = new HashSet();
        // nodes that need to be added to the index.
        Set addedNodes = new HashSet();
        // property events
        List propEvents = new ArrayList();

        while (events.hasNext()) {
            EventImpl e = (EventImpl) events.nextEvent();
            long type = e.getType();
            if (type == Event.NODE_ADDED) {
                addedNodes.add(e.getChildUUID());
            } else if (type == Event.NODE_REMOVED) {
                removedNodes.add(e.getChildUUID());
            } else {
                propEvents.add(e);
            }
        }

        // sort out property events
        for (int i = 0; i < propEvents.size(); i++) {
            EventImpl event = (EventImpl) propEvents.get(i);
            String nodeUUID = event.getParentUUID();
            if (event.getType() == Event.PROPERTY_ADDED) {
                if (addedNodes.add(nodeUUID)) {
                    // only property added
                    // need to re-index
                    removedNodes.add(nodeUUID);
                } else {
                    // the node where this prop belongs to is also new
                }
            } else if (event.getType() == Event.PROPERTY_CHANGED) {
                // need to re-index
                addedNodes.add(nodeUUID);
                removedNodes.add(nodeUUID);
            } else {
                // property removed event is only generated when node still exists
                addedNodes.add(nodeUUID);
                removedNodes.add(nodeUUID);
            }
        }

        for (Iterator it = removedNodes.iterator(); it.hasNext();) {
            try {
                deleteNode((String) it.next());
            } catch (IOException e) {
                log.error("Error deleting node from index.", e);
            }
        }
        for (Iterator it = addedNodes.iterator(); it.hasNext();) {
            try {
                addNode((NodeState) itemMgr.getItemState(new NodeId((String) it.next())));
            } catch (ItemStateException e) {
                log.error("Error indexing node.", e);
            } catch (RepositoryException e) {
                log.error("Error indexing node.", e);
            } catch (IOException e) {
                log.error("Error indexing node.", e);
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("onEvent: indexing finished in "
                    + String.valueOf(System.currentTimeMillis() - time)
                    + " ms.");
        }
    }

}
