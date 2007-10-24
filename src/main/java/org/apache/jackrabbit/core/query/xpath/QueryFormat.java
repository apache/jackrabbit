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
package org.apache.jackrabbit.core.query.xpath;

import org.apache.jackrabbit.core.query.AndQueryNode;
import org.apache.jackrabbit.core.query.DerefQueryNode;
import org.apache.jackrabbit.core.query.ExactQueryNode;
import org.apache.jackrabbit.core.query.LocationStepQueryNode;
import org.apache.jackrabbit.core.query.NodeTypeQueryNode;
import org.apache.jackrabbit.core.query.NotQueryNode;
import org.apache.jackrabbit.core.query.OrQueryNode;
import org.apache.jackrabbit.core.query.OrderQueryNode;
import org.apache.jackrabbit.core.query.PathQueryNode;
import org.apache.jackrabbit.core.query.QueryConstants;
import org.apache.jackrabbit.core.query.QueryNode;
import org.apache.jackrabbit.core.query.QueryNodeVisitor;
import org.apache.jackrabbit.core.query.QueryRootNode;
import org.apache.jackrabbit.core.query.RelationQueryNode;
import org.apache.jackrabbit.core.query.TextsearchQueryNode;
import org.apache.jackrabbit.core.query.PropertyFunctionQueryNode;
import org.apache.jackrabbit.core.query.DefaultQueryNodeVisitor;
import org.apache.jackrabbit.name.NamespaceResolver;
import org.apache.jackrabbit.name.NoPrefixDeclaredException;
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.name.NameFormat;
import org.apache.jackrabbit.name.Path;
import org.apache.jackrabbit.util.ISO8601;
import org.apache.jackrabbit.util.ISO9075;

import javax.jcr.query.InvalidQueryException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

/**
 * Implements the query node tree serialization into a String.
 */
class QueryFormat implements QueryNodeVisitor, QueryConstants {

    /**
     * Will be used to resolve QNames
     */
    private final NamespaceResolver resolver;

    /**
     * The String representation of the query node tree
     */
    private String statement;

    /**
     * List of exception objects created while creating the XPath string
     */
    private List exceptions = new ArrayList();

    private QueryFormat(QueryRootNode root, NamespaceResolver resolver)
            throws InvalidQueryException {
        this.resolver = resolver;
        statement = root.accept(this, new StringBuffer()).toString();
        if (exceptions.size() > 0) {
            Exception e = (Exception) exceptions.get(0);
            throw new InvalidQueryException(e.getMessage(), e);
        }
    }

    /**
     * Creates a XPath <code>String</code> representation of the QueryNode tree
     * argument <code>root</code>.
     *
     * @param root     the query node tree.
     * @param resolver to resolve QNames.
     * @return the XPath string representation of the QueryNode tree.
     * @throws InvalidQueryException the query node tree cannot be represented
     *                               as a XPath <code>String</code>.
     */
    public static String toString(QueryRootNode root, NamespaceResolver resolver)
            throws InvalidQueryException {
        return new QueryFormat(root, resolver).toString();
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

    public Object visit(QueryRootNode node, Object data) {
        StringBuffer sb = (StringBuffer) data;
        node.getLocationNode().accept(this, data);
        if (node.getOrderNode() != null) {
            node.getOrderNode().accept(this, data);
        }
        QName[] selectProps = node.getSelectProperties();
        if (selectProps.length > 0) {
            sb.append('/');
            boolean union = selectProps.length > 1;
            if (union) {
                sb.append('(');
            }
            String pipe = "";
            for (int i = 0; i < selectProps.length; i++) {
                try {
                    sb.append(pipe);
                    sb.append('@');
                    NameFormat.format(ISO9075.encode(selectProps[i]), resolver, sb);
                    pipe = "|";
                } catch (NoPrefixDeclaredException e) {
                    exceptions.add(e);
                }
            }
            if (union) {
                sb.append(')');
            }
        }
        return data;
    }

    public Object visit(OrQueryNode node, Object data) {
        StringBuffer sb = (StringBuffer) data;
        boolean bracket = false;
        if (node.getParent() instanceof AndQueryNode) {
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
            or = " or ";
        }
        if (bracket) {
            sb.append(")");
        }
        return sb;
    }

    public Object visit(AndQueryNode node, Object data) {
        StringBuffer sb = (StringBuffer) data;
        String and = "";
        QueryNode[] operands = node.getOperands();
        for (int i = 0; i < operands.length; i++) {
            sb.append(and);
            operands[i].accept(this, sb);
            and = " and ";
        }
        return sb;
    }

    public Object visit(NotQueryNode node, Object data) {
        StringBuffer sb = (StringBuffer) data;
        QueryNode[] operands = node.getOperands();
        if (operands.length > 0) {
            try {
                NameFormat.format(XPathQueryBuilder.FN_NOT_10, resolver, sb);
                sb.append("(");
                operands[0].accept(this, sb);
                sb.append(")");
            } catch (NoPrefixDeclaredException e) {
                exceptions.add(e);
            }
        }
        return sb;
    }

    public Object visit(ExactQueryNode node, Object data) {
        StringBuffer sb = (StringBuffer) data;
        sb.append("@");
        try {
            NameFormat.format(ISO9075.encode(node.getPropertyName()), resolver, sb);
            sb.append("='");
            NameFormat.format(node.getValue(), resolver, sb);
        } catch (NoPrefixDeclaredException e) {
            exceptions.add(e);
        }
        sb.append("'");
        return sb;
    }

    public Object visit(NodeTypeQueryNode node, Object data) {
        // handled in location step visit
        return data;
    }

    public Object visit(TextsearchQueryNode node, Object data) {
        StringBuffer sb = (StringBuffer) data;
        try {
            NameFormat.format(XPathQueryBuilder.JCR_CONTAINS, resolver, sb);
            sb.append("(");
            Path relPath = node.getRelativePath();
            if (relPath == null) {
                sb.append(".");
            } else {
                Path.PathElement[] elements = relPath.getElements();
                String slash = "";
                for (int i = 0; i < elements.length; i++) {
                    sb.append(slash);
                    slash = "/";
                    if (node.getReferencesProperty() && i == elements.length - 1) {
                        sb.append("@");
                    }
                    if (elements[i].getName().equals(RelationQueryNode.STAR_NAME_TEST)) {
                        sb.append("*");
                    } else {
                        NameFormat.format(ISO9075.encode(elements[i].getName()), resolver, sb);
                    }
                    if (elements[i].getIndex() != 0) {
                        sb.append("[").append(elements[i].getIndex()).append("]");
                    }
                }
            }
            sb.append(", '");
            sb.append(node.getQuery().replaceAll("'", "''"));
            sb.append("')");
        } catch (NoPrefixDeclaredException e) {
            exceptions.add(e);
        }
        return sb;
    }

    public Object visit(PathQueryNode node, Object data) {
        StringBuffer sb = (StringBuffer) data;
        if (node.isAbsolute()) {
            sb.append("/");
        }
        LocationStepQueryNode[] steps = node.getPathSteps();
        String slash = "";
        for (int i = 0; i < steps.length; i++) {
            sb.append(slash);
            steps[i].accept(this, sb);
            slash = "/";
        }
        return sb;
    }

    public Object visit(LocationStepQueryNode node, Object data) {
        StringBuffer sb = (StringBuffer) data;
        if (node.getIncludeDescendants()) {
            sb.append('/');
        }
        final QName[] nodeType = new QName[1];
        node.acceptOperands(new DefaultQueryNodeVisitor() {
            public Object visit(NodeTypeQueryNode node, Object data) {
                nodeType[0] = node.getValue();
                return data;
            }
        }, null);

        if (nodeType[0] != null) {
            sb.append("element(");
        }

        if (node.getNameTest() == null) {
            sb.append("*");
        } else {
            try {
                if (node.getNameTest().getLocalName().length() == 0) {
                    NameFormat.format(XPathQueryBuilder.JCR_ROOT, resolver, sb);
                } else {
                    NameFormat.format(ISO9075.encode(node.getNameTest()), resolver, sb);
                }
            } catch (NoPrefixDeclaredException e) {
                exceptions.add(e);
            }
        }

        if (nodeType[0] != null) {
            sb.append(", ");
            try {
                NameFormat.format(ISO9075.encode(nodeType[0]), resolver, sb);
            } catch (NoPrefixDeclaredException e) {
                exceptions.add(e);
            }
            sb.append(")");
        }

        if (node.getIndex() != LocationStepQueryNode.NONE) {
            sb.append('[').append(node.getIndex()).append(']');
        }
        QueryNode[] predicates = node.getPredicates();
        for (int i = 0; i < predicates.length; i++) {
            // ignore node type query nodes
            if (predicates[i].getType() == QueryNode.TYPE_NODETYPE) {
                continue;
            }
            sb.append('[');
            predicates[i].accept(this, sb);
            sb.append(']');
        }
        return sb;
    }

    public Object visit(DerefQueryNode node, Object data) {
        StringBuffer sb = (StringBuffer) data;
        try {
            NameFormat.format(XPathQueryBuilder.JCR_DEREF, resolver, sb);
            sb.append("(@");
            NameFormat.format(ISO9075.encode(node.getRefProperty()), resolver, sb);
            sb.append(", '");
            if (node.getNameTest() == null) {
                sb.append("*");
            } else {
                NameFormat.format(ISO9075.encode(node.getNameTest()), resolver, sb);
            }
            sb.append("')");
        } catch (NoPrefixDeclaredException e) {
            exceptions.add(e);
        }
        return sb;
    }

    public Object visit(RelationQueryNode node, Object data) {
        StringBuffer sb = (StringBuffer) data;
        try {

            StringBuffer propPath = new StringBuffer();
            // only encode if not position function
            Path relPath = node.getRelativePath();
            if (relPath == null) {
                propPath.append(".");
            } else if (relPath.getNameElement().getName().equals(XPathQueryBuilder.FN_POSITION_FULL)) {
                NameFormat.format(XPathQueryBuilder.FN_POSITION_FULL, resolver, propPath);
            } else {
                Path.PathElement[] elements = relPath.getElements();
                String slash = "";
                for (int i = 0; i < elements.length; i++) {
                    propPath.append(slash);
                    slash = "/";
                    if (i == elements.length - 1 && node.getOperation() != OPERATION_SIMILAR) {
                        propPath.append("@");
                    }
                    if (elements[i].getName().equals(RelationQueryNode.STAR_NAME_TEST)) {
                        propPath.append("*");
                    } else {
                        NameFormat.format(ISO9075.encode(elements[i].getName()), resolver, propPath);
                    }
                    if (elements[i].getIndex() != 0) {
                        propPath.append("[").append(elements[i].getIndex()).append("]");
                    }
                }
            }

            // surround name with property function
            node.acceptOperands(this, propPath);

            if (node.getOperation() == OPERATION_EQ_VALUE) {
                sb.append(propPath).append(" eq ");
                appendValue(node, sb);
            } else if (node.getOperation() == OPERATION_EQ_GENERAL) {
                sb.append(propPath).append(" = ");
                appendValue(node, sb);
            } else if (node.getOperation() == OPERATION_GE_GENERAL) {
                sb.append(propPath).append(" >= ");
                appendValue(node, sb);
            } else if (node.getOperation() == OPERATION_GE_VALUE) {
                sb.append(propPath).append(" ge ");
                appendValue(node, sb);
            } else if (node.getOperation() == OPERATION_GT_GENERAL) {
                sb.append(propPath).append(" > ");
                appendValue(node, sb);
            } else if (node.getOperation() == OPERATION_GT_VALUE) {
                sb.append(propPath).append(" gt ");
                appendValue(node, sb);
            } else if (node.getOperation() == OPERATION_LE_GENERAL) {
                sb.append(propPath).append(" <= ");
                appendValue(node, sb);
            } else if (node.getOperation() == OPERATION_LE_VALUE) {
                sb.append(propPath).append(" le ");
                appendValue(node, sb);
            } else if (node.getOperation() == OPERATION_LIKE) {
                NameFormat.format(XPathQueryBuilder.JCR_LIKE, resolver, sb);
                sb.append("(").append(propPath).append(", ");
                appendValue(node, sb);
                sb.append(")");
            } else if (node.getOperation() == OPERATION_LT_GENERAL) {
                sb.append(propPath).append(" < ");
                appendValue(node, sb);
            } else if (node.getOperation() == OPERATION_LT_VALUE) {
                sb.append(propPath).append(" lt ");
                appendValue(node, sb);
            } else if (node.getOperation() == OPERATION_NE_GENERAL) {
                sb.append(propPath).append(" != ");
                appendValue(node, sb);
            } else if (node.getOperation() == OPERATION_NE_VALUE) {
                sb.append(propPath).append(" ne ");
                appendValue(node, sb);
            } else if (node.getOperation() == OPERATION_NULL) {
                NameFormat.format(XPathQueryBuilder.FN_NOT, resolver, sb);
                sb.append("(").append(propPath).append(")");
            } else if (node.getOperation() == OPERATION_NOT_NULL) {
                sb.append(propPath);
            } else if (node.getOperation() == OPERATION_SIMILAR) {
                NameFormat.format(XPathQueryBuilder.REP_SIMILAR, resolver, sb);
                sb.append("(").append(propPath).append(", ");
                appendValue(node, sb);
            } else if (node.getOperation() == OPERATION_SPELLCHECK) {
                NameFormat.format(XPathQueryBuilder.REP_SPELLCHECK, resolver, sb);
                sb.append("(");
                appendValue(node, sb);
                sb.append(")");
            } else {
                exceptions.add(new InvalidQueryException("Invalid operation: " + node.getOperation()));
            }
        } catch (NoPrefixDeclaredException e) {
            exceptions.add(e);
        }
        return sb;
    }

    public Object visit(OrderQueryNode node, Object data) {
        StringBuffer sb = (StringBuffer) data;
        sb.append(" order by");
        OrderQueryNode.OrderSpec[] specs = node.getOrderSpecs();
        String comma = "";
        try {
            for (int i = 0; i < specs.length; i++) {
                sb.append(comma);
                QName prop = ISO9075.encode(specs[i].getProperty());
                sb.append(" @");
                NameFormat.format(prop, resolver, sb);
                if (!specs[i].isAscending()) {
                    sb.append(" descending");
                }
                comma = ",";
            }
        } catch (NoPrefixDeclaredException e) {
            exceptions.add(e);
        }
        return data;
    }

    public Object visit(PropertyFunctionQueryNode node, Object data) {
        StringBuffer sb = (StringBuffer) data;
        String functionName = node.getFunctionName();
        try {
            if (functionName.equals(PropertyFunctionQueryNode.LOWER_CASE)) {
                sb.insert(0, NameFormat.format(XPathQueryBuilder.FN_LOWER_CASE, resolver) + "(");
                sb.append(")");
            } else if (functionName.equals(PropertyFunctionQueryNode.UPPER_CASE)) {
                sb.insert(0, NameFormat.format(XPathQueryBuilder.FN_UPPER_CASE, resolver) + "(");
                sb.append(")");
            } else {
                exceptions.add(new InvalidQueryException("Unsupported function: " + functionName));
            }
        } catch (NoPrefixDeclaredException e) {
            exceptions.add(e);
        }
        return sb;
    }

    //----------------------------< internal >----------------------------------

    /**
     * Appends the value of a relation node to the <code>StringBuffer</code>
     * <code>sb</code>.
     *
     * @param node the relation node.
     * @param b    where to append the value.
     * @throws NoPrefixDeclaredException if a prefix declaration is missing for
     *                                   a namespace URI.
     */
    private void appendValue(RelationQueryNode node, StringBuffer b)
            throws NoPrefixDeclaredException {
        if (node.getValueType() == TYPE_LONG) {
            b.append(node.getLongValue());
        } else if (node.getValueType() == TYPE_DOUBLE) {
            b.append(node.getDoubleValue());
        } else if (node.getValueType() == TYPE_STRING) {
            b.append("'").append(node.getStringValue().replaceAll("'", "''")).append("'");
        } else if (node.getValueType() == TYPE_DATE || node.getValueType() == TYPE_TIMESTAMP) {
            Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            cal.setTime(node.getDateValue());
            NameFormat.format(XPathQueryBuilder.XS_DATETIME, resolver, b);
            b.append("('").append(ISO8601.format(cal)).append("')");
        } else if (node.getValueType() == TYPE_POSITION) {
            if (node.getPositionValue() == LocationStepQueryNode.LAST) {
                b.append("last()");
            } else {
                b.append(node.getPositionValue());
            }
        } else {
            exceptions.add(new InvalidQueryException("Invalid type: " + node.getValueType()));
        }
    }
}
