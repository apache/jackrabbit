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
package org.apache.jackrabbit.spi.commons.query;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.jcr.RepositoryException;

/**
 * <code>PropertyFunctionQueryNode</code> allows to place function calls on properties
 * in a query. Supported function names are:
 * <ul>
 * <li><code>upper-case</code> as specified in <a href="http://www.w3.org/TR/xquery-operators/#func-upper-case">fn:upper-case()</a></li>
 * <li><code>lower-case</code> as specified in <a href="http://www.w3.org/TR/xquery-operators/#func-lower-case">fn:lower-case()</a></li>
 * </ul>
 */
public class PropertyFunctionQueryNode extends QueryNode {

    /**
     * Requests that property values in a {@link RelationQueryNode} are
     * converted to upper case before they are matched with the literal.
     */
    public static final String UPPER_CASE = "upper-case";

    /**
     * Requests that property values in a {@link RelationQueryNode} are
     * converted to lower case before they are matched with the literal.
     */
    public static final String LOWER_CASE = "lower-case";

    /**
     * The set of supported function names.
     */
    private static final Set SUPPORTED_FUNCTION_NAMES;

    static {
        Set tmp = new HashSet();
        tmp.add(UPPER_CASE);
        tmp.add(LOWER_CASE);
        SUPPORTED_FUNCTION_NAMES = Collections.unmodifiableSet(tmp);
    }

    /**
     * The function name.
     */
    private final String functionName;

    /**
     * Creates a property function query node. This query node describes a
     * function which is applied to a property parameter of the
     * <code>parent</code> query node.
     *
     * @param parent       the query node where this function is applied to.
     * @param functionName the name of the function which is applied to
     *                     <code>parent</code>.
     * @throws IllegalArgumentException if <code>functionName</code> is not a
     *                                  supported function.
     */
    protected PropertyFunctionQueryNode(QueryNode parent, String functionName)
            throws IllegalArgumentException {
        super(parent);
        if (!SUPPORTED_FUNCTION_NAMES.contains(functionName)) {
            throw new IllegalArgumentException("unknown function name");
        }
        this.functionName = functionName;
    }

    /**
     * {@inheritDoc}
     * @throws RepositoryException
     */
    public Object accept(QueryNodeVisitor visitor, Object data) throws RepositoryException {
        return visitor.visit(this, data);
    }

    /**
     * Returns the type of this node.
     *
     * @return the type of this node.
     */
    public int getType() {
        return QueryNode.TYPE_PROP_FUNCTION;
    }

    /**
     * {@inheritDoc}
     */
    public boolean equals(Object obj) {
        if (obj instanceof PropertyFunctionQueryNode) {
            PropertyFunctionQueryNode other = (PropertyFunctionQueryNode) obj;
            return functionName.equals(other.functionName);
        }
        return false;
    }

    /**
     * @return the name of this function.
     */
    public String getFunctionName() {
        return functionName;
    }

    /**
     * {@inheritDoc}
     */
    public boolean needsSystemTree() {
        return false;
    }

}
