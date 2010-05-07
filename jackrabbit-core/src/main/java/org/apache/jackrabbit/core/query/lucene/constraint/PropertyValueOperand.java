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
import javax.jcr.Property;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.spi.commons.query.qom.PropertyValueImpl;
import org.apache.jackrabbit.core.query.lucene.ScoreNode;
import org.apache.jackrabbit.core.query.lucene.Util;
import org.apache.jackrabbit.core.state.ItemStateManager;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.NoSuchItemStateException;
import org.apache.jackrabbit.core.PropertyId;
import org.apache.jackrabbit.core.PropertyImpl;
import org.apache.jackrabbit.core.SessionImpl;

/**
 * <code>PropertyValueOperand</code> implements a property value operand.
 */
public class PropertyValueOperand extends DynamicOperand {

    /**
     * The QOM operand.
     */
    private final PropertyValueImpl operand;

    /**
     * Creates a new property value operand.
     *
     * @param operand the QOM operand.
     */
    public PropertyValueOperand(PropertyValueImpl operand) {
        super();
        this.operand = operand;
    }

    /**
     * Returns the property state for the given score node or <code>null</code>
     * if none exists.
     *
     * @param sn the current score node.
     * @param context the evaluation context.
     * @return the property state or <code>null</code>.
     * @throws IOException if an error occurs while reading.
     */
    public final PropertyState getPropertyState(ScoreNode sn,
                                          EvaluationContext context)
            throws IOException {
        ItemStateManager ism = context.getItemStateManager();
        PropertyId propId = new PropertyId(sn.getNodeId(), operand.getPropertyQName());
        try {
            return (PropertyState) ism.getItemState(propId);
        } catch (NoSuchItemStateException e) {
            return null;
        } catch (ItemStateException e) {
            throw Util.createIOException(e);
        }
    }

    /**
     * Returns the property for the given score node or <code>null</code> if
     * none exists.
     *
     * @param sn the current score node.
     * @param context the evaluation context.
     * @return the property or <code>null</code>.
     * @throws IOException if an error occurs while reading.
     */
    public final Property getProperty(ScoreNode sn,
                                      EvaluationContext context)
            throws IOException {
        SessionImpl session = context.getSession();
        try {
            Node n = session.getNodeById(sn.getNodeId());
            return n.getProperty(operand.getPropertyName());
        } catch (PathNotFoundException e) {
            return null;
        } catch (RepositoryException e) {
            throw Util.createIOException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Value[] getValues(ScoreNode sn, EvaluationContext context)
            throws IOException {
        PropertyImpl prop = (PropertyImpl) getProperty(sn, context);
        if (prop == null) {
            return EMPTY;
        } else {
            try {
                if (prop.isMultiple()) {
                    return prop.getValues();
                } else {
                    return new Value[]{prop.getValue()};
                }
            } catch (RepositoryException e) {
                throw Util.createIOException(e);
            }
        }
    }
}
