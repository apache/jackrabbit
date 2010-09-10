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

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.spi.Path;

final class NamePath extends RelativePath {

    /** Serial version UID */
    private static final long serialVersionUID = -2887665244213430950L;

    private final Element element;

    public NamePath(Path parent, Element element) {
        super(parent);
        assert element.denotesName();
        this.element = element;
    }

    protected int getDepthModifier() {
        return 1;
    }

    protected Path getParent() throws RepositoryException {
        if (parent != null) {
            return parent;
        } else {
            return new CurrentPath(null);
        }
    }

    public boolean isCanonical() {
        return parent != null && parent.isCanonical();
    }

    public boolean isNormalized() {
        return parent == null
            || (parent.isNormalized()
                    && !parent.getNameElement().denotesCurrent());
    }

    public Path getNormalizedPath() throws RepositoryException {
        if (isNormalized()) {
            return this;
        } else {
            // parent is guaranteed to be !null
            Path normalized = parent.getNormalizedPath();
            if (normalized.getNameElement().denotesCurrent()) {
                normalized = null; // special case: ./a
            }
            return new NamePath(normalized, element);
        }
    }

    public Path getCanonicalPath() throws RepositoryException {
        if (isCanonical()) {
            return this;
        } else if (parent != null) {
            return new NamePath(parent.getCanonicalPath(), element);
        } else {
            throw new RepositoryException(
                    "There is no canonical representation of " + this);
        }
    }

    public Element getNameElement() {
        return element;
    }

    public String getString() {
        if (parent != null) {
            return parent.getString() + Path.DELIMITER + element.getString();
        } else {
            return element.getString();
        }
    }

}
