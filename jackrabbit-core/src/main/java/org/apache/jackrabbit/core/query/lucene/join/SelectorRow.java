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

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.query.qom.PropertyValue;

import org.apache.jackrabbit.commons.query.qom.OperandEvaluator;

/**
 * A row implementation for a query with just a single selector.
 */
public class SelectorRow extends AbstractRow {

    private final String selector;

    private final Node node;

    private final double score;

    public SelectorRow(
            Map<String, PropertyValue> columns, OperandEvaluator evaluator,
            String selector, Node node, double score) {
        super(columns, evaluator);
        this.selector = selector;
        this.node = node;
        this.score = score;
    }

    public Node getNode() {
        return node;
    }

    public Node getNode(String selectorName) throws RepositoryException {
        checkSelectorName(selectorName);
        return node;
    }

    public double getScore() {
        return score;
    }

    public double getScore(String selectorName) throws RepositoryException {
        checkSelectorName(selectorName);
        return score;
    }

    private void checkSelectorName(String name) throws RepositoryException {
        if (!selector.equals(name)) {
            throw new RepositoryException(
                    "Selector " + name + " is not included in this row");
        }
    }

    //--------------------------------------------------------------< Object >

    public String toString() {
        return "{ " + selector + ": " + node + " }";
    }

}
