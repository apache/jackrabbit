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
package org.apache.jackrabbit.jcr2spi.hierarchy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.jackrabbit.spi.Name;

import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;

/**
 * <code>ChildNodeAttic</code>...
 */
class ChildNodeAttic {

    private static Logger log = LoggerFactory.getLogger(ChildNodeAttic.class);

    private Set<NodeEntryImpl> attic = new HashSet<NodeEntryImpl>();

    ChildNodeAttic() {
    }

    boolean isEmpty() {
        return attic.isEmpty();
    }

    boolean contains(Name name, int index) {
        for (NodeEntryImpl ne : attic) {
            if (ne.matches(name, index)) {
                return true;
            }
        }
        return false;
    }

    boolean contains(Name name, int index, String uniqueId) {
        for (NodeEntryImpl ne : attic) {
            if (uniqueId != null && uniqueId.equals(ne.getUniqueID())) {
                return true;
            } else if (ne.matches(name, index)) {
                return true;
            }
        }
        // not found
        return false;
    }

    List<NodeEntryImpl> get(Name name) {
        List<NodeEntryImpl> l = new ArrayList<NodeEntryImpl>();
        for (NodeEntryImpl ne : attic) {
            if (ne.matches(name)) {
                l.add(ne);
            }
        }
        return l;
    }

    /**
     *
     * @param name The original name of the NodeEntry before it has been moved.
     * @param index The original index of the NodeEntry before it has been moved.
     * @return
     */
    NodeEntry get(Name name, int index) {
        for (NodeEntryImpl ne : attic) {
            if (ne.matches(name, index)) {
                return ne;
            }
        }
        // not found
        return null;
    }

    /**
     *
     * @param uniqueId
     * @return
     */
    NodeEntry get(String uniqueId) {
        if (uniqueId == null) {
            throw new IllegalArgumentException();
        }
        for (NodeEntry ne : attic) {
            if (uniqueId.equals(ne.getUniqueID())) {
                return ne;
            }
        }
        // not found
        return null;
    }

    void add(NodeEntryImpl movedEntry) {
        attic.add(movedEntry);
    }

    boolean remove(NodeEntry movedEntry) {
        if (attic.contains(movedEntry)) {
            return attic.remove(movedEntry);
        }
        return false;
    }

    Iterator<NodeEntryImpl> iterator() {
        return attic.iterator();
    }
    
    void clear() {
        if (attic != null) {
            attic.clear();
        }
    }
}