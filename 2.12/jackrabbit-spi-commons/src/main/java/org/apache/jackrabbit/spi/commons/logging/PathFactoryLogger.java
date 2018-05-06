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
package org.apache.jackrabbit.spi.commons.logging;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.PathFactory;
import org.apache.jackrabbit.spi.Path.Element;

/**
 * Log wrapper for a {@link PathFactory}.
 */
public class PathFactoryLogger extends AbstractLogger implements PathFactory {
    private final PathFactory pathFactory;

    /**
     * Create a new instance for the given <code>pathFactory</code> which uses
     * <code>writer</code> for persisting log messages.
     * @param pathFactory
     * @param writer
     */
    public PathFactoryLogger(PathFactory pathFactory, LogWriter writer) {
        super(writer);
        this.pathFactory = pathFactory;
    }

    /**
     * @return  the wrapped PathFactory
     */
    public PathFactory getPathFactory() {
        return pathFactory;
    }

    public Path create(final Path parent, final Path relPath, final boolean normalize)
            throws RepositoryException {

        return (Path) execute(new Callable() {
            public Object call() throws RepositoryException {
                return pathFactory.create(parent, relPath, normalize);
            }}, "create(Path, Path, boolean)", new Object[]{parent, relPath, Boolean.valueOf(normalize)});
    }

    public Path create(final Path parent, final Name name, final boolean normalize)
            throws RepositoryException {

        return (Path) execute(new Callable() {
            public Object call() throws RepositoryException {
                return pathFactory.create(parent, name, normalize);
            }}, "create(Path, Name, boolean)", new Object[]{parent, name, Boolean.valueOf(normalize)});
    }

    public Path create(final Path parent, final Name name, final int index, final boolean normalize)
            throws RepositoryException {

        return (Path) execute(new Callable() {
            public Object call() throws RepositoryException {
                return pathFactory.create(parent, name, index, normalize);
            }}, "create(Path, Name, int, boolean)", new Object[]{parent, name, new Integer(index),
                    Boolean.valueOf(normalize)});
    }

    public Path create(final Name name) {
        return (Path) execute(new SafeCallable() {
            public Object call() {
                return pathFactory.create(name);
            }}, "create(Name)", new Object[]{name});
    }

    public Path create(final Name name, final int index) {
        return (Path) execute(new SafeCallable() {
            public Object call() {
                return pathFactory.create(name, index);
            }}, "create(Name, int)", new Object[]{name, new Integer(index)});
    }

    public Path create(final Element element) {
        return (Path) execute(new SafeCallable() {
            public Object call() {
                return pathFactory.create(element);
            }}, "create(Element)", new Object[]{element});
    }

    public Path create(final Element[] elements) {
        return (Path) execute(new SafeCallable() {
            public Object call() {
                return pathFactory.create(elements);
            }}, "create(Element[])", new Object[]{elements});
    }

    public Path create(final String pathString) {
        return (Path) execute(new SafeCallable() {
            public Object call() {
                return pathFactory.create(pathString);
            }}, "create(String)", new Object[]{pathString});
    }

    public Element createElement(final Name name) {
        return (Element) execute(new SafeCallable() {
            public Object call() {
                return pathFactory.createElement(name);
            }}, "createElement(Name)", new Object[]{name});
    }

    public Element createElement(final Name name, final int index) {
        return (Element) execute(new SafeCallable() {
            public Object call() {
                return pathFactory.createElement(name, index);
            }}, "createElement(Name)", new Object[]{name, new Integer(index)});
    }

    public Element createElement(final String identifier) throws IllegalArgumentException {
        return (Element) execute(new SafeCallable() {
            public Object call() {
                return pathFactory.createElement(identifier);
            }}, "createElement(String)", new Object[]{identifier});
    }

    public Element getCurrentElement() {
        return (Element) execute(new SafeCallable() {
            public Object call() {
                return pathFactory.getCurrentElement();
            }}, "getCurrentElement()", new Object[]{});
    }

    public Element getParentElement() {
        return (Element) execute(new SafeCallable() {
            public Object call() {
                return pathFactory.getParentElement();
            }}, "getParentElement()", new Object[]{});
    }

    public Element getRootElement() {
        return (Element) execute(new SafeCallable() {
            public Object call() {
                return pathFactory.getRootElement();
            }}, "getRootElement()", new Object[]{});
    }

    public Path getRootPath() {
        return (Path) execute(new SafeCallable() {
            public Object call() {
                return pathFactory.getRootPath();
            }}, "getRootPath()", new Object[]{});
    }

}
