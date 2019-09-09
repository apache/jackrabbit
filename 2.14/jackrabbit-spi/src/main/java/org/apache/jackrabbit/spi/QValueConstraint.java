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
package org.apache.jackrabbit.spi;

import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.RepositoryException;

/**
 * <code>QValueConstraint</code> is used to check the syntax of a value
 * constraint and to test if a specific value satisfies it.
 *
 * @see PropertyDefinition#getValueConstraints()
 */
public interface QValueConstraint {

    /**
     * Empty array of <code>QValueConstraint</code>.
     */
    public static final QValueConstraint[] EMPTY_ARRAY = new QValueConstraint[0];

    /**
     * Check if the specified value matches this constraint.
     *
     * @param value The value to be tested.
     * @throws ConstraintViolationException If the specified value is
     * <code>null</code> or does not matches the constraint.
     * @throws RepositoryException If another error occurs.
     */
    void check(QValue value) throws ConstraintViolationException, RepositoryException;

    /**
     * For constraints that are not namespace prefix mapping sensitive this
     * method returns the same defined in
     * <code>{@link PropertyDefinition#getValueConstraints()}</code>.
     * <p>
     * Those that are namespace prefix mapping sensitive (e.g.
     * <code>NameConstraint</code>, <code>PathConstraint</code> and
     * <code>ReferenceConstraint</code>) return an internal string.
     *
     * @return the internal definition String
     */
    String getString();

}