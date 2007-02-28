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
package org.apache.jackrabbit.spi.rmi.common;

import org.apache.jackrabbit.spi.QueryResultRow;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.QValue;

import java.io.Serializable;

/**
 * <code>QueryResultRowImpl</code> implements a serializable
 * <code>QueryResultRow</code>. This implementation requires that the passed
 * {@link org.apache.jackrabbit.spi.QValue}s passed in the constructor are
 * serializable!
 */
public class QueryResultRowImpl implements QueryResultRow, Serializable {

    /**
     * The node id of the underlying row.
     */
    private final NodeId nodeId;

    /**
     * The score value for this row.
     */
    private final double score;

    /**
     * The QValues for this row.
     */
    private final QValue[] values;

    /**
     * Creates a new query result row for the given <code>row</code>.
     *
     * @param nodeId the id of the node this row represents.
     * @param score  the score value for this row
     * @param values the values for this row
     */
    public QueryResultRowImpl(NodeId nodeId, double score, QValue[] values) {
        this.nodeId = nodeId;
        this.score = score;
        this.values = values;
    }

    /**
     * {@inheritDoc}
     */
    public NodeId getNodeId() {
        return nodeId;
    }

    /**
     * {@inheritDoc}
     */
    public double getScore() {
        return score;
    }

    /**
     * {@inheritDoc}
     */
    public QValue[] getValues() {
        QValue[] vals = new QValue[values.length];
        System.arraycopy(values, 0, vals, 0, values.length);
        return vals;
    }
}
