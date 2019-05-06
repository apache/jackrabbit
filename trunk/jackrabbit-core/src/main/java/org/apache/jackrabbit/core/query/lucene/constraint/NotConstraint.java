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

import java.io.IOException;

import org.apache.jackrabbit.core.query.lucene.ScoreNode;
import org.apache.jackrabbit.spi.Name;

/**
 * <code>NotConstraint</code> implements a NOT constraint.
 */
public class NotConstraint implements Constraint {

    /**
     * The single operand.
     */
    private final Constraint constraint;

    /**
     * Creates a new NOT constraint with the given <code>constraint</code> as
     * its operand.
     *
     * @param constraint the operand.
     */
    public NotConstraint(Constraint constraint) {
        this.constraint = constraint;
    }

    /**
     * {@inheritDoc}
     */
    public boolean evaluate(ScoreNode[] row,
                            Name[] selectorNames,
                            EvaluationContext context)
            throws IOException {
        return !constraint.evaluate(row, selectorNames, context);
    }
}
