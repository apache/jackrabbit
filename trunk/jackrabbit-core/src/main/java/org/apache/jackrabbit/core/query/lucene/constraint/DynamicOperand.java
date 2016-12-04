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
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.query.lucene.ScoreNode;

/**
 * <code>DynamicOperand</code> is a base class for dynamic operands.
 */
public abstract class DynamicOperand {

    /**
     * An empty {@link Value} array.
     */
    protected static final Value[] EMPTY = new Value[0];

    /**
     * Returns the values for the given score node <code>sn</code> of this
     * dynamic operand. If there are no values for the given score node, then
     * an empty array is returned.
     *
     * @param sn      the current score node.
     * @param context the evaluation context.
     * @return the values for the given score node.
     * @throws RepositoryException if an error occurs while retrieving the value.
     */
    public abstract Value[] getValues(ScoreNode sn, EvaluationContext context)
            throws RepositoryException;
}
