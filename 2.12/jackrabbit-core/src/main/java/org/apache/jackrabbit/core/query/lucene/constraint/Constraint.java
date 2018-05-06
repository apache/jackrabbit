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
 * <code>Constraint</code> defines an interface for a QOM constraint.
 */
public interface Constraint {

    /**
     * Evaluates this constraint for the given row.
     *
     * @param row           the current row of score nodes.
     * @param selectorNames the selector names associated with <code>row</code>.
     * @param context       the evaluation context.
     * @return <code>true</code> if the row satisfies the constraint,
     *         <code>false</code> otherwise.
     * @throws IOException if an error occurs while evaluating the constraint.
     */
    public boolean evaluate(ScoreNode[] row,
                            Name[] selectorNames,
                            EvaluationContext context) throws IOException;
}
