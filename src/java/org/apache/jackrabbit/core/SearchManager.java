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

import org.apache.jackrabbit.core.observation.EventImpl;
import org.apache.jackrabbit.core.observation.SynchronousEventListener;
import org.apache.jackrabbit.core.search.NamespaceMappings;
import org.apache.jackrabbit.core.search.OrderQueryNode;
import org.apache.jackrabbit.core.search.QueryRootNode;
import org.apache.jackrabbit.core.search.lucene.*;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.ItemStateProvider;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.log4j.Logger;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.Query;

import javax.jcr.RepositoryException;
import javax.jcr.access.Permission;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventType;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Acts as a global entry point to execute queries and index nodes.
 */
public class SearchManager implements SynchronousEventListener {

    private static final Logger log = Logger.getLogger(SearchManager.class);

    private final SearchIndex index;

    private final ItemStateProvider stateProvider;

    private final HierarchyManager hmgr;

    private final SessionImpl session;

    private final NamespaceMappings nsMappings;

    public SearchManager(ItemStateProvider stateProvider,
                         HierarchyManager hmgr,
                         SessionImpl session,
                         String indexPath) throws IOException {
        this.stateProvider = stateProvider;
        this.hmgr = hmgr;
        this.session = session;
        index = new SearchIndex(indexPath, new StandardAnalyzer());
        nsMappings = new NamespaceMappings(new File(indexPath, "ns_mappings.properties"));
    }

    public void addNode(NodeState node, Path path) throws IOException {
        // FIXME rather throw RepositoryException?
        log.debug("add node to index: " + path);
        Document doc = NodeIndexer.createDocument(node, stateProvider, path, nsMappings);
        index.addDocument(doc);
    }

    public void updateNode(NodeState node, Path path) throws IOException {
        log.debug("update index for node: " + path);
        deleteNode(path, node.getUUID());
        addNode(node, path);
    }

    public void deleteNode(Path path, String uuid) throws IOException {
        if (log.isDebugEnabled()) {
            log.debug("remove node from index: " + path.toString());
        }
        index.removeDocument(new Term(FieldNames.UUID, uuid));
    }

    public void close() {
        index.close();
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
        boolean ascending = true;
        String[] orderProperties = null;
        if (orderNode != null) {
            ascending = orderNode.isAscending();
            orderProperties = orderNode.getOrderByProperties();
        } else {
            orderProperties = new String[0];
        }


        List uuids;
        AccessManagerImpl accessMgr = session.getAccessManager();

        // execute it
        try {
            Hits result = index.executeQuery(query, orderProperties, ascending);
            uuids = new ArrayList(result.length());
            for (int i = 0; i < result.length(); i++) {
                String uuid = result.doc(i).get(FieldNames.UUID);
                // check access
                if (accessMgr.isGranted(new NodeId(uuid), Permission.READ_ITEM)) {
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

        // FIXME optimize operations on index.
        // only one cycle of document removes and document adds

        while (events.hasNext()) {
            try {
                EventImpl e = (EventImpl) events.nextEvent();
                long type = e.getType();
                if (type == EventType.CHILD_NODE_ADDED) {

                    Path path = Path.create(e.getNodePath() + ((e.getNodePath().length() > 1) ? "/" : "") + e.getChildName(),
                            session.getNamespaceResolver(),
                            true);

                    path = getIndexlessPath(path);

                    ItemId id = new NodeId(e.getChildUUID());
                    addNode((NodeState) stateProvider.getItemState(id), path);


                } else if (type == EventType.CHILD_NODE_REMOVED) {

                    Path path = Path.create(e.getNodePath() + ((e.getNodePath().length() > 1) ? "/" : "") + e.getChildName(),
                            session.getNamespaceResolver(),
                            true);
                    deleteNode(path, e.getChildUUID());

                } else if (type == EventType.PROPERTY_ADDED
                        || type == EventType.PROPERTY_CHANGED
                        || type == EventType.PROPERTY_REMOVED) {

                    Path path = Path.create(e.getNodePath(),
                            session.getNamespaceResolver(),
                            true);
                    modified.add(path);
                }
            } catch (MalformedPathException e) {
                log.error("error indexing node.", e);
            } catch (ItemStateException e) {
                log.error("error indexing node.", e);
            } catch (RepositoryException e) {
                log.error("error indexing node.", e);
            } catch (IOException e) {
                log.error("error indexing node.", e);
            }
        }

        for (Iterator it = modified.iterator(); it.hasNext();) {
            try {
                Path path = (Path) it.next();
                ItemId id = hmgr.resolvePath(path);
                path = getIndexlessPath(path);
                updateNode((NodeState) stateProvider.getItemState(id), path);
            } catch (ItemStateException e) {
                log.error("error indexing node.", e);
            } catch (RepositoryException e) {
                log.error("error indexing node.", e);
            } catch (IOException e) {
                log.error("error indexing node.", e);
            }
        }
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
