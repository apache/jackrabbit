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

import org.apache.jackrabbit.core.ItemManager;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.spi.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.ArrayList;

/**
 * Implements a ScoreNodeIterator that returns the score nodes in document order.
 */
class DocOrderScoreNodeIterator implements ScoreNodeIterator {

    /** Logger instance for this class */
    private static final Logger log = LoggerFactory.getLogger(DocOrderScoreNodeIterator.class);

    /** A node iterator with ordered nodes */
    private ScoreNodeIterator orderedNodes;

    /** Unordered list of {@link ScoreNode}[]s. */
    private final List<ScoreNode[]> scoreNodes;

    /** ItemManager to turn UUIDs into Node instances */
    protected final ItemManager itemMgr;

    /**
     * Apply document order on the score nodes with this selectorIndex.
     */
    private final int selectorIndex;

    /**
     * Creates a <code>DocOrderScoreNodeIterator</code> that orders the nodes in
     * <code>scoreNodes</code> in document order.
     *
     * @param itemMgr       the item manager of the session executing the
     *                      query.
     * @param scoreNodes    the ids of the matching nodes with their score
     *                      value. <code>List&lt;ScoreNode[]></code>
     * @param selectorIndex apply document order on the score nodes with this
     *                      selectorIndex.
     */
    DocOrderScoreNodeIterator(ItemManager itemMgr,
                              List<ScoreNode[]> scoreNodes,
                              int selectorIndex) {
        this.itemMgr = itemMgr;
        this.scoreNodes = scoreNodes;
        this.selectorIndex = selectorIndex;
    }

    /**
     * {@inheritDoc}
     */
    public Object next() {
        return nextScoreNodes();
    }

    /**
     * {@inheritDoc}
     */
    public ScoreNode[] nextScoreNodes() {
        initOrderedIterator();
        return orderedNodes.nextScoreNodes();
    }

    /**
     * @throws UnsupportedOperationException always.
     */
    public void remove() {
        throw new UnsupportedOperationException("remove");
    }

    /**
     * {@inheritDoc}
     */
    public void skip(long skipNum) {
        initOrderedIterator();
        orderedNodes.skip(skipNum);
    }

    /**
     * Returns the number of nodes in this iterator.
     * </p>
     * Note: The number returned by this method may differ from the number
     * of nodes actually returned by calls to hasNext() / getNextNode()! This
     * is because this iterator works on a lazy instantiation basis and while
     * iterating over the nodes some of them might have been deleted in the
     * meantime. Those will not be returned by getNextNode(). As soon as an
     * invalid node is detected, the size of this iterator is adjusted.
     *
     * @return the number of node in this iterator.
     */
    public long getSize() {
        if (orderedNodes != null) {
            return orderedNodes.getSize();
        } else {
            return scoreNodes.size();
        }
    }

    /**
     * {@inheritDoc}
     */
    public long getPosition() {
        initOrderedIterator();
        return orderedNodes.getPosition();
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasNext() {
        initOrderedIterator();
        return orderedNodes.hasNext();
    }

    //------------------------< internal >--------------------------------------

    /**
     * Initializes the NodeIterator in document order
     */
    private void initOrderedIterator() {
        if (orderedNodes != null) {
            return;
        }
        long time = System.currentTimeMillis();
        ScoreNode[][] nodes = scoreNodes.toArray(new ScoreNode[scoreNodes.size()][]);

        final List<NodeId> invalidIDs = new ArrayList<NodeId>(2);

        do {
            if (invalidIDs.size() > 0) {
                // previous sort run was not successful -> remove failed uuids
                List<ScoreNode[]> tmp = new ArrayList<ScoreNode[]>();
                for (ScoreNode[] node : nodes) {
                    if (!invalidIDs.contains(node[selectorIndex].getNodeId())) {
                        tmp.add(node);
                    }
                }
                nodes = tmp.toArray(new ScoreNode[tmp.size()][]);
                invalidIDs.clear();
            }

            try {
                // sort the uuids
                Arrays.sort(nodes, new Comparator<ScoreNode[]>() {
                    public int compare(ScoreNode[] o1, ScoreNode[] o2) {
                        ScoreNode n1 = o1[selectorIndex];
                        ScoreNode n2 = o2[selectorIndex];
                        // handle null values
                        // null is considered less than any value
                        if (n1 == n2) {
                            return 0;
                        } else if (n1 == null) {
                            return -1;
                        } else if (n2 == null) {
                            return 1;
                        }
                        try {
                            NodeImpl node1;
                            try {
                                node1 = (NodeImpl) itemMgr.getItem(n1.getNodeId());
                            } catch (RepositoryException e) {
                                log.warn("Node " + n1.getNodeId() + " does not exist anymore: " + e);
                                // node does not exist anymore
                                invalidIDs.add(n1.getNodeId());
                                SortFailedException sfe = new SortFailedException();
                                sfe.initCause(e);
                                throw sfe;
                            }
                            NodeImpl node2;
                            try {
                                node2 = (NodeImpl) itemMgr.getItem(n2.getNodeId());
                            } catch (RepositoryException e) {
                                log.warn("Node " + n2.getNodeId() + " does not exist anymore: " + e);
                                // node does not exist anymore
                                invalidIDs.add(n2.getNodeId());
                                SortFailedException sfe = new SortFailedException();
                                sfe.initCause(e);
                                throw sfe;
                            }
                            Path.Element[] path1 = node1.getPrimaryPath().getElements();
                            Path.Element[] path2 = node2.getPrimaryPath().getElements();

                            // find nearest common ancestor
                            int commonDepth = 0; // root
                            while (path1.length > commonDepth && path2.length > commonDepth) {
                                if (path1[commonDepth].equals(path2[commonDepth])) {
                                    commonDepth++;
                                } else {
                                    break;
                                }
                            }
                            // path elements at last depth were equal
                            commonDepth--;

                            // check if either path is an ancestor of the other
                            if (path1.length - 1 == commonDepth) {
                                // path1 itself is ancestor of path2
                                return -1;
                            }
                            if (path2.length - 1 == commonDepth) {
                                // path2 itself is ancestor of path1
                                return 1;
                            }
                            // get common ancestor node
                            NodeImpl commonNode = (NodeImpl) node1.getAncestor(commonDepth);
                            // move node1/node2 to the commonDepth + 1
                            // node1 and node2 then will be child nodes of commonNode
                            node1 = (NodeImpl) node1.getAncestor(commonDepth + 1);
                            node2 = (NodeImpl) node2.getAncestor(commonDepth + 1);
                            for (NodeIterator it = commonNode.getNodes(); it.hasNext();) {
                                Node child = it.nextNode();
                                if (child.isSame(node1)) {
                                    return -1;
                                } else if (child.isSame(node2)) {
                                    return 1;
                                }
                            }
                            log.error("Internal error: unable to determine document order of nodes:");
                            log.error("\tNode1: " + node1.getPath());
                            log.error("\tNode2: " + node2.getPath());
                        } catch (RepositoryException e) {
                            log.error("Exception while sorting nodes in document order: " + e.toString(), e);
                        }
                        // if we get here something went wrong
                        // remove both uuids from array
                        invalidIDs.add(n1.getNodeId());
                        invalidIDs.add(n2.getNodeId());
                        // terminate sorting
                        throw new SortFailedException();
                    }
                });
            } catch (SortFailedException e) {
                // retry
            }

        } while (invalidIDs.size() > 0);

        if (log.isDebugEnabled()) {
            log.debug("" + nodes.length + " node(s) ordered in " + (System.currentTimeMillis() - time) + " ms");
        }
        orderedNodes = new ScoreNodeIteratorImpl(nodes);
    }

    /**
     * Indicates that sorting failed.
     */
    @SuppressWarnings("serial")
    private static final class SortFailedException extends RuntimeException {
    }
}
