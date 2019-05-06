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

import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.spi.commons.nodetype.InvalidConstraintException;

/**
 * <code>BooleanConstraint</code> ...
 */
class BooleanConstraint extends ValueConstraint {

    private final boolean reqBool;

    public BooleanConstraint(String definition) throws InvalidConstraintException {
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
     * @see org.apache.jackrabbit.spi.QValueConstraint#check(QValue)
     */
    public void check(QValue value) throws ConstraintViolationException, RepositoryException {
        if (value == null) {
            throw new ConstraintViolationException("null value does not satisfy the constraint '"  + getString() + "'");
        }
        switch (value.getType()) {
            case PropertyType.BOOLEAN:
                boolean b = Boolean.valueOf(value.getString());
                if (b != reqBool) {
                    throw new ConstraintViolationException("'" + b + "' does not satisfy the constraint '" + getString() + "'");
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
