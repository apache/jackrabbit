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
package org.apache.jackrabbit.core.query.lucene.join;

import java.io.IOException;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.query.lucene.ScoreNode;
import org.apache.jackrabbit.core.query.lucene.MultiColumnQueryHits;
import org.apache.jackrabbit.core.HierarchyManager;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.name.PathBuilder;

/**
 * <code>AncestorPathNodeJoin</code> implements an ancestor path node join
 * condition.
 */
public class AncestorPathNodeJoin extends AbstractCondition {

    /**
     * A score node map with the score nodes from the inner query hits, indexed
     * by the path of the inner query hits.
     */
    private final ScoreNodeMap contextIndex = new ScoreNodeMap();

    /**
     * The hierarchy manager.
     */
    private final HierarchyManager hmgr;

    /**
     * The relative path from the outer to the inner query hits.
     */
    private final Path relPath;

    /**
     * Creates an ancestor path node join.
     *
     * @param context             the inner query hits.
     * @param contextSelectorName the selector name for the inner query hits.
     * @param relPath             the relative path of the join condition.
     * @param hmgr                the hierarchy manager of the workspace.
     * @throws IOException if an error occurs while reading from the index.
     */
    public AncestorPathNodeJoin(MultiColumnQueryHits context,
                                Name contextSelectorName,
                                Path relPath,
                                HierarchyManager hmgr) throws IOException {
        super(context);
        this.hmgr = hmgr;
        this.relPath = relPath;
        int idx = getIndex(context, contextSelectorName);
        ScoreNode[] nodes;
        while ((nodes = context.nextScoreNodes()) != null) {
            try {
                Path p = hmgr.getPath(nodes[idx].getNodeId());
                contextIndex.addScoreNodes(p, nodes);
            } catch (RepositoryException e) {
                // ignore
            }
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * The outer query hits loop contains the ancestor nodes.
     */
    public ScoreNode[][] getMatchingScoreNodes(ScoreNode ancestor)
            throws IOException {
        try {
            Path ancestorPath = hmgr.getPath(ancestor.getNodeId());
            PathBuilder builder = new PathBuilder(ancestorPath);
            builder.addAll(relPath.getElements());
            return contextIndex.getScoreNodes(builder.getPath().getNormalizedPath());
        } catch (RepositoryException e) {
            // ignore, probably does not exist anymore
            return null;
        }
    }
}
