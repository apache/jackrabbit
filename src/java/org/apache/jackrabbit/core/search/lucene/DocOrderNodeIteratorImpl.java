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
package org.apache.jackrabbit.core.search.lucene;

import org.apache.jackrabbit.core.ItemManager;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.Path;
import org.apache.log4j.Logger;

import javax.jcr.NodeIterator;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.util.Arrays;
import java.util.Comparator;

/**
 * Implements a NodeIterator that returns the nodes in document order.
 */
class DocOrderNodeIteratorImpl extends NodeIteratorImpl {

    /** Logger instance for this class */
    private static final Logger log = Logger.getLogger(DocOrderNodeIteratorImpl.class);

    /** A node iterator with ordered nodes */
    private final NodeIteratorImpl orderedNodes;

    /**
     * Creates a <code>DocOrderNodeIteratorImpl</code> that orders the nodes
     * with <code>uuids</code> in document order.
     * @param itemMgr the item manager of the session executing the query.
     * @param uuids the uuids of the nodes.
     * @param scores the score values of the nodes.
     */
    DocOrderNodeIteratorImpl(final ItemManager itemMgr, String[] uuids, Float[] scores) {
        super(itemMgr, uuids, scores);
        long time = System.currentTimeMillis();
        ScoreNode[] nodes = new ScoreNode[uuids.length];
        for (int i = 0; i < uuids.length; i++) {
            nodes[i] = new ScoreNode(uuids[i], scores[i]);
        }

        Arrays.sort(nodes, new Comparator() {
            public int compare(Object o1, Object o2) {
                ScoreNode n1 = (ScoreNode) o1;
                ScoreNode n2 = (ScoreNode) o2;
                try {
                    NodeImpl node1 = (NodeImpl) itemMgr.getItem(new NodeId(n1.uuid));
                    NodeImpl node2 = (NodeImpl) itemMgr.getItem(new NodeId(n2.uuid));
                    Path.PathElement[] path1 = node1.getPrimaryPath().getElements();
                    Path.PathElement[] path2 = node2.getPrimaryPath().getElements();

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
                    return 0;
                } catch (RepositoryException e) {
                    log.error("Exception while sorting nodes in document order: " + e.toString(), e);
                    // todo ???
                    return 0;
                }
            }
        });

        for (int i = 0; i < nodes.length; i++) {
            uuids[i] = nodes[i].uuid;
            scores[i] = nodes[i].score;
        }
        if (log.isDebugEnabled()) {
            log.debug("" + uuids.length + " node(s) ordered in " + (System.currentTimeMillis() - time) + " ms");
        }
        orderedNodes = new NodeIteratorImpl(itemMgr, uuids, scores);
    }

    /**
     * {@inheritDoc}
     */
    public Node nextNode() {
        return orderedNodes.nextNode();
    }

    /**
     * {@inheritDoc}
     */
    public void skip(long skipNum) {
        orderedNodes.skip(skipNum);
    }

    /**
     * {@inheritDoc}
     */
    public long getSize() {
        return orderedNodes.getSize();
    }

    /**
     * {@inheritDoc}
     */
    public long getPosition() {
        return orderedNodes.getPosition();
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
    public boolean hasNext() {
        return orderedNodes.hasNext();
    }

    /**
     * {@inheritDoc}
     */
    public Object next() {
        return orderedNodes.next();
    }

    /**
     * {@inheritDoc}
     */
    float getScore() {
        return orderedNodes.getScore();
    }

    /**
     * {@inheritDoc}
     */
    NodeImpl nextNodeImpl() {
        return orderedNodes.nextNodeImpl();
    }

    //------------------------< internal >--------------------------------------

    private static final class ScoreNode {

        final String uuid;

        final Float score;

        ScoreNode(String uuid, Float score) {
            this.uuid = uuid;
            this.score = score;
        }
    }
}
