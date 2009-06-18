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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.ConstraintViolationException;

import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.spi.commons.nodetype.InvalidConstraintException;

/**
 * <code>NumericConstraint</code> ...
 */
class NumericConstraint extends ValueConstraint {

    private final boolean lowerInclusive;

    private final Double lowerLimit;

    private final boolean upperInclusive;

    private final Double upperLimit;

    public NumericConstraint(String definition) throws InvalidConstraintException {
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
                    if (lowerLimit > upperLimit) {
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
                if (number < lowerLimit) {
                    throw new ConstraintViolationException(number
                            + " does not satisfy the constraint '"
                            + getString() + "'");
                }
            } else {
                if (number <= lowerLimit) {
                    throw new ConstraintViolationException(number
                            + " does not satisfy the constraint '"
                            + getString() + "'");
                }
            }
        }
        if (upperLimit != null) {
            if (upperInclusive) {
                if (number > upperLimit) {
                    throw new ConstraintViolationException(number
                            + " does not satisfy the constraint '"
                            + getString() + "'");
                }
            } else {
                if (number >= upperLimit) {
                    throw new ConstraintViolationException(number
                            + " does not satisfy the constraint '"
                            + getString() + "'");
                }
            }
        }
    }

    /**
     * @see org.apache.jackrabbit.spi.QValueConstraint#check(QValue)
     */
    public void check(QValue value) throws ConstraintViolationException, RepositoryException {
        if (value == null) {
            throw new ConstraintViolationException("null value does not satisfy the constraint '"
                    + getString() + "'");
        }
        switch (value.getType()) {
            case PropertyType.LONG:
                check(value.getLong());
                return;

            case PropertyType.DOUBLE:
                check(value.getDouble());
                return;

            case PropertyType.DECIMAL:
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
