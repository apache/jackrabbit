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

/**
 * Helper class used to build a path from pre-parsed path elements.
 * <p>
 * Note that this class does neither validate the format of the path elements nor
 * does it validate the format of the entire path.
 * This class should therefore only be used in situations, where the elements
 * and the resulting path are known to be valid. The regular way of creating
 * a <code>Path</code> object is by calling any of the
 * <code>PathFactory.create()</code>methods.
 */
public final class PathBuilder {

    /**
     * The path factory
     */
    private final PathFactory factory;

    /**
     * The current path
     */
    private Path path = null;

    /**
     * Creates a new PathBuilder to create a Path using the
     * {@link PathFactoryImpl default PathFactory}. See
     * {@link PathBuilder#PathBuilder(PathFactory)} for a constructor explicitly
     * specifying the factory to use.
     */
    public PathBuilder() {
        this(PathFactoryImpl.getInstance());
    }

    /**
     * Creates a new PathBuilder.
     *
     * @param factory The PathFactory used to create the elements and the final path.
     */
    public PathBuilder(PathFactory factory) {
        this.factory = factory;
    }

    /**
     * Creates a new PathBuilder and initialized it with the given path
     * elements.
     *
     * @param elements
     */
    public PathBuilder(Path.Element[] elements) {
        this();
        path = factory.create(elements);
    }

    /**
     * Creates a new PathBuilder and initialized it with elements of the
     * given path.
     *
     * @param parent
     */
    public PathBuilder(Path parent) {
        this();
        path = parent;
    }

    /**
     * Adds the {@link org.apache.jackrabbit.spi.PathFactory#getRootElement()}.
     */
    public void addRoot() {
        if (path != null) {
            path = RootPath.ROOT_PATH.resolve(path);
        } else {
            path = RootPath.ROOT_PATH;
        }
    }

    /**
     * Adds the given elements
     *
     * @param elements
     */
    public void addAll(Path.Element[] elements) {
        for (Path.Element element : elements) {
            if (path != null) {
                path = path.resolve(element);
            } else {
                path = factory.create(element);
            }
        }
    }

    /**
     * Inserts the element at the beginning of the path to be built.
     *
     * @param elem
     */
    public void addFirst(Path.Element elem) {
        Path first = factory.create(elem);
        if (path != null) {
            path = first.resolve(path);
        } else {
            path = first;
        }
    }

    /**
     * Inserts the element at the beginning of the path to be built.
     *
     * @param name
     */
    public void addFirst(Name name) {
        addFirst(factory.createElement(name));
    }

    /**
     * Inserts the element at the beginning of the path to be built.
     *
     * @param name
     * @param index
     */
    public void addFirst(Name name, int index) {
        addFirst(factory.createElement(name, index));
    }

    /**
     * Inserts the element at the end of the path to be built.
     *
     * @param elem
     */
    public void addLast(Path.Element elem) {
        if (path != null) {
            path = path.resolve(elem);
        } else {
            path = factory.create(elem);
        }
    }

    /**
     * Inserts the element at the end of the path to be built.
     *
     * @param name
     */
    public void addLast(Name name) {
        addLast(name, Path.INDEX_UNDEFINED);
    }

    /**
     * Inserts the element at the end of the path to be built.
     *
     * @param name
     * @param index
     */
    public void addLast(Name name, int index) {
        path = new NamePath(path, name, index);
    }

    /**
     * Assembles the built path and returns a new {@link Path}.
     *
     * @return a new {@link Path}
     * @throws MalformedPathException if the internal path element queue is empty.
     */
    public Path getPath() throws MalformedPathException {
        if (path != null) {
            return path;
        } else {
            throw new MalformedPathException("empty path");
        }
    }
}