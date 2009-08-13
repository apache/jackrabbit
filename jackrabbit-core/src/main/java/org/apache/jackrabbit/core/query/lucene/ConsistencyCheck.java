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

import org.apache.jackrabbit.core.state.ItemStateManager;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.ChildNodeEntry;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.lucene.document.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;

/**
 * Implements a consistency check on the search index. Currently the following
 * checks are implemented:
 * <ul>
 * <li>Does not node exist in the ItemStateManager? If it does not exist
 * anymore the node is deleted from the index.</li>
 * <li>Is the parent of a node also present in the index? If it is not present it
 * will be indexed.</li>
 * <li>Is a node indexed multiple times? If that is the case, all occurrences
 * in the index for such a node are removed, and the node is re-indexed.</li>
 * </ul>
 */
public class ConsistencyCheck {

    /**
     * Logger instance for this class
     */
    private static final Logger log = LoggerFactory.getLogger(ConsistencyCheck.class);

    /**
     * The ItemStateManager of the workspace.
     */
    private final ItemStateManager stateMgr;

    /**
     * The index to check.
     */
    private final MultiIndex index;

    /**
     * All the document ids within the index.
     */
    private Set<NodeId> documentIds;

    /**
     * List of all errors.
     */
    private final List<ConsistencyCheckError> errors =
        new ArrayList<ConsistencyCheckError>();

    /**
     * Private constructor.
     */
    private ConsistencyCheck(MultiIndex index, ItemStateManager mgr) {
        this.index = index;
        this.stateMgr = mgr;
    }

    /**
     * Runs the consistency check on <code>index</code>.
     *
     * @param index the index to check.
     * @param mgr   the ItemStateManager from where to load content.
     * @return the consistency check with the results.
     * @throws IOException if an error occurs while checking.
     */
    static ConsistencyCheck run(MultiIndex index, ItemStateManager mgr) throws IOException {
        ConsistencyCheck check = new ConsistencyCheck(index, mgr);
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
                    log.warn("Exception while reparing: " + e);
                } else {
                    if (!(e instanceof IOException)) {
                        e = new IOException(e.getMessage());
                    }
                    throw (IOException) e;
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
        // Ids of multiple nodes in the index
        Set<NodeId> multipleEntries = new HashSet<NodeId>();
        // collect all documents ids
        documentIds = new HashSet<NodeId>();
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
                if (stateMgr.hasItemState(id)) {
                    if (!documentIds.add(id)) {
                        multipleEntries.add(id);
                    }
                } else {
                    errors.add(new NodeDeleted(id));
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
                String parentUUIDString = d.get(FieldNames.PARENT);
                NodeId parentId = null;
                if (parentUUIDString.length() > 0) {
                    parentId = new NodeId(parentUUIDString);
                }
                if (parentId == null || documentIds.contains(parentId)) {
                    continue;
                }
                // parent is missing
                if (stateMgr.hasItemState(parentId)) {
                    errors.add(new MissingAncestor(id, parentId));
                } else {
                    errors.add(new UnknownParent(id, parentId));
                }
            }
        } finally {
            reader.release();
        }
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
        StringBuffer path = new StringBuffer();
        List<ChildNodeEntry> elements = new ArrayList<ChildNodeEntry>();
        try {
            while (node.getParentId() != null) {
                NodeId parentId = node.getParentId();
                NodeState parent = (NodeState) stateMgr.getItemState(parentId);
                ChildNodeEntry entry = parent.getChildNodeEntry(node.getNodeId());
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
         * @throws IOException if an error occurs while repairing.
         */
        public void repair() throws IOException {
            NodeId ancestorId = parentId;
            while (ancestorId != null && !documentIds.contains(ancestorId)) {
                try {
                    NodeState n = (NodeState) stateMgr.getItemState(ancestorId);
                    log.info("Reparing missing node " + getPath(n));
                    Document d = index.createDocument(n);
                    index.addDocument(d);
                    documentIds.add(n.getNodeId());
                    ancestorId = n.getParentId();
                } catch (ItemStateException e) {
                    throw new IOException(e.toString());
                } catch (RepositoryException e) {
                    throw new IOException(e.toString());
                }
            }
        }
    }

    /**
     * The parent of a node is not available through the ItemStateManager.
     */
    private class UnknownParent extends ConsistencyCheckError {

        private UnknownParent(NodeId id, NodeId parentId) {
            super("Node " + id + " has unknown parent: " + parentId, id);
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
        public void repair() throws IOException {
            log.warn("Unknown parent for " + id + " cannot be repaired");
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
        public void repair() throws IOException {
            // first remove all occurrences
            index.removeAllDocuments(id);
            // then re-index the node
            try {
                NodeState node = (NodeState) stateMgr.getItemState(id);
                log.info("Re-indexing duplicate node occurrences in index: " + getPath(node));
                Document d = index.createDocument(node);
                index.addDocument(d);
                documentIds.add(node.getNodeId());
            } catch (ItemStateException e) {
                throw new IOException(e.toString());
            } catch (RepositoryException e) {
                throw new IOException(e.toString());
            }
        }
    }

    /**
     * Indicates that a node has been deleted but is still in the index.
     */
    private class NodeDeleted extends ConsistencyCheckError {

        NodeDeleted(NodeId id) {
            super("Node " + id + " does not longer exist.", id);
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
    }
}
