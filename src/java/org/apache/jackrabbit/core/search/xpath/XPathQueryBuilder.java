/*
 * Copyright 2004 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.core.search.xpath;

import org.apache.jackrabbit.core.search.QueryRootNode;
import org.apache.jackrabbit.core.search.PathQueryNode;
import org.apache.jackrabbit.core.search.LocationStepQueryNode;
import org.apache.jackrabbit.core.search.QueryNode;
import org.apache.jackrabbit.core.search.AndQueryNode;
import org.apache.jackrabbit.core.search.NAryQueryNode;
import org.apache.jackrabbit.core.search.OrQueryNode;
import org.apache.jackrabbit.core.search.RelationQueryNode;
import org.apache.jackrabbit.core.search.NotQueryNode;
import org.apache.jackrabbit.core.NamespaceResolver;
import org.apache.jackrabbit.core.QName;
import org.apache.jackrabbit.core.SearchManager;
import org.apache.jackrabbit.core.NoPrefixDeclaredException;

import javax.jcr.query.InvalidQueryException;
import javax.jcr.util.ISO8601;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Calendar;

/**
 * Query builder that translates a XPath statement into a query tree structure.
 */
public class XPathQueryBuilder implements XPathVisitor {

    /** QName for 'fn:not' */
    private static final QName FN_NOT = new QName(SearchManager.NS_FN_URI, "not");

    /** QName for 'not' as defined in XPath 1.0 (no prefix) */
    private static final QName FN_NOT_10 = new QName("", "not");

    /** QName for xs:dateTime */
    private static final QName XS_DATETIME = new QName(SearchManager.NS_XS_URI, "dateTime");

    /** String constant for operator 'eq' */
    private static final String OP_EQ = "eq";

    /** String constant for operator 'ne' */
    private static final String OP_NE = "ne";

    /** String constant for operator 'gt' */
    private static final String OP_GT = "gt";

    /** String constant for operator 'ge' */
    private static final String OP_GE = "ge";

    /** String constant for operator 'lt' */
    private static final String OP_LT = "lt";

    /** String constant for operator 'le' */
    private static final String OP_LE = "le";

    /** String constant for operator '=' */
    private static final String OP_SIGN_EQ = "=";

    /** String constant for operator '!=' */
    private static final String OP_SIGN_NE = "!=";

    /** String constant for operator '>' */
    private static final String OP_SIGN_GT = ">";

    /** String constant for operator '>=' */
    private static final String OP_SIGN_GE = ">=";

    /** String constant for operator '<' */
    private static final String OP_SIGN_LT = "<";

    /** String constant for operator '<=' */
    private static final String OP_SIGN_LE = "<=";

    /** The root <code>QueryNode</code> */
    private final QueryRootNode root = new QueryRootNode();

    /** The {@link org.apache.jackrabbit.core.NamespaceResolver} in use */
    private final NamespaceResolver resolver;

    /** List of exceptions that are created while building the query tree */
    private final List exceptions = new ArrayList();

    /**
     * Creates a new <code>XPathQueryBuilder</code> instance.
     * @param statement the XPath statement.
     * @param resolver the namespace resolver to use.
     * @throws InvalidQueryException if the XPath statement is malformed.
     */
    private XPathQueryBuilder(String statement, NamespaceResolver resolver)
            throws InvalidQueryException {
        this.resolver = resolver;
        try {
            XPath query = new XPath(new StringReader(statement));
            query.XPath2().jjtAccept(this, root);
        } catch (ParseException e) {
            throw new InvalidQueryException(e.getMessage(), e);
        } catch (Exception e) {
            // also catch any other exception
            throw new InvalidQueryException(e.getMessage(), e);
        }
        if (exceptions.size() > 0) {
            // simply report the first one
            Exception e = (Exception) exceptions.get(0);
            if (e instanceof InvalidQueryException) {
                // just re-throw
                throw (InvalidQueryException) e;
            } else {
                // otherwise package
                throw new InvalidQueryException(e.getMessage(), e);
            }
        }
    }

    /**
     * Creates a <code>QueryNode</code> tree from a XPath statement.
     * @param statement the XPath statement.
     * @param resolver the namespace resolver to use.
     * @return the <code>QueryNode</code> tree for the XPath statement.
     * @throws InvalidQueryException if the XPath statement is malformed.
     */
    public static QueryRootNode createQuery(String statement,
                                            NamespaceResolver resolver)
            throws InvalidQueryException {
        QueryRootNode root = new XPathQueryBuilder(statement, resolver).getRootNode();
        LocationStepQueryNode[] steps = root.getLocationNode().getPathSteps();
        if (steps.length > 0) {
            String nameTest = steps[0].getNameTest();
            if (nameTest == null || nameTest.length() > 0) {
                throw new InvalidQueryException("Path must be absolute: " + statement);
            }
        }
        return root;
    }

    /**
     * Returns the root node of the <code>QueryNode</code> tree.
     * @return the root node of the <code>QueryNode</code> tree.
     */
    QueryRootNode getRootNode() {
        return root;
    }

    //---------------------< XPathVisitor >-------------------------------------

    /**
     * Implements the generic visit method for this <code>XPathVisitor</code>.
     * @param node the current node as created by the XPath parser.
     * @param data the current <code>QueryNode</code> created by this
     *  <code>XPathVisitor</code>.
     * @return the current <code>QueryNode</code>. Can be different from
     *  <code>data</code>.
     */
    public Object visit(SimpleNode node, Object data) {
        switch (node.getId()) {
            case XPathTreeConstants.JJTXPATH:
                data = createPathQueryNode(node);
                break;
            case XPathTreeConstants.JJTROOT:
                ((PathQueryNode) data).addPathStep(new LocationStepQueryNode((QueryNode) data, "", false));
                break;
            case XPathTreeConstants.JJTROOTDESCENDANTS:
                ((PathQueryNode) data).addPathStep(new LocationStepQueryNode((QueryNode) data, "", false));
                break;
            case XPathTreeConstants.JJTSTEPEXPR:
                if (isAttributeAxis(node)) {
                    // @todo do what?
                    // traverse
                    node.childrenAccept(this, data);
                } else {
                    if (data instanceof PathQueryNode) {
                        data = createLocationStep(node, (PathQueryNode) data);
                    } else {
                        exceptions.add(new InvalidQueryException("Only attribute axis is allowed in predicate"));
                    }
                }
                break;
            case XPathTreeConstants.JJTNAMETEST:
                if (data instanceof LocationStepQueryNode
                        || data instanceof RelationQueryNode) {
                    createNameTest(node, (QueryNode) data);
                } else {
                    // traverse
                    node.childrenAccept(this, data);
                }
                break;
            case XPathTreeConstants.JJTOREXPR:
                NAryQueryNode parent = (NAryQueryNode) data;
                data = new OrQueryNode(parent);
                parent.addOperand((QueryNode) data);
                // traverse
                node.childrenAccept(this, data);
                break;
            case XPathTreeConstants.JJTANDEXPR:
                parent = (NAryQueryNode) data;
                data = new AndQueryNode(parent);
                parent.addOperand((QueryNode) data);
                // traverse
                node.childrenAccept(this, data);
                break;
            case XPathTreeConstants.JJTCOMPARISONEXPR:
                createExpression(node, (NAryQueryNode) data);
                break;
            case XPathTreeConstants.JJTSTRINGLITERAL:
            case XPathTreeConstants.JJTDECIMALLITERAL:
            case XPathTreeConstants.JJTDOUBLELITERAL:
            case XPathTreeConstants.JJTINTEGERLITERAL:
                if (data instanceof RelationQueryNode) {
                    assignValue(node, (RelationQueryNode) data);
                } else {
                    exceptions.add(new InvalidQueryException("Internal error: data is not a RelationQueryNode"));
                }
                break;
            case XPathTreeConstants.JJTFUNCTIONCALL:
                data = createFunction(node, (QueryNode) data);
                break;
            default:
                // per default traverse
                node.childrenAccept(this, data);
        }
        return data;
    }

    //----------------------< internal >----------------------------------------

    /**
     * Creates a <code>LocationStepQueryNode</code> at the current position
     * in parent.
     * @param node the current node in the xpath syntax tree.
     * @param parent the parent <code>PathQueryNode</code>.
     * @return the created <code>LocationStepQueryNode</code>.
     */
    private LocationStepQueryNode createLocationStep(SimpleNode node, PathQueryNode parent) {
        LocationStepQueryNode queryNode = null;
        boolean descenant = false;
        Node p = node.jjtGetParent();
        for (int i = 0; i < p.jjtGetNumChildren(); i++) {
            SimpleNode c = (SimpleNode) p.jjtGetChild(i);
            if (c == node) {
                queryNode = new LocationStepQueryNode(parent, null, descenant);
                parent.addPathStep(queryNode);
                break;
            }
            descenant = (c.getId() == XPathTreeConstants.JJTSLASHSLASH
                    || c.getId() == XPathTreeConstants.JJTROOTDESCENDANTS);
        }

        node.childrenAccept(this, queryNode);

        return queryNode;
    }

    /**
     * Creates a name test either for a <code>LocationStepQueryNode</code> or
     * for a <code>RelationQueryNode</code>.
     * @param node the current node in the xpath syntax tree.
     * @param queryNode either a <code>LocationStepQueryNode</code> or a
     *   <code>RelationQueryNode</code>.
     */
    private void createNameTest(SimpleNode node, QueryNode queryNode) {
        if (node.jjtGetNumChildren() > 0) {
            SimpleNode child = (SimpleNode) node.jjtGetChild(0);
            if (child.getId() == XPathTreeConstants.JJTQNAME) {
                if (queryNode instanceof LocationStepQueryNode) {
                    ((LocationStepQueryNode) queryNode).setNameTest(child.getValue());
                } else if (queryNode instanceof RelationQueryNode) {
                    ((RelationQueryNode) queryNode).setProperty(child.getValue());
                }
            } else if (child.getId() == XPathTreeConstants.JJTSTAR) {
                if (queryNode instanceof LocationStepQueryNode) {
                    ((LocationStepQueryNode) queryNode).setNameTest(null);
                }
            } else {
                exceptions.add(new InvalidQueryException("Unsupported location for name test: " + child));
            }
        }
    }

    /**
     * Creates a new {@link org.apache.jackrabbit.core.search.RelationQueryNode}
     * with <code>queryNode</code> as its parent node.
     * @param node a comparison expression node.
     * @param queryNode the current <code>QueryNode</code>.
     */
    private void createExpression(SimpleNode node, NAryQueryNode queryNode) {
        if (node.getId() != XPathTreeConstants.JJTCOMPARISONEXPR) {
            throw new IllegalArgumentException("node must be of type ComparisonExpr");
        }
        // get operation type
        String opType = node.getValue();
        int type = 0;
        if (opType.equals(OP_EQ) || opType.equals(OP_SIGN_EQ)) {
            type = RelationQueryNode.OPERATION_EQ;
        } else if (opType.equals(OP_GE) || opType.equals(OP_SIGN_GE)) {
            type = RelationQueryNode.OPERATION_GE;
        } else if (opType.equals(OP_GT) || opType.equals(OP_SIGN_GT)) {
            type = RelationQueryNode.OPERATION_GT;
        } else if (opType.equals(OP_LE) || opType.equals(OP_SIGN_LE)) {
            type = RelationQueryNode.OPERATION_LE;
        } else if (opType.equals(OP_LT) || opType.equals(OP_SIGN_LT)) {
            type = RelationQueryNode.OPERATION_LT;
        } else if (opType.equals(OP_NE) || opType.equals(OP_SIGN_NE)) {
            type = RelationQueryNode.OPERATION_NE;
        } else {
            exceptions.add(new InvalidQueryException("Unsupported ComparisonExpr type:" + node.getValue()));
        }

        RelationQueryNode rqn = new RelationQueryNode(queryNode, type);

        // traverse
        node.childrenAccept(this, rqn);
        queryNode.addOperand(rqn);
    }

    /**
     * Creates the primary path query node.
     * @param node xpath node representing the root of the parsed tree.
     * @return
     */
    private PathQueryNode createPathQueryNode(SimpleNode node) {
        root.setLocationNode(new PathQueryNode(root));
        node.childrenAccept(this, root.getLocationNode());
        return root.getLocationNode();
    }

    /**
     * Assigns a value to the <code>queryNode</code>.
     * @param node must be of type string, decimal, double or integer; otherwise
     *   an InvalidQueryException is added to {@link #exceptions}.
     * @param queryNode current node in the query tree.
     */
    private void assignValue(SimpleNode node, RelationQueryNode queryNode) {
        if (node.getId() == XPathTreeConstants.JJTSTRINGLITERAL) {
            String value = node.getValue().substring(1, node.getValue().length() - 1);
            if (node.getValue().charAt(0) == '"') {
                value = value.replaceAll("\"\"", "\"");
            } else {
                value = value.replaceAll("''", "'");
            }
            queryNode.setStringValue(value);
        } else if (node.getId() == XPathTreeConstants.JJTDECIMALLITERAL) {
            queryNode.setDoubleValue(Double.parseDouble(node.getValue()));
        } else if (node.getId() == XPathTreeConstants.JJTDOUBLELITERAL) {
            queryNode.setDoubleValue(Double.parseDouble(node.getValue()));
        } else if (node.getId() == XPathTreeConstants.JJTINTEGERLITERAL) {
            queryNode.setLongValue(Long.parseLong(node.getValue()));
        } else {
            exceptions.add(new InvalidQueryException("Unsupported literal type:" + node.toString()));
        }
    }

    /**
     * Creates a function based on <code>node</code>.
     * @param node the function node from the xpath tree.
     * @param queryNode the current query node.
     * @return
     */
    private QueryNode createFunction(SimpleNode node, QueryNode queryNode) {
        // find out function name
        String fName = ((SimpleNode) node.jjtGetChild(0)).getValue();
        fName = fName.substring(0, fName.length() - 1);
        try {
            if (FN_NOT.toJCRName(resolver).equals(fName)
                    || FN_NOT_10.toJCRName(resolver).equals(fName)) {
                if (queryNode instanceof NAryQueryNode) {
                    QueryNode not = new NotQueryNode(queryNode);
                    ((NAryQueryNode) queryNode).addOperand(not);
                    queryNode = not;
                    // traverse
                    if (node.jjtGetNumChildren() == 2) {
                        node.jjtGetChild(1).jjtAccept(this, queryNode);
                    } else {
                        exceptions.add(new InvalidQueryException("fn:not only supports one expression argument"));
                    }
                } else {
                    exceptions.add(new InvalidQueryException("Unsupported location for function fn:not"));
                }
            } else if (XS_DATETIME.toJCRName(resolver).equals(fName)) {
                // check arguments
                if (node.jjtGetNumChildren() == 2) {
                    if (queryNode instanceof RelationQueryNode) {
                        RelationQueryNode rel = (RelationQueryNode) queryNode;
                        SimpleNode literal = (SimpleNode) node.jjtGetChild(1).jjtGetChild(0);
                        if (literal.getId() == XPathTreeConstants.JJTSTRINGLITERAL) {
                            String value = literal.getValue();
                            // strip quotes
                            value = value.substring(1, value.length() - 1);
                            Calendar c = ISO8601.parse(value);
                            if (c == null) {
                                exceptions.add(new InvalidQueryException("Unable to parse string literal for xs:dateTime: " + value));
                            } else {
                                rel.setDateValue(c.getTime());
                            }
                        } else {
                            exceptions.add(new InvalidQueryException("Wrong argument type for xs:dateTime"));
                        }
                    } else {
                        exceptions.add(new InvalidQueryException("Unsupported location for function xs:dateTime"));
                    }
                } else {
                    // wrong number of arguments
                    exceptions.add(new InvalidQueryException("Wrong number of arguments for xs:dateTime"));
                }
            } else {
                exceptions.add(new InvalidQueryException("Unsupported function: " + fName));
            }
        } catch (NoPrefixDeclaredException e) {
            exceptions.add(e);
        }
        return queryNode;
    }

    /**
     * Returns true if <code>node</code> has a child node which is the attribute
     * axis.
     * @param node a node with type {@link org.apache.jackrabbit.core.search.xpath.XPathTreeConstants.JJTSTEPEXPR}.
     * @return <code>true</code> if this step expression uses the attribute axis.
     */
    private boolean isAttributeAxis(SimpleNode node) {
        for (int i = 0; i < node.jjtGetNumChildren(); i++) {
            if (((SimpleNode) node.jjtGetChild(i)).getId() == XPathTreeConstants.JJTAT) {
                return true;
            }
        }
        return false;
    }

}
