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

import javax.jcr.ItemNotFoundException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.query.lucene.ScoreNode;

/**
 * <code>NodeNameOperand</code> implements a node name operand.
 */
public class NodeNameOperand extends DynamicOperand {

    /**
     * Returns the name of the node denoted by the given score node
     * <code>sn</code>.
     *
     * @param sn      the score node.
     * @param context the evaluation context.
     * @return the node name.
     * @throws RepositoryException if an error occurs while reading the name.
     */
    public Value[] getValues(ScoreNode sn, EvaluationContext context)
            throws RepositoryException {
        SessionImpl session = context.getSession();
        try {
            String name = session.getNodeById(sn.getNodeId()).getName();
            return new Value[]{session.getValueFactory().createValue(name, PropertyType.NAME)};
        } catch (ItemNotFoundException e) {
            // access denied to score node
            return new Value[0];
        }
    }
}
