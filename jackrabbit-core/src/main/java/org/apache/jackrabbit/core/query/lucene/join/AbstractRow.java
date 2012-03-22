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
package org.apache.jackrabbit.core.query.lucene.join;

import java.util.Map;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.query.Row;
import javax.jcr.query.qom.Operand;
import javax.jcr.query.qom.PropertyValue;

import org.apache.jackrabbit.commons.query.qom.OperandEvaluator;

abstract class AbstractRow implements Row {

    private final Map<String, PropertyValue> columns;

    private final OperandEvaluator evaluator;

    protected AbstractRow(
            Map<String, PropertyValue> columns, OperandEvaluator evaluator) {
        this.columns = columns;
        this.evaluator = evaluator;
    }

    public Value[] getValues() throws RepositoryException {
        Value[] values = new Value[columns.size()];
        int i = 0;
        for (String columnName : columns.keySet()) {
            values[i++] = getValue(columnName);
        }
        return values;
    }

    public Value getValue(String columnName)
            throws ItemNotFoundException, RepositoryException {
        Operand operand = columns.get(columnName);
        if (operand != null) {
            return evaluator.getValue(operand, this);
        } else {
            throw new ItemNotFoundException(
                    "Column " + columnName + " is not included in this row");
        }
    }

    public String getPath() throws RepositoryException {
        Node node = getNode();
        if (node != null) {
            return node.getPath();
        } else {
            return null;
        }
    }

    public String getPath(String selectorName) throws RepositoryException {
        Node node = getNode(selectorName);
        if (node != null) {
            return node.getPath();
        } else {
            return null;
        }
    }

}
