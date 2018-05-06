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
package org.apache.jackrabbit.core.query.lucene;

import org.apache.commons.io.IOExceptionWithCause;
import org.apache.jackrabbit.core.HierarchyManager;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.cluster.ClusterException;
import org.apache.jackrabbit.core.cluster.ClusterNode;
import org.apache.jackrabbit.core.persistence.IterablePersistenceManager;
import org.apache.jackrabbit.core.persistence.PersistenceManager;
import org.apache.jackrabbit.core.state.ItemState;
import org.apache.jackrabbit.core.state.ItemStateManager;
import org.apache.jackrabbit.core.state.NoSuchItemStateException;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.ChildNodeEntry;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.commons.conversion.MalformedPathException;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.spi.commons.name.PathBuilder;
import org.apache.lucene.document.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.ItemNotFoundException;
import javax.jcr.RepositoryException;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

/**
 * Implements a consistency check on the search index. Currently the following
 * checks are implemented:
 * <ul>
 * <li>Does the node exist in the ItemStateManager? If it does not exist
 * anymore the node is deleted from the index.</li>
 * <li>Is the parent of a node also present in the index? If it is not present it
 * will be indexed.</li>
 * <li>Is a node indexed multiple times? If that is the case, all occurrences
 * in the index for such a node are removed, and the node is re-indexed.</li>
 * <li>Is a node missing from the index? If so, it is added.</li>
 * </ul>
 */
public class ConsistencyCheck {

    /**
     * Logger instance for this class
     */
    private static final Logger log = LoggerFactory.getLogger(ConsistencyCheck.class);

    /**
     * The number of nodes to fetch at once from the persistence manager. Defaults to 8kb
     */
    private static final int NODESATONCE = Integer.getInteger("org.apache.jackrabbit.checker.nodesatonce", 1024 * 8);

    private final SearchIndex handler;

    /**
     * The ItemStateManager of the workspace.
     */
    private final ItemStateManager stateMgr;

    /**
     * The PersistenceManager of the workspace.
     */
    private IterablePersistenceManager pm;

    /**
     * The index to check.
     */
    private final MultiIndex index;

    /**
     * All the node ids and whether they were found in the index.
     */
    private Map<NodeId, Boolean> nodeIds;

    /**
     * Paths of nodes that are not be indexed
     */
    private Set<Path> excludedPaths;

    /**
     * Paths of nodes that will be excluded from consistency check
     */
    private final Set<Path> ignoredPaths = new HashSet<Path>();

    /**
     * List of all errors.
     */
    private final List<ConsistencyCheckError> errors =
        new ArrayList<ConsistencyCheckError>();

    /**
     * Private constructor.
     */
    private ConsistencyCheck(MultiIndex index, SearchIndex handler, Set<NodeId> excludedIds) {
        this.index = index;
        this.handler = handler;
        final HierarchyManager hierarchyManager = handler.getContext().getHierarchyManager();
        excludedPaths = new HashSet<Path>(excludedIds.size());
        for (NodeId excludedId : excludedIds) {
            try {
                final Path path = hierarchyManager.getPath(excludedId);
                excludedPaths.add(path);
            } catch (ItemNotFoundException e) {
                log.warn("Excluded node does not exist");
            } catch (RepositoryException e) {
                log.error("Failed to get excluded path", e);
            }
        }

        //JCR-3773: ignore the tree jcr:nodeTypes
        PathBuilder pathBuilder = new PathBuilder();
        pathBuilder.addRoot();
        pathBuilder.addLast(NameConstants.JCR_NODETYPES);
        try {
            Path path = pathBuilder.getPath();
            log.info("consistency check will skip " + path);
            ignoredPaths.add(path);
        } catch (MalformedPathException e) {
            //will never happen
            log.error("Malformed path", e);
        }

        this.stateMgr = handler.getContext().getItemStateManager();
        final PersistenceManager pm = handler.getContext().getPersistenceManager();
        if (pm instanceof IterablePersistenceManager) {
            this.pm = (IterablePersistenceManager) pm;
        }
    }

    /**
     * Runs the consistency check on <code>index</code>.
     *
     *
     *
     * @param index the index to check.
     * @param handler the QueryHandler to use.
     * @param excludedIds the set of node ids that are not indexed
     * @return the consistency check with the results.
     * @throws IOException if an error occurs while checking.
     */
    static ConsistencyCheck run(MultiIndex index, SearchIndex handler, final Set<NodeId> excludedIds)
            throws IOException {
        ConsistencyCheck check = new ConsistencyCheck(index, handler, excludedIds);
        check.run();
        return check;
    }

    /**
     * Repairs detected errors during the consistency check.
     * @param ignoreFailure if <code>true</code> repair failures are ignored,
     *   the repair continues without throwing an exception. If
     *   <code>false</code> the repair procedure is aborted on the first
     *   repair failure.
     * @throws IOException if a repair failure occurs.
     */
    public void repair(boolean ignoreFailure) throws IOException {
        if (errors.size() == 0) {
            log.info("No errors found.");
            return;
        }
        int notRepairable = 0;
        for (ConsistencyCheckError error : errors) {
            try {
                if (error.repairable()) {
                    error.repair();
                } else {
                    log.warn("Not repairable: " + error);
                    notRepairable++;
                }
            } catch (Exception e) {
                if (ignoreFailure) {
                    log.warn("Exception while repairing: " + error, e);
                } else if (e instanceof IOException) {
                    throw (IOException) e;
                } else {
                    throw new IOExceptionWithCause(e);
                }
            }
        }
        log.info("Repaired " + (errors.size() - notRepairable) + " errors.");
        if (notRepairable > 0) {
            log.warn("" + notRepairable + " error(s) not repairable.");
        }
    }

    /**
     * Returns the errors detected by the consistency check.
     * @return the errors detected by the consistency check.
     */
    public List<ConsistencyCheckError> getErrors() {
        return new ArrayList<ConsistencyCheckError>(errors);
    }

    /**
     * Runs the consistency check.
     * @throws IOException if an error occurs while running the check.
     */
    private void run() throws IOException {
        log.info("Checking index of workspace " + handler.getContext().getWorkspace());
        loadNodes();
        if (nodeIds != null) {
            checkIndexConsistency();
            checkIndexCompleteness();
        }
    }

    public void doubleCheckErrors() {
        if (!errors.isEmpty()) {
            log.info("Double checking errors");
            final ClusterNode clusterNode = handler.getContext().getClusterNode();
            if (clusterNode != null) {
                try {
                    clusterNode.sync();
                } catch (ClusterException e) {
                    log.error("Could not sync cluster node for double checking errors");
                }
            }
            final Iterator<ConsistencyCheckError> iterator = errors.iterator();
            while (iterator.hasNext()) {
                try {
                    final ConsistencyCheckError error = iterator.next();
                    if (!error.doubleCheck(handler, stateMgr)) {
                        log.info("False positive: " + error.toString());
                        iterator.remove();
                    }
                } catch (RepositoryException e) {
                    log.error("Failed to double check consistency error", e);
                } catch (IOException e) {
                    log.error("Failed to double check consistency error", e);
                }
            }
        }
    }

    private void loadNodes() {
        log.info("Loading nodes");
        try {
            int count = 0;
            Map<NodeId, Boolean> nodeIds = new HashMap<NodeId, Boolean>();
            List<NodeId> batch = pm.getAllNodeIds(null, NODESATONCE);
            NodeId lastId = null;
            while (!batch.isEmpty()) {
                for (NodeId nodeId : batch) {
                    lastId = nodeId;

                    count++;
                    if (count % 1000 == 0) {
                        log.info(pm + ": loaded " + count + " node ids...");
                    }

                    nodeIds.put(nodeId, Boolean.FALSE);

                }
                batch = pm.getAllNodeIds(lastId, NODESATONCE);
            }
            if (pm.exists(lastId)) {
                this.nodeIds = nodeIds;
            } else {
                log.info("Failed to read all nodes, starting over");
                loadNodes();
            }
        } catch (ItemStateException e) {
            log.error("Exception while loading items to check", e);
        } catch (RepositoryException e) {
            log.error("Exception while loading items to check", e);
        }
    }

    private void checkIndexConsistency() throws IOException {
        log.info("Checking index consistency");
        // Ids of multiple nodes in the index
        Set<NodeId> multipleEntries = new HashSet<NodeId>();
        CachingMultiIndexReader reader = index.getIndexReader();
        try {
            for (int i = 0; i < reader.maxDoc(); i++) {
                if (i > 10 && i % (reader.maxDoc() / 5) == 0) {
                    long progress = Math.round((100.0 * (float) i) / ((float) reader.maxDoc() * 2f));
                    log.info("progress: " + progress + "%");
                }
                if (reader.isDeleted(i)) {
                    continue;
                }
                Document d = reader.document(i, FieldSelectors.UUID);
                NodeId id = new NodeId(d.get(FieldNames.UUID));
                if (!isIgnored(id)) {
                    boolean nodeExists = nodeIds.containsKey(id);
                    if (nodeExists) {
                        Boolean alreadyIndexed = nodeIds.put(id, Boolean.TRUE);
                        if (alreadyIndexed) {
                            multipleEntries.add(id);
                        }
                    } else {
                        errors.add(new NodeDeleted(id));
                    }
                }
            }
        } finally {
            reader.release();
        }

        // create multiple entries errors
        for (NodeId id : multipleEntries) {
            errors.add(new MultipleEntries(id));
        }

        reader = index.getIndexReader();
        try {
            // run through documents again and check parent
            for (int i = 0; i < reader.maxDoc(); i++) {
                if (i > 10 && i % (reader.maxDoc() / 5) == 0) {
                    long progress = Math.round((100.0 * (float) i) / ((float) reader.maxDoc() * 2f));
                    log.info("progress: " + (progress + 50) + "%");
                }
                if (reader.isDeleted(i)) {
                    continue;
                }
                Document d = reader.document(i, FieldSelectors.UUID_AND_PARENT);
                NodeId id = new NodeId(d.get(FieldNames.UUID));
                if (!nodeIds.containsKey(id) || isIgnored(id)) {
                    // this node is ignored or was already marked for deletion
                    continue;
                }
                String parent = d.get(FieldNames.PARENT);
                if (parent == null || parent.isEmpty()) {
                    continue;
                }
                final NodeId parentId = new NodeId(parent);

                boolean parentExists = nodeIds.containsKey(parentId);
                boolean parentIndexed = parentExists && nodeIds.get(parentId);
                if (parentIndexed) {
                    continue;
                } else if (id.equals(RepositoryImpl.SYSTEM_ROOT_NODE_ID)
                        && parentId.equals(RepositoryImpl.ROOT_NODE_ID)) {
                    continue; // special case for the /jcr:system node
                }

                // parent is missing from index
                if (parentExists) {
                    errors.add(new MissingAncestor(id, parentId));
                } else {
                    try {
                        final ItemState itemState = stateMgr.getItemState(id);
                        if (parentId.equals(itemState.getParentId())) {
                            // orphaned node
                            errors.add(new UnknownParent(id, parentId));
                        } else {
                            errors.add(new WrongParent(id, parentId, itemState.getParentId()));
                        }
                    } catch (ItemStateException ignored) {
                    }
                }
            }
        } finally {
            reader.release();
        }

    }

    private void checkIndexCompleteness() {
        log.info("Checking index completeness");
        int i = 0;
        int size = nodeIds.size();
        for (Map.Entry<NodeId, Boolean> entry : nodeIds.entrySet()) {
            // check whether all nodes in the repository are indexed
            NodeId nodeId = entry.getKey();
            boolean indexed = entry.getValue();
            try {
                if (++i > 10 && i % (size / 10) == 0) {
                    long progress = Math.round((100.0 * (float) i) / (float) size);
                    log.info("progress: " + progress + "%");
                }
                if (!indexed && !isIgnored(nodeId) && !isExcluded(nodeId)) {
                    NodeState nodeState = getNodeState(nodeId);
                    if (nodeState != null && !isBrokenNode(nodeId, nodeState)) {
                        errors.add(new NodeAdded(nodeId));
                    }
                }
            } catch (ItemStateException e) {
                log.error("Failed to check node: " + nodeId, e);
            }
        }
    }

    private boolean isExcluded(NodeId id) {
        try {
            final HierarchyManager hierarchyManager = handler.getContext().getHierarchyManager();
            final Path path = hierarchyManager.getPath(id);
            for (Path excludedPath : excludedPaths) {
                if (excludedPath.isEquivalentTo(path) || excludedPath.isAncestorOf(path)) {
                    return true;
                }
            }
        } catch (RepositoryException ignored) {
        }
        return false;
    }

    private boolean isIgnored(NodeId id) {
        try {
            final HierarchyManager hierarchyManager = handler.getContext().getHierarchyManager();
            final Path path = hierarchyManager.getPath(id);
            for (Path excludedPath : ignoredPaths) {
                if (excludedPath.isEquivalentTo(path) || excludedPath.isAncestorOf(path)) {
                    return true;
                }
            }
        } catch (RepositoryException ignored) {
        }
        return false;
    }

    private NodeState getNodeState(NodeId nodeId) throws ItemStateException {
        try {
            return (NodeState) stateMgr.getItemState(nodeId);
        } catch (NoSuchItemStateException e) {
            return null;
        }
    }

    private boolean isBrokenNode(final NodeId nodeId, final NodeState nodeState) throws ItemStateException {
        final NodeId parentId = nodeState.getParentId();
        if (parentId != null) {
            final NodeState parentState = getNodeState(parentId);
            if (parentState == null) {
                log.warn("Node missing from index is orphaned node: " + nodeId);
                return true;
            }
            if (!parentState.hasChildNodeEntry(nodeId)) {
                log.warn("Node missing from index is abandoned node: " + nodeId);
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the path for <code>node</code>. If an error occurs this method
     * returns the uuid of the node.
     *
     * @param node the node to retrieve the path from
     * @return the path of the node or its uuid.
     */
    private String getPath(NodeState node) {
        // remember as fallback
        String uuid = node.getNodeId().toString();
        StringBuilder path = new StringBuilder();
        List<ChildNodeEntry> elements = new ArrayList<ChildNodeEntry>();
        try {
            while (node.getParentId() != null) {
                NodeId parentId = node.getParentId();
                NodeState parent = (NodeState) stateMgr.getItemState(parentId);
                ChildNodeEntry entry = parent.getChildNodeEntry(node.getNodeId());
                if (entry == null) {
                    log.warn("Failed to build path: abandoned child {} of node {}. " +
                            "Please run a repository consistency check", node.getNodeId(), parentId);
                    return uuid;
                }
                elements.add(entry);
                node = parent;
            }
            for (int i = elements.size() - 1; i > -1; i--) {
                ChildNodeEntry entry = elements.get(i);
                path.append('/').append(entry.getName().getLocalName());
                if (entry.getIndex() > 1) {
                    path.append('[').append(entry.getIndex()).append(']');
                }
            }
            if (path.length() == 0) {
                path.append('/');
            }
            return path.toString();
        } catch (ItemStateException e) {
            return uuid;
        }
    }

    //-------------------< ConsistencyCheckError classes >----------------------

    /**
     * One or more ancestors of an indexed node are not available in the index.
     */
    private class MissingAncestor extends ConsistencyCheckError {

        private final NodeId parentId;

        private MissingAncestor(NodeId id, NodeId parentId) {
            super("Parent of " + id + " missing in index. Parent: " + parentId, id);
            this.parentId = parentId;
        }

        /**
         * Returns <code>true</code>.
         * @return <code>true</code>.
         */
        public boolean repairable() {
            return true;
        }

        /**
         * Repairs the missing node by indexing the missing ancestors.
         * @throws Exception if an error occurs while repairing.
         */
        public void repair() throws Exception {
            NodeId ancestorId = parentId;
            while (ancestorId != null && nodeIds.containsKey(ancestorId) && nodeIds.get(ancestorId)) {
                NodeState n = (NodeState) stateMgr.getItemState(ancestorId);
                log.info("Repairing missing node " + getPath(n) + " (" + ancestorId + ")");
                Document d = index.createDocument(n);
                index.addDocument(d);
                nodeIds.put(n.getNodeId(), Boolean.TRUE);
                ancestorId = n.getParentId();
            }
        }

        @Override
        boolean doubleCheck(SearchIndex handler, ItemStateManager stateManager)
                throws RepositoryException, IOException {
            final List<Document> documents = handler.getNodeDocuments(id);
            for (Document document : documents) {
                final String parent = document.get(FieldNames.PARENT);
                if (parent != null && !parent.isEmpty()) {
                    final NodeId parentId = new NodeId(parent);
                    if (handler.getNodeDocuments(parentId).isEmpty()) {
                        return true;
                    }
                }
            }
            return false;

        }
    }

    /**
     * The parent of a node is not in the repository
     */
    private static class UnknownParent extends ConsistencyCheckError {

        private NodeId parentId;

        private UnknownParent(NodeId id, NodeId parentId) {
            super("Node " + id + " has unknown parent: " + parentId, id);
            this.parentId = parentId;
        }

        /**
         * Not reparable (yet).
         * @return <code>false</code>.
         */
        public boolean repairable() {
            return false;
        }

        /**
         * No operation.
         */
        public void repair() {
            log.warn("Unknown parent for " + id + " cannot be repaired");
        }

        @Override
        boolean doubleCheck(SearchIndex handler, ItemStateManager stateManager)
                throws IOException, RepositoryException {
            final List<Document> documents = handler.getNodeDocuments(id);
            for (Document document : documents) {
                final String parent = document.get(FieldNames.PARENT);
                if (parent != null && !parent.isEmpty()) {
                    final NodeId parentId = new NodeId(parent);
                    if (parentId.equals(this.parentId) && !stateManager.hasItemState(parentId)) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    /**
     * The parent as indexed does not correspond with the actual parent in the repository
     */
    private class WrongParent extends ConsistencyCheckError {

        private NodeId indexedParentId;

        private WrongParent(NodeId id, NodeId indexedParentId, NodeId actualParentId) {
            super("Node " + id + " has wrong parent: " + indexedParentId + ", should be : " + actualParentId, id);
            this.indexedParentId = indexedParentId;
        }

        @Override
        public boolean repairable() {
            return true;
        }

        /**
         * Reindex node.
         */
        @Override
        void repair() throws Exception {
            index.removeAllDocuments(id);
            try {
                NodeState node = (NodeState) stateMgr.getItemState(id);
                log.info("Re-indexing node with wrong parent in index: " + getPath(node));
                Document d = index.createDocument(node);
                index.addDocument(d);
                nodeIds.put(node.getNodeId(), Boolean.TRUE);
            } catch (NoSuchItemStateException e) {
                log.info("Not re-indexing node with wrong parent because node no longer exists");
            }
        }

        @Override
        boolean doubleCheck(final SearchIndex handler, final ItemStateManager stateManager)
                throws RepositoryException, IOException {
            final List<Document> documents = handler.getNodeDocuments(id);
            for (Document document : documents) {
                final String parent = document.get(FieldNames.PARENT);
                if (parent != null && !parent.isEmpty()) {
                    final NodeId parentId = new NodeId(parent);
                    if (parentId.equals(indexedParentId) && !stateManager.hasItemState(parentId)) {
                        return true;
                    }
                }
            }
            return false;
        }

    }

    /**
     * A node is present multiple times in the index.
     */
    private class MultipleEntries extends ConsistencyCheckError {

        MultipleEntries(NodeId id) {
            super("Multiple entries found for node " + id, id);
        }

        /**
         * Returns <code>true</code>.
         * @return <code>true</code>.
         */
        public boolean repairable() {
            return true;
        }

        /**
         * Removes the nodes with the identical uuids from the index and
         * re-index the node.
         * @throws IOException if an error occurs while repairing.
         */
        public void repair() throws Exception {
            // first remove all occurrences
            index.removeAllDocuments(id);
            // then re-index the node
            try {
                NodeState node = (NodeState) stateMgr.getItemState(id);
                log.info("Re-indexing duplicate node occurrences in index: " + getPath(node));
                Document d = index.createDocument(node);
                index.addDocument(d);
                nodeIds.put(node.getNodeId(), Boolean.TRUE);
            } catch (NoSuchItemStateException e) {
                log.info("Not re-indexing node with multiple occurrences because node no longer exists");
            }
        }

        @Override
        boolean doubleCheck(SearchIndex handler, ItemStateManager stateManager)
                throws RepositoryException, IOException {
            return handler.getNodeDocuments(id).size() > 1;
        }
    }

    /**
     * Indicates that a node has been deleted but is still in the index.
     */
    private class NodeDeleted extends ConsistencyCheckError {

        NodeDeleted(NodeId id) {
            super("Node " + id + " no longer exists.", id);
        }

        /**
         * Returns <code>true</code>.
         * @return <code>true</code>.
         */
        public boolean repairable() {
            return true;
        }

        /**
         * Deletes the nodes from the index.
         * @throws IOException if an error occurs while repairing.
         */
        public void repair() throws IOException {
            log.info("Removing deleted node from index: " + id);
            index.removeDocument(id);
        }

        @Override
        boolean doubleCheck(SearchIndex handler, ItemStateManager stateManager)
                throws RepositoryException, IOException {
            final List<Document> documents = handler.getNodeDocuments(id);
            if (!documents.isEmpty()) {
                if (!stateManager.hasItemState(id)) {
                    return true;
                }
            }
            return false;
        }
    }

    private class NodeAdded extends ConsistencyCheckError {

        NodeAdded(final NodeId id) {
            super("Node " + id + " is missing.", id);
        }

        @Override
        public boolean repairable() {
            return true;
        }

        @Override
        void repair() throws Exception {
            try {
                NodeState nodeState = (NodeState) stateMgr.getItemState(id);
                log.info("Adding missing node to index: " + getPath(nodeState));
                final Iterator<NodeId> remove = Collections.<NodeId>emptyList().iterator();
                final Iterator<NodeState> add = Collections.singletonList(nodeState).iterator();
                handler.updateNodes(remove, add);
            } catch (NoSuchItemStateException e) {
                log.info("Not adding missing node because node no longer exists");
            }
        }

        @Override
        boolean doubleCheck(SearchIndex handler, ItemStateManager stateManager)
                throws RepositoryException, IOException {
            final List<Document> documents = handler.getNodeDocuments(id);
            if (documents.isEmpty()) {
                if (stateManager.hasItemState(id)) {
                    return true;
                }
            }
            return false;
        }

    }
}
