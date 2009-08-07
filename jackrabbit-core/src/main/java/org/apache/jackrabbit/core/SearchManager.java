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
package org.apache.jackrabbit.core;

import org.apache.jackrabbit.core.config.SearchConfig;
import org.apache.jackrabbit.core.fs.FileSystem;
import org.apache.jackrabbit.core.fs.FileSystemException;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.observation.EventImpl;
import org.apache.jackrabbit.core.observation.SynchronousEventListener;
import org.apache.jackrabbit.core.query.AbstractQueryImpl;
import org.apache.jackrabbit.core.query.QueryHandler;
import org.apache.jackrabbit.core.query.QueryHandlerContext;
import org.apache.jackrabbit.core.query.QueryObjectModelImpl;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.NodeStateIterator;
import org.apache.jackrabbit.core.state.SharedItemStateManager;
import org.apache.jackrabbit.core.persistence.PersistenceManager;
import org.apache.jackrabbit.spi.commons.conversion.MalformedPathException;
import org.apache.jackrabbit.spi.commons.query.jsr283.qom.QueryObjectModel;
import org.apache.jackrabbit.spi.commons.query.qom.QueryObjectModelTree;
import org.apache.jackrabbit.spi.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.NamespaceException;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;

/**
 * Acts as a global entry point to execute queries and index nodes.
 */
public class SearchManager implements SynchronousEventListener {

    /**
     * Logger instance for this class
     */
    private static final Logger log = LoggerFactory.getLogger(SearchManager.class);

    /**
     * Namespace URI for xpath functions
     */
    private static final String NS_FN_PREFIX = "fn";
    public static final String NS_FN_URI = "http://www.w3.org/2005/xpath-functions";

    /**
     * Deprecated namespace URI for xpath functions
     */
    private static final String NS_FN_OLD_PREFIX = "fn_old";
    public static final String NS_FN_OLD_URI = "http://www.w3.org/2004/10/xpath-functions";

    /**
     * Namespace URI for XML schema
     */
    private static final String NS_XS_PREFIX = "xs";
    public static final String NS_XS_URI = "http://www.w3.org/2001/XMLSchema";

    /**
     * Name of the parameter that indicates the query implementation class.
     */
    private static final String PARAM_QUERY_IMPL = "queryClass";

    /**
     * The search configuration.
     */
    private final SearchConfig config;

    /**
     * The node type registry.
     */
    private final NodeTypeRegistry ntReg;

    /**
     * The shared item state manager instance for the workspace.
     */
    private final SharedItemStateManager itemMgr;

    /**
     * The underlying persistence manager.
     */
    private final PersistenceManager pm;

    /**
     * Storage for search index
     */
    private final FileSystem fs;

    /**
     * The root node for this search manager.
     */
    private final NodeId rootNodeId;

    /**
     * QueryHandler where query execution is delegated to
     */
    private QueryHandler handler;

    /**
     * QueryHandler of the parent search manager or <code>null</code> if there
     * is none.
     */
    private final QueryHandler parentHandler;

    /**
     * The namespace registry of the repository.
     */
    private final NamespaceRegistryImpl nsReg;

    /**
     * ID of the node that should be excluded from indexing or <code>null</code>
     * if no node should be excluded.
     */
    private final NodeId excludedNodeId;

    /**
     * Path that will be excluded from indexing.
     */
    private Path excludePath;

    /**
     * Creates a new <code>SearchManager</code>.
     *
     * @param config         the search configuration.
     * @param nsReg          the namespace registry.
     * @param ntReg          the node type registry.
     * @param itemMgr        the shared item state manager.
     * @param pm             the underlying persistence manager.
     * @param rootNodeId     the id of the root node.
     * @param parentMgr      the parent search manager or <code>null</code> if
     *                       there is no parent search manager.
     * @param excludedNodeId id of the node that should be excluded from
     *                       indexing. Any descendant of that node will also be
     *                       excluded from indexing.
     * @throws RepositoryException if the search manager cannot be initialized
     */
    public SearchManager(SearchConfig config,
                         final NamespaceRegistryImpl nsReg,
                         NodeTypeRegistry ntReg,
                         SharedItemStateManager itemMgr,
                         PersistenceManager pm,
                         NodeId rootNodeId,
                         SearchManager parentMgr,
                         NodeId excludedNodeId) throws RepositoryException {
        this.fs = config.getFileSystem();
        this.config = config;
        this.ntReg = ntReg;
        this.nsReg = nsReg;
        this.itemMgr = itemMgr;
        this.pm = pm;
        this.rootNodeId = rootNodeId;
        this.parentHandler = (parentMgr != null) ? parentMgr.handler : null;
        this.excludedNodeId = excludedNodeId;

        // register namespaces
        safeRegisterNamespace(NS_XS_PREFIX, NS_XS_URI);
        try {
            if (nsReg.getPrefix(NS_FN_OLD_URI).equals(NS_FN_PREFIX)) {
                // old uri is mapped to 'fn' prefix -> re-map
                String prefix = NS_FN_OLD_PREFIX;
                try {
                    // Find a free prefix
                    for (int i = 2; true; i++) {
                        nsReg.getURI(prefix);
                        prefix = NS_FN_OLD_PREFIX + i;
                    }
                } catch (NamespaceException e) {
                    // Re-map the old fn URI to that prefix
                    nsReg.registerNamespace(prefix, NS_FN_OLD_URI);
                }
            }
        } catch (NamespaceException e) {
            // does not yet exist
            safeRegisterNamespace(NS_FN_OLD_PREFIX, NS_FN_OLD_URI);
        }
        // at this point the 'fn' prefix shouldn't be assigned anymore
        safeRegisterNamespace(NS_FN_PREFIX, NS_FN_URI);

        if (excludedNodeId != null) {
            HierarchyManagerImpl hmgr =
                new HierarchyManagerImpl(rootNodeId, itemMgr);
            excludePath = hmgr.getPath(excludedNodeId);
        }

        // initialize query handler
        initializeQueryHandler();
    }

    /**
     * Registers a namespace using the given prefix hint. Does nothing
     * if the namespace is already registered. If the given prefix hint
     * is not yet registered as a prefix, then it is used as the prefix
     * of the registered namespace. Otherwise a unique prefix is generated
     * based on the given hint.
     *
     * @param prefixHint the prefix hint
     * @param uri the namespace URI
     * @throws NamespaceException if an illegal attempt is made to register
     *                            a mapping
     * @throws RepositoryException if an unexpected error occurs
     * @see javax.jcr.NamespaceRegistry#registerNamespace(String, String)
     */
    private void safeRegisterNamespace(String prefixHint, String uri)
            throws NamespaceException, RepositoryException {
        try {
            // Check if the namespace is already registered
            nsReg.getPrefix(uri);
            // ... it is, so do nothing.
        } catch (NamespaceException e1) {
            // ... it is not, try to find a unique prefix.
            String prefix = prefixHint;
            try {
                for (int suffix = 2; true; suffix++) {
                    // Is this prefix already registered?
                    nsReg.getURI(prefix);
                    // ... it is, generate a new prefix and try again.
                    prefix = prefixHint + suffix;
                }
            } catch (NamespaceException e2) {
                // ... it is not, register the namespace with this prefix.
                nsReg.registerNamespace(prefix, uri);
            }
        }
    }

    /**
     * Closes this <code>SearchManager</code> and also closes the
     * {@link FileSystem} configured in {@link SearchConfig}.
     */
    public void close() {
        try {
            shutdownQueryHandler();

            if (fs != null) {
                fs.close();
            }
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
        AbstractQueryImpl query = createQueryInstance();
        query.init(session, itemMgr, handler, statement, language);
        return query;
    }

    /**
     * Creates a query object model that can be executed on the workspace.
     *
     * @param session   the session of the user executing the query.
     * @param qomTree   the query object model tree, representing the query.
     * @param langugage the original language of the query statement.
     * @return the query object model for the query.
     * @throws InvalidQueryException the the query object model tree is
     *                               considered invalid by the query handler
     *                               implementation.
     * @throws RepositoryException   if any other error occurs.
     */
    public QueryObjectModel createQueryObjectModel(SessionImpl session,
                                                   QueryObjectModelTree qomTree,
                                                   String langugage)
            throws InvalidQueryException, RepositoryException {
        QueryObjectModelImpl qom = new QueryObjectModelImpl();
        qom.init(session, session.getItemManager(), handler, qomTree, langugage);
        return qom;
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
        AbstractQueryImpl query = createQueryInstance();
        query.init(session, itemMgr, handler, node);
        return query;
    }

    /**
     * Checks if the given event should be excluded based on the
     * {@link #excludePath} setting.
     *
     * @param event observation event
     * @return <code>true</code> if the event should be excluded,
     *         <code>false</code> otherwise
     */
    private boolean isExcluded(EventImpl event) {
        try {
            return excludePath != null
                && excludePath.isAncestorOf(event.getQPath());
        } catch (MalformedPathException ex) {
            log.error("Error filtering events.", ex);
            return false;
        } catch (RepositoryException ex) {
            log.error("Error filtering events.", ex);
            return false;
        }

    }

    //------------------------< for testing only >------------------------------

    /**
     * @return the query handler implementation.
     */
    public QueryHandler getQueryHandler() {
        return handler;
    }

    //---------------< EventListener interface >--------------------------------

    public void onEvent(EventIterator events) {
        log.debug("onEvent: indexing started");
        long time = System.currentTimeMillis();

        // nodes that need to be removed from the index.
        final Set removedNodes = new HashSet();
        // nodes that need to be added to the index.
        final Map addedNodes = new HashMap();
        // property events
        List propEvents = new ArrayList();

        while (events.hasNext()) {
            EventImpl e = (EventImpl) events.nextEvent();
            if (!isExcluded(e)) {
                long type = e.getType();
                if (type == Event.NODE_ADDED) {
                    addedNodes.put(e.getChildId(), e);
                    // quick'n dirty fix for JCR-905
                    if (e.isExternal()) {
                        removedNodes.add(e.getChildId());
                    }
                    if (e.isShareableChildNode()) {
                        // simply re-index shareable nodes
                        removedNodes.add(e.getChildId());
                    }
                } else if (type == Event.NODE_REMOVED) {
                    removedNodes.add(e.getChildId());
                    if (e.isShareableChildNode()) {
                        // check if there is a node remaining in the shared set
                        if (itemMgr.hasItemState(e.getChildId())) {
                            addedNodes.put(e.getChildId(), e);
                        }
                    }
                } else {
                    propEvents.add(e);
                }
            }
        }

        // sort out property events
        for (int i = 0; i < propEvents.size(); i++) {
            EventImpl e = (EventImpl) propEvents.get(i);
            NodeId nodeId = e.getParentId();
            if (e.getType() == Event.PROPERTY_ADDED) {
                if (addedNodes.put(nodeId, e) == null) {
                    // only property added
                    // need to re-index
                    removedNodes.add(nodeId);
                } else {
                    // the node where this prop belongs to is also new
                }
            } else if (e.getType() == Event.PROPERTY_CHANGED) {
                // need to re-index
                addedNodes.put(nodeId, e);
                removedNodes.add(nodeId);
            } else {
                // property removed event is only generated when node still exists
                addedNodes.put(nodeId, e);
                removedNodes.add(nodeId);
            }
        }

        NodeStateIterator addedStates = new NodeStateIterator() {
            private final Iterator iter = addedNodes.keySet().iterator();

            public void remove() {
                throw new UnsupportedOperationException();
            }

            public boolean hasNext() {
                return iter.hasNext();
            }

            public Object next() {
                return nextNodeState();
            }

            public NodeState nextNodeState() {
                NodeState item = null;
                NodeId id = (NodeId) iter.next();
                try {
                    item = (NodeState) itemMgr.getItemState(id);
                } catch (ItemStateException ise) {
                    // check whether this item state change originated from
                    // an external event
                    EventImpl e = (EventImpl) addedNodes.get(id);
                    if (e == null || !e.isExternal()) {
                        log.error("Unable to index node " + id + ": does not exist");
                    } else {
                        log.info("Node no longer available " + id + ", skipped.");
                    }
                }
                return item;
            }
        };
        NodeIdIterator removedIds = new NodeIdIterator() {
            private final Iterator iter = removedNodes.iterator();

            public NodeId nextNodeId() throws NoSuchElementException {
                return (NodeId) iter.next();
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }

            public boolean hasNext() {
                return iter.hasNext();
            }

            public Object next() {
                return nextNodeId();
            }
        };

        if (removedNodes.size() > 0 || addedNodes.size() > 0) {
            try {
                handler.updateNodes(removedIds, addedStates);
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

    /**
     * Creates a new instance of an {@link AbstractQueryImpl} which is not
     * initialized.
     *
     * @return an new query instance.
     * @throws RepositoryException if an error occurs while creating a new query
     *                             instance.
     */
    protected AbstractQueryImpl createQueryInstance() throws RepositoryException {
        try {
            String queryImplClassName = handler.getQueryClass();
            Object obj = Class.forName(queryImplClassName).newInstance();
            if (obj instanceof AbstractQueryImpl) {
                return (AbstractQueryImpl) obj;
            } else {
                throw new IllegalArgumentException(queryImplClassName
                        + " is not of type " + AbstractQueryImpl.class.getName());
            }
        } catch (Throwable t) {
            throw new RepositoryException("Unable to create query: " + t.toString(), t);
        }
    }

    //------------------------< internal >--------------------------------------

    /**
     * Initializes the query handler.
     *
     * @throws RepositoryException if the query handler cannot be initialized.
     */
    private void initializeQueryHandler() throws RepositoryException {
        // initialize query handler
        try {
            handler = (QueryHandler) config.newInstance();
            QueryHandlerContext context
                    = new QueryHandlerContext(fs, itemMgr, pm, rootNodeId,
                            ntReg, nsReg, parentHandler, excludedNodeId);
            handler.init(context);
        } catch (Exception e) {
            throw new RepositoryException(e.getMessage(), e);
        }
    }

    /**
     * Shuts down the query handler. If the query handler is already shut down
     * this method does nothing.
     *
     * @throws IOException if an error occurs while shutting down the query
     *                     handler.
     */
    private void shutdownQueryHandler() throws IOException {
        if (handler != null) {
            handler.close();
            handler = null;
        }
    }
}
