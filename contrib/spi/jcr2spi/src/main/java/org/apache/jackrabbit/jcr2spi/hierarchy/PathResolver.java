/*
 * $Id$
 *
 * Copyright 1997-2005 Day Management AG
 * Barfuesserplatz 6, 4001 Basel, Switzerland
 * All Rights Reserved.
 *
 * This software is the confidential and proprietary information of
 * Day Management AG, ("Confidential Information"). You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Day.
 */
package org.apache.jackrabbit.jcr2spi.hierarchy;

import org.apache.jackrabbit.name.Path;
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.name.MalformedPathException;
import org.apache.jackrabbit.jcr2spi.state.ItemStateFactory;
import org.apache.jackrabbit.jcr2spi.state.ItemState;
import org.apache.jackrabbit.jcr2spi.state.ItemStateException;
import org.apache.jackrabbit.jcr2spi.state.NoSuchItemStateException;
import org.apache.jackrabbit.spi.IdFactory;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.PropertyId;

import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

/**
 * <code>PathResolver</code> resolves a relative Path starting at a given
 * ItemState and returns the Item where the path points to.
 */
public class PathResolver {

    /**
     * The starting point to resolve the path.
     */
    private final NodeEntry start;

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
    private PathResolver(NodeEntry start, Path relPath) {
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
     * @throws IllegalArgumentException if path is absolute or not normalized
     * or starts with a parent ('..') path element.
     * @throws PathNotFoundException the the path cannot be resolved.
     * @throws RepositoryException if an error occurs while retrieving the item state.
     */
    public static HierarchyEntry resolve(NodeEntry start, Path path, ItemStateFactory isf, IdFactory idFactory) throws PathNotFoundException, RepositoryException {
        return new PathResolver(start, path).resolve(isf, idFactory);
    }

    /**
     * Looks up the <code>ItemState</code> at <code>path</code> starting at
     * <code>start</code>.
     *
     * @param start   the starting point.
     * @param path the path to resolve.
     * @return the resolved HierarchyEntry or <code>null</code> if the item is not
     * available.
     * @throws IllegalArgumentException if path is absolute or not normalized
     * or starts with a parent ('..') path element.
     */
    public static HierarchyEntry lookup(NodeEntry start, Path path) {
        return new PathResolver(start, path).lookup();
    }

    /**
     * Resolves the path.
     *
     * @return the hierarchy entry identified by the path.
     * @throws PathNotFoundException the the path cannot be resolved.
     * @throws RepositoryException if an error occurs while retrieving the item state.
     */
    private HierarchyEntry resolve(ItemStateFactory isf, IdFactory idFactory) throws PathNotFoundException, RepositoryException {
        // TODO: check again.
        NodeEntry entry = start;
        Path.PathElement[] elems = path.getElements();
        for (int i = 0; i < elems.length; i++) {
            Path.PathElement elem = elems[i];
            // check for root element
            if (elem.denotesRoot()) {
                if (start.getParent() != null) {
                    throw new PathNotFoundException("NodeEntry out of 'hierarchy'" + path.toString());
                } else {
                    continue;
                }
            }

            int index = elem.getNormalizedIndex();
            QName name = elem.getName();

            // first try to resolve to nodeEntry
            if (entry.hasNodeEntry(name, index)) {
                NodeEntry cne = entry.getNodeEntry(name, index);
                entry = cne;
            } else if (index == Path.INDEX_DEFAULT // property must not have index
                    && entry.hasPropertyEntry(name)
                    && i == path.getLength() - 1) { // property must be final path element
                PropertyEntry pe = entry.getPropertyEntry(name);
                return pe;
            } else {
                // try to resolve the HierarchyEntry corresponding to the given path
                Path remaingPath;
                try {
                    Path.PathBuilder pb = new Path.PathBuilder();
                    for (int j = i; j < elems.length; j++) {
                        pb.addLast(elems[j]);
                    }
                    remaingPath = pb.getPath();
                } catch (MalformedPathException e) {
                    throw new RepositoryException(e);
                }

                try {
                    ItemState state = retrieveItemState(remaingPath, index, entry, isf, idFactory);
                    return state.getHierarchyEntry();
                } catch (NoSuchItemStateException e) {
                    throw new PathNotFoundException(path.toString(), e);
                } catch (ItemStateException e) {
                    throw new RepositoryException(e);
                }
            }
        }
        return entry;
    }

    /**
     * Unknown entry (not-existing or not yet loaded):
     * Skip all intermediate entries and directly try to load the ItemState
     * (including building the itermediate entries.<br>
     * If that fails NoSuchItemStateException is thrown.<br>
     *
     * Since 'path' might be ambigous (Node or Property), apply some logic:<br>
     * 1) first try Node<br>
     * 2) if the NameElement does not have SNS-index => try Property<br>
     * 3) else throw directly
     *
     * @param remainingPath
     * @param index
     * @param anyParent
     * @param isf
     * @param idFactory
     * @return
     * @throws NoSuchItemStateException
     * @throws ItemStateException
     */
    private ItemState retrieveItemState(Path remainingPath, int index,
                                        NodeEntry anyParent, ItemStateFactory isf,
                                        IdFactory idFactory)
        throws NoSuchItemStateException, ItemStateException, PathNotFoundException {
        NodeId anyId = anyParent.getId();
        NodeId nodeId = idFactory.createNodeId(anyId, remainingPath);
        try {
            return isf.createDeepNodeState(nodeId, anyParent);
        } catch (NoSuchItemStateException e) {
            if (index == Path.INDEX_DEFAULT) {
                // possibly  propstate
                nodeId = (remainingPath.getLength() == 1) ? anyId : idFactory.createNodeId(anyId, remainingPath.getAncestor(1));
                PropertyId id = idFactory.createPropertyId(nodeId, remainingPath.getNameElement().getName());
                return isf.createDeepPropertyState(id, anyParent);
            } else {
                // rethrow
                throw new NoSuchItemStateException(e.getMessage(), e);
            }
        }
    }

    /**
     * Resolves the path but returns <code>null</code> if the entry has not yet
     * been loaded.
     *
     * @return the HierarchyEntry or <code>null</code> if the entry does not exist.
     */
    private HierarchyEntry lookup() {
        NodeEntry entry = start;
        for (int i = 0; i < path.getLength(); i++) {
            Path.PathElement elem = path.getElement(i);
            // check for root element
            if (elem.denotesRoot()) {
                if (start.getParent() != null) {
                    return null;
                } else {
                    continue;
                }
            }

            int index = elem.getNormalizedIndex();
            QName name = elem.getName();

            // first try to resolve node
            if (entry.hasNodeEntry(name, index)) {
                NodeEntry cne = entry.getNodeEntry(name, index);
                entry = cne;
            } else if (index == Path.INDEX_DEFAULT // property must not have index
                    && entry.hasPropertyEntry(name)
                    && i == path.getLength() - 1) { // property must be final path element
                PropertyEntry pe = entry.getPropertyEntry(name);
                return pe;
            } else {
                return null;
            }
        }
        return entry;
    }
}
