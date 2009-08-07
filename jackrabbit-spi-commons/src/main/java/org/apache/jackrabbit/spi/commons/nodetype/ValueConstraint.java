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
package org.apache.jackrabbit.spi.commons.nodetype;

import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.conversion.NameResolver;
import org.apache.jackrabbit.spi.commons.conversion.NameException;
import org.apache.jackrabbit.spi.commons.conversion.PathResolver;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.spi.NameFactory;
import org.apache.jackrabbit.value.DateValue;
import org.apache.jackrabbit.spi.commons.name.PathFactoryImpl;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;
import javax.jcr.NamespaceException;
import javax.jcr.nodetype.ConstraintViolationException;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

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

//---------------------------------------------< Subclass BooleanConstraint >---
/**
 * <code>BooleanConstraint</code> ...
 */
class BooleanConstraint extends ValueConstraint {
    final boolean reqBool;

    BooleanConstraint(String definition) throws InvalidConstraintException {
        super(definition);

        // constraint format: 'true' or 'false'
        if (definition.equals("true")) {
            reqBool = true;
        } else if (definition.equals("false")) {
            reqBool = false;
        } else {
            String msg = "'" + definition
                    + "' is not a valid value constraint format for BOOLEAN values";
            log.debug(msg);
            throw new InvalidConstraintException(msg);
        }
    }

    /**
     * @see ValueConstraint#check(QValue)
     */
    void check(QValue value) throws ConstraintViolationException, RepositoryException {
        if (value == null) {
            throw new ConstraintViolationException("null value does not satisfy the constraint '"  + getQualifiedDefinition() + "'");
        }
        switch (value.getType()) {
            case PropertyType.BOOLEAN:
                boolean b = Boolean.valueOf(value.getString()).booleanValue();
                if (b != reqBool) {
                    throw new ConstraintViolationException("'" + b + "' does not satisfy the constraint '" + getQualifiedDefinition() + "'");
                }
                return;

            default:
                String msg = "BOOLEAN constraint can not be applied to value of type: "
                        + PropertyType.nameFromValue(value.getType());
                log.debug(msg);
                throw new RepositoryException(msg);
        }
    }
}

//----------------------------------------------< Subclass StringConstraint >---
/**
 * <code>StringConstraint</code> ...
 */
class StringConstraint extends ValueConstraint {
    final Pattern pattern;

    StringConstraint(String definition) throws InvalidConstraintException {
        super(definition);

        // constraint format: regexp
        try {
            pattern = Pattern.compile(definition);
        } catch (PatternSyntaxException pse) {
            String msg = "'" + definition + "' is not valid regular expression syntax";
            log.debug(msg);
            throw new InvalidConstraintException(msg, pse);
        }
    }

    /**
     * @see ValueConstraint#check(QValue)
     */
    void check(QValue value) throws ConstraintViolationException, RepositoryException {
        if (value == null) {
            throw new ConstraintViolationException("null value does not satisfy the constraint '" + getQualifiedDefinition() + "'");
        }
        switch (value.getType()) {
            case PropertyType.STRING:
                String text = value.getString();
                Matcher matcher = pattern.matcher(text);
                if (!matcher.matches()) {
                    throw new ConstraintViolationException("'" + text  + "' does not satisfy the constraint '" + getQualifiedDefinition() + "'");
                }
                return;

            default:
                String msg = "STRING constraint can not be applied to value of type: " + PropertyType.nameFromValue(value.getType());
                log.debug(msg);
                throw new RepositoryException(msg);
        }
    }
}

//---------------------------------------------< Subclass NumericConstraint >---
/**
 * <code>NumericConstraint</code> ...
 */
class NumericConstraint extends ValueConstraint {
    final boolean lowerInclusive;
    final Double lowerLimit;
    final boolean upperInclusive;
    final Double upperLimit;

    NumericConstraint(String definition) throws InvalidConstraintException {
        super(definition);

        // format: '(<min>, <max>)',  '[<min>, <max>]', '(, <max>)' etc.
        Pattern pattern = Pattern.compile("([\\(\\[]) *(\\-?\\d+\\.?\\d*)? *, *(\\-?\\d+\\.?\\d*)? *([\\)\\]])");
        Matcher matcher = pattern.matcher(definition);
        if (matcher.matches()) {
            try {
                // group 1 is lower inclusive/exclusive
                String s = matcher.group(1);
                lowerInclusive = s.equals("[");
                // group 2 is lower limit
                s = matcher.group(2);
                if (s == null || s.length() == 0) {
                    lowerLimit = null;
                } else {
                    lowerLimit = Double.valueOf(matcher.group(2));
                }
                // group 3 is upper limit
                s = matcher.group(3);
                if (s == null || s.length() == 0) {
                    upperLimit = null;
                } else {
                    upperLimit = Double.valueOf(matcher.group(3));
                }
                // group 4 is lower inclusive/exclusive
                s = matcher.group(4);
                upperInclusive = s.equals("]");
                if (lowerLimit == null && upperLimit == null) {
                    String msg = "'" + definition + "' is not a valid value constraint"
                            + " format for numeric types: neither lower- nor upper-limit specified";
                    log.debug(msg);
                    throw new InvalidConstraintException(msg);
                }
                if (lowerLimit != null && upperLimit != null) {
                    if (lowerLimit.doubleValue() > upperLimit.doubleValue()) {
                        String msg = "'" + definition
                                + "' is not a valid value constraint format for numeric types: lower-limit exceeds upper-limit";
                        log.debug(msg);
                        throw new InvalidConstraintException(msg);
                    }
                }
            } catch (NumberFormatException nfe) {
                String msg = "'" + definition
                        + "' is not a valid value constraint format for numeric types";
                log.debug(msg);
                throw new InvalidConstraintException(msg, nfe);
            }
        } else {
            String msg = "'" + definition
                    + "' is not a valid value constraint format for numeric values";
            log.debug(msg);
            throw new InvalidConstraintException(msg);
        }
    }

    private void check(double number) throws ConstraintViolationException {
        if (lowerLimit != null) {
            if (lowerInclusive) {
                if (number < lowerLimit.doubleValue()) {
                    throw new ConstraintViolationException(number
                            + " does not satisfy the constraint '"
                            + getQualifiedDefinition() + "'");
                }
            } else {
                if (number <= lowerLimit.doubleValue()) {
                    throw new ConstraintViolationException(number
                            + " does not satisfy the constraint '"
                            + getQualifiedDefinition() + "'");
                }
            }
        }
        if (upperLimit != null) {
            if (upperInclusive) {
                if (number > upperLimit.doubleValue()) {
                    throw new ConstraintViolationException(number
                            + " does not satisfy the constraint '"
                            + getQualifiedDefinition() + "'");
                }
            } else {
                if (number >= upperLimit.doubleValue()) {
                    throw new ConstraintViolationException(number
                            + " does not satisfy the constraint '"
                            + getQualifiedDefinition() + "'");
                }
            }
        }
    }

    /**
     * @see ValueConstraint#check(QValue)
     */
    void check(QValue value) throws ConstraintViolationException, RepositoryException {
        if (value == null) {
            throw new ConstraintViolationException("null value does not satisfy the constraint '"
                    + getQualifiedDefinition() + "'");
        }
        switch (value.getType()) {
            case PropertyType.LONG:
                check(value.getLong());
                return;

            case PropertyType.DOUBLE:
                check(value.getDouble());
                return;

            case PropertyType.BINARY:
                long length = value.getLength();
                if (length != -1) {
                    check(length);
                } else {
                    log.warn("failed to determine length of binary value");
                }
                return;

            default:
                String msg = "numeric constraint can not be applied to value of type: "
                        + PropertyType.nameFromValue(value.getType());
                log.debug(msg);
                throw new RepositoryException(msg);
        }
    }
}

//------------------------------------------------< Subclass DateConstraint >---
/**
 * <code>DateConstraint</code> ...
 */
class DateConstraint extends ValueConstraint {

    final boolean lowerInclusive;
    final Calendar lowerLimit;
    final boolean upperInclusive;
    final Calendar upperLimit;

    DateConstraint(String definition) throws InvalidConstraintException {
        super(definition);

        // format: '(<fromDate>, <toDate>)', '[<fromDate>, <toDate>]', '[, <toDate>]' etc.
        Pattern pattern = Pattern.compile("([\\(\\[]) *([0-9TZ\\.\\+-:]*)? *, *([0-9TZ\\.\\+-:]*)? *([\\)\\]])");
        Matcher matcher = pattern.matcher(definition);
        if (matcher.matches()) {
            try {
                // group 1 is lower inclusive/exclusive
                String s = matcher.group(1);
                lowerInclusive = s.equals("[");
                // group 2 is lower limit
                s = matcher.group(2);
                if (s == null || s.length() == 0) {
                    lowerLimit = null;
                } else {
                    lowerLimit = DateValue.valueOf(matcher.group(2)).getDate();
                }
                // group 3 is upper limit
                s = matcher.group(3);
                if (s == null || s.length() == 0) {
                    upperLimit = null;
                } else {
                    upperLimit = DateValue.valueOf(matcher.group(3)).getDate();
                }
                // group 4 is upepr inclusive/exclusive
                s = matcher.group(4);
                upperInclusive = s.equals("]");

                if (lowerLimit == null && upperLimit == null) {
                    String msg = "'" + definition
                            + "' is not a valid value constraint format for dates: neither min- nor max-date specified";
                    log.debug(msg);
                    throw new InvalidConstraintException(msg);
                }
                if (lowerLimit != null && upperLimit != null) {
                    if (lowerLimit.after(upperLimit)) {
                        String msg = "'" + definition
                                + "' is not a valid value constraint format for dates: min-date > max-date";
                        log.debug(msg);
                        throw new InvalidConstraintException(msg);
                    }
                }
            } catch (ValueFormatException vfe) {
                String msg = "'" + definition
                        + "' is not a valid value constraint format for dates";
                log.debug(msg);
                throw new InvalidConstraintException(msg, vfe);
            } catch (RepositoryException re) {
                String msg = "'" + definition
                        + "' is not a valid value constraint format for dates";
                log.debug(msg);
                throw new InvalidConstraintException(msg, re);
            }
        } else {
            String msg = "'" + definition
                    + "' is not a valid value constraint format for dates";
            log.debug(msg);
            throw new InvalidConstraintException(msg);
        }
    }

    private void check(Calendar cal) throws ConstraintViolationException {
        if (cal == null) {
            throw new ConstraintViolationException("null value does not satisfy the constraint '" + getQualifiedDefinition() + "'");
        }
        if (lowerLimit != null) {
            if (lowerInclusive) {
                if (cal.getTimeInMillis() < lowerLimit.getTimeInMillis()) {
                    throw new ConstraintViolationException(cal
                            + " does not satisfy the constraint '"
                            + getQualifiedDefinition() + "'");
                }
            } else {
                if (cal.getTimeInMillis() <= lowerLimit.getTimeInMillis()) {
                    throw new ConstraintViolationException(cal
                            + " does not satisfy the constraint '"
                            + getQualifiedDefinition() + "'");
                }
            }
        }
        if (upperLimit != null) {
            if (upperInclusive) {
                if (cal.getTimeInMillis() > upperLimit.getTimeInMillis()) {
                    throw new ConstraintViolationException(cal
                            + " does not satisfy the constraint '"
                            + getQualifiedDefinition() + "'");
                }
            } else {
                if (cal.getTimeInMillis() >= upperLimit.getTimeInMillis()) {
                    throw new ConstraintViolationException(cal
                            + " does not satisfy the constraint '"
                            + getQualifiedDefinition() + "'");
                }
            }
        }
    }

    /**
     * @see ValueConstraint#check(QValue)
     */
    void check(QValue value) throws ConstraintViolationException, RepositoryException {
        if (value == null) {
            throw new ConstraintViolationException("null value does not satisfy the constraint '" + getQualifiedDefinition() + "'");
        }
        switch (value.getType()) {
            case PropertyType.DATE:
                check(value.getCalendar());
                return;

            default:
                String msg = "DATE constraint can not be applied to value of type: "
                        + PropertyType.nameFromValue(value.getType());
                log.debug(msg);
                throw new RepositoryException(msg);
        }
    }
}

//------------------------------------------------< Subclass PathConstraint >---
/**
 * <code>PathConstraint</code> ...
 */
class PathConstraint extends ValueConstraint {

    final Path path;
    final boolean deep;

    static PathConstraint create(String qualifiedDefinition) throws InvalidConstraintException {
        // constraint format: qualified absolute or relative path with optional trailing wildcard
        boolean deep = qualifiedDefinition.endsWith("*");
        Path path;
        // TODO improve. don't rely on a specific factory impl
        if (deep) {
            path = PathFactoryImpl.getInstance().create(qualifiedDefinition.substring(0, qualifiedDefinition.length() - 1));
        } else {
            path = PathFactoryImpl.getInstance().create(qualifiedDefinition);
        }
        return new PathConstraint(qualifiedDefinition, path, deep);
    }

    static PathConstraint create(String definition, PathResolver resolver)
            throws InvalidConstraintException {
        try {
            StringBuffer qualifiedDefinition = new StringBuffer();
            // constraint format: absolute or relative path with optional
            // trailing wildcard
            boolean deep = definition.endsWith("/*");
            if (deep) {
                // trim trailing wildcard before building path
                if (definition.equals("/*")) {
                    definition = "/";
                    qualifiedDefinition.append('*');
                } else {
                    definition = definition.substring(0, definition.length() - 2);
                    qualifiedDefinition.append("/*");
                }
            }
            Path path = resolver.getQPath(definition);
            qualifiedDefinition.insert(0, path.getString());

            return new PathConstraint(qualifiedDefinition.toString(), path, deep);
        } catch (NameException e) {
            String msg = "Invalid path expression specified as value constraint: " + definition;
            log.debug(msg);
            throw new InvalidConstraintException(msg, e);
        } catch (NamespaceException e) {
            String msg = "Invalid path expression specified as value constraint: " + definition;
            log.debug(msg);
            throw new InvalidConstraintException(msg, e);
        }
    }

    private PathConstraint(String qualifiedDefinition, Path path, boolean deep) throws InvalidConstraintException {
        super(qualifiedDefinition);
        this.path = path;
        this.deep = deep;
    }

    /**
     * Uses {@link NamePathResolver#getJCRPath(Path)} to convert the
     * qualified <code>Path</code> into a JCR path.
     *
     * @see ValueConstraint#getDefinition(NamePathResolver)
     * @param resolver
     */
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
            return getQualifiedDefinition();
        }
    }

    /**
     * @see ValueConstraint#check(QValue)
     */
    void check(QValue value) throws ConstraintViolationException, RepositoryException {
        if (value == null) {
            throw new ConstraintViolationException("null value does not satisfy the constraint '" + getQualifiedDefinition() + "'");
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
                                + getQualifiedDefinition() + "'");
                        }
                    } catch (RepositoryException e) {
                        // can't compare relative with absolute path
                        throw new ConstraintViolationException(p
                            + " does not satisfy the constraint '"
                            + getQualifiedDefinition() + "'");
                    }
                } else {
                    // exact match required
                    if (!p0.equals(p1)) {
                        throw new ConstraintViolationException(p
                            + " does not satisfy the constraint '"
                            + getQualifiedDefinition() + "'");
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

//------------------------------------------------< Subclass NameConstraint >---
/**
 * <code>NameConstraint</code> ...
 */
class NameConstraint extends ValueConstraint {

    private final Name name;

    static NameConstraint create(String qualifiedDefinition) {
        // constraint format: String representation of qualified name
        return new NameConstraint(qualifiedDefinition, NAME_FACTORY.create(qualifiedDefinition));
    }

    static NameConstraint create(String definition, NameResolver resolver)
            throws InvalidConstraintException {
        // constraint format: JCR name in prefix form
        try {
            Name name = resolver.getQName(definition);
            return new NameConstraint(name.toString(), name);
        } catch (NameException e) {
            String msg = "Invalid name constraint: " + definition;
            log.debug(msg);
            throw new InvalidConstraintException(msg, e);
        } catch (NamespaceException e) {
            String msg = "Invalid name constraint: " + definition;
            log.debug(msg);
            throw new InvalidConstraintException(msg, e);
        }
    }

    private NameConstraint(String qualifiedDefinition, Name name) {
        super(qualifiedDefinition);
        this.name = name;
    }

    /**
     * Uses {@link NamePathResolver#getJCRName(Name)} to convert the
     * qualified <code>Name</code> into a JCR name.
     *
     * @see ValueConstraint#getDefinition(NamePathResolver)
     * @param resolver
     */
    public String getDefinition(NamePathResolver resolver) {
        try {
            return resolver.getJCRName(name);
        } catch (NamespaceException e) {
            // should never get here, return raw definition as fallback
            return getQualifiedDefinition();
        }
    }

    /**
     * @see ValueConstraint#check(QValue)
     */
    void check(QValue value) throws ConstraintViolationException, RepositoryException {
        if (value == null) {
            throw new ConstraintViolationException("null value does not satisfy the constraint '" + getQualifiedDefinition() + "'");
        }
        switch (value.getType()) {
            case PropertyType.NAME:
                Name n = value.getName();
                if (!name.equals(n)) {
                    throw new ConstraintViolationException(n
                            + " does not satisfy the constraint '"
                            + getQualifiedDefinition() + "'");
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

//-------------------------------------------< Subclass ReferenceConstraint >---
/**
 * <code>ReferenceConstraint</code> ...
 */
class ReferenceConstraint extends ValueConstraint {

    private final Name ntName;

    static ReferenceConstraint create(String qualifiedDefinition) {
        // constraint format: String representation of qualified name
        return new ReferenceConstraint(qualifiedDefinition, NAME_FACTORY.create(qualifiedDefinition));
    }

    static ReferenceConstraint create(String definition, NameResolver resolver)
            throws InvalidConstraintException {
        // constraint format: JCR name in prefix form
        try {
            Name name = resolver.getQName(definition);
            return new ReferenceConstraint(name.toString(), name);
        } catch (NameException e) {
            String msg = "Invalid name constraint: " + definition;
            log.debug(msg);
            throw new InvalidConstraintException(msg, e);
        } catch (NamespaceException e) {
            String msg = "Invalid name constraint: " + definition;
            log.debug(msg);
            throw new InvalidConstraintException(msg, e);
        }
    }

    private ReferenceConstraint(String qualifiedDefinition, Name ntName) {
        super(qualifiedDefinition);
        this.ntName = ntName;
    }

    /**
     * Uses {@link NamePathResolver#getJCRName(Name)} to convert the
     * qualified <code>Name</code> into a JCR name.
     *
     * @see ValueConstraint#getDefinition(NamePathResolver)
     * @param resolver
     */
    public String getDefinition(NamePathResolver resolver) {
        try {
            return resolver.getJCRName(ntName);
        } catch (NamespaceException e) {
            // should never get here, return raw definition as fallback
            return getQualifiedDefinition();
        }
    }

    /**
     * @see ValueConstraint#check(QValue)
     */
    void check(QValue value) throws ConstraintViolationException, RepositoryException {
        if (value == null) {
            throw new ConstraintViolationException("Null value does not satisfy the constraint '" + getQualifiedDefinition() + "'");
        }
        switch (value.getType()) {
            case PropertyType.REFERENCE:
                // TODO check REFERENCE value constraint (requires a session)
                log.warn("validation of REFERENCE constraint is not yet implemented");
                return;

            default:
                String msg = "REFERENCE constraint can not be applied to value of type: "
                        + PropertyType.nameFromValue(value.getType());
                log.debug(msg);
                throw new RepositoryException(msg);
        }
    }
}



