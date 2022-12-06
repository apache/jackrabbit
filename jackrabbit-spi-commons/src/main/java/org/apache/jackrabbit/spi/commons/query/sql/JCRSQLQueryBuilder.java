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

import org.apache.jackrabbit.spi.commons.query.AndQueryNode;
import org.apache.jackrabbit.spi.commons.query.LocationStepQueryNode;
import org.apache.jackrabbit.spi.commons.query.NAryQueryNode;
import org.apache.jackrabbit.spi.commons.query.NodeTypeQueryNode;
import org.apache.jackrabbit.spi.commons.query.NotQueryNode;
import org.apache.jackrabbit.spi.commons.query.OrQueryNode;
import org.apache.jackrabbit.spi.commons.query.OrderQueryNode;
import org.apache.jackrabbit.spi.commons.query.PathQueryNode;
import org.apache.jackrabbit.spi.commons.query.QueryConstants;
import org.apache.jackrabbit.spi.commons.query.QueryNode;
import org.apache.jackrabbit.spi.commons.query.QueryRootNode;
import org.apache.jackrabbit.spi.commons.query.RelationQueryNode;
import org.apache.jackrabbit.spi.commons.query.TextsearchQueryNode;
import org.apache.jackrabbit.spi.commons.query.PropertyFunctionQueryNode;
import org.apache.jackrabbit.spi.commons.query.QueryNodeFactory;
import org.apache.jackrabbit.spi.commons.conversion.NameException;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.apache.jackrabbit.spi.commons.name.PathBuilder;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.commons.conversion.MalformedPathException;
import org.apache.jackrabbit.spi.commons.conversion.NameResolver;
import org.apache.jackrabbit.util.ISO8601;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.collections4.map.AbstractReferenceMap.ReferenceStrength;
import org.apache.commons.collections4.map.ReferenceMap;

import javax.jcr.query.InvalidQueryException;
import javax.jcr.NamespaceException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Map;
import java.io.StringReader;

/**
 * Implements the query builder for the JCR SQL syntax.
 */
public class JCRSQLQueryBuilder implements JCRSQLParserVisitor {

    /**
     * logger instance for this class
     */
    private static final Logger log = LoggerFactory.getLogger(JCRSQLQueryBuilder.class);

    /**
     * DateFormat pattern for type
     * {@link org.apache.jackrabbit.spi.commons.query.QueryConstants#TYPE_DATE}.
     */
    private static final String DATE_PATTERN = "yyyy-MM-dd";

    /**
     * Map of reusable JCRSQL parser instances indexed by NamespaceResolver.
     */
    private static Map<NameResolver, JCRSQLParser> parsers = new ReferenceMap<>(ReferenceStrength.WEAK, ReferenceStrength.WEAK);

    /**
     * The root node of the sql query syntax tree
     */
    private final ASTQuery stmt;

    /**
     * The root query node
     */
    private QueryRootNode root;

    /**
     * To resolve QNames
     */
    private NameResolver resolver;

    /**
     * Query node to gather the constraints defined in the WHERE clause
     */
    private final AndQueryNode constraintNode;

    /**
     * The Name of the node type in the from clause.
     */
    private Name nodeTypeName;

    /**
     * List of PathQueryNode constraints that need to be merged
     */
    private final List pathConstraints = new ArrayList();

    /**
     * The query node factory.
     */
    private final QueryNodeFactory factory;

    /**
     * Creates a new <code>JCRSQLQueryBuilder</code>.
     *
     * @param statement the root node of the SQL syntax tree.
     * @param resolver  a namespace resolver to use for names in the
     *                  <code>statement</code>.
     * @param factory   the query node factory.
     */
    private JCRSQLQueryBuilder(ASTQuery statement,
                               NameResolver resolver,
                               QueryNodeFactory factory) {
        this.stmt = statement;
        this.resolver = resolver;
        this.factory = factory;
        this.constraintNode =  factory.createAndQueryNode(null);
    }

    /**
     * Creates a <code>QueryNode</code> tree from a SQL <code>statement</code>
     * using the passed query node <code>factory</code>.
     *
     * @param statement the SQL statement.
     * @param resolver  the namespace resolver to use.
     * @return the <code>QueryNode</code> tree.
     * @throws InvalidQueryException if <code>statement</code> is malformed.
     */
    public static QueryRootNode createQuery(String statement,
                                            NameResolver resolver,
                                            QueryNodeFactory factory)
            throws InvalidQueryException {
        try {
            // get parser
            JCRSQLParser parser;
            synchronized (parsers) {
                parser = parsers.get(resolver);
                if (parser == null) {
                    parser = new JCRSQLParser(new StringReader(statement));
                    parser.setNameResolver(resolver);
                    parsers.put(resolver, parser);
                }
            }

            JCRSQLQueryBuilder builder;
            // guard against concurrent use within same session
            synchronized (parser) {
                parser.ReInit(new StringReader(statement));
                builder = new JCRSQLQueryBuilder(parser.Query(), resolver, factory);
            }
            return builder.getRootNode();
        } catch (ParseException e) {
            throw new InvalidQueryException(e.getMessage());
        } catch (IllegalArgumentException e) {
            throw new InvalidQueryException(e.getMessage());
        } catch (Throwable t) {
            // javacc parser may also throw an error in some cases
            throw new InvalidQueryException(t.getMessage());
        }
    }

    /**
     * Creates a String representation of the query node tree in SQL syntax.
     *
     * @param root     the root of the query node tree.
     * @param resolver to resolve QNames.
     * @return a String representation of the query node tree.
     * @throws InvalidQueryException if the query node tree cannot be converted
     *                               into a String representation due to restrictions in SQL.
     */
    public static String toString(QueryRootNode root, NameResolver resolver)
            throws InvalidQueryException {
        return QueryFormat.toString(root, resolver);
    }

    /**
     * Parses the statement and returns the root node of the <code>QueryNode</code>
     * tree.
     *
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
        root = factory.createQueryRootNode();
        root.setLocationNode(factory.createPathQueryNode(root));

        // pass to select, from, where, ...
        node.childrenAccept(this, root);

        // use //* if no path has been set
        PathQueryNode pathNode = root.getLocationNode();
        pathNode.setAbsolute(true);
        if (pathConstraints.size() == 0) {
            LocationStepQueryNode step = factory.createLocationStepQueryNode(pathNode);
            step.setNameTest(null);
            step.setIncludeDescendants(true);
            pathNode.addPathStep(step);
        } else {
            try {
                while (pathConstraints.size() > 1) {
                    // merge path nodes
                    MergingPathQueryNode path = null;
                    for (Iterator it = pathConstraints.iterator(); it.hasNext();) {
                        path = (MergingPathQueryNode) it.next();
                        if (path.needsMerge()) {
                            break;
                        } else {
                            path = null;
                        }
                    }
                    if (path == null) {
                        throw new IllegalArgumentException("Invalid combination of jcr:path clauses");
                    } else {
                        pathConstraints.remove(path);
                        MergingPathQueryNode[] paths = (MergingPathQueryNode[]) pathConstraints.toArray(new MergingPathQueryNode[pathConstraints.size()]);
                        paths = path.doMerge(paths);
                        pathConstraints.clear();
                        pathConstraints.addAll(Arrays.asList(paths));
                    }
                }
            } catch (NoSuchElementException e) {
                throw new IllegalArgumentException("Invalid combination of jcr:path clauses");
            }
            MergingPathQueryNode path = (MergingPathQueryNode) pathConstraints.get(0);
            LocationStepQueryNode[] steps = path.getPathSteps();
            for (int i = 0; i < steps.length; i++) {
                LocationStepQueryNode step = factory.createLocationStepQueryNode(pathNode);
                step.setNameTest(steps[i].getNameTest());
                step.setIncludeDescendants(steps[i].getIncludeDescendants());
                step.setIndex(steps[i].getIndex());
                pathNode.addPathStep(step);
            }
        }

        if (constraintNode.getNumOperands() == 1) {
            // attach operand to last path step
            LocationStepQueryNode[] steps = pathNode.getPathSteps();
            steps[steps.length - 1].addPredicate(constraintNode.getOperands()[0]);
        } else if (constraintNode.getNumOperands() > 1) {
            // attach constraint to last path step
            LocationStepQueryNode[] steps = pathNode.getPathSteps();
            steps[steps.length - 1].addPredicate(constraintNode);
        }

        if (nodeTypeName != null) {
            // add node type constraint
            LocationStepQueryNode[] steps = pathNode.getPathSteps();
            NodeTypeQueryNode nodeType
                    = factory.createNodeTypeQueryNode(steps[steps.length - 1], nodeTypeName);
            steps[steps.length - 1].addPredicate(nodeType);
        }

        return root;
    }

    public Object visit(ASTSelectList node, Object data) {
        final QueryRootNode root = (QueryRootNode) data;

        node.childrenAccept(new DefaultParserVisitor() {
            public Object visit(ASTIdentifier node, Object data) {
                root.addSelectProperty(node.getName());
                return data;
            }

            public Object visit(ASTExcerptFunction node, Object data) {
                root.addSelectProperty(NameFactoryImpl.getInstance().create(Name.NS_REP_URI, "excerpt(.)"));
                return data;
            }
        }, root);

        return data;
    }

    public Object visit(ASTFromClause node, Object data) {
        QueryRootNode root = (QueryRootNode) data;

        return node.childrenAccept(new DefaultParserVisitor() {
            public Object visit(ASTIdentifier node, Object data) {
                if (!node.getName().equals(NameConstants.NT_BASE)) {
                    // node is either primary or mixin node type
                    nodeTypeName = node.getName();
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
        QueryNode predicateNode;

        try {
            final Name[] tmp = new Name[2];
            final ASTLiteral[] value = new ASTLiteral[1];
            node.childrenAccept(new DefaultParserVisitor() {
                public Object visit(ASTIdentifier node, Object data) {
                    if (tmp[0] == null) {
                        tmp[0] = node.getName();
                    } else if (tmp[1] == null) {
                        tmp[1] = node.getName();
                    }
                    return data;
                }

                public Object visit(ASTLiteral node, Object data) {
                    value[0] = node;
                    return data;
                }

                public Object visit(ASTLowerFunction node, Object data) {
                    getIdentifier(node);
                    return data;
                }

                public Object visit(ASTUpperFunction node, Object data) {
                    getIdentifier(node);
                    return data;
                }

                private void getIdentifier(SimpleNode node) {
                    if (node.jjtGetNumChildren() > 0) {
                        Node n = node.jjtGetChild(0);
                        if (n instanceof ASTIdentifier) {
                            ASTIdentifier identifier = (ASTIdentifier) n;
                            if (tmp[0] == null) {
                                tmp[0] = identifier.getName();
                            } else if (tmp[1] == null) {
                                tmp[1] = identifier.getName();
                            }
                        }
                    }
                }
            }, data);
            Name identifier = tmp[0];

            if (identifier != null && identifier.equals(NameConstants.JCR_PATH)) {
                if (tmp[1] != null) {
                    // simply ignore, this is a join of a mixin node type
                } else {
                    createPathQuery(value[0].getValue(), parent.getType());
                }
                // done
                return data;
            }

            if (type == QueryConstants.OPERATION_BETWEEN) {
                AndQueryNode between = factory.createAndQueryNode(parent);
                RelationQueryNode rel = createRelationQueryNode(between,
                        identifier, QueryConstants.OPERATION_GE_GENERAL, (ASTLiteral) node.children[1]);
                node.childrenAccept(this, rel);
                between.addOperand(rel);
                rel = createRelationQueryNode(between,
                        identifier, QueryConstants.OPERATION_LE_GENERAL, (ASTLiteral) node.children[2]);
                node.childrenAccept(this, rel);
                between.addOperand(rel);
                predicateNode = between;
            } else if (type == QueryConstants.OPERATION_GE_GENERAL
                    || type == QueryConstants.OPERATION_GT_GENERAL
                    || type == QueryConstants.OPERATION_LE_GENERAL
                    || type == QueryConstants.OPERATION_LT_GENERAL
                    || type == QueryConstants.OPERATION_NE_GENERAL
                    || type == QueryConstants.OPERATION_EQ_GENERAL) {
                predicateNode = createRelationQueryNode(parent,
                        identifier, type, value[0]);
                node.childrenAccept(this, predicateNode);
            } else if (type == QueryConstants.OPERATION_LIKE) {
                ASTLiteral pattern = value[0];
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
                node.childrenAccept(this, predicateNode);
            } else if (type == QueryConstants.OPERATION_IN) {
                OrQueryNode in = factory.createOrQueryNode(parent);
                for (int i = 1; i < node.children.length; i++) {
                    RelationQueryNode rel = createRelationQueryNode(in,
                            identifier, QueryConstants.OPERATION_EQ_VALUE, (ASTLiteral) node.children[i]);
                    node.childrenAccept(this, rel);
                    in.addOperand(rel);
                }
                predicateNode = in;
            } else if (type == QueryConstants.OPERATION_NULL
                    || type == QueryConstants.OPERATION_NOT_NULL) {
                predicateNode = createRelationQueryNode(parent,
                        identifier, type, null);
            } else if (type == QueryConstants.OPERATION_SIMILAR) {
                ASTLiteral literal;
                if (node.children.length == 1) {
                    literal = (ASTLiteral) node.children[0];
                } else {
                    literal = (ASTLiteral) node.children[1];
                }
                predicateNode = createRelationQueryNode(parent, identifier, type, literal);
            } else if (type == QueryConstants.OPERATION_SPELLCHECK) {
                predicateNode = createRelationQueryNode(parent,
                        NameConstants.JCR_PRIMARYTYPE, type,
                        (ASTLiteral) node.children[0]);
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
        OrQueryNode orQuery = factory.createOrQueryNode(parent);
        // pass to operands
        node.childrenAccept(this, orQuery);

        if (orQuery.getNumOperands() > 0) {
            parent.addOperand(orQuery);
        }
        return parent;
    }

    public Object visit(ASTAndExpression node, Object data) {
        NAryQueryNode parent = (NAryQueryNode) data;
        AndQueryNode andQuery = factory.createAndQueryNode(parent);
        // pass to operands
        node.childrenAccept(this, andQuery);

        if (andQuery.getNumOperands() > 0) {
            parent.addOperand(andQuery);
        }
        return parent;
    }

    public Object visit(ASTNotExpression node, Object data) {
        NAryQueryNode parent = (NAryQueryNode) data;
        NotQueryNode notQuery = factory.createNotQueryNode(parent);
        // pass to operand
        node.childrenAccept(this, notQuery);

        if (notQuery.getNumOperands() > 0) {
            parent.addOperand(notQuery);
        }
        return parent;
    }

    public Object visit(ASTBracketExpression node, Object data) {
        // bracket expression only has influence on how the syntax tree
        // is created.
        // simply pass on to children
        return node.childrenAccept(this, data);
    }

    public Object visit(ASTLiteral node, Object data) {
        // do nothing
        return data;
    }

    public Object visit(ASTIdentifier node, Object data) {
        // do nothing
        return data;
    }

    public Object visit(ASTOrderByClause node, Object data) {
        QueryRootNode root = (QueryRootNode) data;

        OrderQueryNode order = factory.createOrderQueryNode(root);
        root.setOrderNode(order);
        node.childrenAccept(this, order);
        return root;
    }

    public Object visit(ASTOrderSpec node, Object data) {
        OrderQueryNode order = (OrderQueryNode) data;

        final Name[] identifier = new Name[1];

        // collect identifier
        node.childrenAccept(new DefaultParserVisitor() {
            public Object visit(ASTIdentifier node, Object data) {
                identifier[0] = node.getName();
                return data;
            }
        }, data);

        OrderQueryNode.OrderSpec spec = new OrderQueryNode.OrderSpec(identifier[0], true);
        order.addOrderSpec(spec);

        node.childrenAccept(this, spec);

        return data;
    }

    public Object visit(ASTAscendingOrderSpec node, Object data) {
        // do nothing ascending is default anyway
        return data;
    }

    public Object visit(ASTDescendingOrderSpec node, Object data) {
        OrderQueryNode.OrderSpec spec = (OrderQueryNode.OrderSpec) data;
        spec.setAscending(false);
        return data;
    }

    public Object visit(ASTContainsExpression node, Object data) {
        NAryQueryNode parent = (NAryQueryNode) data;
        try {
            Path relPath = null;
            if (node.getPropertyName() != null) {
                PathBuilder builder = new PathBuilder();
                builder.addLast(node.getPropertyName());
                relPath = builder.getPath();
            }
            TextsearchQueryNode tsNode = factory.createTextsearchQueryNode(parent, node.getQuery());
            tsNode.setRelativePath(relPath);
            tsNode.setReferencesProperty(true);
            parent.addOperand(tsNode);
        } catch (MalformedPathException e) {
            // path is always valid
        }
        return parent;
    }

    public Object visit(ASTLowerFunction node, Object data) {
        RelationQueryNode parent = (RelationQueryNode) data;
        if (parent.getValueType() != QueryConstants.TYPE_STRING) {
            String msg = "LOWER() function is only supported for String literal";
            throw new IllegalArgumentException(msg);
        }
        parent.addOperand(factory.createPropertyFunctionQueryNode(parent, PropertyFunctionQueryNode.LOWER_CASE));
        return parent;
    }

    public Object visit(ASTUpperFunction node, Object data) {
        RelationQueryNode parent = (RelationQueryNode) data;
        if (parent.getValueType() != QueryConstants.TYPE_STRING) {
            String msg = "UPPER() function is only supported for String literal";
            throw new IllegalArgumentException(msg);
        }
        parent.addOperand(factory.createPropertyFunctionQueryNode(parent, PropertyFunctionQueryNode.UPPER_CASE));
        return parent;
    }

    public Object visit(ASTExcerptFunction node, Object data) {
        // do nothing
        return data;
    }

    //------------------------< internal >--------------------------------------

    /**
     * Creates a new {@link org.apache.jackrabbit.spi.commons.query.RelationQueryNode}.
     *
     * @param parent        the parent node for the created <code>RelationQueryNode</code>.
     * @param propertyName  the property name for the relation.
     * @param operationType the operation type.
     * @param literal       the literal value for the relation or
     *                      <code>null</code> if the relation does not have a
     *                      literal (e.g. IS NULL).
     * @return a <code>RelationQueryNode</code>.
     * @throws IllegalArgumentException if the literal value does not conform
     *                                  to its type. E.g. a malformed String representation of a date.
     */
    private RelationQueryNode createRelationQueryNode(QueryNode parent,
                                                      Name propertyName,
                                                      int operationType,
                                                      ASTLiteral literal)
            throws IllegalArgumentException {

        RelationQueryNode node = null;

        try {
            Path relPath = null;
            if (propertyName != null) {
                PathBuilder builder = new PathBuilder();
                builder.addLast(propertyName);
                relPath = builder.getPath();
            }
            if (literal == null) {
                node = factory.createRelationQueryNode(parent, operationType);
                node.setRelativePath(relPath);
            } else if (literal.getType() == QueryConstants.TYPE_DATE) {
                SimpleDateFormat format = new SimpleDateFormat(DATE_PATTERN);
                Date date = format.parse(literal.getValue());
                node = factory.createRelationQueryNode(parent, operationType);
                node.setRelativePath(relPath);
                node.setDateValue(date);
            } else if (literal.getType() == QueryConstants.TYPE_DOUBLE) {
                double d = Double.parseDouble(literal.getValue());
                node = factory.createRelationQueryNode(parent, operationType);
                node.setRelativePath(relPath);
                node.setDoubleValue(d);
            } else if (literal.getType() == QueryConstants.TYPE_LONG) {
                long l = Long.parseLong(literal.getValue());
                node = factory.createRelationQueryNode(parent, operationType);
                node.setRelativePath(relPath);
                node.setLongValue(l);
            } else if (literal.getType() == QueryConstants.TYPE_STRING) {
                node = factory.createRelationQueryNode(parent, operationType);
                node.setRelativePath(relPath);
                node.setStringValue(literal.getValue());
            } else if (literal.getType() == QueryConstants.TYPE_TIMESTAMP) {
                Calendar c = ISO8601.parse(literal.getValue());
                node = factory.createRelationQueryNode(parent, operationType);
                node.setRelativePath(relPath);
                node.setDateValue(c.getTime());
            }
        } catch (java.text.ParseException e) {
            throw new IllegalArgumentException(e.toString());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(e.toString());
        } catch (MalformedPathException e) {
            // path is always valid, but throw anyway
            throw new IllegalArgumentException(e.getMessage());
        }

        if (node == null) {
            throw new IllegalArgumentException("Unknown type for literal: " + literal.getType());
        }
        return node;
    }

    /**
     * Creates <code>LocationStepQueryNode</code>s from a <code>path</code>.
     *
     * @param path      the path pattern
     * @param operation the type of the parent node
     */
    private void createPathQuery(String path, int operation) {
        MergingPathQueryNode pathNode = new MergingPathQueryNode(operation,
                factory.createPathQueryNode(null).getValidJcrSystemNodeTypeNames());
        pathNode.setAbsolute(true);

        if (path.equals("/")) {
            pathNode.addPathStep(factory.createLocationStepQueryNode(pathNode));
            pathConstraints.add(pathNode);
            return;
        }

        String[] names = path.split("/");

        for (int i = 0; i < names.length; i++) {
            if (names[i].length() == 0) {
                if (i == 0) {
                    // root
                    pathNode.addPathStep(factory.createLocationStepQueryNode(pathNode));
                } else {
                    // descendant '//' -> invalid path
                    // todo throw or ignore?
                    // we currently do not throw and add location step for an
                    // empty name (which is basically the root node)
                    pathNode.addPathStep(factory.createLocationStepQueryNode(pathNode));
                }
            } else {
                int idx = names[i].indexOf('[');
                String name;
                int index = LocationStepQueryNode.NONE;
                if (idx > -1) {
                    // contains index
                    name = names[i].substring(0, idx);
                    String suffix = names[i].substring(idx);
                    String indexStr = suffix.substring(1, suffix.length() - 1);
                    if (indexStr.equals("%")) {
                        // select all same name siblings
                        index = LocationStepQueryNode.NONE;
                    } else {
                        try {
                            index = Integer.parseInt(indexStr);
                        } catch (NumberFormatException e) {
                            log.warn("Unable to parse index for path element: " + names[i]);
                        }
                    }
                    if (name.equals("%")) {
                        name = null;
                    }
                } else {
                    // no index specified
                    // - index defaults to 1 if there is an explicit name test
                    // - index defaults to NONE if name test is %
                    name = names[i];
                    if (name.equals("%")) {
                        name = null;
                    } else {
                        index = 1;
                    }
                }
                Name qName = null;
                if (name != null) {
                    try {
                        qName = resolver.getQName(name);
                    } catch (NamespaceException e) {
                        throw new IllegalArgumentException("Illegal name: " + name);
                    } catch (NameException e) {
                        throw new IllegalArgumentException("Illegal name: " + name);
                    }
                }
                // if name test is % this means also search descendants
                boolean descendant = name == null;
                LocationStepQueryNode step = factory.createLocationStepQueryNode(pathNode);
                step.setNameTest(qName);
                step.setIncludeDescendants(descendant);
                if (index > 0) {
                    step.setIndex(index);
                }
                pathNode.addPathStep(step);
            }
        }
        pathConstraints.add(pathNode);
    }

    /**
     * Translates a pattern using the escape character <code>from</code> into
     * a pattern using the escape character <code>to</code>.
     *
     * @param pattern the pattern to translate
     * @param from    the currently used escape character.
     * @param to      the new escape character to use.
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

    /**
     * Extends the <code>PathQueryNode</code> with merging capability. A
     * <code>PathQueryNode</code> <code>n1</code> can be merged with another
     * node <code>n2</code> in the following case:
     * <p>
     * <code>n1</code> contains a location step at position <code>X</code> with
     * a name test that matches any node and has the descending flag set. Where
     * <code>X</code> &lt; number of location steps.
     * <code>n2</code> contains no location step to match any node name and
     * the sequence of name tests is the same as the sequence of name tests
     * of <code>n1</code>.
     * The merged node then contains a location step at position <code>X</code>
     * with the name test of the location step at position <code>X+1</code> and
     * the descending flag set.
     * <p>
     * The following path patterns:<br/>
     * <code>/foo/%/bar</code> OR <code>/foo/bar</code><br/>
     * are merged into:<br/>
     * <code>/foo//bar</code>.
     * <p>
     * The path patterns:<br/>
     * <code>/foo/%</code> AND NOT <code>/foo/%/%</code><br/>
     * are merged into:<br/>
     * <code>/foo/*</code>
     */
    private static class MergingPathQueryNode extends PathQueryNode {

        /**
         * The operation type of the parent node
         */
        private int operation;

        /**
         * Creates a new <code>MergingPathQueryNode</code> with the operation
         * type of a parent node. <code>operation</code> must be one of:
         * {@link org.apache.jackrabbit.spi.commons.query.QueryNode#TYPE_OR},
         * {@link org.apache.jackrabbit.spi.commons.query.QueryNode#TYPE_AND} or
         * {@link org.apache.jackrabbit.spi.commons.query.QueryNode#TYPE_NOT}.
         *
         * @param operation the operation type of the parent node.
         * @param validJcrSystemNodeTypeNames names of valid node types under
         *        /jcr:system.
         */
        MergingPathQueryNode(
                int operation, Collection<Name> validJcrSystemNodeTypeNames) {
            super(null, validJcrSystemNodeTypeNames);
            if (operation != QueryNode.TYPE_OR && operation != QueryNode.TYPE_AND && operation != QueryNode.TYPE_NOT) {
                throw new IllegalArgumentException("operation");
            }
            this.operation = operation;
        }

        /**
         * Merges this node with a node from <code>nodes</code>. If a merge
         * is not possible an NoSuchElementException is thrown.
         *
         * @param nodes the nodes to try to merge with.
         * @return the merged array containing a merged version of this node.
         */
        MergingPathQueryNode[] doMerge(MergingPathQueryNode[] nodes) {
            if (operation == QueryNode.TYPE_OR) {
                return doOrMerge(nodes);
            } else {
                return doAndMerge(nodes);
            }
        }

        /**
         * Merges two nodes into a node which selects any child nodes of a
         * given node.
         * <p>
         * Example:<br/>
         * The path patterns:<br/>
         * <code>/foo/%</code> AND NOT <code>/foo/%/%</code><br/>
         * are merged into:<br/>
         * <code>/foo/*</code>
         *
         * @param nodes the nodes to merge with.
         * @return the merged nodes.
         */
        private MergingPathQueryNode[] doAndMerge(MergingPathQueryNode[] nodes) {
            if (operation == QueryNode.TYPE_AND) {
                // check if there is an node with operation OP_AND_NOT
                MergingPathQueryNode n = null;
                for (int i = 0; i < nodes.length; i++) {
                    if (nodes[i].operation == QueryNode.TYPE_NOT) {
                        n = nodes[i];
                        nodes[i] = this;
                    }
                }
                if (n == null) {
                    throw new NoSuchElementException("Merging not possible with any node");
                } else {
                    return n.doAndMerge(nodes);
                }
            }
            // check if this node is valid as an operand
            if (operands.size() < 3) {
                throw new NoSuchElementException("Merging not possible");
            }
            int size = operands.size();
            LocationStepQueryNode n1 = (LocationStepQueryNode) operands.get(size - 1);
            LocationStepQueryNode n2 = (LocationStepQueryNode) operands.get(size - 2);
            if (n1.getNameTest() != null || n2.getNameTest() != null
                    || !n1.getIncludeDescendants() || !n2.getIncludeDescendants()) {
                throw new NoSuchElementException("Merging not possible");
            }
            // find a node to merge with
            MergingPathQueryNode matchedNode = null;
            for (int i = 0; i < nodes.length; i++) {
                if (nodes[i].operands.size() == operands.size() - 1) {
                    boolean match = true;
                    for (int j = 0; j < operands.size() - 1 && match; j++) {
                        LocationStepQueryNode step = (LocationStepQueryNode) operands.get(j);
                        LocationStepQueryNode other = (LocationStepQueryNode) nodes[i].operands.get(j);
                        match &= (step.getNameTest() == null) ? other.getNameTest() == null : step.getNameTest().equals(other.getNameTest());
                    }
                    if (match) {
                        matchedNode = nodes[i];
                        break;
                    }
                }
            }
            if (matchedNode == null) {
                throw new NoSuchElementException("Merging not possible with any node");
            }
            // change descendants flag to only match child nodes
            // that's the result of the merge.
            ((LocationStepQueryNode) matchedNode.operands.get(matchedNode.operands.size() - 1)).setIncludeDescendants(false);
            return nodes;
        }

        /**
         * Merges two nodes into one node selecting a node on the
         * descendant-or-self axis.
         * <p>
         * Example:<br/>
         * The following path patterns:<br/>
         * <code>/foo/%/bar</code> OR <code>/foo/bar</code><br/>
         * are merged into:<br/>
         * <code>/foo//bar</code>.
         *
         * @param nodes the node to merge.
         * @return the merged nodes.
         */
        private MergingPathQueryNode[] doOrMerge(MergingPathQueryNode[] nodes) {
            // compact this
            MergingPathQueryNode compacted = new MergingPathQueryNode(
                    QueryNode.TYPE_OR, getValidJcrSystemNodeTypeNames());
            for (Iterator it = operands.iterator(); it.hasNext();) {
                LocationStepQueryNode step = (LocationStepQueryNode) it.next();
                if (step.getIncludeDescendants() && step.getNameTest() == null) {
                    // check if has next
                    if (it.hasNext()) {
                        LocationStepQueryNode next = (LocationStepQueryNode) it.next();
                        next.setIncludeDescendants(true);
                        compacted.addPathStep(next);
                    } else {
                        compacted.addPathStep(step);
                    }
                } else {
                    compacted.addPathStep(step);
                }
            }

            MergingPathQueryNode matchedNode = null;
            for (int i = 0; i < nodes.length; i++) {
                // loop over the steps and compare the names
                if (nodes[i].operands.size() == compacted.operands.size()) {
                    boolean match = true;
                    Iterator compactedSteps = compacted.operands.iterator();
                    Iterator otherSteps = nodes[i].operands.iterator();
                    while (match && compactedSteps.hasNext()) {
                        LocationStepQueryNode n1 = (LocationStepQueryNode) compactedSteps.next();
                        LocationStepQueryNode n2 = (LocationStepQueryNode) otherSteps.next();
                        match &= (n1.getNameTest() == null) ? n2.getNameTest() == null : n1.getNameTest().equals(n2.getNameTest());
                    }
                    if (match) {
                        matchedNode = nodes[i];
                        break;
                    }
                }
            }
            if (matchedNode == null) {
                throw new NoSuchElementException("Merging not possible with any node.");
            }
            // construct new list
            List mergedList = new ArrayList(Arrays.asList(nodes));
            mergedList.remove(matchedNode);
            mergedList.add(compacted);
            return (MergingPathQueryNode[]) mergedList.toArray(new MergingPathQueryNode[mergedList.size()]);
        }

        /**
         * Returns <code>true</code> if this node needs merging; <code>false</code>
         * otherwise.
         *
         * @return <code>true</code> if this node needs merging; <code>false</code>
         *         otherwise.
         */
        boolean needsMerge() {
            for (Iterator it = operands.iterator(); it.hasNext();) {
                LocationStepQueryNode step = (LocationStepQueryNode) it.next();
                if (step.getIncludeDescendants() && step.getNameTest() == null) {
                    return true;
                }
            }
            return false;
        }
    }
}
