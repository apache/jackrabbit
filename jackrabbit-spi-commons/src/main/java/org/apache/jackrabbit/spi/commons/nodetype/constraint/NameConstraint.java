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

import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.spi.commons.conversion.NameException;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.conversion.NameResolver;
import org.apache.jackrabbit.spi.commons.nodetype.InvalidConstraintException;

/**
 * <code>NameConstraint</code> ...
 */
class NameConstraint extends ValueConstraint {

    private final Name name;

    static NameConstraint create(String nameString) {
        // constraint format: String representation of a Name object
        return new NameConstraint(nameString, NAME_FACTORY.create(nameString));
    }

    static NameConstraint create(String jcrName, NameResolver resolver)
            throws InvalidConstraintException {
        // constraint format: A JCR name string.
        try {
            Name name = resolver.getQName(jcrName);
            return new NameConstraint(name.toString(), name);
        } catch (NameException e) {
            String msg = "Invalid name constraint: " + jcrName;
            log.debug(msg);
            throw new InvalidConstraintException(msg, e);
        } catch (NamespaceException e) {
            String msg = "Invalid name constraint: " + jcrName;
            log.debug(msg);
            throw new InvalidConstraintException(msg, e);
        }
    }

    private NameConstraint(String nameString, Name name) {
        super(nameString);
        this.name = name;
    }

    /**
     * Uses {@link NamePathResolver#getJCRName(Name)} to convert the
     * <code>Name</code> identifying this constraint into a JCR name String.
     *
     * @see ValueConstraint#getDefinition(NamePathResolver)
     * @param resolver name-path resolver
     */
    @Override
    public String getDefinition(NamePathResolver resolver) {
        try {
            return resolver.getJCRName(name);
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
            case PropertyType.NAME:
                Name n = value.getName();
                if (!name.equals(n)) {
                    throw new ConstraintViolationException(n
                            + " does not satisfy the constraint '"
                            + getString() + "'");
                }
                return;

            default:
                String msg = "NAME constraint can not be applied to value of type: "
                        + PropertyType.nameFromValue(value.getType());
                log.debug(msg);
                throw new RepositoryException(msg);
        }
    }

}
