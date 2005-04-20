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
package org.apache.jackrabbit.core.lock;

import org.apache.jackrabbit.core.Path;
import org.apache.jackrabbit.core.QName;

import java.util.*;

/**
 * Generic path map that associates information with the individual path elements
 * of a path.
 */
public class PathMap {

    /**
     * Root element
     */
    private final Child root = new Child(null, Path.ROOT.getNameElement());

    /**
     * Map a path to a child. If <code>exact</code> is <code>false</code>,
     * returns the last available item along the path that is stored in the map.
     * @param path path to map
     * @param exact flag indicating whether an exact match is required
     * @return child, maybe <code>null</code> if <code>exact</code> is
     *         <code>true</code>
     */
    public Child map(Path path, boolean exact) {
        Path.PathElement[] elements = path.getElements();
        Child current = root;

        for (int i = 1; i < elements.length; i++) {
            Child next = current.getChild(elements[i], false);
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
     * Create a child given by its path. The path map will create any necessary
     * intermediate children.
     * @param path path to child
     * @param obj object to store at destination
     */
    public void put(Path path, Object obj) {
        Path.PathElement[] elements = path.getElements();
        Child current = root;

        for (int i = 1; i < elements.length; i++) {
            current = current.getChild(elements[i], true);
        }
        current.obj = obj;
    }

    /**
     * Ressurrect a child previously removed, given by its new path and the
     * child structure.
     * @param path path to child
     * @param zombie previously removed child object to store at destination
     */
    public void resurrect(Path path, Child zombie) {
        Path.PathElement[] elements = path.getElements();
        Child current = root;

        for (int i = 1; i < elements.length; i++) {
            current = current.getChild(elements[i], true);
        }

        current.children = zombie.children;
        current.childrenCount = zombie.childrenCount;
        current.obj = zombie.obj;
    }

    /**
     * Traverse the path map and call back requester.
     * @param includeEmpty if <code>true</code> invoke call back on every child
     *                     regardless, whether the associated object is empty
     *                     or not; otherwise call back on non-empty children
     *                     only
     */
    public void traverse(ChildVisitor visitor, boolean includeEmpty) {
        root.traverse(visitor, includeEmpty);
    }

    /**
     * Internal class holding the object associated with a certain
     * path element.
     */
    public static class Child {

        /**
         * Parent child
         */
        private final Child parent;

        /**
         * Map of immediate children of this child.
         */
        private Map children;

        /**
         * Number of non-null children
         */
        private int childrenCount;

        /**
         * Object associated with this child
         */
        private Object obj;

        /**
         * QName associated with this child
         */
        private QName name;

        /**
         * index associated with this child
         */
        private int index;

        /**
         * Create a new instance of this class.
         * @param parent parent of this child
         * @param element path element of this child
         * @param obj associated object
         */
        Child(Child parent, Path.PathElement element, Object obj) {
            this.parent = parent;
            this.name = element.getName();
            this.index = element.getIndex();
            this.obj = obj;
        }

        /**
         * Create a new instance of this class.
         * @param parent parent of this child
         * @param element path element of this child
         */
        Child(Child parent, Path.PathElement element) {
            this(parent, element, null);
        }

        /**
         * Insert an empty child. Will shift all children having an index
         * greater than or equal to the child inserted to the right.
         * @param element child's path element
         */
        public void insertChild(Path.PathElement element) {
            int index = getOneBasedIndex(element) - 1;
            if (children != null) {
                ArrayList list = (ArrayList) children.get(element.getName());
                if (list != null && list.size() > index) {
                    for (int i = index; i < list.size(); i++) {
                        Child child = (Child) list.get(i);
                        if (child != null) {
                            child.index++;
                        }
                    }
                    list.add(index, null);
                }
            }
        }

        /**
         * Remove a child. Will shift all children having an index greater than
         * the child removed to the left.
         * @param element child's path element
         * @return removed child, may be <code>null</code>
         */
        public Child removeChild(Path.PathElement element) {
            int index = getOneBasedIndex(element) - 1;
            if (children != null) {
                ArrayList list = (ArrayList) children.get(element.getName());
                if (list != null && list.size() > index) {
                    for (int i = index + 1; i < list.size(); i++) {
                        Child child = (Child) list.get(i);
                        if (child != null) {
                            child.index--;
                        }
                    }
                    Child child = (Child) list.remove(index);
                    if (child != null) {
                        childrenCount--;
                    }
                    if (obj == null && childrenCount == 0) {
                        remove();
                    }
                    return child;
                }
            }
            return null;
        }

        /**
         * Return a child matching a path element. If a child doesn not exist
         * at that position and <code>create</code> is <code>true</code> a
         * new child will be created.
         * @param element child's path element
         * @param create flag indicating whether this child should be
         *        created if not available
         */
        private Child getChild(Path.PathElement element, boolean create) {
            int index = getOneBasedIndex(element) - 1;
            Child child = null;

            if (children != null) {
                ArrayList list = (ArrayList) children.get(element.getName());
                if (list != null && list.size() > index) {
                    child = (Child) list.get(index);
                }
            }
            if (child == null && create) {
                if (children == null) {
                    children = new HashMap();
                }
                ArrayList list = (ArrayList) children.get(element.getName());
                if (list == null) {
                    list = new ArrayList();
                    children.put(element.getName(), list);
                }
                while (list.size() < index) {
                    list.add(null);
                }
                child = new Child(this, element);
                list.add(child);
                childrenCount++;
            }
            return child;
        }

        /**
         * Remove this child. Delegates the call to the parent item.
         */
        public void remove() {
            if (parent != null) {
                parent.removeChild(getPathElement());
            }
        }

        /**
         * Return the object associated with this child
         * @return object associated with this child
         */
        public Object get() {
            return obj;
        }

        /**
         * Set the object associated with this child
         * @param obj object associated with this child
         */
        public void set(Object obj) {
            this.obj = obj;

            if (obj == null && childrenCount == 0) {
                remove();
            }
        }

        /**
         * Return a path element pointing to this child
         * @return path element
         */
        public Path.PathElement getPathElement() {
            return Path.create(name, index).getNameElement();
        }

        /**
         * Checks whether this child has the specified path
         * @return path path to compare to
         */
        public boolean hasPath(Path path) {
            return hasPath(path.getElements(), path.getLength());
        }

        /**
         * Checks whether this child has the specified path, given by
         * path elements.
         * @param elements path elements to compare to
         * @param len number of elements to compare to
         * @return <code>true</code> if this child has the path given;
         *         otherwise <code>false</code>
         */
        private boolean hasPath(Path.PathElement[] elements, int len) {
            if (getPathElement().equals(elements[len - 1])) {
                if (parent != null) {
                    return parent.hasPath(elements, len - 1);
                }
                return true;
            }
            return false;
        }

        /**
         * Return 1-based index of a path element.
         */
        private static int getOneBasedIndex(Path.PathElement element) {
            int index = element.getIndex();
            if (index == 0) {
                return 1;
            } else {
                return index;
            }
        }

        /**
         * Recursively invoked traversal method.
         */
        public void traverse(ChildVisitor visitor, boolean includeEmpty) {
            if (children != null) {
                Iterator iter = children.values().iterator();
                while (iter.hasNext()) {
                    ArrayList list = (ArrayList) iter.next();
                    for (int i = 0; i < list.size(); i++) {
                        Child child = (Child) list.get(i);
                        if (child != null) {
                            child.traverse(visitor, includeEmpty);
                        }
                    }
                }
            }
            if (includeEmpty || obj != null) {
                visitor.childVisited(this);
            }
        }

    }

    /**
     * Child visitor used in {@link PathMap#traverse}
     */
    public interface ChildVisitor {

        /**
         * Invoked for every child visited on a tree traversal
         * @param child child visited
         */
        void childVisited(Child child);
    }
}
