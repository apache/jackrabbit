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
package org.apache.jackrabbit.spi.commons.nodetype.constraint;

import javax.jcr.NamespaceException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.ConstraintViolationException;

import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.spi.PathFactory;
import org.apache.jackrabbit.spi.commons.conversion.NameException;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.conversion.PathResolver;
import org.apache.jackrabbit.spi.commons.name.PathFactoryImpl;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.spi.commons.nodetype.InvalidConstraintException;

/**
 * <code>PathConstraint</code> ...
 */
class PathConstraint extends ValueConstraint {

    static final String WILDCARD = Path.DELIMITER + NameConstants.ANY_NAME.toString();
    static final String JCR_WILDCARD = "/*";
    // TODO improve. don't rely on a specific factory impl
    private static final PathFactory PATH_FACTORY = PathFactoryImpl.getInstance();

    private final Path path;
    private final boolean deep;

    static PathConstraint create(String pathString) {
        // constraint format: String representation of an absolute or relative
        // Path object with optionally having a trailing wild card
        if (WILDCARD.equals(pathString)) {
            return new PathConstraint(pathString, PATH_FACTORY.getRootPath(), true);
        } else {
            boolean deep = pathString.endsWith(WILDCARD);
            Path path;
            if (deep) {
                path = PATH_FACTORY.create(pathString.substring(0, pathString.length() - WILDCARD.length()));
            } else {
                path = PATH_FACTORY.create(pathString);
            }
            return new PathConstraint(pathString, path, deep);
        }
    }

    static PathConstraint create(String jcrPath, PathResolver resolver)
            throws InvalidConstraintException {
        try {
            // constraint format: absolute or relative path with optional
            // trailing wild card
            boolean deep = jcrPath.endsWith(JCR_WILDCARD);
            Path path;
            if (JCR_WILDCARD.equals(jcrPath)) {
                path = PATH_FACTORY.getRootPath();
            } else {
                if (deep) {
                    // trim trailing wild card before building path
                    jcrPath = jcrPath.substring(0, jcrPath.length() - JCR_WILDCARD.length());
                }
                path = resolver.getQPath(jcrPath);
            }
            StringBuffer definition = new StringBuffer(path.getString());
            if (deep) {
                definition.append(WILDCARD);
            }
            return new PathConstraint(definition.toString(), path, deep);
        } catch (NameException e) {
            String msg = "Invalid path expression specified as value constraint: " + jcrPath;
            log.debug(msg);
            throw new InvalidConstraintException(msg, e);
        } catch (NamespaceException e) {
            String msg = "Invalid path expression specified as value constraint: " + jcrPath;
            log.debug(msg);
            throw new InvalidConstraintException(msg, e);
        }
    }

    private PathConstraint(String pathString, Path path, boolean deep) {
        super(pathString);
        this.path = path;
        this.deep = deep;
    }

    /**
     * Uses {@link NamePathResolver#getJCRPath(Path)} to convert the
     * <code>Path</code> present with this constraint into a JCR path.
     *
     * @see ValueConstraint#getDefinition(NamePathResolver)
     * @param resolver name-path resolver
     */
    @Override
    public String getDefinition(NamePathResolver resolver) {
        try {
            String p = resolver.getJCRPath(path);
            if (!deep) {
                return p;
            } else if (path.denotesRoot()) {
                return p + "*";
            } else {
                return p + "/*";
            }
        } catch (NamespaceException e) {
            // should never get here, return raw definition as fallback
            return getString();
        }
    }

    /**
     * @see org.apache.jackrabbit.spi.QValueConstraint#check(QValue)
     */
    public void check(QValue value) throws ConstraintViolationException, RepositoryException {
        if (value == null) {
            throw new ConstraintViolationException("null value does not satisfy the constraint '" + getString() + "'");
        }
        switch (value.getType()) {
            case PropertyType.PATH:
                Path p = value.getPath();
                // normalize paths before comparing them
                Path p0, p1;
                try {
                    p0 = path.getNormalizedPath();
                    p1 = p.getNormalizedPath();
                } catch (RepositoryException e) {
                    throw new ConstraintViolationException("path not valid: " + e);
                }
                if (deep) {
                    try {
                        if (!p0.isAncestorOf(p1)) {
                            throw new ConstraintViolationException(p
                                + " does not satisfy the constraint '"
                                + getString() + "'");
                        }
                    } catch (RepositoryException e) {
                        // can't compare relative with absolute path
                        throw new ConstraintViolationException(p
                            + " does not satisfy the constraint '"
                            + getString() + "'");
                    }
                } else {
                    // exact match required
                    if (!p0.equals(p1)) {
                        throw new ConstraintViolationException(p
                            + " does not satisfy the constraint '"
                            + getString() + "'");
                    }
                }
                return;

            default:
                String msg = "PATH constraint can not be applied to value of type: "
                        + PropertyType.nameFromValue(value.getType());
                log.debug(msg);
                throw new RepositoryException(msg);
        }
    }

}
