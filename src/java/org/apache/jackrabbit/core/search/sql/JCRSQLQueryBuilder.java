/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
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
package org.apache.jackrabbit.core.search.sql;

import org.apache.jackrabbit.core.search.QueryRootNode;
import org.apache.jackrabbit.core.search.AndQueryNode;
import org.apache.jackrabbit.core.search.NodeTypeQueryNode;
import org.apache.jackrabbit.core.search.NAryQueryNode;
import org.apache.jackrabbit.core.search.OrQueryNode;
import org.apache.jackrabbit.core.search.NotQueryNode;
import org.apache.jackrabbit.core.search.OrderQueryNode;
import org.apache.jackrabbit.core.search.RelationQueryNode;
import org.apache.jackrabbit.core.search.Constants;
import org.apache.jackrabbit.core.search.QueryNode;
import org.apache.jackrabbit.core.search.TextsearchQueryNode;
import org.apache.jackrabbit.core.search.PathQueryNode;
import org.apache.jackrabbit.core.search.LocationStepQueryNode;
import org.apache.jackrabbit.core.NamespaceResolver;
import org.apache.jackrabbit.core.QName;
import org.apache.jackrabbit.core.NamespaceRegistryImpl;
import org.apache.jackrabbit.core.IllegalNameException;
import org.apache.jackrabbit.core.UnknownPrefixException;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.log4j.Logger;

import javax.jcr.query.InvalidQueryException;
import javax.jcr.util.ISO8601;
import java.util.List;
import java.util.ArrayList;
import java.util.Date;
import java.util.Calendar;
import java.text.SimpleDateFormat;

/**
 * Implements the query builder for the JCR SQL syntax.
 */
public class JCRSQLQueryBuilder implements JCRSQLParserVisitor {

    /** logger instance for this class */
    private static final Logger log = Logger.getLogger(JCRSQLQueryBuilder.class);

    /**
     * DateFormat pattern for type
     * {@link org.apache.jackrabbit.core.search.Constants.TYPE_DATE}.
     */
    private static final String DATE_PATTERN = "yyyy-MM-dd";

    /**
     * QName for jcr:path
     */
    static final QName JCR_PATH = new QName(NamespaceRegistryImpl.NS_JCR_URI, "path");

    /** The root node of the sql query syntax tree */
    private final ASTQuery stmt;

    /** The root query node */
    private QueryRootNode root;

    /** To resolve QNames */
    private NamespaceResolver resolver;

    /** Query node to gather the constraints defined in the WHERE clause */
    private final AndQueryNode constraintNode = new AndQueryNode(null);

    public JCRSQLQueryBuilder() {
        stmt = null;
    }

    /**
     * Creates a new <code>JCRSQLQueryBuilder</code>.
     * @param statement the root node of the SQL syntax tree.
     * @param resolver a namespace resolver to use for names in the
     *   <code>statement</code>.
     */
    private JCRSQLQueryBuilder(ASTQuery statement, NamespaceResolver resolver) {
        this.stmt = statement;
        this.resolver = resolver;
    }

    /**
     * Creates a <code>QueryNode</code> tree from a SQL <code>statement</code>.
     * @param statement the SQL statement.
     * @param resolver the namespace resolver to use.
     * @return the <code>QueryNode</code> tree.
     * @throws InvalidQueryException if <code>statement</code> is malformed.
     */
    public static QueryRootNode createQuery(String statement, NamespaceResolver resolver)
            throws InvalidQueryException {
        try {
            JCRSQLQueryBuilder builder = new JCRSQLQueryBuilder(JCRSQLParser.parse(statement, resolver), resolver);
            return builder.getRootNode();
        } catch (ParseException e) {
            throw new InvalidQueryException(e.getMessage());
        } catch (IllegalArgumentException e) {
            throw new InvalidQueryException(e.getMessage());
        }
    }

    /**
     * Creates a String representation of the query node tree in SQL syntax.
     * @param root the root of the query node tree.
     * @param resolver to resolve QNames.
     * @return a String representation of the query node tree.
     * @throws InvalidQueryException if the query node tree cannot be converted
     *   into a String representation due to restrictions in SQL.
     */
    public static String toString(QueryRootNode root, NamespaceResolver resolver)
            throws InvalidQueryException {
        return QueryFormat.toString(root, resolver);
    }

    /**
     * Parses the statement and returns the root node of the <code>QueryNode</code>
     * tree.
     * @return the root node of the <code>QueryNode</code> tree.
     */
    private QueryRootNode getRootNode() {
        if (root == null) {
            stmt.jjtAccept(this, null);
        }
        return root;
    }

    //----------------< JCRSQLParserVisitor >------------------------------------

    public Object visit(SimpleNode node, Object data) {
        // do nothing, should never be called actually
        return data;
    }

    public Object visit(ASTQuery node, Object data) {
        root = new QueryRootNode();
        root.setLocationNode(new PathQueryNode(root));

        // pass to select, from, where, ...
        node.childrenAccept(this, root);

        // use //* if no path has been set
        PathQueryNode pathNode = root.getLocationNode();
        if (pathNode.getPathSteps().length == 0) {
            pathNode.addPathStep(new LocationStepQueryNode(pathNode, new QName("", ""), false));
            pathNode.addPathStep(new LocationStepQueryNode(pathNode, null, true));
        }

        // attach constraint to last path step
        LocationStepQueryNode[] steps = pathNode.getPathSteps();
        steps[steps.length - 1].addPredicate(constraintNode);

        return root;
    }

    public Object visit(ASTSelectList node, Object data) {
        final QueryRootNode root = (QueryRootNode) data;

        node.childrenAccept(new DefaultParserVisitor() {
            public Object visit(ASTIdentifier node, Object data) {
                root.addSelectProperty(node.getName());
                return data;
            }
        }, root);

        return data;
    }

    public Object visit(ASTFromClause node, Object data) {
        QueryRootNode root = (QueryRootNode) data;

        return node.childrenAccept(new DefaultParserVisitor() {
            public Object visit(ASTIdentifier node, Object data) {
                if (!node.getName().equals(NodeTypeRegistry.NT_BASE)) {
                    // node is either primary or mixin node type
                    NodeTypeQueryNode nodeType
                            = new NodeTypeQueryNode(constraintNode, node.getName());
                    constraintNode.addOperand(nodeType);
                }
                return data;
            }
        }, root);
    }

    public Object visit(ASTWhereClause node, Object data) {
        return node.childrenAccept(this, constraintNode);
    }

    public Object visit(ASTPredicate node, Object data) {
        NAryQueryNode parent = (NAryQueryNode) data;

        int type = node.getOperationType();
        QueryNode predicateNode = null;

        try {
            final QName[] tmp = new QName[1];
            node.childrenAccept(new DefaultParserVisitor() {
                public Object visit(ASTIdentifier node, Object data) {
                    // only assign first identifier
                    tmp[0] = (tmp[0] == null) ? node.getName() : tmp[0];
                    return data;
                }
            }, data);
            QName identifier = tmp[0];

            if (identifier.equals(JCR_PATH)) {
                if (node.children[1] instanceof ASTIdentifier) {
                    // simply ignore, this is a join of a mixin node type
                } else {
                    createPathQuery(((ASTLiteral) node.children[1]).getValue());
                }
                // done
                return data;
            }

            if (type == Constants.OPERATION_BETWEEN) {
                AndQueryNode between = new AndQueryNode(parent);
                RelationQueryNode rel = createRelationQueryNode(between,
                        identifier, Constants.OPERATION_GE_VALUE, (ASTLiteral) node.children[1]);
                between.addOperand(rel);
                rel = createRelationQueryNode(between,
                        identifier, Constants.OPERATION_LE_VALUE, (ASTLiteral) node.children[2]);
                between.addOperand(rel);
                predicateNode = between;
            } else if (type == Constants.OPERATION_EQ_VALUE) {
                if (node.children[1] instanceof ASTIdentifier) {
                    // simply ignore, this is a join of a mixin node type
                } else {
                    predicateNode = createRelationQueryNode(parent,
                            identifier, type, (ASTLiteral) node.children[1]);
                }
            } else if (type == Constants.OPERATION_GE_VALUE
                    || type == Constants.OPERATION_GT_VALUE
                    || type == Constants.OPERATION_LE_VALUE
                    || type == Constants.OPERATION_LT_VALUE
                    || type == Constants.OPERATION_NE_VALUE) {
                predicateNode = createRelationQueryNode(parent,
                        identifier, type, (ASTLiteral) node.children[1]);
            } else if (type == Constants.OPERATION_EQ_GENERAL
                    || type == Constants.OPERATION_NE_GENERAL) {
                predicateNode = createRelationQueryNode(parent,
                        identifier, type, (ASTLiteral) node.children[0]);
            } else if (type == Constants.OPERATION_LIKE) {
                ASTLiteral pattern = (ASTLiteral) node.children[1];
                if (node.getEscapeString() != null) {
                    if (node.getEscapeString().length() == 1) {
                        // backslash is the escape character we use internally
                        pattern.setValue(translateEscaping(pattern.getValue(), node.getEscapeString().charAt(0), '\\'));
                    } else {
                        throw new IllegalArgumentException("ESCAPE string value must have length 1: '" + node.getEscapeString() + "'");
                    }
                } else {
                    // no escape character specified.
                    // if the pattern contains any backslash characters we need
                    // to escape them.
                    pattern.setValue(pattern.getValue().replaceAll("\\\\", "\\\\\\\\"));
                }
                predicateNode = createRelationQueryNode(parent,
                        identifier, type, pattern);
            } else if (type == Constants.OPERATION_IN) {
                OrQueryNode in = new OrQueryNode(parent);
                for (int i = 1; i < node.children.length; i++) {
                    RelationQueryNode rel = createRelationQueryNode(in,
                            identifier, Constants.OPERATION_EQ_VALUE, (ASTLiteral) node.children[i]);
                    in.addOperand(rel);
                }
                predicateNode = in;
            } else if (type == Constants.OPERATION_NULL
                    || type == Constants.OPERATION_NOT_NULL) {
                // create a dummy literal
                ASTLiteral star = new ASTLiteral(JCRSQLParserTreeConstants.JJTLITERAL);
                star.setType(Constants.TYPE_STRING);
                star.setValue("%");
                predicateNode = createRelationQueryNode(parent,
                        identifier, type, star);
            } else {
                throw new IllegalArgumentException("Unknown operation type: " + type);
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new IllegalArgumentException("Too few arguments in predicate");
        }

        if (predicateNode != null) {
            parent.addOperand(predicateNode);
        }

        return data;
    }

    public Object visit(ASTOrExpression node, Object data) {
        NAryQueryNode parent = (NAryQueryNode) data;
        OrQueryNode orQuery = new OrQueryNode(parent);
        // pass to operands
        node.childrenAccept(this, orQuery);

        parent.addOperand(orQuery);
        return parent;
    }

    public Object visit(ASTAndExpression node, Object data) {
        NAryQueryNode parent = (NAryQueryNode) data;
        AndQueryNode andQuery = new AndQueryNode(parent);
        // pass to operands
        node.childrenAccept(this, andQuery);

        parent.addOperand(andQuery);
        return parent;
    }

    public Object visit(ASTNotExpression node, Object data) {
        NAryQueryNode parent = (NAryQueryNode) data;
        NotQueryNode notQuery = new NotQueryNode(parent);
        // pass to operand
        node.childrenAccept(this, notQuery);

        parent.addOperand(notQuery);
        return parent;
    }

    public Object visit(ASTBracketExpression node, Object data) {
        // bracket expression only has influence on how the syntax tree
        // is created.
        // simply pass on to children
        return node.childrenAccept(this, data);
    }

    public Object visit(ASTLiteral node, Object data) {
        // do nothing, should never be called actually
        return data;
    }

    public Object visit(ASTIdentifier node, Object data) {
        // do nothing, should never be called actually
        return data;
    }

    public Object visit(ASTOrderByClause node, Object data) {
        QueryRootNode root = (QueryRootNode) data;

        // list of QNames
        final List identifiers = new ArrayList();

        // collect identifiers
        node.childrenAccept(new DefaultParserVisitor() {
            public Object visit(ASTIdentifier node, Object data) {
                identifiers.add(node.getName());
                return data;
            }
        }, root);

        QName[] props = (QName[]) identifiers.toArray(new QName[identifiers.size()]);
        boolean[] orders = new boolean[props.length];
        root.setOrderNode(new OrderQueryNode(root, props, orders));
        return root;
    }

    public Object visit(ASTContainsExpression node, Object data) {
        NAryQueryNode parent = (NAryQueryNode) data;
        parent.addOperand(new TextsearchQueryNode(parent, node.getQuery()));
        return parent;
    }

    //------------------------< internal >--------------------------------------

    /**
     * Creates a new {@link org.apache.jackrabbit.core.search.RelationQueryNode}.
     * @param parent the parent node for the created <code>RelationQueryNode</code>.
     * @param propertyName the property name for the relation.
     * @param operationType the operation type.
     * @param literal the literal value for the relation.
     * @return a <code>RelationQueryNode</code>.
     * @throws IllegalArgumentException if the literal value does not conform
     * to its type. E.g. a malformed String representation of a date.
     */
    private RelationQueryNode createRelationQueryNode(QueryNode parent,
                                                      QName propertyName,
                                                      int operationType,
                                                      ASTLiteral literal)
            throws IllegalArgumentException {

        String stringValue = literal.getValue();
        RelationQueryNode node = null;

        try {
            if (literal.getType() == Constants.TYPE_DATE) {
                SimpleDateFormat format = new SimpleDateFormat(DATE_PATTERN);
                Date date = format.parse(stringValue);
                node = new RelationQueryNode(parent, propertyName, date, operationType);
            } else if (literal.getType() == Constants.TYPE_DOUBLE) {
                double d = Double.parseDouble(stringValue);
                node = new RelationQueryNode(parent, propertyName, d, operationType);
            } else if (literal.getType() == Constants.TYPE_LONG) {
                long l = Long.parseLong(stringValue);
                node = new RelationQueryNode(parent, propertyName, l, operationType);
            } else if (literal.getType() == Constants.TYPE_STRING) {
                node = new RelationQueryNode(parent, propertyName, stringValue, operationType);
            } else if (literal.getType() == Constants.TYPE_TIMESTAMP) {
                Calendar c = ISO8601.parse(stringValue);
                node = new RelationQueryNode(parent, propertyName, c.getTime(), operationType);
            }
        } catch (java.text.ParseException e) {
            throw new IllegalArgumentException(e.toString());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(e.toString());
        }

        if (node == null) {
            throw new IllegalArgumentException("Unknown type for literal: " + literal.getType());
        }
        return node;
    }

    /**
     * Creates <code>LocationStepQueryNode</code>s from a <code>path</code>.
     */
    private void createPathQuery(String path) {
        PathQueryNode pathNode = root.getLocationNode();

        String[] names = path.split("/");

        for (int i = 0; i < names.length; i++) {
            if (names[i].length() == 0) {
                if (i == 0) {
                    // root
                    pathNode.addPathStep(new LocationStepQueryNode(pathNode, new QName("", ""), false));
                } else {
                    // descendant '//'
                    // FIXME this is not possible
                }
            } else {
                int idx = names[i].indexOf('[');
                String name = null;
                int index = 0;
                if (idx > -1) {
                    // contains index
                    name = names[i].substring(0, idx);
                    String suffix = names[i].substring(idx);
                    String indexStr = suffix.substring(1, suffix.length() - 1);
                    if (indexStr.equals("%")) {
                        // select all same name siblings
                        index = 0;
                    } else {
                        try {
                            index = Integer.parseInt(indexStr);
                        } catch (NumberFormatException e) {
                            log.warn("Unable to parse index for path element: " + names[i]);
                        }
                    }
                } else {
                    // no index
                    name = names[i];
                    // in SQL this means index 1
                    index = 1;
                }
                if (name.equals("%")) {
                    name = null;
                }
                QName qName = null;
                if (name != null) {
                    try {
                        qName = QName.fromJCRName(name, resolver);
                    } catch (IllegalNameException e) {
                        throw new IllegalArgumentException("Illegal name: " + name);
                    } catch (UnknownPrefixException e) {
                        throw new IllegalArgumentException("Unknown prefix: " + name);
                    }
                }
                // @todo how to specify descendant-or-self?
                LocationStepQueryNode step = new LocationStepQueryNode(pathNode, qName, false);
                if (index > 0) {
                    step.setIndex(index);
                }
                pathNode.addPathStep(step);
            }
        }
    }

    /**
     * Translates a pattern using the escape character <code>from</code> into
     * a pattern using the escape character <code>to</code>.
     * @param pattern the pattern to translate
     * @param from the currently used escape character.
     * @param to the new escape character to use.
     * @return the new pattern using the escape character <code>to</code>.
     */
    private static String translateEscaping(String pattern, char from, char to) {
        // if escape characters are the same OR pattern does not contain any
        // escape characters -> simply return pattern as is.
        if (from == to || (pattern.indexOf(from) < 0 && pattern.indexOf(to) < 0)) {
            return pattern;
        }
        StringBuffer translated = new StringBuffer(pattern.length());
        boolean escaped = false;
        for (int i = 0; i < pattern.length(); i++) {
            if (pattern.charAt(i) == from) {
                if (escaped) {
                    translated.append(from);
                    escaped = false;
                } else {
                    escaped = true;
                }
            } else if (pattern.charAt(i) == to) {
                if (escaped) {
                    translated.append(to).append(to);
                    escaped = false;
                } else {
                    translated.append(to).append(to);
                }
            } else {
                if (escaped) {
                    translated.append(to);
                    escaped = false;
                }
                translated.append(pattern.charAt(i));
            }
        }
        return translated.toString();
    }

}
