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

import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.query.lucene.ScoreNode;
import org.apache.jackrabbit.core.query.lucene.Util;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.value.InternalValue;

/**
 * <code>LengthOperand</code> implements a length operand.
 */
public class LengthOperand extends DynamicOperand {

    /**
     * The property value operand for which to return the length.
     */
    private final PropertyValueOperand property;

    /**
     * Creates a new length operand.
     *
     * @param property the operand for which to return the length.
     */
    public LengthOperand(PropertyValueOperand property) {
        super();
        this.property = property;
    }

    /**
     * {@inheritDoc}
     */
    public Value[] getValues(ScoreNode sn, EvaluationContext context)
            throws IOException {
        PropertyState ps = property.getPropertyState(sn, context);
        if (ps == null) {
            return EMPTY;
        } else {
            try {
                ValueFactory vf = context.getSession().getValueFactory();
                InternalValue[] values = ps.getValues();
                Value[] lengths = new Value[values.length];
                for (int i = 0; i < lengths.length; i++) {
                    lengths[i] = vf.createValue(Util.getLength(values[i]));
                }
                return lengths;
            } catch (RepositoryException e) {
                throw Util.createIOException(e);
            }
        }
    }
}
