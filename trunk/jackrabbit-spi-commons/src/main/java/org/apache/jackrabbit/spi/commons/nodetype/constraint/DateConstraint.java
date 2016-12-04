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

import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;
import javax.jcr.nodetype.ConstraintViolationException;

import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.spi.commons.nodetype.InvalidConstraintException;
import org.apache.jackrabbit.value.DateValue;

/**
 * <code>DateConstraint</code> ...
 */
class DateConstraint extends ValueConstraint {

    private final boolean lowerInclusive;

    private final Calendar lowerLimit;

    private final boolean upperInclusive;

    private final Calendar upperLimit;

    public DateConstraint(String definition) throws InvalidConstraintException {
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
                // group 4 is upper inclusive/exclusive
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
            throw new ConstraintViolationException("null value does not satisfy the constraint '" + getString() + "'");
        }
        if (lowerLimit != null) {
            if (lowerInclusive) {
                if (cal.getTimeInMillis() < lowerLimit.getTimeInMillis()) {
                    throw new ConstraintViolationException(cal
                            + " does not satisfy the constraint '"
                            + getString() + "'");
                }
            } else {
                if (cal.getTimeInMillis() <= lowerLimit.getTimeInMillis()) {
                    throw new ConstraintViolationException(cal
                            + " does not satisfy the constraint '"
                            + getString() + "'");
                }
            }
        }
        if (upperLimit != null) {
            if (upperInclusive) {
                if (cal.getTimeInMillis() > upperLimit.getTimeInMillis()) {
                    throw new ConstraintViolationException(cal
                            + " does not satisfy the constraint '"
                            + getString() + "'");
                }
            } else {
                if (cal.getTimeInMillis() >= upperLimit.getTimeInMillis()) {
                    throw new ConstraintViolationException(cal
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
            throw new ConstraintViolationException("null value does not satisfy the constraint '" + getString() + "'");
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
