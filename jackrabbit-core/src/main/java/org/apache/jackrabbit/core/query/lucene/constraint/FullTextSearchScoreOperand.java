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
 * <code>FullTextSearchScoreOperand</code> implements a full text search score
 * operand.
 */
public class FullTextSearchScoreOperand extends DynamicOperand {

    /**
     * {@inheritDoc}
     */
    public Value[] getValues(ScoreNode sn, EvaluationContext context)
            throws RepositoryException {
        return new Value[]{context.getSession().getValueFactory().createValue(sn.getScore())};
    }
}
