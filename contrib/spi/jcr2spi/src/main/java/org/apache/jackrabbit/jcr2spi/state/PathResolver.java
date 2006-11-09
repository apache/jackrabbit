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
package org.apache.jackrabbit.jcr2spi.state;

import org.apache.jackrabbit.name.Path;
import org.apache.jackrabbit.jcr2spi.state.entry.ChildNodeEntry;

/**
 * <code>PathResolver</code> resolves a relative Path starting at a given
 * ItemState and returns the Item where the path points to.
 */
public class PathResolver {

    /**
     * The starting point to resolve the path.
     */
    private final NodeState start;

    /**
     * The path to resolve.
     */
    private final Path path;

    /**
     * Internal constructor.
     *
     * @param start   the starting point.
     * @param relPath the path to resolve.
     * @throws IllegalArgumentException if not normalized or starts with a
     *                                  parent ('..') path element.
     */
    private PathResolver(NodeState start, Path relPath) {
        if (!relPath.isNormalized() || relPath.getElement(0).denotesParent()) {
            throw new IllegalArgumentException("path must be relative and must " +
                    "not contain parent path elements");
        }
        this.start = start;
        this.path = relPath;
    }

    /**
     * Resolves the path starting at <code>start</code>.
     *
     * @param start   the starting point.
     * @param path the path to resolve.
     * @return the resolved item state.
     * @throws NoSuchItemStateException the the referenced item state does not
     *                                  exist.
     * @throws ItemStateException       if an error occurs while retrieving the
     *                                  item state.
     * @throws IllegalArgumentException if path is absolute or not normalized
     *                                  or starts with a parent ('..') path
     *                                  element.
     */
    public static ItemState resolve(NodeState start, Path path)
            throws NoSuchItemStateException, ItemStateException {
        return new PathResolver(start, path).resolve();
    }

    /**
     * Looks up the <code>ItemState</code> at <code>path</code> starting at
     * <code>start</code>.
     *
     * @param start   the starting point.
     * @param path the path to resolve.
     * @return the resolved item state or <code>null</code> if the item is not
     *         available.
     * @throws NoSuchItemStateException the the referenced item state does not
     *                                  exist.
     * @throws ItemStateException       if an error occurs while retrieving the
     *                                  item state.
     * @throws IllegalArgumentException if path is absolute or not normalized
     *                                  or starts with a parent ('..') path
     *                                  element.
     */
    public static ItemState lookup(NodeState start, Path path)
            throws NoSuchItemStateException, ItemStateException {
        return new PathResolver(start, path).lookup();
    }

    /**
     * Resolves the path.
     *
     * @return the resolved item state.
     * @throws NoSuchItemStateException the the item state does not exist.
     * @throws ItemStateException if an error occurs while retrieving the item state.
     */
    private ItemState resolve()
            throws NoSuchItemStateException, ItemStateException {
        NodeState state = start;
        for (int i = 0; i < path.getLength(); i++) {
            Path.PathElement elem = path.getElement(i);
            // check for root element
            if (elem.denotesRoot()) {
                if (start.getParent() != null) {
                    throw new NoSuchItemStateException(path.toString());
                } else {
                    continue;
                }
            }

            // first try to resolve node
            if (state.hasChildNodeEntry(elem.getName(), elem.getNormalizedIndex())) {
                ChildNodeEntry cne = state.getChildNodeEntry(elem.getName(), elem.getNormalizedIndex());
                state = cne.getNodeState();
            } else if (elem.getIndex() == 0 // property must not have index
                    && state.hasPropertyName(elem.getName())
                    && i == path.getLength() - 1) { // property must be final path element
                return state.getPropertyState(elem.getName());
            } else {
                throw new NoSuchItemStateException(path.toString());
            }
        }
        return state;
    }

    /**
     * Resolves the path but does not return the <code>ItemState</code> if it
     * has not yet been loaded.
     *
     * @return the resolved item state or <code>null</code> if the item is not
     * available.
     * @throws NoSuchItemStateException the the item state does not exist.
     * @throws ItemStateException if an error occurs while retrieving the
     * item state.
     */
    private ItemState lookup()
            throws NoSuchItemStateException, ItemStateException {
        NodeState state = start;
        for (int i = 0; i < path.getLength(); i++) {
            Path.PathElement elem = path.getElement(i);
            // first try to resolve node
            if (state.hasChildNodeEntry(elem.getName(), elem.getNormalizedIndex())) {
                ChildNodeEntry cne = state.getChildNodeEntry(elem.getName(), elem.getNormalizedIndex());
                if (cne.isAvailable()) {
                    state = cne.getNodeState();
                } else {
                    return null;
                }
            } else if (elem.getIndex() == 0 // property must not have index
                    && state.hasPropertyName(elem.getName())
                    && i == path.getLength() - 1) { // property must be final path element
                // TODO: check if available
                return state.getPropertyState(elem.getName());
            } else {
                throw new NoSuchItemStateException(path.toString());
            }
        }
        return state;
    }
}
