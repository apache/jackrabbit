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

import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.PathFactory;
import org.apache.jackrabbit.spi.NameFactory;

import javax.jcr.RepositoryException;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * <code>PathFactoryImpl</code>...
 */
public class PathFactoryImpl implements PathFactory {

    private static PathFactory FACTORY = new PathFactoryImpl();

    private static final String CURRENT_LITERAL = ".";
    private static final String PARENT_LITERAL = "..";

    private static final NameFactory NAME_FACTORY = NameFactoryImpl.getInstance();
    private final static Name CURRENT_NAME = NAME_FACTORY.create(Name.NS_DEFAULT_URI, CURRENT_LITERAL);
    private final static Name PARENT_NAME = NAME_FACTORY.create(Name.NS_DEFAULT_URI, PARENT_LITERAL);
    private final static Name ROOT_NAME = NAME_FACTORY.create(Name.NS_DEFAULT_URI, "");

    /**
     * the root path
     */
    private static final Path ROOT = RootPath.INSTANCE;

    private PathFactoryImpl() {}

    public static PathFactory getInstance() {
        return FACTORY;
    }

    //--------------------------------------------------------< PathFactory >---
    /**
     * @see PathFactory#create(Path, Path, boolean)
     */
    public Path create(Path parent, Path relPath, boolean normalize) throws IllegalArgumentException, RepositoryException {
        if (relPath.isAbsolute()) {
            throw new IllegalArgumentException(
                    "relPath is not a relative path: " + relPath);
        }
        List<Path.Element> l = new ArrayList<Path.Element>();
        l.addAll(Arrays.asList(parent.getElements()));
        l.addAll(Arrays.asList(relPath.getElements()));

        Builder pb;
        try {
            pb = new Builder(l);
        } catch (IllegalArgumentException iae) {
             throw new RepositoryException(iae.getMessage());
        }
        Path path = pb.getPath();
        if (normalize) {
            return path.getNormalizedPath();
        } else {
            return path;
        }
    }

    /**
     * @see PathFactory#create(Path, Name, boolean)
     */
    public Path create(Path parent, Name name, boolean normalize) throws RepositoryException {
        List<Path.Element> elements = new ArrayList<Path.Element>();
        elements.addAll(Arrays.asList(parent.getElements()));
        elements.add(createElement(name));

        Builder pb;
        try {
            pb = new Builder(elements);
        } catch (IllegalArgumentException iae) {
             throw new RepositoryException(iae.getMessage());
        }
        Path path = pb.getPath();
        if (normalize) {
            return path.getNormalizedPath();
        } else {
            return path;
        }
    }

    /**
     * @see PathFactory#create(Path, Name, int, boolean)
     */
    public Path create(Path parent, Name name, int index, boolean normalize) throws IllegalArgumentException, RepositoryException {
        List<Path.Element> elements = new ArrayList<Path.Element>();
        elements.addAll(Arrays.asList(parent.getElements()));
        elements.add(createElement(name, index));

        Builder pb;
        try {
            pb = new Builder(elements);
        } catch (IllegalArgumentException iae) {
             throw new RepositoryException(iae.getMessage());
        }
        Path path = pb.getPath();
        if (normalize) {
            return path.getNormalizedPath();
        } else {
            return path;
        }
    }

    /**
     * @see PathFactory#create(Name)
     */
    public Path create(Name name) throws IllegalArgumentException {
        Path.Element elem = createElement(name);
        return new Builder(new Path.Element[]{elem}).getPath();
    }

    /**
     * @see PathFactory#create(Name, int)
     */
    public Path create(Name name, int index) throws IllegalArgumentException {
        if (index < Path.INDEX_UNDEFINED) {
            throw new IllegalArgumentException(
                    "Index must not be negative: " + name + "[" + index + "]");
        }
        Path.Element elem = createElement(name, index);
        return new Builder(new Path.Element[]{elem}).getPath();
    }

    /**
     * @see PathFactory#create(org.apache.jackrabbit.spi.Path.Element[])
     */
    public Path create(Path.Element[] elements) throws IllegalArgumentException {
        return new Builder(elements).getPath();
    }

    /**
     * @see PathFactory#create(String)
     */
    public Path create(String pathString) throws IllegalArgumentException {
        if (pathString == null || "".equals(pathString)) {
            throw new IllegalArgumentException("No Path literal specified");
        }
        // split into path elements
        int lastPos = 0;
        int pos = pathString.indexOf(Path.DELIMITER);
        ArrayList<Path.Element> list = new ArrayList<Path.Element>();
        while (lastPos >= 0) {
            Path.Element elem;
            if (pos >= 0) {
                elem = createElementFromString(pathString.substring(lastPos, pos));
                lastPos = pos + 1;
                pos = pathString.indexOf(Path.DELIMITER, lastPos);
            } else {
                elem = createElementFromString(pathString.substring(lastPos));
                lastPos = -1;
            }
            list.add(elem);
        }
        return new Builder(list).getPath();
    }

    /**
     * @see PathFactory#createElement(Name)
     */
    public Path.Element createElement(Name name) throws IllegalArgumentException {
        if (name == null) {
            throw new IllegalArgumentException("name must not be null");
        } else if (name.equals(PARENT_NAME)) {
            return ParentElement.INSTANCE;
        } else if (name.equals(CURRENT_NAME)) {
            return CurrentElement.INSTANCE;
        } else if (name.equals(ROOT_NAME)) {
            return RootElement.INSTANCE;
        } else {
            return NameElement.create(name, Path.INDEX_UNDEFINED);
        }
    }

    /**
     * @see PathFactory#createElement(Name, int)
     */
    public Path.Element createElement(Name name, int index) throws IllegalArgumentException {
        if (index < Path.INDEX_UNDEFINED) {
            throw new IllegalArgumentException(
                    "The index may not be negative: " + name + "[" + index + "]");
        } else if (name == null) {
            throw new IllegalArgumentException("The name must not be null");
        } else if (name.equals(PARENT_NAME)
                || name.equals(CURRENT_NAME)
                || name.equals(ROOT_NAME)) {
            throw new IllegalArgumentException(
                    "Special path elements (root, '.' and '..') can not have an explicit index: "
                    + name + "[" + index + "]");
        } else {
            return NameElement.create(name, index);
        }
    }

    public Path.Element createElement(String identifier) throws IllegalArgumentException {
        if (identifier == null) {
            throw new IllegalArgumentException("The id must not be null.");
        } else {
            return new IdentifierElement(identifier);
        }
    }

    /**
     * Create an element from the element string
     */
    private Path.Element createElementFromString(String elementString) {
        if (elementString == null) {
            throw new IllegalArgumentException("null PathElement literal");
        }
        if (elementString.equals(ROOT_NAME.toString())) {
            return RootElement.INSTANCE;
        } else if (elementString.equals(CURRENT_LITERAL)) {
            return CurrentElement.INSTANCE;
        } else if (elementString.equals(PARENT_LITERAL)) {
            return ParentElement.INSTANCE;
        } else if (elementString.startsWith("[") && elementString.endsWith("]") && elementString.length() > 2) {
            return new IdentifierElement(elementString.substring(1, elementString.length()-1));
        }

        int pos = elementString.indexOf('[');
        if (pos == -1) {
            Name name = NAME_FACTORY.create(elementString);
            return NameElement.create(name, Path.INDEX_UNDEFINED);
        }
        Name name = NAME_FACTORY.create(elementString.substring(0, pos));
        int pos1 = elementString.indexOf(']');
        if (pos1 == -1) {
            throw new IllegalArgumentException("invalid PathElement literal: " + elementString + " (missing ']')");
        }
        try {
            int index = Integer.valueOf(elementString.substring(pos + 1, pos1));
            if (index < 1) {
                throw new IllegalArgumentException("invalid PathElement literal: " + elementString + " (index is 1-based)");
            }
            return NameElement.create(name, index);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("invalid PathElement literal: " + elementString + " (" + e.getMessage() + ")");
        }
    }

    /**
     * @see PathFactory#getCurrentElement()
     */
    public Path.Element getCurrentElement() {
        return CurrentElement.INSTANCE;
    }

    /**
     * @see PathFactory#getParentElement()
     */
    public Path.Element getParentElement() {
        return ParentElement.INSTANCE;
    }

    /**
     * @see PathFactory#getRootElement()
     */
    public Path.Element getRootElement() {
        return RootElement.INSTANCE;
    }

    /**
     * @see PathFactory#getRootPath()
     */
    public Path getRootPath() {
        return ROOT;
    }

    /**
     * Builder internal class
     */
    private static final class Builder {

        /**
         * the lpath elements of the constructed path
         */
        private final Path.Element[] elements;

        /**
         * Creates a new Builder and initialized it with the given path
         * elements.
         *
         * @param elemList
         * @throws IllegalArgumentException if the given elements array is null
         * or has a zero length or would otherwise constitute an invalid path
         */
        private Builder(List<Path.Element> elemList) throws IllegalArgumentException {
            this(elemList.toArray(new Path.Element[elemList.size()]));
        }

        /**
         * Creates a new Builder and initialized it with the given path
         * elements.
         *
         * @param elements
         * @throws IllegalArgumentException if the given elements array is null
         * or has a zero length or would otherwise constitute an invalid path
         */
        private Builder(Path.Element[] elements) throws IllegalArgumentException {
            if (elements == null || elements.length == 0) {
                throw new IllegalArgumentException("Cannot build path from null or 0 elements.");
            }

            this.elements = elements;
            if (elements.length > 1) {
                boolean absolute = elements[0].denotesRoot();
                int depth = 0;
                for (int i = 0; i < elements.length; i++) {
                    Path.Element elem = elements[i];
                    if (elem.denotesName()) {
                        depth++;
                    } else if (elem.denotesRoot()) {
                        if (i > 0) {
                            throw new IllegalArgumentException("Invalid path: The root element may only occur at the beginning.");
                        }
                    } else if (elem.denotesIdentifier()) {
                        throw new IllegalArgumentException("Invalid path: The identifier element may only occur at the beginning of a single element path.");
                    } else  if (elem.denotesParent()) {
                        depth--;
                        if (absolute && depth < 0) {
                            throw new IllegalArgumentException("Invalid path: Too many parent elements.");
                        }
                    }
                }
            }
        }

        /**
         * Assembles the built path and returns a new {@link Path}.
         *
         * @return a new {@link Path}
         */
        private Path getPath() {
            Path path = null;
            for (Path.Element element : elements) {
                if (element.denotesCurrent()) {
                    path = new CurrentPath(path);
                } else if (element.denotesIdentifier()) {
                    path = new IdentifierPath(element);
                } else if (element.denotesName()) {
                    path = new NamePath(path, element);
                } else if (element.denotesParent()) {
                    path = new ParentPath(path);
                } else if (element.denotesRoot()) {
                    path = RootPath.INSTANCE;
                }
            }
            return path;
        }
    }

}
