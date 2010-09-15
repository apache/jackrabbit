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

import java.util.ArrayList;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.NameFactory;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.PathFactory;

/**
 * <code>PathFactoryImpl</code>...
 */
public class PathFactoryImpl implements PathFactory {

    private static PathFactory FACTORY = new PathFactoryImpl();

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
        } else {
            Path path = parent.resolve(relPath);
            if (normalize) {
                return path.getNormalizedPath();
            } else {
                return path;
            }
        }
    }

    /**
     * @see PathFactory#create(Path, Name, boolean)
     */
    public Path create(Path parent, Name name, boolean normalize) throws RepositoryException {
        return create(parent, name, Path.INDEX_UNDEFINED, normalize);
    }

    /**
     * @see PathFactory#create(Path, Name, int, boolean)
     */
    public Path create(Path parent, Name name, int index, boolean normalize) throws IllegalArgumentException, RepositoryException {
        if (RootPath.NAME.equals(name)) {
            throw new IllegalArgumentException();
        }
        Path path = new NamePath(parent, name, index);
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
        if (name != null) {
            return create(name, Path.INDEX_UNDEFINED);
        } else {
            throw new IllegalArgumentException("PathFactory.create(null)");
        }
    }

    /**
     * @see PathFactory#create(Name, int)
     */
    public Path create(Name name, int index) throws IllegalArgumentException {
        if (name == null) {
            throw new IllegalArgumentException("PathFactory.create(null, index");
        } else if (index < Path.INDEX_UNDEFINED) {
            throw new IllegalArgumentException(
                    "Index must not be negative: " + name + "[" + index + "]");
        } else if (CurrentPath.NAME.equals(name)) {
            if (index == Path.INDEX_UNDEFINED) {
                return CurrentPath.CURRENT_PATH;
            } else {
                throw new IllegalArgumentException();
            }
        } else if (ParentPath.NAME.equals(name)) {
            if (index == Path.INDEX_UNDEFINED) {
                return ParentPath.PARENT_PATH;
            } else {
                throw new IllegalArgumentException();
            }
        } else if (RootPath.NAME.equals(name)) {
            if (index == Path.INDEX_UNDEFINED) {
                return RootPath.ROOT_PATH;
            } else {
                throw new IllegalArgumentException();
            }
        } else {
            return new NamePath(null, name, index);
        }
    }

    public Path create(Path.Element element) {
        if (element.denotesCurrent()) {
            return CurrentPath.CURRENT_PATH;
        } else if (element.denotesIdentifier()) {
            return new IdentifierPath(element.getIdentifier());
        } else if (element.denotesName()) {
            return new NamePath(null, element.getName(), element.getIndex());
        } else if (element.denotesParent()) {
            return ParentPath.PARENT_PATH;
        } else if (element.denotesRoot()) {
            return RootPath.ROOT_PATH;
        } else {
            throw new IllegalArgumentException(
                    "Unknown path element type: " + element);
        }
    }

    /**
     * @see PathFactory#create(org.apache.jackrabbit.spi.Path.Element[])
     */
    public Path create(Path.Element[] elements) throws IllegalArgumentException {
        Path path = null;
        for (Path.Element element : elements) {
            if (element.denotesCurrent()) {
                path = new CurrentPath(path);
            } else if (element.denotesIdentifier()) {
                if (path != null) {
                    throw new IllegalArgumentException();
                }
                path = new IdentifierPath(element.getIdentifier());
            } else if (element.denotesName()) {
                path = new NamePath(path, element.getName(), element.getIndex());
            } else if (element.denotesParent()) {
                if (path != null && path.isAbsolute() && path.getDepth() == 0) {
                    throw new IllegalArgumentException();
                }
                path = new ParentPath(path);
            } else if (element.denotesRoot()) {
                if (path != null) {
                    throw new IllegalArgumentException();
                }
                path = RootPath.ROOT_PATH;
            }
        }
        return path;
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
        return create(list.toArray(new Path.Element[list.size()]));
    }

    /**
     * @see PathFactory#createElement(Name)
     */
    public Path.Element createElement(Name name) throws IllegalArgumentException {
        if (name == null) {
            throw new IllegalArgumentException("name must not be null");
        } else if (name.equals(ParentPath.NAME)) {
            return ParentPath.PARENT_PATH;
        } else if (name.equals(CurrentPath.NAME)) {
            return CurrentPath.CURRENT_PATH;
        } else if (name.equals(RootPath.NAME)) {
            return RootPath.ROOT_PATH;
        } else {
            return new NamePath(null, name, Path.INDEX_UNDEFINED);
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
        } else if (name.equals(ParentPath.NAME)
                || name.equals(CurrentPath.NAME)
                || name.equals(RootPath.NAME)) {
            throw new IllegalArgumentException(
                    "Special path elements (root, '.' and '..') can not have an explicit index: "
                    + name + "[" + index + "]");
        } else {
            return new NamePath(null, name, index);
        }
    }

    public Path.Element createElement(String identifier) throws IllegalArgumentException {
        if (identifier == null) {
            throw new IllegalArgumentException("The id must not be null.");
        } else {
            return new IdentifierPath(identifier);
        }
    }

    /**
     * Create an element from the element string
     */
    private Path.Element createElementFromString(String elementString) {
        if (elementString == null) {
            throw new IllegalArgumentException("null PathElement literal");
        }
        if (elementString.equals(RootPath.NAME.toString())) {
            return RootPath.ROOT_PATH;
        } else if (elementString.equals(CurrentPath.CURRENT_PATH.getString())) {
            return CurrentPath.CURRENT_PATH;
        } else if (elementString.equals(ParentPath.PARENT_PATH.getString())) {
            return ParentPath.PARENT_PATH;
        } else if (elementString.startsWith("[") && elementString.endsWith("]") && elementString.length() > 2) {
            return new IdentifierPath(
                    elementString.substring(1, elementString.length()-1));
        }

        NameFactory factory = NameFactoryImpl.getInstance();
        int pos = elementString.indexOf('[');
        if (pos == -1) {
            Name name = factory.create(elementString);
            return new NamePath(null, name, Path.INDEX_UNDEFINED);
        }
        Name name = factory.create(elementString.substring(0, pos));
        int pos1 = elementString.indexOf(']');
        if (pos1 == -1) {
            throw new IllegalArgumentException("invalid PathElement literal: " + elementString + " (missing ']')");
        }
        try {
            int index = Integer.valueOf(elementString.substring(pos + 1, pos1));
            if (index < 1) {
                throw new IllegalArgumentException("invalid PathElement literal: " + elementString + " (index is 1-based)");
            }
            return new NamePath(null, name, index);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("invalid PathElement literal: " + elementString + " (" + e.getMessage() + ")");
        }
    }

    /**
     * @see PathFactory#getCurrentElement()
     */
    public Path.Element getCurrentElement() {
        return CurrentPath.CURRENT_PATH;
    }

    /**
     * @see PathFactory#getParentElement()
     */
    public Path.Element getParentElement() {
        return ParentPath.PARENT_PATH;
    }

    /**
     * @see PathFactory#getRootElement()
     */
    public Path.Element getRootElement() {
        return RootPath.ROOT_PATH;
    }

    /**
     * @see PathFactory#getRootPath()
     */
    public Path getRootPath() {
        return RootPath.ROOT_PATH;
    }

}
