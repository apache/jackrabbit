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

import org.apache.jackrabbit.core.fs.FileSystem;
import org.apache.jackrabbit.core.fs.FileSystemException;
import org.apache.jackrabbit.core.observation.EventImpl;
import org.apache.jackrabbit.core.observation.SynchronousEventListener;
import org.apache.jackrabbit.core.search.QueryHandler;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.config.SearchConfig;
import org.apache.log4j.Logger;
import org.apache.commons.collections.BeanMap;

import javax.jcr.RepositoryException;
import javax.jcr.NamespaceException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.ItemNotFoundException;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.Event;
import java.io.IOException;
import java.util.*;

/**
 * Acts as a global entry point to execute queries and index nodes.
 */
public class SearchManager implements SynchronousEventListener {

    /** Logger instance for this class */
    private static final Logger log = Logger.getLogger(SearchManager.class);

    /** Namespace URI for xpath functions */
    // @todo this is not final! What should we use?
    private static final String NS_FN_PREFIX = "fn";
    public static final String NS_FN_URI = "http://www.w3.org/2004/10/xpath-functions";

    /** Namespace URI for XML schema */
    private static final String NS_XS_PREFIX = "xs";
    public static final String NS_XS_URI = "http://www.w3.org/2001/XMLSchema";

    /** HierarchyManager for path resolution */
    private final HierarchyManager hmgr;

    /** Session for accessing Nodes */
    private final SessionImpl session;

    /** Storage for search index */
    private final FileSystem fs;

    /** QueryHandler where query execution is delegated to */
    private final QueryHandler handler;

    public SearchManager(SessionImpl session, SearchConfig config)
            throws RepositoryException {
        this.session = session;
        this.hmgr = session.getHierarchyManager();
        this.fs = config.getFileSystem();

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
            Class handlerClass = Class.forName(config.getHandlerClassName());
            handler = (QueryHandler) handlerClass.newInstance();
            handler.init(fs, session.getItemStateManager());
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
     * @param node the NodeState to add.
     * @param path the path of the node.
     * @throws RepositoryException if an error occurs while indexing the node.
     * @throws IOException if an error occurs while adding the node to the index.
     */
    public void addNode(NodeState node, Path path)
            throws RepositoryException, IOException {
        if (log.isDebugEnabled()) {
            log.debug("add node to index: " + path);
        }
        handler.addNode(node);
    }

    /**
     * Deletes the Node with <code>UUID</code> from the search index.
     * @param path the path of the node to delete.
     * @param uuid the <code>UUID</code> of the node to delete.
     * @throws IOException if an error occurs while deleting the node.
     */
    public void deleteNode(Path path, String uuid) throws IOException {
        if (log.isDebugEnabled()) {
            log.debug("remove node from index: " + path.toString());
        }
        handler.deleteNode(uuid);
    }

    /**
     * Closes this <code>SearchManager</code> and also closes the
     * {@link org.apache.jackrabbit.core.fs.FileSystem} configured in
     * {@link org.apache.jackrabbit.core.config.SearchConfig}.
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
     * @param session the session of the user executing the query.
     * @param itemMgr the item manager of the user executing the query. Needed
     *   to return <code>Node</code> instances in the result set.
     * @param statement the actual query statement.
     * @param language the syntax of the query statement.
     * @return a <code>Query</code> instance to execute.
     *
     * @throws InvalidQueryException if the query is malformed or the
     *   <code>language</code> is unknown.
     * @throws RepositoryException if any other error occurs.
     */
    public Query createQuery(SessionImpl session,
                             ItemManager itemMgr,
                             String statement,
                             String language)
            throws InvalidQueryException, RepositoryException {
        return handler.createQuery(session, itemMgr, statement, language);
    }
    
    /**
     * Creates a query object from a node that can be executed on the workspace.
     *
     * @param session the session of the user executing the query.
     * @param itemMgr the item manager of the user executing the query. Needed
     *   to return <code>Node</code> instances in the result set.
     * @param absPath absolute path to a node of type nt:query.
     * @return a <code>Query</code> instance to execute.
     *
     * @throws InvalidQueryException if <code>absPath</code> is not a valid
     *   persisted query (that is, a node of type nt:query)
     * @throws ItemNotFoundException if there is no node at <code>absPath</code>.
     * @throws RepositoryException if any other error occurs.
     */
    public Query createQuery(SessionImpl session,
                             ItemManager itemMgr,
                             String absPath)
            throws InvalidQueryException, ItemNotFoundException, RepositoryException {
        return handler.createQuery(session, itemMgr, absPath);
    }

    //---------------< EventListener interface >--------------------------------

    public void onEvent(EventIterator events) {
        Set modified = new HashSet();
        Set added = new HashSet();
        log.debug("onEvent: indexing started");
        long time = System.currentTimeMillis();

        // remember nodes we have to index at the end.
        Set pendingNodes = new HashSet();

        // delete removed and modified nodes from index
        while (events.hasNext()) {
            try {
                EventImpl e = (EventImpl) events.nextEvent();
                long type = e.getType();
                if (type == Event.NODE_ADDED) {

                    // @todo use UUIDs for pending nodes?
                    Path path = Path.create(e.getPath(),
                            session.getNamespaceResolver(),
                            true);
                    pendingNodes.add(path);
                    added.add(e.getChildUUID());
                } else if (type == Event.NODE_REMOVED) {

                    Path path = Path.create(e.getPath(),
                            session.getNamespaceResolver(),
                            true);
                    deleteNode(path, e.getChildUUID());

                } else if (type == Event.PROPERTY_ADDED
                        || type == Event.PROPERTY_CHANGED
                        || type == Event.PROPERTY_REMOVED) {

                    Path path = Path.create(e.getPath(),
                            session.getNamespaceResolver(),
                            true).getAncestor(1);

                    if (type == Event.PROPERTY_ADDED) {
                        // do not delete and re-add if associated node got added too
                        if (!added.contains(e.getParentUUID())) {
                            deleteNode(path, e.getParentUUID());
                            modified.add(e.getParentUUID());
                            pendingNodes.add(path);
                        }
                    } else {
                        if (!modified.contains(e.getParentUUID())) {
                            deleteNode(path, e.getParentUUID());
                            modified.add(e.getParentUUID());
                            pendingNodes.add(path);
                        } else {
                            // already deleted
                        }
                    }

                }
            } catch (MalformedPathException e) {
                log.error("error indexing node.", e);
            } catch (RepositoryException e) {
                log.error("error indexing node.", e);
            } catch (IOException e) {
                log.error("error indexing node.", e);
            }
        }

        for (Iterator it = pendingNodes.iterator(); it.hasNext();) {
            try {
                Path path = (Path) it.next();
                ItemId id = hmgr.resolvePath(path);
                addNode((NodeState) session.getItemStateManager().getItemState(id), path);
            } catch (ItemStateException e) {
                log.error("error indexing node.", e);
            } catch (RepositoryException e) {
                log.error("error indexing node.", e);
            } catch (IOException e) {
                log.error("error indexing node.", e);
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("onEvent: indexing finished in "
                    + String.valueOf(System.currentTimeMillis() - time)
                    + " ms.");
        }
    }

}
