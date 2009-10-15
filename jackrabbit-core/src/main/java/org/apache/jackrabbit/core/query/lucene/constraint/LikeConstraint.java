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
package org.apache.jackrabbit.core.query.lucene.constraint;

import java.util.regex.Matcher;

import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.apache.jackrabbit.commons.query.qom.Operator;
import org.apache.jackrabbit.core.query.lucene.Util;
import org.apache.jackrabbit.spi.commons.query.qom.SelectorImpl;

/**
 * <code>LikeConstraint</code> implements a like constraint.
 */
public class LikeConstraint extends ComparisonConstraint {

    /**
     * A regular expression matcher for the like constraint.
     */
    private final Matcher matcher;

    /**
     * Creates a new like constraint.
     *
     * @param operand1 the dynamic operand.
     * @param operand2 the static operand.
     * @param selector the selector for the dynamic operand.
     * @throws RepositoryException if an error occurs reading the string value
     *                             from the static operand.
     */
    public LikeConstraint(DynamicOperand operand1,
                          Value operand2,
                          SelectorImpl selector) throws RepositoryException {
        super(operand1, Operator.LIKE, operand2, selector);
        this.matcher = Util.createRegexp(operand2.getString()).matcher("");
    }

    /**
     * {@inheritDoc}
     */
    protected boolean evaluate(Value op1) throws RepositoryException {
        return matcher.reset(op1.getString()).matches();
    }
}
