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

import org.apache.jackrabbit.core.SessionImpl;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.io.IOException;

/**
 * <code>ChildNodesQueryHits</code> implements query hits that returns the child
 * nodes of another given query hits.
 */
public class ChildNodesQueryHits extends AbstractQueryHits {

    /**
     * The parent query hits.
     */
    private final QueryHits parents;

    /**
     * This session that executes the query.
     */
    private final SessionImpl session;

    /**
     * The current child hits.
     */
    private QueryHits childHits;

    /**
     * Creates a new <code>ChildNodesQueryHits</code> that returns the child
     * nodes of all query hits from the given <code>parents</code>.
     *
     * @param parents the parent query hits.
     * @param session the session that executes the query.
     * @throws IOException if an error occurs while reading from
     *                     <code>parents</code>
     */
    public ChildNodesQueryHits(QueryHits parents, SessionImpl session)
            throws IOException {
        this.parents = parents;
        this.session = session;
        fetchNextChildHits();
    }

    /**
     * {@inheritDoc}
     */
    public void close() throws IOException {
        if (childHits != null) {
            childHits.close();
        }
        parents.close();
    }

    /**
     * {@inheritDoc}
     */
    public ScoreNode nextScoreNode() throws IOException {
        while (childHits != null) {
            ScoreNode sn = childHits.nextScoreNode();
            if (sn != null) {
                return sn;
            } else {
                fetchNextChildHits();
            }
        }
        // if we get here there are no more score nodes
        return null;
    }

    /**
     * Fetches the next {@link #childHits}
     *
     * @throws IOException if an error occurs while reading from the index.
     */
    private void fetchNextChildHits() throws IOException {
        if (childHits != null) {
            childHits.close();
        }
        ScoreNode nextParent = parents.nextScoreNode();
        if (nextParent != null) {
            try {
                Node parent = session.getNodeById(nextParent.getNodeId());
                childHits = new NodeTraversingQueryHits(parent, false, 1);
            } catch (ItemNotFoundException e) {
                // access denied to node, will just skip it
            } catch (RepositoryException e) {
                throw Util.createIOException(e);
            }
        } else {
            childHits = null;
        }
    }
}
