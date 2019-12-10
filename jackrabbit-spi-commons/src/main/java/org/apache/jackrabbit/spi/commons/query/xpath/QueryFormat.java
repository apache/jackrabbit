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
package org.apache.jackrabbit.spi.commons.query.xpath;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.NamespaceException;
import javax.jcr.RepositoryException;
import javax.jcr.query.InvalidQueryException;

import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.commons.conversion.NameResolver;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.apache.jackrabbit.spi.commons.query.AndQueryNode;
import org.apache.jackrabbit.spi.commons.query.DefaultQueryNodeVisitor;
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
import org.apache.jackrabbit.util.ISO9075;

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
     * List of exception objects created while creating the XPath string
     */
    private final List exceptions = new ArrayList();

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
     * Creates a XPath <code>String</code> representation of the QueryNode tree
     * argument <code>root</code>.
     *
     * @param root     the query node tree.
     * @param resolver to resolve QNames.
     * @return the XPath string representation of the QueryNode tree.
     * @throws InvalidQueryException the query node tree cannot be represented
     *                               as a XPath <code>String</code>.
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
        node.getLocationNode().accept(this, data);
        Name[] selectProps = node.getSelectProperties();
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
                    sb.append(resolver.getJCRName(encode(selectProps[i])));
                    pipe = "|";
                } catch (NamespaceException e) {
                    exceptions.add(e);
                }
            }
            if (union) {
                sb.append(')');
            }
        }
        if (node.getOrderNode() != null) {
            node.getOrderNode().accept(this, data);
        }
        return data;
    }

    public Object visit(OrQueryNode node, Object data) throws RepositoryException {
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

    public Object visit(AndQueryNode node, Object data) throws RepositoryException {
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

    public Object visit(NotQueryNode node, Object data) throws RepositoryException {
        StringBuffer sb = (StringBuffer) data;
        QueryNode[] operands = node.getOperands();
        if (operands.length > 0) {
            try {
                sb.append(resolver.getJCRName(XPathQueryBuilder.FN_NOT_10));
                sb.append("(");
                operands[0].accept(this, sb);
                sb.append(")");
            } catch (NamespaceException e) {
                exceptions.add(e);
            }
        }
        return sb;
    }

    public Object visit(ExactQueryNode node, Object data) {
        StringBuffer sb = (StringBuffer) data;
        sb.append("@");
        try {
            Name name = encode(node.getPropertyName());
            sb.append(resolver.getJCRName(name));
            sb.append("='");
            sb.append(resolver.getJCRName(node.getValue()));
        } catch (NamespaceException e) {
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
            sb.append(resolver.getJCRName(XPathQueryBuilder.JCR_CONTAINS));
            sb.append("(");
            Path relPath = node.getRelativePath();
            if (relPath == null) {
                sb.append(".");
            } else {
                Path.Element[] elements = relPath.getElements();
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
                        Name n = encode(elements[i].getName());
                        sb.append(resolver.getJCRName(n));
                    }
                    if (elements[i].getIndex() != 0) {
                        sb.append("[").append(elements[i].getIndex()).append("]");
                    }
                }
            }
            sb.append(", '");
            sb.append(node.getQuery().replaceAll("'", "''"));
            sb.append("')");
        } catch (NamespaceException e) {
            exceptions.add(e);
        }
        return sb;
    }

    public Object visit(PathQueryNode node, Object data) throws RepositoryException {
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

    public Object visit(LocationStepQueryNode node, Object data) throws RepositoryException {
        StringBuffer sb = (StringBuffer) data;
        if (node.getIncludeDescendants()) {
            sb.append('/');
        }
        final Name[] nodeType = new Name[1];
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
                    sb.append(resolver.getJCRName(XPathQueryBuilder.JCR_ROOT));
                } else {
                    sb.append(resolver.getJCRName(encode(node.getNameTest())));
                }
            } catch (NamespaceException e) {
                exceptions.add(e);
            }
        }

        if (nodeType[0] != null) {
            sb.append(", ");
            try {
                sb.append(resolver.getJCRName(encode(nodeType[0])));
            } catch (NamespaceException e) {
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
            sb.append(resolver.getJCRName(XPathQueryBuilder.JCR_DEREF));
            sb.append("(@");
            sb.append(resolver.getJCRName(encode(node.getRefProperty())));
            sb.append(", '");
            if (node.getNameTest() == null) {
                sb.append("*");
            } else {
                sb.append(resolver.getJCRName(encode(node.getNameTest())));
            }
            sb.append("')");
        } catch (NamespaceException e) {
            exceptions.add(e);
        }
        return sb;
    }

    public Object visit(RelationQueryNode node, Object data) throws RepositoryException {
        StringBuffer sb = (StringBuffer) data;
        try {

            StringBuffer propPath = new StringBuffer();
            // only encode if not position function
            PathQueryNode relPath = node.getRelativePath();
            if (relPath == null) {
                propPath.append(".");
            } else if (relPath.getNumOperands() > 0 && XPathQueryBuilder.FN_POSITION_FULL.equals(relPath.getPathSteps()[0].getNameTest())) {
                propPath.append(resolver.getJCRName(XPathQueryBuilder.FN_POSITION_FULL));
            } else {
                LocationStepQueryNode[] steps = relPath.getPathSteps();
                String slash = "";
                for (int i = 0; i < steps.length; i++) {
                    propPath.append(slash);
                    slash = "/";
                    if (i == steps.length - 1 && node.getOperation() != OPERATION_SIMILAR) {
                        // last step
                        propPath.append("@");
                    }
                    visit(steps[i], propPath);
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
                sb.append(resolver.getJCRName(XPathQueryBuilder.JCR_LIKE));
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
                sb.append(resolver.getJCRName(XPathQueryBuilder.FN_NOT));
                sb.append("(").append(propPath).append(")");
            } else if (node.getOperation() == OPERATION_NOT_NULL) {
                sb.append(propPath);
            } else if (node.getOperation() == OPERATION_SIMILAR) {
                sb.append(resolver.getJCRName(XPathQueryBuilder.REP_SIMILAR));
                sb.append("(").append(propPath).append(", ");
                appendValue(node, sb);
                sb.append(")");
            } else if (node.getOperation() == OPERATION_SPELLCHECK) {
                sb.append(resolver.getJCRName(XPathQueryBuilder.REP_SPELLCHECK));
                sb.append("(");
                appendValue(node, sb);
                sb.append(")");
            } else {
                exceptions.add(new InvalidQueryException("Invalid operation: " + node.getOperation()));
            }
        } catch (NamespaceException e) {
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
                Path propPath = specs[i].getPropertyPath();
                Path.Element[] elements = propPath.getElements();
                sb.append(" ");
                String slash = "";
                for (int j = 0; j < elements.length; j++) {
                    sb.append(slash);
                    slash = "/";
                    Path.Element element = elements[j];
                    Name name = encode(element.getName());
                    if (j == elements.length - 1) {
                        // last
                        sb.append("@");
                    }
                    sb.append(resolver.getJCRName(name));
                }
                if (!specs[i].isAscending()) {
                    sb.append(" descending");
                }
                comma = ",";
            }
        } catch (NamespaceException e) {
            exceptions.add(e);
        }
        return data;
    }

    public Object visit(PropertyFunctionQueryNode node, Object data) {
        StringBuffer sb = (StringBuffer) data;
        String functionName = node.getFunctionName();
        try {
            if (functionName.equals(PropertyFunctionQueryNode.LOWER_CASE)) {
                sb.insert(0, resolver.getJCRName(XPathQueryBuilder.FN_LOWER_CASE) + "(");
                sb.append(")");
            } else if (functionName.equals(PropertyFunctionQueryNode.UPPER_CASE)) {
                sb.insert(0, resolver.getJCRName(XPathQueryBuilder.FN_UPPER_CASE) + "(");
                sb.append(")");
            } else {
                exceptions.add(new InvalidQueryException("Unsupported function: " + functionName));
            }
        } catch (NamespaceException e) {
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
     * @throws NamespaceException if a prefix declaration is missing for
     *                                   a namespace URI.
     */
    private void appendValue(RelationQueryNode node, StringBuffer b)
            throws NamespaceException {
        if (node.getValueType() == TYPE_LONG) {
            b.append(node.getLongValue());
        } else if (node.getValueType() == TYPE_DOUBLE) {
            b.append(node.getDoubleValue());
        } else if (node.getValueType() == TYPE_STRING) {
            b.append("'").append(node.getStringValue().replaceAll("'", "''")).append("'");
        } else if (node.getValueType() == TYPE_DATE || node.getValueType() == TYPE_TIMESTAMP) {
            b.append(resolver.getJCRName(XPathQueryBuilder.XS_DATETIME));
            b.append("('").append(ISO8601.format(node.getDateValue())).append("')");
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

    private static Name encode(Name name) {
        String encoded = ISO9075.encode(name.getLocalName());
        if (encoded.equals(name.getLocalName())) {
            return name;
        } else {
            return NameFactoryImpl.getInstance().create(name.getNamespaceURI(), encoded);
        }
    }
}
