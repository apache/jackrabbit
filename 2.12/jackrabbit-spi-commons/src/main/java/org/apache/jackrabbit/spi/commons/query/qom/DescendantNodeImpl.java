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
package org.apache.jackrabbit.spi.commons.query.qom;

import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;

import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.qom.DescendantNode;
import javax.jcr.NamespaceException;

/**
 * <code>DescendantNodeImpl</code>...
 */
public class DescendantNodeImpl
        extends ConstraintImpl
        implements DescendantNode {

    /**
     * A selector name.
     */
    private final Name selectorName;

    /**
     * An absolute path.
     */
    private final Path path;

    DescendantNodeImpl(NamePathResolver resolver,
                       Name selectorName,
                       Path path)
            throws InvalidQueryException, NamespaceException {
        super(resolver);
        this.selectorName = selectorName;
        this.path = path;
        if (!path.isAbsolute()) {
            throw new InvalidQueryException(resolver.getJCRPath(path) +
                    " is not an absolute path");
        }
    }

    /**
     * Gets the name of the selector against which to apply this constraint.
     *
     * @return the selector name; non-null
     */
    public String getSelectorName() {
        return getJCRName(selectorName);
    }

    /**
     * Gets the absolute path.
     *
     * @return the path; non-null
     */
    public String getAncestorPath() {
        return getJCRPath(path);
    }

    /**
     * Gets the name of the selector against which to apply this constraint.
     *
     * @return the selector name; non-null
     */
    public Name getSelectorQName() {
        return selectorName;
    }

    /**
     * Gets the absolute path.
     *
     * @return the path; non-null
     */
    public Path getQPath() {
        return path;
    }

    //------------------------< AbstractQOMNode >-------------------------------

    /**
     * Accepts a <code>visitor</code> and calls the appropriate visit method
     * depending on the type of this QOM node.
     *
     * @param visitor the visitor.
     */
    public Object accept(QOMTreeVisitor visitor, Object data) throws Exception {
        return visitor.visit(this, data);
    }

    //------------------------< Object >----------------------------------------

    public String toString() {
        return "ISDESCENDANTNODE(" + getSelectorName() + ", " + quote(path) + ")";
    }

}
