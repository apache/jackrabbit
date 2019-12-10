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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.jcr.NamespaceException;
import javax.jcr.RepositoryException;
import javax.jcr.query.InvalidQueryException;

import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.commons.conversion.NameResolver;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.spi.commons.query.AndQueryNode;
import org.apache.jackrabbit.spi.commons.query.DerefQueryNode;
import org.apache.jackrabbit.spi.commons.query.ExactQueryNode;
import org.apache.jackrabbit.spi.commons.query.LocationStepQueryNode;
import org.apache.jackrabbit.spi.commons.query.NodeTypeQueryNode;
import org.apache.jackrabbit.spi.commons.query.NotQueryNode;
import org.apache.jackrabbit.spi.commons.query.OrQueryNode;
import org.apache.jackrabbit.spi.commons.query.OrderQueryNode;
import org.apache.jackrabbit.spi.commons.query.PathQueryNode;
import org.apache.jackrabbit.spi.commons.query.PropertyFunctionQueryNode;
import org.apache.jackrabbit.spi.commons.query.QueryConstants;
import org.apache.jackrabbit.spi.commons.query.QueryNode;
import org.apache.jackrabbit.spi.commons.query.QueryNodeVisitor;
import org.apache.jackrabbit.spi.commons.query.QueryRootNode;
import org.apache.jackrabbit.spi.commons.query.RelationQueryNode;
import org.apache.jackrabbit.spi.commons.query.TextsearchQueryNode;
import org.apache.jackrabbit.util.ISO8601;

/**
 * Implements the query node tree serialization into a String.
 */
class QueryFormat implements QueryNodeVisitor, QueryConstants {

    /**
     * Will be used to resolve QNames
     */
    private final NameResolver resolver;

    /**
     * The String representation of the query node tree
     */
    private final String statement;

    /**
     * List of exception objects created while creating the SQL string
     */
    private final List exceptions = new ArrayList();

    /**
     * List of node types
     */
    private final List nodeTypes = new ArrayList();

    private QueryFormat(QueryRootNode root, NameResolver resolver)
            throws RepositoryException {
        this.resolver = resolver;
        statement = root.accept(this, new StringBuffer()).toString();
        if (exceptions.size() > 0) {
            Exception e = (Exception) exceptions.get(0);
            throw new InvalidQueryException(e.getMessage(), e);
        }
    }

    /**
     * Creates a SQL <code>String</code> representation of the QueryNode tree
     * argument <code>root</code>.
     *
     * @param root     the query node tree.
     * @param resolver to resolve QNames.
     * @return the SQL string representation of the QueryNode tree.
     * @throws InvalidQueryException the query node tree cannot be represented
     *                               as a SQL <code>String</code>.
     */
    public static String toString(QueryRootNode root, NameResolver resolver)
            throws InvalidQueryException {
        try {
            return new QueryFormat(root, resolver).toString();
        }
        catch (RepositoryException e) {
            throw new InvalidQueryException(e);
        }
    }

    /**
     * Returns the string representation.
     *
     * @return the string representation.
     */
    public String toString() {
        return statement;
    }

    //-------------< QueryNodeVisitor interface >-------------------------------

    public Object visit(QueryRootNode node, Object data) throws RepositoryException {
        StringBuffer sb = (StringBuffer) data;
        try {
            sb.append("SELECT");

            Name[] selectProps = node.getSelectProperties();
            if (selectProps.length == 0) {
                sb.append(" *");
            } else {
                String comma = "";
                for (int i = 0; i < selectProps.length; i++) {
                    sb.append(comma).append(" ");
                    appendName(selectProps[i], resolver, sb);
                    comma = ",";
                }
            }

            sb.append(" FROM");

            // node type restrictions are within predicates of location nodes
            // therefore we write the where clause first to a temp string to
            // collect the node types.
            StringBuffer tmp = new StringBuffer();
            LocationStepQueryNode[] steps = node.getLocationNode().getPathSteps();
            QueryNode[] predicates = steps[steps.length - 1].getPredicates();
            // are there any relevant predicates?
            for (int i = 0; i < predicates.length; i++) {
                if (predicates[i].getType() != QueryNode.TYPE_NODETYPE) {
                    tmp.append(" WHERE ");
                }
            }
            String and = "";
            for (int i = 0; i < predicates.length; i++) {
                if (predicates[i].getType() != QueryNode.TYPE_NODETYPE) {
                    tmp.append(and);
                    and = " AND ";
                }
                predicates[i].accept(this, tmp);
            }

            // node types have been collected by now
            String comma = "";
            int ntCount = 0;
            for (Iterator it = nodeTypes.iterator(); it.hasNext(); ntCount++) {
                Name nt = (Name) it.next();
                sb.append(comma).append(" ");
                appendName(nt, resolver, sb);
                comma = ",";
            }

            if (ntCount == 0) {
                sb.append(" ");
                sb.append(resolver.getJCRName(NameConstants.NT_BASE));
            }

            // append WHERE clause
            sb.append(tmp.toString());

            if (steps.length == 2
                    && steps[1].getIncludeDescendants()
                    && steps[1].getNameTest() == null) {
                // then this query selects all paths
            } else if (steps.length == 1
                    && steps[0].getIncludeDescendants()
                    && steps[0].getNameTest() == null) {
                // then this query selects all paths
            } else {
                if (predicates.length > 0) {
                    sb.append(" AND ");
                } else {
                    sb.append(" WHERE ");
                }
                node.getLocationNode().accept(this, sb);
            }
        } catch (NamespaceException e) {
            exceptions.add(e);
        }

        if (node.getOrderNode() != null) {
            node.getOrderNode().accept(this, sb);
        }

        return sb;
    }

    public Object visit(OrQueryNode node, Object data) throws RepositoryException {
        StringBuffer sb = (StringBuffer) data;
        boolean bracket = false;
        if (node.getParent() instanceof LocationStepQueryNode
                || node.getParent() instanceof AndQueryNode
                || node.getParent() instanceof NotQueryNode) {
            bracket = true;
        }
        if (bracket) {
            sb.append("(");
        }
        String or = "";
        QueryNode[] operands = node.getOperands();
        for (int i = 0; i < operands.length; i++) {
            sb.append(or);
            operands[i].accept(this, sb);
            or = " OR ";
        }
        if (bracket) {
            sb.append(")");
        }
        return sb;
    }

    public Object visit(AndQueryNode node, Object data) throws RepositoryException {
        StringBuffer sb = (StringBuffer) data;
        boolean bracket = false;
        if (node.getParent() instanceof NotQueryNode) {
            bracket = true;
        }
        if (bracket) {
            sb.append("(");
        }
        String and = "";
        QueryNode[] operands = node.getOperands();
        for (int i = 0; i < operands.length; i++) {
            sb.append(and);
            int len = sb.length();
            operands[i].accept(this, sb);
            // check if something has been written at all
            // might have been a node type query node
            if (sb.length() - len > 0) {
                and = " AND ";
            } else {
                and = "";
            }
        }
        if (bracket) {
            sb.append(")");
        }
        return sb;
    }

    public Object visit(NotQueryNode node, Object data) throws RepositoryException {
        StringBuffer sb = (StringBuffer) data;
        QueryNode[] operands = node.getOperands();
        if (operands.length > 0) {
            sb.append("NOT ");
            operands[0].accept(this, sb);
        }
        return sb;
    }

    public Object visit(ExactQueryNode node, Object data) {
        StringBuffer sb = (StringBuffer) data;
        try {
            appendName(node.getPropertyName(), resolver, sb);
        } catch (NamespaceException e) {
            exceptions.add(e);
        }
        sb.append("='").append(node.getValue()).append("'");
        return sb;
    }

    public Object visit(NodeTypeQueryNode node, Object data) {
        nodeTypes.add(node.getValue());
        return data;
    }

    public Object visit(TextsearchQueryNode node, Object data) {
        StringBuffer sb = (StringBuffer) data;
        // escape quote
        String query = node.getQuery().replaceAll("'", "''");
        sb.append("CONTAINS(");
        if (node.getRelativePath() == null) {
            sb.append("*");
        } else {
            if (node.getRelativePath().getLength() > 1
                    || !node.getReferencesProperty()) {
                exceptions.add(new InvalidQueryException("Child axis not supported in SQL"));
            } else {
                try {
                    appendName(node.getRelativePath().getName(), resolver, sb);
                } catch (NamespaceException e) {
                    exceptions.add(e);
                }
            }
        }
        sb.append(", '");
        sb.append(query).append("')");
        return sb;
    }

    public Object visit(PathQueryNode node, Object data) throws RepositoryException {
        StringBuffer sb = (StringBuffer) data;
        try {
            if (containsDescendantOrSelf(node)) {
                sb.append("(");
                sb.append(resolver.getJCRName(NameConstants.JCR_PATH));
                sb.append(" LIKE '");
                LocationStepQueryNode[] steps = node.getPathSteps();
                for (int i = 0; i < steps.length; i++) {
                    if (steps[i].getNameTest() == null
                            || steps[i].getNameTest().getLocalName().length() > 0) {
                        sb.append('/');
                    }
                    if (steps[i].getIncludeDescendants()) {
                        sb.append("%/");
                    }
                    steps[i].accept(this, sb);
                }
                sb.append('\'');
                sb.append(" OR ");
                sb.append(resolver.getJCRName(NameConstants.JCR_PATH));
                sb.append(" LIKE '");
                for (int i = 0; i < steps.length; i++) {
                    if (steps[i].getNameTest() == null
                            || steps[i].getNameTest().getLocalName().length() > 0) {
                        sb.append('/');
                    }
                    if (steps[i].getNameTest() != null) {
                        steps[i].accept(this, sb);
                    }
                }
                sb.append("')");
            } else if (containsAllChildrenMatch(node)) {
                sb.append(resolver.getJCRName(NameConstants.JCR_PATH));
                sb.append(" LIKE '");
                StringBuffer path = new StringBuffer();
                LocationStepQueryNode[] steps = node.getPathSteps();
                for (int i = 0; i < steps.length; i++) {
                    if (steps[i].getNameTest() == null
                            || steps[i].getNameTest().getLocalName().length() > 0) {
                        path.append('/');
                    }
                    steps[i].accept(this, path);
                }
                sb.append(path);
                sb.append('\'');
                sb.append(" AND NOT ");
                sb.append(resolver.getJCRName(NameConstants.JCR_PATH));
                sb.append(" LIKE '");
                sb.append(path).append("/%").append('\'');
            } else {
                // just do a best effort
                sb.append(resolver.getJCRName(NameConstants.JCR_PATH));
                sb.append(" LIKE '");
                LocationStepQueryNode[] steps = node.getPathSteps();
                for (int i = 0; i < steps.length; i++) {
                    if (steps[i].getNameTest() == null
                            || steps[i].getNameTest().getLocalName().length() > 0) {
                        sb.append('/');
                    }
                    steps[i].accept(this, sb);
                }
                sb.append('\'');
            }
        } catch (NamespaceException e) {
            exceptions.add(e);
        }
        return sb;
    }

    public Object visit(LocationStepQueryNode node, Object data) {
        StringBuffer sb = (StringBuffer) data;
        if (node.getNameTest() == null) {
            sb.append("%");
        } else {
            if (node.getNameTest().getLocalName().length() > 0) {
                try {
                    sb.append(resolver.getJCRName(node.getNameTest()));
                } catch (NamespaceException e) {
                    exceptions.add(e);
                }
                if (node.getIndex() == LocationStepQueryNode.NONE) {
                    sb.append("[%]");
                } else if (node.getIndex() == 1) {
                    // do nothing
                } else {
                    sb.append('[').append(node.getIndex()).append(']');
                }
            } else {
                // empty name test indicates root node
            }
        }
        return sb;
    }

    public Object visit(DerefQueryNode node, Object data) {
        exceptions.add(new InvalidQueryException("jcr:deref() function not supported in SQL"));
        return data;
    }

    public Object visit(RelationQueryNode node, Object data) throws RepositoryException {
        StringBuffer sb = (StringBuffer) data;
        try {
            StringBuffer propName = new StringBuffer();
            PathQueryNode relPath = node.getRelativePath();
            if (relPath == null) {
                propName.append(".");
            } else if (relPath.getPathSteps().length > 1) {
                exceptions.add(new InvalidQueryException("Child axis not supported in SQL"));
                return data;
            } else {
                visit(relPath, data);
            }
            // surround name with property function
            node.acceptOperands(this, propName);

            if (node.getOperation() == OPERATION_EQ_VALUE || node.getOperation() == OPERATION_EQ_GENERAL) {
                sb.append(propName);
                sb.append(" = ");
                appendValue(node, sb);
            } else if (node.getOperation() == OPERATION_GE_VALUE || node.getOperation() == OPERATION_GE_GENERAL) {
                sb.append(propName);
                sb.append(" >= ");
                appendValue(node, sb);
            } else if (node.getOperation() == OPERATION_GT_VALUE || node.getOperation() == OPERATION_GT_GENERAL) {
                sb.append(propName);
                sb.append(" > ");
                appendValue(node, sb);
            } else if (node.getOperation() == OPERATION_LE_VALUE || node.getOperation() == OPERATION_LE_GENERAL) {
                sb.append(propName);
                sb.append(" <= ");
                appendValue(node, sb);
            } else if (node.getOperation() == OPERATION_LIKE) {
                sb.append(propName);
                sb.append(" LIKE ");
                appendValue(node, sb);
            } else if (node.getOperation() == OPERATION_LT_VALUE || node.getOperation() == OPERATION_LT_GENERAL) {
                sb.append(propName);
                sb.append(" < ");
                appendValue(node, sb);
            } else if (node.getOperation() == OPERATION_NE_VALUE || node.getOperation() == OPERATION_NE_GENERAL) {
                sb.append(propName);
                sb.append(" <> ");
                appendValue(node, sb);
            } else if (node.getOperation() == OPERATION_NULL) {
                sb.append(propName);
                sb.append(" IS NULL");
            } else if (node.getOperation() == OPERATION_NOT_NULL) {
                sb.append(propName);
                sb.append(" IS NOT NULL");
            } else if (node.getOperation() == OPERATION_SIMILAR) {
                sb.append("SIMILAR(");
                sb.append(propName);
                sb.append(", ");
                appendValue(node, sb);
                sb.append(")");
            } else if (node.getOperation() == OPERATION_SPELLCHECK) {
                sb.append("SPELLCHECK(");
                appendValue(node, sb);
                sb.append(")");
            } else {
                exceptions.add(new InvalidQueryException("Invalid operation: " + node.getOperation()));
            }

            if (node.getOperation() == OPERATION_LIKE && node.getStringValue().indexOf('\\') > -1) {
                sb.append(" ESCAPE '\\'");
            }
        } catch (NamespaceException e) {
            exceptions.add(e);
        }
        return sb;
    }

    public Object visit(OrderQueryNode node, Object data) {
        StringBuffer sb = (StringBuffer) data;
        sb.append(" ORDER BY");
        OrderQueryNode.OrderSpec[] specs = node.getOrderSpecs();
        if (specs.length > 0) {
            try {
                String comma = "";
                for (int i = 0; i < specs.length; i++) {
                    sb.append(comma).append(" ");
                    Path propPath = specs[i].getPropertyPath();
                    if (propPath.getLength() > 1) {
                        exceptions.add(new InvalidQueryException("SQL does not support relative paths in order by clause"));
                        return sb;
                    }
                    appendName(propPath.getName(), resolver, sb);
                    if (!specs[i].isAscending()) {
                        sb.append(" DESC");
                    }
                    comma = ",";
                }
            } catch (NamespaceException e) {
                exceptions.add(e);
            }
        } else {
            sb.append(" SCORE");
        }
        return sb;
    }

    public Object visit(PropertyFunctionQueryNode node, Object data) {
        StringBuffer sb = (StringBuffer) data;
        String functionName = node.getFunctionName();
        if (functionName.equals(PropertyFunctionQueryNode.LOWER_CASE)) {
            sb.insert(0, "LOWER(").append(")");
        } else if (functionName.equals(PropertyFunctionQueryNode.UPPER_CASE)) {
            sb.insert(0, "UPPER(").append(")");
        } else {
            exceptions.add(new InvalidQueryException("Unsupported function: " + functionName));
        }
        return sb;
    }

    //------------------------< internal >--------------------------------------

    /**
     * Appends the <code>name</code> to the <code>StringBuffer</code>
     * <code>b</code> using the <code>NamespaceResolver</code>
     * <code>resolver</code>. The <code>name</code> is put in double quotes
     * if the local part of <code>name</code> contains a space character.
     *
     * @param name     the <code>Name</code> to print.
     * @param resolver to resolve <code>name</code>.
     * @param b        where to output the <code>name</code>.
     * @throws NamespaceException if <code>name</code> contains a uri
     *                                   that is not declared in <code>resolver</code>.
     */
    private static void appendName(Name name,
                                   NameResolver resolver,
                                   StringBuffer b)
            throws NamespaceException {
        boolean quote = name.getLocalName().indexOf(' ') > -1;
        if (quote) {
            b.append('"');
        }
        b.append(resolver.getJCRName(name));
        if (quote) {
            b.append('"');
        }
    }

    private void appendValue(RelationQueryNode node, StringBuffer b) {
        if (node.getValueType() == TYPE_LONG) {
            b.append(node.getLongValue());
        } else if (node.getValueType() == TYPE_DOUBLE) {
            b.append(node.getDoubleValue());
        } else if (node.getValueType() == TYPE_STRING) {
            b.append("'").append(node.getStringValue().replaceAll("'", "''")).append("'");
        } else if (node.getValueType() == TYPE_DATE || node.getValueType() == TYPE_TIMESTAMP) {
            b.append("TIMESTAMP '").append(ISO8601.format(node.getDateValue())).append("'");
        } else {
            exceptions.add(new InvalidQueryException("Invalid type: " + node.getValueType()));
        }

    }

    /**
     * Returns <code>true</code> if <code>path</code> contains exactly one
     * step with a descendant-or-self axis and an explicit name test; returns
     * <code>false</code> otherwise.
     *
     * @param path the path node.
     * @return <code>true</code> if <code>path</code> contains exactly one
     *         step with a descendant-or-self axis.
     */
    private static boolean containsDescendantOrSelf(PathQueryNode path) {
        LocationStepQueryNode[] steps = path.getPathSteps();
        int count = 0;
        for (int i = 0; i < steps.length; i++) {
            if (steps[i].getNameTest() != null && steps[i].getIncludeDescendants()) {
                count++;
            }
        }
        return count == 1;
    }

    /**
     * Returns <code>true</code> if <code>path</code> contains exactly one
     * location step which matches all node names. That is, matches any children
     * of a given node. That location step must be the last one in the sequence
     * of location steps.
     *
     * @param path the path node.
     * @return <code>true</code> if the last step matches any node name.
     */
    private static boolean containsAllChildrenMatch(PathQueryNode path) {
        LocationStepQueryNode[] steps = path.getPathSteps();
        int count = 0;
        for (int i = 0; i < steps.length; i++) {
            if (steps[i].getNameTest() == null && !steps[i].getIncludeDescendants()) {
                if (i == steps.length - 1 && count == 0) {
                    return true;
                }
                count++;
            }
        }
        return false;
    }
}
