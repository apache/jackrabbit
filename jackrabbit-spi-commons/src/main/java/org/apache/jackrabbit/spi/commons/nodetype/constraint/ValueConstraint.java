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


import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.ConstraintViolationException;

import org.apache.jackrabbit.spi.NameFactory;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.apache.jackrabbit.spi.commons.nodetype.InvalidConstraintException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>ValueConstraint</code> and its subclasses are used to check the
 * syntax of a value constraint and to test if a specific value satisfies
 * it.
 */
public abstract class ValueConstraint {

    protected static Logger log = LoggerFactory.getLogger(ValueConstraint.class);

    public static final ValueConstraint[] EMPTY_ARRAY = new ValueConstraint[0];

    // TODO improve. don't rely on a specific factory impl
    static final NameFactory NAME_FACTORY = NameFactoryImpl.getInstance();

    private final String qualifiedDefinition;

    protected ValueConstraint(String qualifiedDefinition) {
        this.qualifiedDefinition = qualifiedDefinition;
    }

    /**
     * For constraints that are not namespace prefix mapping sensitive this
     * method returns the same result as <code>{@link #getQualifiedDefinition()}</code>.
     * <p/>
     * Those that are namespace prefix mapping sensitive (e.g.
     * <code>NameConstraint</code>, <code>PathConstraint</code> and
     * <code>ReferenceConstraint</code>) use the given <code>nsResolver</code>
     * to reflect the current mapping in the returned value.
     * In other words: subclasses, that need to make a conversion to JCR value
     * must overwrite this and return a value that has all qualified names
     * and path elements resolved.
     *
     * @return the definition of this constraint.
     * @see #getQualifiedDefinition()
     * @param resolver
     */
    public String getDefinition(NamePathResolver resolver) {
        return qualifiedDefinition;
    }

    /**
     * By default the qualified definition is the same as the JCR definition.
     *
     * @return the qualified definition String
     * @see #getDefinition(NamePathResolver)
     */
    public String getQualifiedDefinition() {
        return qualifiedDefinition;
    }

    /**
     * Check if the specified value matches the this constraint.
     *
     * @param value The value to be tested.
     * @throws ConstraintViolationException If the specified value is
     * <code>null</code> or does not matches the constraint.
     * @throws RepositoryException If another error occurs.
     */
    abstract void check(QValue value) throws ConstraintViolationException, RepositoryException;

    //-----------------------------------------< java.lang.Object overrides >---
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        } else if (other instanceof ValueConstraint) {
            return qualifiedDefinition.equals(((ValueConstraint) other).qualifiedDefinition);
        } else {
            return false;
        }
    }

    /**
     * Returns the hashCode of the definition String
     *
     * @return the hashCode of the definition String
     * @see Object#hashCode()
     */
    public int hashCode() {
        return qualifiedDefinition.hashCode();
    }

    //-----------------------------------< static factory and check methods >---
    /**
     * Create a new <code>ValueConstraint</code> from the String representation.
     * Note, that the definition must be in the qualified format in case the type
     * indicates {@link PropertyType#NAME}, {@link PropertyType#PATH} or {@link PropertyType#REFERENCE}
     *
     * @param type
     * @param qualifiedDefinition
     * @return
     * @throws InvalidConstraintException
     */
    public static ValueConstraint create(int type, String qualifiedDefinition)
        throws InvalidConstraintException {
        if (qualifiedDefinition == null) {
            throw new IllegalArgumentException("illegal definition (null)");
        }
        switch (type) {
            // constraints which are not qName senstive
            case PropertyType.STRING:
                return new StringConstraint(qualifiedDefinition);

            case PropertyType.BOOLEAN:
                return new BooleanConstraint(qualifiedDefinition);

            case PropertyType.BINARY:
                return new NumericConstraint(qualifiedDefinition);

            case PropertyType.DATE:
                return new DateConstraint(qualifiedDefinition);

            case PropertyType.LONG:
            case PropertyType.DOUBLE:
                return new NumericConstraint(qualifiedDefinition);

            // qName sensitive constraints: create from qualified string
            case PropertyType.NAME:
                return NameConstraint.create(qualifiedDefinition);

            case PropertyType.PATH:
                return PathConstraint.create(qualifiedDefinition);

            case PropertyType.WEAKREFERENCE:
            case PropertyType.REFERENCE:
                return ReferenceConstraint.create(qualifiedDefinition);

            default:
                throw new IllegalArgumentException("unknown/unsupported target type for constraint: "
                        + PropertyType.nameFromValue(type));
        }
    }

    /**
     *
     * @param type
     * @param definition
     * @param resolver
     * @return
     * @throws InvalidConstraintException
     */
    public static ValueConstraint create(int type, String definition,
                                         NamePathResolver resolver)
            throws InvalidConstraintException {
        if (definition == null) {
            throw new IllegalArgumentException("Illegal definition (null) for ValueConstraint.");
        }
        switch (type) {
            case PropertyType.STRING:
                return new StringConstraint(definition);

            case PropertyType.BOOLEAN:
                return new BooleanConstraint(definition);

            case PropertyType.BINARY:
                return new NumericConstraint(definition);

            case PropertyType.DATE:
                return new DateConstraint(definition);

            case PropertyType.LONG:
            case PropertyType.DOUBLE:
                return new NumericConstraint(definition);

            case PropertyType.NAME:
                return NameConstraint.create(definition, resolver);

            case PropertyType.PATH:
                return PathConstraint.create(definition, resolver);

            case PropertyType.WEAKREFERENCE:
            case PropertyType.REFERENCE:
                return ReferenceConstraint.create(definition, resolver);

            default:
                throw new IllegalArgumentException("Unknown/unsupported target type for constraint: " + PropertyType.nameFromValue(type));
        }
    }

    /**
     * Tests if the value constraints defined in the property definition
     * <code>pd</code> are satisfied by the the specified <code>values</code>.
     * <p/>
     * Note that the <i>protected</i> flag is not checked. Also note that no
     * type conversions are attempted if the type of the given values does not
     * match the required type as specified in the given definition.
     *
     * @param pd
     * @param values
     * @throws ConstraintViolationException
     */
    public static void checkValueConstraints(QPropertyDefinition pd, QValue[] values)
            throws ConstraintViolationException, RepositoryException {
        // check multi-value flag
        if (!pd.isMultiple() && values != null && values.length > 1) {
            throw new ConstraintViolationException("the property is not multi-valued");
        }

        String[] constraints = pd.getValueConstraints();
        if (constraints == null || constraints.length == 0) {
            // no constraints to check
            return;
        }
        if (values != null && values.length > 0) {
            // check value constraints on every value
            for (int i = 0; i < values.length; i++) {
                // constraints are OR-ed together
                boolean satisfied = false;
                ConstraintViolationException cve = null;
                for (int j = 0; j < constraints.length && !satisfied; j++) {
                    try {
                        ValueConstraint cnstr = ValueConstraint.create(pd.getRequiredType(), constraints[j]);
                        cnstr.check(values[i]);
                        satisfied = true;
                    } catch (ConstraintViolationException e) {
                        cve = e;
                    } catch (InvalidConstraintException e) {
                        cve = new ConstraintViolationException(e.getMessage(), e);
                    }
                }
                if (!satisfied) {
                    // re-throw last exception we encountered
                    throw cve;
                }
            }
        }
    }

}
