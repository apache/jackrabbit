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
package org.apache.jackrabbit.spi.commons.query.qom;

/**
 * <code>DefaultQOMTreeVisitor</code> default implementation of a {@link
 * QOMTreeVisitor}.
 */
public class DefaultQOMTreeVisitor implements QOMTreeVisitor {

    /**
     * Does nothing and returns <code>data</code>.
     */
    public Object visit(AndImpl node, Object data) throws Exception {
        return data;
    }

    /**
     * Does nothing and returns <code>data</code>.
     */
    public Object visit(BindVariableValueImpl node, Object data) throws Exception {
        return data;
    }

    /**
     * Does nothing and returns <code>data</code>.
     */
    public Object visit(ChildNodeImpl node, Object data) throws Exception {
        return data;
    }

    /**
     * Does nothing and returns <code>data</code>.
     */
    public Object visit(ChildNodeJoinConditionImpl node, Object data) throws Exception {
        return data;
    }

    /**
     * Does nothing and returns <code>data</code>.
     */
    public Object visit(ColumnImpl node, Object data) throws Exception {
        return data;
    }

    /**
     * Does nothing and returns <code>data</code>.
     */
    public Object visit(ComparisonImpl node, Object data) throws Exception {
        return data;
    }

    /**
     * Does nothing and returns <code>data</code>.
     */
    public Object visit(DescendantNodeImpl node, Object data) throws Exception {
        return data;
    }

    /**
     * Does nothing and returns <code>data</code>.
     */
    public Object visit(DescendantNodeJoinConditionImpl node, Object data) throws Exception {
        return data;
    }

    /**
     * Does nothing and returns <code>data</code>.
     */
    public Object visit(EquiJoinConditionImpl node, Object data) throws Exception {
        return data;
    }

    /**
     * Does nothing and returns <code>data</code>.
     */
    public Object visit(FullTextSearchImpl node, Object data) throws Exception {
        return data;
    }

    /**
     * Does nothing and returns <code>data</code>.
     */
    public Object visit(FullTextSearchScoreImpl node, Object data) throws Exception {
        return data;
    }

    /**
     * Does nothing and returns <code>data</code>.
     */
    public Object visit(JoinImpl node, Object data) throws Exception {
        return data;
    }

    /**
     * Does nothing and returns <code>data</code>.
     */
    public Object visit(LengthImpl node, Object data) throws Exception {
        return data;
    }

    /**
     * Does nothing and returns <code>data</code>.
     */
    public Object visit(LiteralImpl node, Object data) throws Exception {
        return data;
    }

    /**
     * Does nothing and returns <code>data</code>.
     */
    public Object visit(LowerCaseImpl node, Object data) throws Exception {
        return data;
    }

    /**
     * Does nothing and returns <code>data</code>.
     */
    public Object visit(NodeLocalNameImpl node, Object data) throws Exception {
        return data;
    }

    /**
     * Does nothing and returns <code>data</code>.
     */
    public Object visit(NodeNameImpl node, Object data) throws Exception {
        return data;
    }

    /**
     * Does nothing and returns <code>data</code>.
     */
    public Object visit(NotImpl node, Object data) throws Exception {
        return data;
    }

    /**
     * Does nothing and returns <code>data</code>.
     */
    public Object visit(OrderingImpl node, Object data) throws Exception {
        return data;
    }

    /**
     * Does nothing and returns <code>data</code>.
     */
    public Object visit(OrImpl node, Object data) throws Exception {
        return data;
    }

    /**
     * Does nothing and returns <code>data</code>.
     */
    public Object visit(PropertyExistenceImpl node, Object data) throws Exception {
        return data;
    }

    /**
     * Does nothing and returns <code>data</code>.
     */
    public Object visit(PropertyValueImpl node, Object data) throws Exception {
        return data;
    }

    /**
     * Does nothing and returns <code>data</code>.
     */
    public Object visit(QueryObjectModelTree node, Object data) throws Exception {
        return data;
    }

    /**
     * Does nothing and returns <code>data</code>.
     */
    public Object visit(SameNodeImpl node, Object data) throws Exception {
        return data;
    }

    /**
     * Does nothing and returns <code>data</code>.
     */
    public Object visit(SameNodeJoinConditionImpl node, Object data) throws Exception {
        return data;
    }

    /**
     * Does nothing and returns <code>data</code>.
     */
    public Object visit(SelectorImpl node, Object data) throws Exception {
        return data;
    }

    /**
     * Does nothing and returns <code>data</code>.
     */
    public Object visit(UpperCaseImpl node, Object data) throws Exception {
        return data;
    }
}
