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

import javax.jcr.query.InvalidQueryException;
import javax.jcr.util.ISO8601;
import java.util.Date;

/**
 * 
 * @author Marcel Reutegger
 * @version $Revision:  $, $Date:  $
 */
public class JCRQLQueryBuilder implements JCRQLParserVisitor {

    private final ASTStatement stmt;

    private QueryNode currentNode;

    private QueryRootNode root;

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
	    root = (QueryRootNode)stmt.jjtAccept(this, null);
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

	node.childrenAccept(this, root);
	// pass to select, from, location, where, textsearch clause
	return root;
    }

    public Object visit(ASTSelectClause node, Object data) {
	QueryRootNode root = (QueryRootNode)data;
	String[] properties = node.getProperties();
	for (int i = 0; i < properties.length; i++) {
	    root.addSelectProperty(properties[i]);
	}
	return data;
    }

    public Object visit(ASTFromClause node, Object data) {
	QueryRootNode root = (QueryRootNode)data;
	// add node type query to parent
	node.childrenAccept(this, root.getConstraintNode());

	return data;
    }

    public Object visit(ASTNodeType node, Object data) {
        NAryQueryNode parent = (NAryQueryNode)data;
	parent.addOperand(new NodeTypeQueryNode(parent, node.getName()));
	return parent;
    }

    public Object visit(ASTLocationClause node, Object data) {
	QueryRootNode root = (QueryRootNode)data;
	root.setLocationNode(new PathQueryNode(root, node.getStringLocation(),
		node.getType()));
	return root;
    }

    public Object visit(ASTWhereClause node, Object data) {
	QueryRootNode root = (QueryRootNode)data;
        // just pass it to the expression
	return node.childrenAccept(this, root.getConstraintNode());
    }

    public Object visit(ASTOrExpr node, Object data) {
	NAryQueryNode parent = (NAryQueryNode)data;
	OrQueryNode orQuery = new OrQueryNode(parent);
	// pass to operands
	node.childrenAccept(this, orQuery);

	parent.addOperand(orQuery);
	return parent;
    }

    public Object visit(ASTAndExpr node, Object data) {
	NAryQueryNode parent = (NAryQueryNode)data;
	AndQueryNode andQuery = new AndQueryNode(parent);
	// pass to operands
	node.childrenAccept(this, andQuery);

	parent.addOperand(andQuery);
	return parent;
    }

    public Object visit(ASTNotExpr node, Object data) {
	NAryQueryNode parent = (NAryQueryNode)data;
	NotQueryNode notQuery = new NotQueryNode(parent);
	// pass to operand
	node.childrenAccept(this, notQuery);

	parent.addOperand(notQuery);
	return parent;
    }

    public Object visit(ASTRelExpr node, Object data) {
	NAryQueryNode parent = (NAryQueryNode)data;
	ASTValue value = (ASTValue)node.jjtGetChild(0);
	int type = value.getType();
	RelationQueryNode rel = null;

	if (type == Constants.TYPE_DATE) {
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
	QueryRootNode root = (QueryRootNode)data;
	root.setTextsearchNode(new TextsearchQueryNode(root, node.getQuery()));
	return root;
    }

    public Object visit(ASTOrderClause node, Object data) {
	QueryRootNode root = (QueryRootNode)data;
	root.setOrderNode(
		new OrderQueryNode(root, node.getProperties(), node.isAscending()));
	return root;
    }
}
