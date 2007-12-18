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
package org.apache.jackrabbit.core.nodetype;

import org.apache.jackrabbit.core.value.BLOBFileValue;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.spi.commons.conversion.MalformedPathException;
import org.apache.jackrabbit.spi.commons.conversion.NameException;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.value.DateValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    final String definition;

    protected ValueConstraint(String definition) {
        this.definition = definition;
    }

    /**
     * Returns the original (raw) definition of this constraint.
     *
     * @return the original (raw) definition of this constraint.
     */
    public String getDefinition() {
        return definition;
    }

    /**
     * For constraints that are not namespace prefix mapping sensitive this
     * method returns the same result as <code>{@link #getDefinition()}</code>.
     * <p/>
     * Those that are namespace prefix mapping sensitive (e.g.
     * <code>NameConstraint</code>, <code>PathConstraint</code> and
     * <code>ReferenceConstraint</code>) use the given <code>nsResolver</code>
     * to reflect the current mapping in the returned value.
     *
     * @return the definition of this constraint.
     * @param resolver
     */
    public String getDefinition(NamePathResolver resolver) {
        return definition;
    }

    public static ValueConstraint create(int type, String definition,
                                         NamePathResolver resolver)
            throws InvalidConstraintException {
        if (definition == null) {
            throw new IllegalArgumentException("illegal definition (null)");
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
                return new NameConstraint(definition, resolver);

            case PropertyType.PATH:
                return new PathConstraint(definition, resolver);

            case PropertyType.REFERENCE:
                return new ReferenceConstraint(definition, resolver);

            default:
                throw new IllegalArgumentException("unknown/unsupported target type for constraint: "
                        + PropertyType.nameFromValue(type));
        }
    }

    abstract void check(InternalValue value) throws ConstraintViolationException, RepositoryException;

    //-------------------------------------------< java.lang.Object overrides >
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        } else if (other instanceof ValueConstraint) {
            return definition.equals(((ValueConstraint) other).definition);
        } else {
            return false;
        }
    }

    public int hashCode() {
        return definition.hashCode();
    }
}

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

    void check(boolean bool) throws ConstraintViolationException {
        if (bool != reqBool) {
            throw new ConstraintViolationException("'" + bool
                    + "' does not satisfy the constraint '" + definition + "'");
        }
    }

    void check(InternalValue value) throws ConstraintViolationException, RepositoryException {
        if (value == null) {
            throw new ConstraintViolationException("null value does not satisfy the constraint '"
                    + definition + "'");
        }
        switch (value.getType()) {
            case PropertyType.BOOLEAN:
                check(value.getBoolean());
                return;

            default:
                String msg = "BOOLEAN constraint can not be applied to value of type: "
                        + PropertyType.nameFromValue(value.getType());
                log.debug(msg);
                throw new RepositoryException(msg);
        }
    }
}

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

    void check(String text) throws ConstraintViolationException {
        if (text == null) {
            throw new ConstraintViolationException("null value does not satisfy the constraint '"
                    + definition + "'");
        }
        Matcher matcher = pattern.matcher(text);
        if (!matcher.matches()) {
            throw new ConstraintViolationException("'" + text
                    + "' does not satisfy the constraint '" + definition + "'");
        }
    }

    void check(InternalValue value) throws ConstraintViolationException, RepositoryException {
        if (value == null) {
            throw new ConstraintViolationException("null value does not satisfy the constraint '"
                    + definition + "'");
        }
        switch (value.getType()) {
            case PropertyType.STRING:
                check(value.toString());
                return;

            default:
                String msg = "STRING constraint can not be applied to value of type: "
                        + PropertyType.nameFromValue(value.getType());
                log.debug(msg);
                throw new RepositoryException(msg);
        }
    }
}

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

    void check(double number) throws ConstraintViolationException {
        if (lowerLimit != null) {
            if (lowerInclusive) {
                if (number < lowerLimit.doubleValue()) {
                    throw new ConstraintViolationException(number
                            + " does not satisfy the constraint '"
                            + definition + "'");
                }
            } else {
                if (number <= lowerLimit.doubleValue()) {
                    throw new ConstraintViolationException(number
                            + " does not satisfy the constraint '"
                            + definition + "'");
                }
            }
        }
        if (upperLimit != null) {
            if (upperInclusive) {
                if (number > upperLimit.doubleValue()) {
                    throw new ConstraintViolationException(number
                            + " does not satisfy the constraint '"
                            + definition + "'");
                }
            } else {
                if (number >= upperLimit.doubleValue()) {
                    throw new ConstraintViolationException(number
                            + " does not satisfy the constraint '"
                            + definition + "'");
                }
            }
        }
    }

    void check(InternalValue value) throws ConstraintViolationException, RepositoryException {
        if (value == null) {
            throw new ConstraintViolationException("null value does not satisfy the constraint '"
                    + definition + "'");
        }
        switch (value.getType()) {
            case PropertyType.LONG:
                check(value.getLong());
                return;

            case PropertyType.DOUBLE:
                check(value.getDouble());
                return;

            case PropertyType.BINARY:
                BLOBFileValue blob = value.getBLOBFileValue();
                long length = blob.getLength();
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

    void check(Calendar cal) throws ConstraintViolationException {
        if (cal == null) {
            throw new ConstraintViolationException("null value does not satisfy the constraint '"
                    + definition + "'");
        }
        if (lowerLimit != null) {
            if (lowerInclusive) {
                if (cal.getTimeInMillis() < lowerLimit.getTimeInMillis()) {
                    throw new ConstraintViolationException(cal
                            + " does not satisfy the constraint '"
                            + definition + "'");
                }
            } else {
                if (cal.getTimeInMillis() <= lowerLimit.getTimeInMillis()) {
                    throw new ConstraintViolationException(cal
                            + " does not satisfy the constraint '"
                            + definition + "'");
                }
            }
        }
        if (upperLimit != null) {
            if (upperInclusive) {
                if (cal.getTimeInMillis() > upperLimit.getTimeInMillis()) {
                    throw new ConstraintViolationException(cal
                            + " does not satisfy the constraint '"
                            + definition + "'");
                }
            } else {
                if (cal.getTimeInMillis() >= upperLimit.getTimeInMillis()) {
                    throw new ConstraintViolationException(cal
                            + " does not satisfy the constraint '"
                            + definition + "'");
                }
            }
        }
    }

    void check(InternalValue value) throws ConstraintViolationException, RepositoryException {
        if (value == null) {
            throw new ConstraintViolationException("null value does not satisfy the constraint '"
                    + definition + "'");
        }
        switch (value.getType()) {
            case PropertyType.DATE:
                check(value.getDate());
                return;

            default:
                String msg = "DATE constraint can not be applied to value of type: "
                        + PropertyType.nameFromValue(value.getType());
                log.debug(msg);
                throw new RepositoryException(msg);
        }
    }
}

/**
 * <code>PathConstraint</code> ...
 */
class PathConstraint extends ValueConstraint {
    final Path path;
    final boolean deep;

    PathConstraint(String definition, NamePathResolver resolver)
            throws InvalidConstraintException {
        super(definition);

        // constraint format: absolute or relative path with optional trailing wildcard
        deep = definition.endsWith("*");
        if (deep) {
            // trim trailing wildcard before building path
            definition = definition.substring(0, definition.length() - 1);
        }
        try {
            path = resolver.getQPath(definition);
        } catch (NameException e) {
            String msg = "invalid path expression specified as value constraint: "
                    + definition;
            log.debug(msg);
            throw new InvalidConstraintException(msg, e);
        } catch (NamespaceException e) {
            String msg = "invalid path expression specified as value constraint: "
                    + definition;
            log.debug(msg);
            throw new InvalidConstraintException(msg, e);
        }
    }

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
            return definition;
        }
    }

    void check(InternalValue value) throws ConstraintViolationException, RepositoryException {
        if (value == null) {
            throw new ConstraintViolationException("null value does not satisfy the constraint '"
                    + definition + "'");
        }
        switch (value.getType()) {
            case PropertyType.PATH:
                Path p = value.getPath();
                // normalize paths before comparing them
                Path p0, p1;
                try {
                    p0 = path.getNormalizedPath();
                    p1 = p.getNormalizedPath();
                } catch (MalformedPathException e) {
                    throw new ConstraintViolationException("path not valid: " + e);
                }
                if (deep) {
                    try {
                        if (!p0.isAncestorOf(p1)) {
                            throw new ConstraintViolationException(p
                                    + " does not satisfy the constraint '"
                                    + definition + "'");
                        }
                    } catch (MalformedPathException mpe) {
                        // can't compare relative with absolute path
                        throw new ConstraintViolationException(p
                                + " does not satisfy the constraint '"
                                + definition + "'");
                    }
                } else {
                    // exact match required
                    if (!p0.equals(p1)) {
                        throw new ConstraintViolationException(p
                                + " does not satisfy the constraint '"
                                + definition + "'");
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

/**
 * <code>NameConstraint</code> ...
 */
class NameConstraint extends ValueConstraint {
    final Name name;

    NameConstraint(String definition, NamePathResolver resolver)
            throws InvalidConstraintException {
        super(definition);

        // constraint format: JCR name in prefix form
        try {
            name = resolver.getQName(definition);
        } catch (NameException e) {
            String msg = "invalid name specified as value constraint: "
                    + definition;
            log.debug(msg);
            throw new InvalidConstraintException(msg, e);
        } catch (NamespaceException e) {
            String msg = "invalid name specified as value constraint: "
                    + definition;
            log.debug(msg);
            throw new InvalidConstraintException(msg, e);
        }
    }

    public String getDefinition(NamePathResolver resolver) {
        try {
            return resolver.getJCRName(name);
        } catch (NamespaceException e) {
            // should never get here, return raw definition as fallback
            return definition;
        }
    }

    void check(InternalValue value) throws ConstraintViolationException, RepositoryException {
        if (value == null) {
            throw new ConstraintViolationException("null value does not satisfy the constraint '"
                    + definition + "'");
        }
        switch (value.getType()) {
            case PropertyType.NAME:
                Name n = value.getQName();
                if (!name.equals(n)) {
                    throw new ConstraintViolationException(n
                            + " does not satisfy the constraint '"
                            + definition + "'");
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

/**
 * <code>ReferenceConstraint</code> ...
 */
class ReferenceConstraint extends ValueConstraint {
    final Name ntName;

    ReferenceConstraint(String definition, NamePathResolver resolver) throws InvalidConstraintException {
        super(definition);

        // format: node type name
        try {
            ntName = resolver.getQName(definition);
        } catch (NameException e) {
            String msg = "invalid node type name specified as value constraint: "
                    + definition;
            log.debug(msg);
            throw new InvalidConstraintException(msg, e);
        } catch (NamespaceException e) {
            String msg = "invalid node type name specified as value constraint: "
                    + definition;
            log.debug(msg);
            throw new InvalidConstraintException(msg, e);
        }
    }

    public String getDefinition(NamePathResolver resolver) {
        try {
            return resolver.getJCRName(ntName);
        } catch (NamespaceException e) {
            // should never get here, return raw definition as fallback
            return definition;
        }
    }

    Name getNodeTypeName() {
        return ntName;
    }

    void check(InternalValue value) throws ConstraintViolationException, RepositoryException {
        if (value == null) {
            throw new ConstraintViolationException("null value does not satisfy the constraint '"
                    + definition + "'");
        }
        switch (value.getType()) {
            case PropertyType.REFERENCE:
                // @todo check REFERENCE value constraint (requires a session)
/*
                UUID targetUUID = (UUID) value.internalValue();
                NodeImpl targetNode = (NodeImpl) session.getNodeByUUID(targetUUID.toString());
                if (!targetNode.isNodeType(ntName)) {
                    throw new ConstraintViolationException("the node with uuid "
                            + targetUUID + " does not satisfy the constraint '" + definition + "'");
                }
*/
                log.info("validation of REFERENCE constraint is not yet implemented");
                return;

            default:
                String msg = "REFERENCE constraint can not be applied to value of type: "
                        + PropertyType.nameFromValue(value.getType());
                log.debug(msg);
                throw new RepositoryException(msg);
        }
    }
}


