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
package org.apache.jackrabbit.core.search.jcrql;

import org.apache.jackrabbit.core.search.*;
import org.apache.log4j.Logger;

import javax.jcr.query.InvalidQueryException;
import javax.jcr.util.ISO8601;
import java.util.Date;
import java.util.Arrays;

/**
 * Query builder that translates a JCRQL statement into query tree structure.
 */
public class JCRQLQueryBuilder implements JCRQLParserVisitor {

    private static final Logger log = Logger.getLogger(JCRQLQueryBuilder.class);

    /**
     * Match path exact
     */
    public static final int TYPE_EXACT = 1;

    /**
     * Match child nodes of path
     */
    public static final int TYPE_CHILDREN = 2;

    /**
     * Match descendant nodes or self of path
     */
    public static final int TYPE_DESCENDANT_SELF = 3;

    private final ASTStatement stmt;

    private QueryRootNode root;

    private AndQueryNode constraintNode = new AndQueryNode(null);

    private JCRQLQueryBuilder(ASTStatement stmt) {
        this.stmt = stmt;
    }

    public static QueryRootNode createQuery(String statement)
            throws InvalidQueryException {
        try {
            JCRQLQueryBuilder builder = new JCRQLQueryBuilder(JCRQLParser.parse(statement));
            return builder.getRootNode();
        } catch (ParseException e) {
            throw new InvalidQueryException(e.getMessage());
        }
    }

    QueryRootNode getRootNode() {
        if (root == null) {
            root = (QueryRootNode) stmt.jjtAccept(this, null);
        }
        return root;
    }

    //----------------< JCRQLParserVisitor >------------------------------------

    public Object visit(SimpleNode node, Object data) {
        // FIXME throw exception? unsupported
        return data;
    }

    public Object visit(ASTStatement node, Object data) {
        QueryRootNode root = new QueryRootNode();
        root.setLocationNode(new PathQueryNode(root));

        // pass to select, from, location, where, textsearch clause
        node.childrenAccept(this, root);

        // use //* if no path has been set
        PathQueryNode pathNode = root.getLocationNode();
        if (pathNode.getPathSteps().length == 0) {
            pathNode.addPathStep(new LocationStepQueryNode(pathNode, "", false));
            pathNode.addPathStep(new LocationStepQueryNode(pathNode, null, true));
        }

        // attach constraint to last path step
        LocationStepQueryNode[] steps = pathNode.getPathSteps();
        steps[steps.length - 1].addPredicate(constraintNode);

        return root;
    }

    public Object visit(ASTSelectClause node, Object data) {
        QueryRootNode root = (QueryRootNode) data;
        String[] properties = node.getProperties();
        for (int i = 0; i < properties.length; i++) {
            root.addSelectProperty(properties[i]);
        }
        return data;
    }

    public Object visit(ASTFromClause node, Object data) {
        node.childrenAccept(this, constraintNode);

        return data;
    }

    public Object visit(ASTNodeType node, Object data) {
        NAryQueryNode parent = (NAryQueryNode) data;
        parent.addOperand(new NodeTypeQueryNode(parent, node.getName()));
        return parent;
    }

    public Object visit(ASTLocationClause node, Object data) {
        QueryRootNode root = (QueryRootNode) data;
        PathQueryNode pathNode = root.getLocationNode();

        String path = node.getStringLocation();
        String[] names = path.split("/");
        for (int i = 0; i < names.length; i++) {
            if (names[i].length() == 0) {
                if (i == 0) {
                    // root
                    pathNode.addPathStep(new LocationStepQueryNode(pathNode, "", false));
                } else {
                    log.error("Internal error: descendant-or-self axis not allowed here");
                }
            } else {
                int idx = names[i].indexOf('[');
                String name = null;
                int index = 0;
                if (idx > -1) {
                    // contains index
                    name = names[i].substring(0, idx);
                    String suffix = names[i].substring(idx);
                    try {
                        index = Integer.parseInt(suffix.substring(1, suffix.length() - 1));
                    } catch (NumberFormatException e) {
                        log.warn("Unable to parse index for path element: " + names[i]);
                    }
                } else {
                    // no index
                    name = names[i];
                }
                if (name.equals("%") || name.equals("*")) {
                    name = null;
                }
                LocationStepQueryNode step = new LocationStepQueryNode(pathNode, name, false);
                if (index > 0) {
                    step.setIndex(index);
                }
                pathNode.addPathStep(step);
            }
        }

        if (node.getType() == TYPE_CHILDREN) {
            pathNode.addPathStep(new LocationStepQueryNode(pathNode, null, false));
        } else if (node.getType() == TYPE_DESCENDANT_SELF) {
            //LocationStepQueryNode[] steps = pathNode.getPathSteps();
            //steps[steps.length - 1].setIncludeDescendants(true);
            pathNode.addPathStep(new LocationStepQueryNode(pathNode, null, true));
        }

        return root;
    }

    public Object visit(ASTWhereClause node, Object data) {
        // just pass it to the expression
        return node.childrenAccept(this, constraintNode);
    }

    public Object visit(ASTOrExpr node, Object data) {
        NAryQueryNode parent = (NAryQueryNode) data;
        OrQueryNode orQuery = new OrQueryNode(parent);
        // pass to operands
        node.childrenAccept(this, orQuery);

        parent.addOperand(orQuery);
        return parent;
    }

    public Object visit(ASTAndExpr node, Object data) {
        NAryQueryNode parent = (NAryQueryNode) data;
        AndQueryNode andQuery = new AndQueryNode(parent);
        // pass to operands
        node.childrenAccept(this, andQuery);

        parent.addOperand(andQuery);
        return parent;
    }

    public Object visit(ASTNotExpr node, Object data) {
        NAryQueryNode parent = (NAryQueryNode) data;
        NotQueryNode notQuery = new NotQueryNode(parent);
        // pass to operand
        node.childrenAccept(this, notQuery);

        parent.addOperand(notQuery);
        return parent;
    }

    public Object visit(ASTRelExpr node, Object data) {
        NAryQueryNode parent = (NAryQueryNode) data;
        ASTValue value = (ASTValue) node.jjtGetChild(0);
        int type = value.getType();
        RelationQueryNode rel = null;

        if (type == Constants.TYPE_TIMESTAMP) {
            Date date = ISO8601.parse(value.getValue()).getTime();
            rel = new RelationQueryNode(parent,
                    node.getProperty(),
                    date,
                    node.getOperationType());
        } else if (type == Constants.TYPE_DOUBLE) {
            double d = Double.parseDouble(value.getValue());
            rel = new RelationQueryNode(parent,
                    node.getProperty(),
                    d,
                    node.getOperationType());
        } else if (type == Constants.TYPE_LONG) {
            long l = Long.parseLong(value.getValue());
            rel = new RelationQueryNode(parent,
                    node.getProperty(),
                    l,
                    node.getOperationType());
        } else if (type == Constants.TYPE_STRING) {
            rel = new RelationQueryNode(parent,
                    node.getProperty(),
                    value.getValue(),
                    node.getOperationType());
        }

        if (rel != null) {
            parent.addOperand(rel);
        }
        return parent;
    }

    public Object visit(ASTBracketExpr node, Object data) {
        // bracket expression only has influence on how the syntax tree
        // is created.
        // simply pass on to children
        return node.childrenAccept(this, data);
    }

    public Object visit(ASTValue node, Object data) {
        // FIXME throw Unsupported exception?
        return data;
    }

    public Object visit(ASTTextsearchClause node, Object data) {
        constraintNode.addOperand(new TextsearchQueryNode(constraintNode, node.getQuery()));
        return data;
    }

    public Object visit(ASTOrderClause node, Object data) {
        QueryRootNode root = (QueryRootNode) data;
        String[] props = node.getProperties();
        boolean[] orders = new boolean[props.length];
        Arrays.fill(orders, node.isAscending());
        root.setOrderNode(new OrderQueryNode(root, props, orders));
        return root;
    }
}
