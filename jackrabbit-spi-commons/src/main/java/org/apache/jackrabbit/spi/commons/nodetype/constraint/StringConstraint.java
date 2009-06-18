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
import java.util.regex.PatternSyntaxException;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.ConstraintViolationException;

import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.spi.commons.nodetype.InvalidConstraintException;

/**
 * <code>StringConstraint</code> ...
 */
class StringConstraint extends ValueConstraint {

    private final Pattern pattern;

    public StringConstraint(String definition) throws InvalidConstraintException {
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
     * @see org.apache.jackrabbit.spi.QValueConstraint#check(QValue)
     */
    public void check(QValue value) throws ConstraintViolationException, RepositoryException {
        if (value == null) {
            throw new ConstraintViolationException("null value does not satisfy the constraint '" + getString() + "'");
        }
        switch (value.getType()) {
            case PropertyType.STRING:
            case PropertyType.URI:
                String text = value.getString();
                Matcher matcher = pattern.matcher(text);
                if (!matcher.matches()) {
                    throw new ConstraintViolationException("'" + text  + "' does not satisfy the constraint '" + getString() + "'");
                }
                return;

            default:
                String msg = "String constraint can not be applied to value of type: " + PropertyType.nameFromValue(value.getType());
                log.debug(msg);
                throw new RepositoryException(msg);
        }
    }

}
