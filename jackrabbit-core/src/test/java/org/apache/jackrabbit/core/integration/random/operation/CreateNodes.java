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
package org.apache.jackrabbit.core.integration.random.operation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * <code>CreateNodes</code> creates a node hierarchy with a given number of
 * levels and nodes per level.
 */
public class CreateNodes extends Operation {

    private static final Logger log = LoggerFactory.getLogger(CreateNodes.class);

    private final int numLevels;

    private final int nodesPerLevel;

    private final String[] mixins;

    private final int saveInterval;

    public CreateNodes(Session s,
                       String path,
                       int numLevels,
                       int nodesPerLevel,
                       String[] mixins,
                       int saveInterval) {
        super(s, path);
        this.numLevels = numLevels;
        this.nodesPerLevel = nodesPerLevel;
        this.mixins = mixins;
        this.saveInterval = saveInterval;
    }

    /**
     * Returns the top of the created node hierarchy.
     */
    public NodeIterator execute() throws Exception {
        Node n = getNode();
        addMixins(n);
        createNodes(n, nodesPerLevel, numLevels, 0);
        return wrapWithIterator(n);
    }

    private int createNodes(Node n, int nodesPerLevel, int levels, int count)
            throws RepositoryException {
        levels--;
        for (int i = 0; i < nodesPerLevel; i++) {
            Node child = n.addNode("node" + i);
            count++;
            addMixins(child);
            log.info("Create node {}", child.getPath());
            if (count % saveInterval == 0) {
                getSession().save();
                log.debug("Created " + (count / 1000) + "k nodes");
            }
            if (levels > 0) {
                count = createNodes(child, nodesPerLevel, levels, count);
            }
        }
        if (levels == 0) {
            // final save
            getSession().save();
        }
        return count;
    }

    private void addMixins(Node node) throws RepositoryException {
        for (int i = 0; i < mixins.length; i++) {
            if (!node.isNodeType(mixins[i])) {
                node.addMixin(mixins[i]);
            }
        }
    }
}
