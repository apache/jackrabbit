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
package org.apache.jackrabbit.commons.query;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.query.Row;

public class SimpleRow implements Row {

    private final String[] columnNames;

    private final Value[] values;

    private final String[] selectorNames;

    private final Node[] nodes;

    private final double[] scores;

    public SimpleRow(
            String[] columnNames, Value[] values,
            String[] selectorNames, Node[] nodes, double[] scores) {
        this.columnNames = columnNames;
        this.values = values;
        this.selectorNames = selectorNames;
        this.nodes = nodes;
        this.scores = scores;
    }

    public Value[] getValues() {
        return values;
    }

    public Value getValue(String columnName) throws ItemNotFoundException {
        for (int i = 0; i < columnNames.length; i++) {
            if (columnNames[i].equals(columnName)) {
                return values[i];
            }
        }
        throw new ItemNotFoundException("Unknown column name: " + columnName);
    }

    public Node getNode() throws RepositoryException {
        checkSingleSelector();
        return nodes[0];
    }

    public Node getNode(String selectorName) throws RepositoryException {
        return nodes[getSelectorIndex(selectorName)];
    }

    public String getPath() throws RepositoryException {
        return getNode().getPath();
    }

    public String getPath(String selectorName) throws RepositoryException {
        return getNode(selectorName).getPath();
    }

    public double getScore() throws RepositoryException {
        checkSingleSelector();
        return scores[0];
    }

    public double getScore(String selectorName) throws RepositoryException {
        return scores[getSelectorIndex(selectorName)];
    }

    private void checkSingleSelector() throws RepositoryException {
        if (nodes.length != 1) {
            throw new RepositoryException("This row has more than one selector");
        }
    }

    private int getSelectorIndex(String selector) throws RepositoryException {
        for (int i = 0; i < selectorNames.length; i++) {
            if (selectorNames[i].equals(selector)) {
                return i;
            }
        }
        throw new RepositoryException("Unknown selector name: " + selector);
    }
}
