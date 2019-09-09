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
package org.apache.jackrabbit.spi.commons.query.sql;

/**
 * Implements a {@link JCRSQLParserVisitor} with default method implementations.
 * All visit method simply return the <code>data</code> parameter.
 */
class DefaultParserVisitor implements JCRSQLParserVisitor {

    public Object visit(SimpleNode node, Object data) {
        return data;
    }

    public Object visit(ASTQuery node, Object data) {
        return data;
    }

    public Object visit(ASTSelectList node, Object data) {
        return data;
    }

    public Object visit(ASTFromClause node, Object data) {
        return data;
    }

    public Object visit(ASTWhereClause node, Object data) {
        return data;
    }

    public Object visit(ASTPredicate node, Object data) {
        return data;
    }

    public Object visit(ASTOrExpression node, Object data) {
        return data;
    }

    public Object visit(ASTAndExpression node, Object data) {
        return data;
    }

    public Object visit(ASTNotExpression node, Object data) {
        return data;
    }

    public Object visit(ASTBracketExpression node, Object data) {
        return data;
    }

    public Object visit(ASTLiteral node, Object data) {
        return data;
    }

    public Object visit(ASTIdentifier node, Object data) {
        return data;
    }

    public Object visit(ASTOrderByClause node, Object data) {
        return data;
    }

    public Object visit(ASTContainsExpression node, Object data) {
        return data;
    }

    public Object visit(ASTOrderSpec node, Object data) {
        return data;
    }

    public Object visit(ASTAscendingOrderSpec node, Object data) {
        return data;
    }

    public Object visit(ASTDescendingOrderSpec node, Object data) {
        return data;
    }

    public Object visit(ASTLowerFunction node, Object data) {
        return data;
    }

    public Object visit(ASTUpperFunction node, Object data) {
        return data;
    }

    public Object visit(ASTExcerptFunction node, Object data) {
        return data;
    }
}
