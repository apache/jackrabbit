/*
 * Copyright 2004 The Apache Software Foundation.
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
import org.apache.jackrabbit.core.fs.FileSystemResource;
import org.apache.jackrabbit.core.fs.FileSystemException;
import org.apache.jackrabbit.core.observation.EventImpl;
import org.apache.jackrabbit.core.observation.SynchronousEventListener;
import org.apache.jackrabbit.core.search.NamespaceMappings;
import org.apache.jackrabbit.core.search.OrderQueryNode;
import org.apache.jackrabbit.core.search.QueryRootNode;
import org.apache.jackrabbit.core.search.lucene.*;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.ItemStateProvider;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.config.SearchConfig;
import org.apache.log4j.Logger;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.Query;
import org.apache.commons.collections.BeanMap;

import javax.jcr.RepositoryException;
import javax.jcr.NamespaceException;
import javax.jcr.NamespaceRegistry;
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

    /** Name of the file to persist search internal namespace mappings */
    private static final String NS_MAPPING_FILE = "ns_mappings.properties";

    /** Namespace URI for xpath functions */
    // @todo this is not final! What should we use?
    private static final String NS_FN_PREFIX = "fn";
    public static final String NS_FN_URI = "http://www.w3.org/2004/10/xpath-functions";

    /** Namespace URI for XML schema */
    private static final String NS_XS_PREFIX = "xs";
    public static final String NS_XS_URI = "http://www.w3.org/2001/XMLSchema";

    /** The actual search index */
    private final SearchIndex index;

    /** State manager to retrieve content */
    private final ItemStateProvider stateProvider;

    /** HierarchyManager for path resolution */
    private final HierarchyManager hmgr;

    /** Session for accessing Nodes */
    private final SessionImpl session;

    /** Storage for search index */
    private final FileSystem fs;

    /** Namespace resolver for search internal prefixes */
    private final NamespaceMappings nsMappings;

    public SearchManager(SessionImpl session, SearchConfig config)
            throws RepositoryException, IOException {
        this.session = session;
        this.stateProvider = session.getItemStateManager();
        this.hmgr = session.getHierarchyManager();
        this.fs = config.getFileSystem();
        index = new SearchIndex(fs, new StandardAnalyzer());
        FileSystemResource mapFile = new FileSystemResource(fs, NS_MAPPING_FILE);
        nsMappings = new NamespaceMappings(mapFile);

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

        // set properties
        BeanMap bm = new BeanMap(this);
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
     * @throws IOException if an error occurs while adding the node to
     * the search index.
     */
    public void addNode(NodeState node, Path path) throws IOException {
        if (log.isDebugEnabled()) {
            log.debug("add node to index: " + path);
        }
        Document doc = NodeIndexer.createDocument(node, stateProvider, path, nsMappings);
        index.addDocument(doc);
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
        index.removeDocument(new Term(FieldNames.UUID, uuid));
    }

    /**
     * Closes this <code>SearchManager</code> and also closes the
     * {@link org.apache.jackrabbit.core.fs.FileSystem} passed in the
     * constructor of this <code>SearchManager</code>.
     */
    public void close() {
        index.close();
        try {
            fs.close();
        } catch (FileSystemException e) {
            log.error("Exception closing FileSystem.", e);
        }
    }

    public QueryResultImpl execute(ItemManager itemMgr,
                                   QueryRootNode root,
                                   SessionImpl session)
            throws RepositoryException {

        // build lucene query
        Query query = LuceneQueryBuilder.createQuery(root,
                session, nsMappings, index.getAnalyzer());

        OrderQueryNode orderNode = root.getOrderNode();
        // FIXME according to spec this should be descending
        // by default. this contrasts to standard sql semantics
        // where default is ascending.
        boolean[] orderSpecs = null;
        String[] orderProperties = null;
        if (orderNode != null) {
            orderProperties = orderNode.getOrderByProperties();
            orderSpecs = orderNode.getOrderBySpecs();
        } else {
            orderProperties = new String[0];
            orderSpecs = new boolean[0];
        }


        List uuids;
        AccessManagerImpl accessMgr = session.getAccessManager();

        // execute it
        try {
            Hits result = index.executeQuery(query, orderProperties, orderSpecs);
            uuids = new ArrayList(result.length());
            for (int i = 0; i < result.length(); i++) {
                String uuid = result.doc(i).get(FieldNames.UUID);
                // check access
                if (accessMgr.isGranted(new NodeId(uuid), AccessManager.READ)) {
                    uuids.add(uuid);
                }
            }
        } catch (IOException e) {
            uuids = Collections.EMPTY_LIST;
        }

        // return QueryResult
        return new QueryResultImpl(itemMgr,
                (String[]) uuids.toArray(new String[uuids.size()]),
                root.getSelectProperties());
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
                path = getIndexlessPath(path);
                addNode((NodeState) stateProvider.getItemState(id), path);
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

    //---------------------< properties >---------------------------------------

    public void setUseCompoundFile(boolean b) {
        index.setUseCompoundFile(b);
    }

    public void setMinMergeDocs(int minMergeDocs) {
        index.setMinMergeDocs(minMergeDocs);
    }

    public void setMaxMergeDocs(int maxMergeDocs) {
        index.setMaxMergeDocs(maxMergeDocs);
    }

    public void setMergeFactor(int mergeFactor) {
        index.setMergeFactor(mergeFactor);
    }

    //-----------------------< internal >---------------------------------------

    /**
     * Returns a <code>Path</code>, which contains the same sequence of path
     * elements as <code>p</code>, but has cut off any existing indexes on the
     * path elements.
     *
     * @param p the source path, possibly containing indexed path elements.
     * @return a <code>Path</code> without indexed path elements.
     */
    private Path getIndexlessPath(Path p) {
        boolean hasIndexes = false;
        Path.PathElement[] elements = p.getElements();
        for (int i = 0; i < elements.length && !hasIndexes; i++) {
            hasIndexes = (elements[i].getIndex() > 0);
        }

        if (hasIndexes) {
            // create Path without indexes
            Path.PathBuilder builder = new Path.PathBuilder();
            builder.addRoot();
            for (int i = 1; i < elements.length; i++) {
                builder.addLast(elements[i].getName());
            }
            try {
                return builder.getPath();
            } catch (MalformedPathException e) {
                // will never happen, because Path p is always valid
                log.error("internal error: malformed path.", e);
            }
        }
        // return original path if it does not contain indexed path elements
        return p;
    }
}
