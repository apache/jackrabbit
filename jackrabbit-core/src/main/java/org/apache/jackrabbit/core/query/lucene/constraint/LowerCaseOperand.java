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

import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.query.lucene.ScoreNode;

/**
 * <code>LowerCaseOperand</code> implements a lower case operand.
 */
public class LowerCaseOperand extends DynamicOperand {

    /**
     * The dynamic operand for which to lower case the value.
     */
    private final DynamicOperand operand;

    /**
     * Creates a new lower case operand.
     *
     * @param operand the operand to lower case the value.
     */
    public LowerCaseOperand(DynamicOperand operand) {
        super();
        this.operand = operand;
    }

    /**
     * {@inheritDoc}
     */
    public Value[] getValues(ScoreNode sn, EvaluationContext context)
            throws RepositoryException {
        ValueFactory vf = context.getSession().getValueFactory();
        Value[] values = operand.getValues(sn, context);
        for (int i = 0; i < values.length; i++) {
            values[i] = vf.createValue(values[i].getString().toLowerCase());
        }
        return values;
    }
}
