/*
 * Copyright 2004 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.jcr.core.nodetype;

import org.apache.log4j.Logger;
import org.apache.jackrabbit.jcr.core.BLOBFileValue;
import org.apache.jackrabbit.jcr.core.InternalValue;
import org.apache.jackrabbit.jcr.core.NamespaceResolver;
import org.apache.jackrabbit.jcr.core.QName;

import javax.jcr.*;
import javax.jcr.nodetype.ConstraintViolationException;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * <code>ValueConstraint</code> and its subclasses are used to check the
 * syntax of a value constraint and to test if a specific value satisfies
 * it.
 *
 * @author Stefan Guggisberg
 * @version $Revision: 1.15 $, $Date: 2004/08/25 16:44:49 $
 */
public abstract class ValueConstraint {
    protected static Logger log = Logger.getLogger(ValueConstraint.class);

    final String definition;

    protected ValueConstraint(String definition) {
	this.definition = definition;
    }

    public String getDefinition() {
	return definition;
    }

    public static ValueConstraint create(int type, String definition) throws InvalidConstraintException {
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
		// @todo implement NAME value constraint
		//return new NameConstraint(definition, nsResolver);
		return new StringConstraint(".*");

	    case PropertyType.PATH:
		// @todo implement PATH value constraint
		//return new PathConstraint(definition, nsResolver);
		return new StringConstraint(".*");

	    case PropertyType.REFERENCE:
		// @todo implement REFERENCE value constraint
		//return new ReferenceConstraint(definition, ntReg, nsResolver);
		return new ReferenceConstraint(definition, null, null);

	    default:
		throw new IllegalArgumentException("unknown/unsupported target type for constraint: " + PropertyType.nameFromValue(type));
	}
    }

    abstract void check(InternalValue value) throws ConstraintViolationException, RepositoryException;
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
	    String msg = "'" + definition + "' is not a valid value constraint format for BOOLEAN values";
	    log.error(msg);
	    throw new InvalidConstraintException(msg);
	}
    }

    void check(Boolean bool) throws ConstraintViolationException {
	if (bool == null) {
	    throw new ConstraintViolationException("null value does not satisfy the constraint '" + definition + "'");
	}
	check(bool.booleanValue());
    }

    void check(boolean bool) throws ConstraintViolationException {
	if (bool != reqBool) {
	    throw new ConstraintViolationException("'" + bool + "' does not satisfy the constraint '" + definition + "'");
	}
    }

    void check(InternalValue value) throws ConstraintViolationException, RepositoryException {
	if (value == null) {
	    throw new ConstraintViolationException("null value does not satisfy the constraint '" + definition + "'");
	}
	switch (value.getType()) {
	    case PropertyType.BOOLEAN:
		check((Boolean) value.internalValue());
		return;

	    default:
		String msg = "BOOLEAN constraint can not be applied to value of type: " + PropertyType.nameFromValue(value.getType());
		log.error(msg);
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
	    log.error(msg, pse);
	    throw new InvalidConstraintException(msg, pse);
	}
    }

    void check(String text) throws ConstraintViolationException {
	if (text == null) {
	    throw new ConstraintViolationException("null value does not satisfy the constraint '" + definition + "'");
	}
	Matcher matcher = pattern.matcher(text);
	if (!matcher.matches()) {
	    throw new ConstraintViolationException("'" + text + "' does not satisfy the constraint '" + definition + "'");
	}
    }

    void check(InternalValue value) throws ConstraintViolationException, RepositoryException {
	if (value == null) {
	    throw new ConstraintViolationException("null value does not satisfy the constraint '" + definition + "'");
	}
	switch (value.getType()) {
	    case PropertyType.STRING:
		check(value.toString());
		return;

	    default:
		String msg = "STRING constraint can not be applied to value of type: " + PropertyType.nameFromValue(value.getType());
		log.error(msg);
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
		    String msg = "'" + definition + "' is not a valid value constraint format for numeric types: neither lower- nor upper-limit specified";
		    log.error(msg);
		    throw new InvalidConstraintException(msg);
		}
		if (lowerLimit != null && upperLimit != null) {
		    if (lowerLimit.doubleValue() > upperLimit.doubleValue()) {
			String msg = "'" + definition + "' is not a valid value constraint format for numeric types: lower-limit exceeds upper-limit";
			log.error(msg);
			throw new InvalidConstraintException(msg);
		    }
		}
	    } catch (NumberFormatException nfe) {
		String msg = "'" + definition + "' is not a valid value constraint format for numeric types";
		log.error(msg, nfe);
		throw new InvalidConstraintException(msg, nfe);
	    }
	} else {
	    String msg = "'" + definition + "' is not a valid value constraint format for numeric values";
	    log.error(msg);
	    throw new InvalidConstraintException(msg);
	}
    }

    void check(Double number) throws ConstraintViolationException {
	if (number == null) {
	    throw new ConstraintViolationException("null value does not satisfy the constraint '" + definition + "'");
	}
	check(number.doubleValue());
    }

    void check(Long number) throws ConstraintViolationException {
	if (number == null) {
	    throw new ConstraintViolationException("null value does not satisfy the constraint '" + definition + "'");
	}
	check(number.doubleValue());
    }

    void check(double number) throws ConstraintViolationException {
	if (lowerLimit != null) {
	    if (lowerInclusive) {
		if (number < lowerLimit.doubleValue()) {
		    throw new ConstraintViolationException(number + " does not satisfy the constraint '" + definition + "'");
		}
	    } else {
		if (number <= lowerLimit.doubleValue()) {
		    throw new ConstraintViolationException(number + " does not satisfy the constraint '" + definition + "'");
		}
	    }
	}
	if (upperLimit != null) {
	    if (upperInclusive) {
		if (number > upperLimit.doubleValue()) {
		    throw new ConstraintViolationException(number + " does not satisfy the constraint '" + definition + "'");
		}
	    } else {
		if (number >= upperLimit.doubleValue()) {
		    throw new ConstraintViolationException(number + " does not satisfy the constraint '" + definition + "'");
		}
	    }
	}
    }

    void check(InternalValue value) throws ConstraintViolationException, RepositoryException {
	if (value == null) {
	    throw new ConstraintViolationException("null value does not satisfy the constraint '" + definition + "'");
	}
	switch (value.getType()) {
	    case PropertyType.LONG:
		check((Long) value.internalValue());
		break;

	    case PropertyType.DOUBLE:
		check((Double) value.internalValue());
		return;

	    case PropertyType.BINARY:
		BLOBFileValue blob = (BLOBFileValue) value.internalValue();
		long length = blob.getLength();
		if (length != -1) {
		    check(length);
		} else {
		    log.warn("failed to determine length of binary value");
		    return;
		}

	    default:
		String msg = "numeric constraint can not be applied to value of type: " + PropertyType.nameFromValue(value.getType());
		log.error(msg);
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
		    String msg = "'" + definition + "' is not a valid value constraint format for dates: neither min- nor max-date specified";
		    log.error(msg);
		    throw new InvalidConstraintException(msg);
		}
		if (lowerLimit != null && upperLimit != null) {
		    if (lowerLimit.after(upperLimit)) {
			String msg = "'" + definition + "' is not a valid value constraint format for dates: min-date > max-date";
			log.error(msg);
			throw new InvalidConstraintException(msg);
		    }
		}
	    } catch (ValueFormatException vfe) {
		String msg = "'" + definition + "' is not a valid value constraint format for dates";
		log.error(msg, vfe);
		throw new InvalidConstraintException(msg, vfe);
	    } catch (RepositoryException re) {
		String msg = "'" + definition + "' is not a valid value constraint format for dates";
		log.error(msg, re);
		throw new InvalidConstraintException(msg, re);
	    }
	} else {
	    String msg = "'" + definition + "' is not a valid value constraint format for dates";
	    log.error(msg);
	    throw new InvalidConstraintException(msg);
	}
    }

    void check(Calendar cal) throws ConstraintViolationException {
	if (cal == null) {
	    throw new ConstraintViolationException("null value does not satisfy the constraint '" + definition + "'");
	}
	if (lowerLimit != null) {
	    if (lowerInclusive) {
		if (cal.getTimeInMillis() < lowerLimit.getTimeInMillis()) {
		    throw new ConstraintViolationException(cal + " does not satisfy the constraint '" + definition + "'");
		}
	    } else {
		if (cal.getTimeInMillis() <= lowerLimit.getTimeInMillis()) {
		    throw new ConstraintViolationException(cal + " does not satisfy the constraint '" + definition + "'");
		}
	    }
	}
	if (upperLimit != null) {
	    if (upperInclusive) {
		if (cal.getTimeInMillis() > upperLimit.getTimeInMillis()) {
		    throw new ConstraintViolationException(cal + " does not satisfy the constraint '" + definition + "'");
		}
	    } else {
		if (cal.getTimeInMillis() >= upperLimit.getTimeInMillis()) {
		    throw new ConstraintViolationException(cal + " does not satisfy the constraint '" + definition + "'");
		}
	    }
	}
    }

    void check(InternalValue value) throws ConstraintViolationException, RepositoryException {
	if (value == null) {
	    throw new ConstraintViolationException("null value does not satisfy the constraint '" + definition + "'");
	}
	switch (value.getType()) {
	    case PropertyType.DATE:
		check((Calendar) value.internalValue());
		return;

	    default:
		String msg = "DATE constraint can not be applied to value of type: " + PropertyType.nameFromValue(value.getType());
		log.error(msg);
		throw new RepositoryException(msg);
	}
    }
}

/**
 * <code>PathConstraint</code> ...
 */
class PathConstraint extends ValueConstraint {
    final Pattern pattern;

    PathConstraint(String definition) throws InvalidConstraintException {
	super(definition);

	// constraint format: regexp
	try {
	    pattern = Pattern.compile(definition);
	} catch (PatternSyntaxException pse) {
	    String msg = "'" + definition + "' is not valid regular expression syntax";
	    log.error(msg, pse);
	    throw new InvalidConstraintException(msg, pse);
	}
    }

    void check(String text) throws ConstraintViolationException {
	if (text == null) {
	    throw new ConstraintViolationException("null value does not satisfy the constraint '" + definition + "'");
	}
	Matcher matcher = pattern.matcher(text);
	if (!matcher.matches()) {
	    throw new ConstraintViolationException("'" + text + "' does not satisfy the constraint '" + definition + "'");
	}
    }

    void check(InternalValue value) throws ConstraintViolationException, RepositoryException {
	if (value == null) {
	    throw new ConstraintViolationException("null value does not satisfy the constraint '" + definition + "'");
	}
	// @todo check PATH value constraint (need a NamespaceResolver)
	/*
	switch (value.getType()) {
	    case PropertyType.PATH:
		Path p = (Path) value.internalValue();
		check(p.toJCRPath(nsResolver));
		return;

	    default:
		String msg = "PATH constraint can not be applied to value of type: " + PropertyType.nameFromValue(value.getType());
		log.error(msg);
		throw new RepositoryException(msg);
	}
	*/
	log.warn("validation of PATH constraint is not yet implemented");
    }
}

/**
 * <code>NameConstraint</code> ...
 */
class NameConstraint extends ValueConstraint {
    final Pattern pattern;

    NameConstraint(String definition) throws InvalidConstraintException {
	super(definition);

	// constraint format: regexp
	try {
	    pattern = Pattern.compile(definition);
	} catch (PatternSyntaxException pse) {
	    String msg = "'" + definition + "' is not valid regular expression syntax";
	    log.error(msg, pse);
	    throw new InvalidConstraintException(msg, pse);
	}
    }

    void check(String text) throws ConstraintViolationException {
	if (text == null) {
	    throw new ConstraintViolationException("null value does not satisfy the constraint '" + definition + "'");
	}
	Matcher matcher = pattern.matcher(text);
	if (!matcher.matches()) {
	    throw new ConstraintViolationException("'" + text + "' does not satisfy the constraint '" + definition + "'");
	}
    }

    void check(InternalValue value) throws ConstraintViolationException, RepositoryException {
	if (value == null) {
	    throw new ConstraintViolationException("null value does not satisfy the constraint '" + definition + "'");
	}
	// @todo check NAME value constraint (need a NamespaceResolver)
	/*
	switch (value.getType()) {
	    case PropertyType.NAME:
		QName name = (QName) value.internalValue();
		check(name.toJCRName(nsResolver);
		return;

	    default:
		String msg = "NAME constraint can not be applied to value of type: " + PropertyType.nameFromValue(value.getType());
		log.error(msg);
		throw new RepositoryException(msg);
	}
	*/
	log.warn("validation of NAME constraint is not yet implemented");
    }
}

/**
 * <code>ReferenceConstraint</code> ...
 */
class ReferenceConstraint extends ValueConstraint {
    final QName[] ntNames;

    ReferenceConstraint(String definition, NodeTypeRegistry ntReg, NamespaceResolver nsResolver) throws InvalidConstraintException {
	super(definition);

	// format: comma-separated list of node type names
	String[] strings = definition.split(",\\s*");
	ntNames = new QName[strings.length];
	/*
	for (int i = 0; i < strings.length; i++) {
	    // every node type specified must be registered
	    String s = strings[i];
	    try {
		ntNames[i] = QName.fromJCRName(s, nsResolver);
	    } catch (UnknownPrefixException upe) {
		String msg = "invalid node type specified as value constraint: " + s;
		log.error(msg, upe);
		throw new InvalidConstraintException(msg, upe);
	    }
	    try {
		ntReg.getEffectiveNodeType(ntNames[i]);
	    } catch (Exception e) {
		String msg = "invalid node type specified as value constraint: " + ntNames[i];
		log.error(msg, e);
		throw new InvalidConstraintException(msg, e);
	    }
	}
	*/
    }

    QName[] getNodeTypeNames() {
	return ntNames;
    }

    void check(Node target) throws ConstraintViolationException {
	if (target == null) {
	    throw new ConstraintViolationException("null value does not satisfy the constraint '" + definition + "'");
	}
    }

    void check(InternalValue value) throws ConstraintViolationException, RepositoryException {
	if (value == null) {
	    throw new ConstraintViolationException("null value does not satisfy the constraint '" + definition + "'");
	}
	// @todo check REFERENCE value constraint (need a session)
	/*
	switch (value.getType()) {
	    case PropertyType.REFERENCE:
		UUID targetUUID = (UUID) value.internalValue();
		check(session.getNodeByUUID(targetUUID.toString()));
		return;

	    default:
		String msg = "REFERENCE constraint can not be applied to value of type: " + PropertyType.nameFromValue(value.getType());
		log.error(msg);
		throw new RepositoryException(msg);
	}
	*/
	log.warn("validation of REFERENCE constraint is not yet implemented");
    }
}


