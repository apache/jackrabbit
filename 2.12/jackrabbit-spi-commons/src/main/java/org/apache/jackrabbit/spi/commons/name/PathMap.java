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
package org.apache.jackrabbit.spi.commons.name;

import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.PathFactory;
import org.apache.jackrabbit.spi.commons.conversion.MalformedPathException;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;


/**
 * Generic path map that associates information with the individual path elements
 * of a path.
 */
public class PathMap<T> {

    private static final PathFactory PATH_FACTORY = PathFactoryImpl.getInstance();

    /**
     * Root element
     */
    private final Element<T> root =
        new Element<T>(PATH_FACTORY.getRootElement());

    /**
     * Map a path to a child. If <code>exact</code> is <code>false</code>,
     * returns the last available item along the path that is stored in the map.
     * @param path path to map
     * @param exact flag indicating whether an exact match is required
     * @return child, maybe <code>null</code> if <code>exact</code> is
     *         <code>true</code>
     */
    public Element<T> map(Path path, boolean exact) {
        Path.Element[] elements = path.getElements();
        Element<T> current = root;

        for (int i = 1; i < elements.length; i++) {
            Element<T> next = current.getChild(elements[i]);
            if (next == null) {
                if (exact) {
                    return null;
                }
                break;
            }
            current = next;
        }
        return current;
    }

    /**
     * Create an element given by its path. The path map will create any necessary
     * intermediate elements.
     * @param path path to child
     * @param obj object to store at destination
     */
    public Element<T> put(Path path, T obj) {
        Element<T> element = put(path);
        element.obj = obj;
        return element;
    }

    /**
     * Put an element given by its path. The path map will create any necessary
     * intermediate elements.
     * @param path path to child
     * @param element element to store at destination
     */
    public void put(Path path, Element<T> element) {
        Path.Element[] elements = path.getElements();
        Element<T> current = root;

        for (int i = 1; i < elements.length - 1; i++) {
            Element<T> next = current.getChild(elements[i]);
            if (next == null) {
                next = current.createChild(elements[i]);
            }
            current = next;
        }
        current.put(path.getNameElement(), element);
    }

    /**
     * Create an empty child given by its path.
     * @param path path to child
     */
    public Element<T> put(Path path) {
        Path.Element[] elements = path.getElements();
        Element<T> current = root;

        for (int i = 1; i < elements.length; i++) {
            Element<T> next = current.getChild(elements[i]);
            if (next == null) {
                next = current.createChild(elements[i]);
            }
            current = next;
        }
        return current;
    }

    /**
     * Traverse the path map and call back requester. This method visits the root
     * first, then its children.
     * @param includeEmpty if <code>true</code> invoke call back on every child
     *                     regardless, whether the associated object is empty
     *                     or not; otherwise call back on non-empty children
     *                     only
     */
    public void traverse(ElementVisitor<T> visitor, boolean includeEmpty) {
        root.traverse(visitor, includeEmpty);
    }

    /**
     * Internal class holding the object associated with a certain
     * path element.
     */
    public final static class Element<T> {

        /**
         * Parent element
         */
        private Element<T> parent;

        /**
         * Map of immediate children
         */
        private Map<Name, List<Element<T>>> children;

        /**
         * Number of non-empty children
         */
        private int childrenCount;

        /**
         * Object associated with this element
         */
        private T obj;

        /**
         * Path.Element suitable for path construction associated with this
         * element. The path element will never have a default index. Instead an
         * undefined index value is set in that case.
         */
        private Path.Element pathElement;

        /**
         * 1-based index associated with this element where index=0 is
         * equivalent to index=1.
         */
        private int index;

        /**
         * Create a new instance of this class with a path element.
         * @param nameIndex path element of this child
         */
        private Element(Path.Element nameIndex) {
            this.index = nameIndex.getIndex();
            if (nameIndex.denotesName()) {
                updatePathElement(nameIndex.getName(), index);
            } else {
                // root, current or parent
                this.pathElement = nameIndex;
            }
        }

        /**
         * Create a child of this node inside the path map.
         * @param nameIndex position where child is created
         * @return child
         */
        private Element<T> createChild(Path.Element nameIndex) {
            Element<T> element = new Element<T>(nameIndex);
            put(nameIndex, element);
            return element;
        }

        /**
         * Updates the {@link #pathElement} with a new name and index value.
         *
         * @param name the new name.
         * @param index the new index.
         */
        private void updatePathElement(Name name, int index) {
            if (index == Path.INDEX_DEFAULT) {
                pathElement = PATH_FACTORY.createElement(name);
            } else {
                pathElement = PATH_FACTORY.createElement(name, index);
            }
        }

        /**
         * Insert an empty child. Will shift all children having an index
         * greater than or equal to the child inserted to the right.
         * @param nameIndex position where child is inserted
         */
        public void insert(Path.Element nameIndex) {
            // convert 1-based index value to 0-base value
            int index = getZeroBasedIndex(nameIndex);
            if (children != null) {
                List<Element<T>> list = children.get(nameIndex.getName());
                if (list != null && list.size() > index) {
                    for (int i = index; i < list.size(); i++) {
                        Element<T> element = list.get(i);
                        if (element != null) {
                            element.index = element.getNormalizedIndex() + 1;
                            element.updatePathElement(element.getName(), element.index);
                        }
                    }
                    list.add(index, null);
                }
            }
        }

        /**
         * Return an element matching a name and index.
         * @param nameIndex position where child is located
         * @return element matching <code>nameIndex</code> or <code>null</code> if
         *         none exists.
         */
        private Element<T> getChild(Path.Element nameIndex) {
            // convert 1-based index value to 0-base value
            int index = getZeroBasedIndex(nameIndex);
            Element<T> element = null;

            if (children != null) {
                List<Element<T>> list = children.get(nameIndex.getName());
                if (list != null && list.size() > index) {
                    element = list.get(index);
                }
            }
            return element;
        }

        /**
         * Link a child of this node. Position is given by <code>nameIndex</code>.
         * @param nameIndex position where child should be located
         * @param element element to add
         */
        public void put(Path.Element nameIndex, Element<T> element) {
            // convert 1-based index value to 0-base value
            int index = getZeroBasedIndex(nameIndex);
            if (children == null) {
                children = new HashMap<Name, List<Element<T>>>();
            }
            List<Element<T>> list = children.get(nameIndex.getName());
            if (list == null) {
                list = new ArrayList<Element<T>>();
                children.put(nameIndex.getName(), list);
            }
            while (list.size() < index) {
                list.add(null);
            }
            if (list.size() == index) {
                list.add(element);
            } else {
                list.set(index, element);
            }

            element.parent = this;
            element.index = nameIndex.getIndex();
            element.updatePathElement(nameIndex.getName(), element.index);

            childrenCount++;
        }

        /**
         * Remove a child. Will shift all children having an index greater than
         * the child removed to the left. If there are no more children left in
         * this element and no object is associated with this element, the
         * element itself gets removed.
         *
         * @param nameIndex child's path element
         * @return removed child, may be <code>null</code>
         */
        public Element<T> remove(Path.Element nameIndex) {
            return remove(nameIndex, true, true);
        }

        /**
         * Remove a child. If <code>shift</code> is set to <code>true</code>,
         * will shift all children having an index greater than the child
         * removed to the left. If <code>removeIfEmpty</code> is set to
         * <code>true</code> and there are no more children left in
         * this element and no object is associated with this element, the
         * element itself gets removed.
         *
         * @param nameIndex child's path element
         * @param shift whether to shift same name siblings having a greater
         *              index to the left
         * @param removeIfEmpty remove this element itself if it contains
         *                      no more children and is not associated to
         *                      an element
         * @return removed child, may be <code>null</code>
         */
        private Element<T> remove(Path.Element nameIndex, boolean shift,
                               boolean removeIfEmpty) {

            // convert 1-based index value to 0-base value
            int index = getZeroBasedIndex(nameIndex);
            if (children == null) {
                return null;
            }
            List<Element<T>> list = children.get(nameIndex.getName());
            if (list == null || list.size() <= index) {
                return null;
            }
            Element<T> element = list.set(index, null);
            if (shift) {
                for (int i = index + 1; i < list.size(); i++) {
                    Element<T> sibling = list.get(i);
                    if (sibling != null) {
                        sibling.index--;
                        sibling.updatePathElement(sibling.getName(), sibling.index);
                    }
                }
                list.remove(index);
            }
            if (element != null) {
                element.parent = null;
                childrenCount--;
            }
            if (removeIfEmpty && childrenCount == 0 && obj == null && parent != null) {
                parent.remove(getPathElement(), shift, true);
            }
            return element;
        }

        /**
         * Remove this element. Delegates the call to the parent item.
         * Index of same name siblings will be shifted!
         */
        public void remove() {
            remove(true);
        }

        /**
         * Remove this element. Delegates the call to the parent item.
         * @param shift if index of same name siblings will be shifted.
         */
        public void remove(boolean shift) {
            if (parent != null) {
                parent.remove(getPathElement(), shift, true);
            } else {
                // Removing the root node is not possible: if it has become
                // invalid, remove all its children and the associated object
                children = null;
                childrenCount = 0;
                obj = null;
            }
        }

        /**
         * Remove all children of this element. Removes this element itself
         * if this element does not contain associated information.
         */
        public void removeAll() {
            children = null;
            childrenCount = 0;

            if (obj == null && parent != null) {
                parent.remove(getPathElement(), false, true);
            }
        }

        /**
         * Sets a new list of children of this element.
         *
         * @param children map of children; keys are of type
         *                 <code>Path.PathElement</code> and values
         *                 are of type <code>Element</code>
         */
        public void setChildren(Map<Path.Element, Element<T>> children) {
            // Remove all children without removing the element itself
            this.children = null;
            childrenCount = 0;

            // Now add back all items
            for (Map.Entry<Path.Element, Element<T>> entry : children.entrySet()) {
                put(entry.getKey(), entry.getValue());
            }

            // Special case: if map was empty, handle like removeAll()
            if (childrenCount == 0 && obj == null && parent != null) {
                parent.remove(getPathElement(), false, true);
            }
        }

        /**
         * Return the object associated with this element
         * @return object associated with this element
         */
        public T get() {
            return obj;
        }

        /**
         * Set the object associated with this element
         * @param obj object associated with this element
         */
        public void set(T obj) {
            this.obj = obj;

            if (obj == null && childrenCount == 0 && parent != null) {
                parent.remove(getPathElement(), false, true);
            }
        }

        /**
         * Return the name of this element
         * @return name
         */
        public Name getName() {
            return pathElement.getName();
        }

        /**
         * Return the non-normalized 1-based index of this element. Note that
         * this method can return a value of 0 which should be treated as 1.
         * @return index
         * @see #getNormalizedIndex()
         */
        public int getIndex() {
            return index;
        }

        /**
         * Return the 1-based index of this element.
         * Same as {@link #getIndex()} except that an {@link Path#INDEX_UNDEFINED
         * undefined index} value is automatically converted to the
         * {@link Path#INDEX_DEFAULT default index} value.
         * @return 1-based index
         */
        public int getNormalizedIndex() {
            return pathElement.getNormalizedIndex();
        }

        /**
         * Return a path element pointing to this element
         * @return path element
         */
        public Path.Element getPathElement() {
            if (index < Path.INDEX_DEFAULT) {
                return PATH_FACTORY.createElement(getName());
            } else {
                return PATH_FACTORY.createElement(getName(), index);
            }
        }

        /**
         * Return the path of this element.
         * @return path
         * @throws MalformedPathException if building the path fails
         */
        public Path getPath() throws MalformedPathException {
            if (parent == null) {
                return PATH_FACTORY.getRootPath();
            }

            PathBuilder builder = new PathBuilder();
            getPath(builder);
            return builder.getPath();
        }

        /**
         * Internal implementation of {@link #getPath()} that populates entries
         * in a builder. On exit, <code>builder</code> contains the path
         * of this element
         */
        private void getPath(PathBuilder builder) {
            if (parent == null) {
                builder.addRoot();
                return;
            }
            parent.getPath(builder);
            builder.addLast(pathElement);
        }

        /**
         * Checks whether this element has the specified path. Introduced to
         * avoid catching a <code>MalformedPathException</code> for simple
         * path comparisons.
         * @param path path to compare to
         * @return <code>true</code> if this child has the path
         *         <code>path</code>, <code>false</code> otherwise
         */
        public boolean hasPath(Path path) {
            return hasPath(path.getElements(), path.getLength());
        }

        /**
         * Checks whether this element has the specified path, given by
         * path elements.
         * @param elements path elements to compare to
         * @param len number of elements to compare to
         * @return <code>true</code> if this element has the path given;
         *         otherwise <code>false</code>
         */
        private boolean hasPath(Path.Element[] elements, int len) {
            if (getPathElement().equals(elements[len - 1])) {
                if (parent != null) {
                    return parent.hasPath(elements, len - 1);
                }
                return true;
            }
            return false;
        }

        /**
         * Return 0-based index of a path element.
         */
        private static int getZeroBasedIndex(Path.Element nameIndex) {
            return nameIndex.getNormalizedIndex() - 1;
        }

        /**
         * Recursively invoked traversal method. This method visits the element
         * first, then its children.
         * @param visitor visitor to invoke
         * @param includeEmpty if <code>true</code> invoke call back on every
         *        element regardless, whether the associated object is empty
         *        or not; otherwise call back on non-empty children only
         */
        public void traverse(ElementVisitor<T> visitor, boolean includeEmpty) {
            if (includeEmpty || obj != null) {
                visitor.elementVisited(this);
            }
            if (children != null) {
                for (List<Element<T>>list : children.values()) {
                    for (Element<T> element : list) {
                        if (element != null) {
                            element.traverse(visitor, includeEmpty);
                        }
                    }
                }
            }
        }

        /**
         * Return the depth of this element. Defined to be <code>0</code> for the
         * root element and <code>n + 1</code> for some element if the depth of
         * its parent is <code>n</code>.
         */
        public int getDepth() {
            if (parent != null) {
                return parent.getDepth() + 1;
            }
            // Root
            return Path.ROOT_DEPTH;
        }

        /**
         * Return a flag indicating whether the specified node is a
         * child of this node.
         * @param other node to check
         */
        public boolean isAncestorOf(Element<T> other) {
            Element<T> parent = other.parent;
            while (parent != null) {
                if (parent == this) {
                    return true;
                }
                parent = parent.parent;
            }
            return false;
        }

        /**
         * Return the parent of this element
         * @return parent or <code>null</code> if this is the root element
         */
        public Element<T> getParent() {
            return parent;
        }

        /**
         * Return the children count of this element
         * @return children count
         */
        public int getChildrenCount() {
            return childrenCount;
        }

        /**
         * Return an iterator over all of this element's children. Every
         * element returned by this iterator is of type {@link Element}.
         */
        public List<Element<T>> getChildren() {
            ArrayList<Element<T>> result = new ArrayList<Element<T>>();
            if (children != null) {
                for (List<Element<T>> list : children.values()) {
                    for (Element<T> element : list) {
                        if (element != null) {
                            result.add(element);
                        }
                    }
                }
            }
            return result;
        }

        /**
         * Map a relPath starting at <code>this</code> Element. If
         * <code>exact</code> is <code>false</code>, returns the last available
         * item along the relPath that is stored in the map.
         *
         * @param relPath relPath to map
         * @param exact   flag indicating whether an exact match is required
         * @return descendant, maybe <code>null</code> if <code>exact</code> is
         *         <code>true</code>
         */
        public Element<T> getDescendant(Path relPath, boolean exact) {
            Path.Element[] elements = relPath.getElements();
            Element<T> current = this;

            for (int i = 0; i < elements.length; i++) {
                Element<T> next = current.getChild(elements[i]);
                if (next == null) {
                    if (exact) {
                        return null;
                    }
                    break;
                }
                current = next;
            }
            return current;
        }
    }

    /**
     * Element visitor used in {@link PathMap#traverse}
     */
    public interface ElementVisitor<T> {

        /**
         * Invoked for every element visited on a tree traversal
         * @param element element visited
         */
        void elementVisited(Element<T> element);
    }
}
