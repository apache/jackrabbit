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
package org.apache.jackrabbit.spi;

/**
 * <code>QueryResultRow</code> represents the SPI equivalent of a query result
 * row. It provides access to the id of the Node this row represents as well
 * as to the score and to the qualified values represent in this result row.
 */
public interface QueryResultRow {

    /**
     * Returns {@link NodeId} of node this result row represents.
     *
     * @return node id of the <code>Node</code> this result row represents.
     */
    public NodeId getNodeId();

    /**
     * Returns score of this result row.
     *
     * @return score of this result row.
     */
    public double getScore();

    /**
     * Returns an array of <code>QValue</code>s.
     *
     * @return an array of <code>QValue</code>s representing the values present
     * in this result row.
     * @see javax.jcr.query.Row#getValue(String)
     * @see javax.jcr.query.Row#getValues()
     */
    public QValue[] getValues();
}
