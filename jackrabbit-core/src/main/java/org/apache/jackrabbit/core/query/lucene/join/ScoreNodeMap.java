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

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

import org.apache.jackrabbit.core.query.lucene.ScoreNode;

/**
 * <code>ScoreNodeMap</code> implements a simple mapping of an arbitrary key
 * to an array of <code>ScoreNode[]</code>.
 */
public final class ScoreNodeMap {

    /**
     * The internal map.
     */
    private final Map<Object, Object> map = new HashMap<Object, Object>();

    /**
     * Adds <code>scoreNodes</code> to this map under the given <code>key</code>.
     * If there already exists a mapping with the given <code>key</code> the
     * <code>scoreNodes</code> are added to the existing mapping. The add
     * operation works as follows:
     * <ul>
     * <li>If the existing value for <code>key</code> is a <code>ScoreNode[]</code>,
     * then the value is turned into a <code>List</code> and the existing value
     * as well as the new value are added to the <code>List</code>. Finally
     * the <code>List</code> is uses as the new value for the mapping.
     * </li>
     * <li>If the existing value for <code>key</code> is a <code>List</code> the
     * <code>scoreNodes</code> are simply added to the <code>List</code>.
     * </li>
     * </ul>
     *
     * @param key   the lookup key.
     * @param nodes the score nodes.
     */
    public void addScoreNodes(Object key, ScoreNode[] nodes) {
        Object existing = map.get(key);
        if (existing == null) {
            existing = nodes;
            map.put(key, existing);
        } else if (existing instanceof List) {
            @SuppressWarnings("unchecked")
            List<ScoreNode[]> existingNodes = (List<ScoreNode[]>) existing;
            existingNodes.add(nodes);
        } else {
            // ScoreNode[]
            List<ScoreNode[]> tmp = new ArrayList<ScoreNode[]>();
            tmp.add((ScoreNode[]) existing);
            tmp.add(nodes);
            existing = tmp;
            map.put(key, existing);
        }
    }

    /**
     * Returns an array of <code>ScoreNode[]</code> for the given
     * <code>key</code>.
     *
     * @param key the key.
     * @return an array of <code>ScoreNode[]</code> that match the given
     *         <code>key</code> or <code>null</code> if there is none.
     */
    public ScoreNode[][] getScoreNodes(Object key) {
        Object sn = map.get(key);
        if (sn == null) {
            return null;
        } else if (sn instanceof List) {
            @SuppressWarnings("unchecked")
            List<ScoreNode[]> list = (List<ScoreNode[]>) sn;
            return list.toArray(new ScoreNode[list.size()][]);
        } else {
            // ScoreNode[]
            return new ScoreNode[][]{(ScoreNode[]) sn};
        }
    }
}
