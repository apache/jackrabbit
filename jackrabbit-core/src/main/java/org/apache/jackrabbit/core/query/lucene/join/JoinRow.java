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
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.query.Row;
import javax.jcr.query.qom.PropertyValue;

import org.apache.jackrabbit.commons.query.qom.OperandEvaluator;

public class JoinRow extends AbstractRow {

    private final Row leftRow;

    private final Set<String> leftSelectors;

    private final Row rightRow;

    private final Set<String> rightSelectors;

    public JoinRow(
            Map<String, PropertyValue> columns, OperandEvaluator evaluator,
            Row leftRow, Set<String> leftSelectors,
            Row rightRow, Set<String> rightSelectors) {
        super(columns, evaluator);
        this.leftRow = leftRow;
        this.leftSelectors = leftSelectors;
        this.rightRow = rightRow;
        this.rightSelectors = rightSelectors;
    }

    public Node getNode() throws RepositoryException {
        throw new RepositoryException();
    }

    public Node getNode(String selectorName) throws RepositoryException {
        Row row = getRow(selectorName);
        if (row != null) {
            return row.getNode(selectorName);
        } else {
            return null;
        }
    }

    public double getScore() throws RepositoryException {
        throw new RepositoryException();
    }

    public double getScore(String selectorName) throws RepositoryException {
        Row row = getRow(selectorName);
        if (row != null) {
            return row.getScore(selectorName);
        } else {
            return 0.0;
        }
    }

    private Row getRow(String selector) throws RepositoryException {
        if (leftSelectors.contains(selector)) {
            return leftRow;
        } else if (rightSelectors.contains(selector)) {
            return rightRow;
        } else {
            throw new RepositoryException(
                    "Selector " + selector + " is not included in this row");
        }
    }

    //--------------------------------------------------------------< Object >

    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("{ ");
        for (String selector : leftSelectors) {
            builder.append(selector);
            builder.append("=");
            try {
                builder.append(leftRow.getNode(selector));
            } catch (RepositoryException e) {
                builder.append(e.getMessage());
            }
            builder.append(" ");
        }
        for (String selector : rightSelectors) {
            builder.append(selector);
            builder.append("=");
            if(rightRow != null){
                try {
                    builder.append(rightRow.getNode(selector));
                } catch (RepositoryException e) {
                    builder.append(e.getMessage());
                }
            }else{
                builder.append("null");
            }
            builder.append(" ");
        }
        builder.append("}");
        return builder.toString();
    }

}
