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
package org.apache.jackrabbit.core.query.qom;

/**
 * <code>QOMTreeVisitor</code>...
 */
public interface QOMTreeVisitor {

    public void visit(AndImpl node, Object data);

    public void visit(BindVariableValueImpl node, Object data);

    public void visit(ChildNodeImpl node, Object data);

    public void visit(ChildNodeJoinConditionImpl node, Object data);

    public void visit(ColumnImpl node, Object data);

    public void visit(ComparisonImpl node, Object data);

    public void visit(DescendantNodeImpl node, Object data);

    public void visit(DescendantNodeJoinConditionImpl node, Object data);

    public void visit(EquiJoinConditionImpl node, Object data);

    public void visit(FullTextSearchImpl node, Object data);

    public void visit(FullTextSearchScoreImpl node, Object data);

    public void visit(JoinImpl node, Object data);

    public void visit(LengthImpl node, Object data);

    public void visit(LowerCaseImpl node, Object data);

    public void visit(NodeLocalNameImpl node, Object data);

    public void visit(NodeNameImpl node, Object data);

    public void visit(NotImpl node, Object data);

    public void visit(OrderingImpl node, Object data);

    public void visit(OrImpl node, Object data);

    public void visit(PropertyExistenceImpl node, Object data);

    public void visit(PropertyValueImpl node, Object data);

    public void visit(QueryObjectModelTree node, Object data);

    public void visit(SameNodeImpl node, Object data);

    public void visit(SameNodeJoinConditionImpl node, Object data);

    public void visit(SelectorImpl node, Object data);

    public void visit(UpperCaseImpl node, Object data);
}
