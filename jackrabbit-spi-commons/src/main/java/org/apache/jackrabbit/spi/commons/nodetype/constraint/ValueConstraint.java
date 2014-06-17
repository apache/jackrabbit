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
import org.apache.jackrabbit.spi.QValueConstraint;
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
public abstract class ValueConstraint implements QValueConstraint {

    protected static Logger log = LoggerFactory.getLogger(ValueConstraint.class);

    public static final ValueConstraint[] EMPTY_ARRAY = new ValueConstraint[0];

    // TODO improve. don't rely on a specific factory impl
    static final NameFactory NAME_FACTORY = NameFactoryImpl.getInstance();

    private final String definition;

    protected ValueConstraint(String definition) {
        this.definition = definition;
    }

    /**
     * For constraints that are not namespace prefix mapping sensitive this
     * method returns the same result as <code>{@link #getString()}</code>.
     * <p>
     * Those that are namespace prefix mapping sensitive (e.g.
     * <code>NameConstraint</code>, <code>PathConstraint</code> and
     * <code>ReferenceConstraint</code>) use the given <code>nsResolver</code>
     * to reflect the current mapping in the returned value.
     * In other words: subclasses, that need to make a conversion to JCR value
     * must overwrite this and return a value that has the <code>Name</code>s
     * or <code>Path</code> properly resolved to their JCR representation.
     *
     * @return the definition of this constraint.
     * @see #getString ()
     * @param resolver name-path resolver
     * @see NamePathResolver#getJCRName(org.apache.jackrabbit.spi.Name)
     * @see NamePathResolver#getJCRPath(org.apache.jackrabbit.spi.Path) 
     */
    public String getDefinition(NamePathResolver resolver) {
        return definition;
    }

    //---------------------------------------------------< QValueConstraint >---
    /**
     * @see org.apache.jackrabbit.spi.QValueConstraint#getString()
     */ 
    public String getString() {
        return definition;
    }

    //---------------------------------------------------< java.lang.Object >---
    /**
     * Same as {@link #getString()}
     * @return the internal definition String
     * @see Object#toString()
     */
    @Override
    public String toString() {
        return getString();
    }

    /**
     * @see Object#equals(Object)
     */
    @Override
    public boolean equals(Object other) {
        return other == this
                || other instanceof ValueConstraint
                && definition.equals(((ValueConstraint) other).definition);
    }

    /**
     * Returns the hashCode of the definition String
     *
     * @return the hashCode of the definition String
     * @see Object#hashCode()
     */
    @Override
    public int hashCode() {
        return definition.hashCode();
    }

    //-----------------------------------< static factory and check methods >---
    /**
     * Create a new <code>ValueConstraint</code> from the String representation.
     * Note, that the definition must be independent of session specific namespace
     * mappings in case of the following constraint types:
     * <ul><li>{@link PropertyType#NAME},</li>
     * <li>{@link PropertyType#PATH} or</li>
     * <li>{@link PropertyType#REFERENCE}</li>
     * </ul>
     *
     * @param type required type
     * @param definition The internal definition string.
     * @return a new value constraint
     * @throws InvalidConstraintException if the constraint is not valid.
     * @see #create(int, String, NamePathResolver) for the corresponding
     * method that allows to pass the JCR representation of a constraint
     * definition.
     */
    public static ValueConstraint create(int type, String definition)
        throws InvalidConstraintException {
        if (definition == null) {
            throw new IllegalArgumentException("illegal definition (null)");
        }
        switch (type) {
            // constraints which are not qName sensitive
            case PropertyType.STRING:
            case PropertyType.URI:
                return new StringConstraint(definition);

            case PropertyType.BOOLEAN:
                return new BooleanConstraint(definition);

            case PropertyType.BINARY:
                return new NumericConstraint(definition);

            case PropertyType.DATE:
                return new DateConstraint(definition);

            case PropertyType.LONG:
            case PropertyType.DOUBLE:
            case PropertyType.DECIMAL:
                return new NumericConstraint(definition);

            case PropertyType.NAME:
                return NameConstraint.create(definition);

            case PropertyType.PATH:
                return PathConstraint.create(definition);

            case PropertyType.REFERENCE:
            case PropertyType.WEAKREFERENCE:
                return ReferenceConstraint.create(definition);

            default:
                throw new IllegalArgumentException("unknown/unsupported target type for constraint: "
                        + PropertyType.nameFromValue(type));
        }
    }

    /**
     * Create a new <code>ValueConstraint</code> array from the String
     * representation. Note, that the definition must be in the internal format
     * in case of the following types:
     * <ul><li>{@link PropertyType#NAME},</li>
     * <li>{@link PropertyType#PATH} or</li>
     * <li>{@link PropertyType#REFERENCE}</li>
     * </ul>
     *
     * @param type the required type
     * @param definition internal definition strings
     * @return the array of constraints
     * @throws InvalidConstraintException if one of the constraints is invalid
     */
    public static ValueConstraint[] create(int type, String[] definition)
            throws InvalidConstraintException {
        if (definition == null || definition.length == 0) {
            return ValueConstraint.EMPTY_ARRAY;
        }
        ValueConstraint[] ret = new ValueConstraint[definition.length];
        for (int i=0; i<ret.length; i++) {
            ret[i] = ValueConstraint.create(type, definition[i]);
        }
        return ret;
    }

    /**
     * Create a new <code>ValueConstraint</code> array from the specified JCR
     * representations.
     *
     * @param type the required type
     * @param jcrDefinition The definition strings as exposed through the JCR API.
     * @param resolver name-path resolver
     * @return the array of constraints
     * @throws InvalidConstraintException if one of the constraints is invalid
     */
    public static ValueConstraint[] create(int type, String jcrDefinition[], NamePathResolver resolver)
            throws InvalidConstraintException {
        if (jcrDefinition == null || jcrDefinition.length == 0) {
            return ValueConstraint.EMPTY_ARRAY;
        }
        ValueConstraint[] ret = new ValueConstraint[jcrDefinition.length];
        for (int i=0; i<ret.length; i++) {
            ret[i] = ValueConstraint.create(type, jcrDefinition[i], resolver);
        }
        return ret;
    }

    /**
     *
     * @param type required type
     * @param jcrDefinition A JCR representation of a value constraint definition.
     * @param resolver name-path resolver
     * @return a new value constraint
     * @throws InvalidConstraintException if the constraint is invalid
     */
    public static ValueConstraint create(int type, String jcrDefinition,
                                         NamePathResolver resolver)
            throws InvalidConstraintException {
        if (jcrDefinition == null) {
            throw new IllegalArgumentException("Illegal definition (null) for ValueConstraint.");
        }
        switch (type) {
            case PropertyType.STRING:
            case PropertyType.URI:
                return new StringConstraint(jcrDefinition);

            case PropertyType.BOOLEAN:
                return new BooleanConstraint(jcrDefinition);

            case PropertyType.BINARY:
                return new NumericConstraint(jcrDefinition);

            case PropertyType.DATE:
                return new DateConstraint(jcrDefinition);

            case PropertyType.LONG:
            case PropertyType.DOUBLE:
            case PropertyType.DECIMAL:
                return new NumericConstraint(jcrDefinition);

            case PropertyType.NAME:
                return NameConstraint.create(jcrDefinition, resolver);

            case PropertyType.PATH:
                return PathConstraint.create(jcrDefinition, resolver);

            case PropertyType.REFERENCE:
            case PropertyType.WEAKREFERENCE:
                return ReferenceConstraint.create(jcrDefinition, resolver);

            default:
                throw new IllegalArgumentException("Unknown/unsupported target type for constraint: " + PropertyType.nameFromValue(type));
        }
    }

    /**
     * Tests if the value constraints defined in the property definition
     * <code>pd</code> are satisfied by the the specified <code>values</code>.
     * <p>
     * Note that the <i>protected</i> flag is not checked. Also note that no
     * type conversions are attempted if the type of the given values does not
     * match the required type as specified in the given definition.
     *
     * @param pd property definition
     * @param values values to check
     * @throws ConstraintViolationException if the constraints are violated
     */
    public static void checkValueConstraints(QPropertyDefinition pd, QValue[] values)
            throws ConstraintViolationException, RepositoryException {
        // check multi-value flag
        if (!pd.isMultiple() && values != null && values.length > 1) {
            throw new ConstraintViolationException("the property is not multi-valued");
        }

        QValueConstraint[] constraints = pd.getValueConstraints();
        if (constraints == null || constraints.length == 0) {
            // no constraints to check
            return;
        }
        if (values != null && values.length > 0) {
            // check value constraints on every value
            for (QValue value : values) {
                // constraints are OR-ed together
                boolean satisfied = false;
                ConstraintViolationException cve = null;
                for (int j = 0; j < constraints.length && !satisfied; j++) {
                    try {
                        constraints[j].check(value);
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
