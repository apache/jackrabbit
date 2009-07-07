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

import java.util.Arrays;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;

/**
 * Utility class to dump a {@link QueryNode} tree to a StringBuffer.
 */
public class QueryTreeDump implements QueryNodeVisitor {

    /**
     * Current indentation level
     */
    private int indent;

    /**
     * Padding array filled with spaces
     */
    private static char[] PADDING = new char[255];

    /**
     * The padding character: whitespace.
     */
    private static final char PADDING_CHAR = ' ';

    static {
        Arrays.fill(PADDING, PADDING_CHAR);
    }

    /**
     * Dumps the node tree to buffer.
     * @param node the root node.
     * @param buffer where to dump the tree.
     * @throws RepositoryException
     */
    private QueryTreeDump(QueryNode node, StringBuffer buffer) throws RepositoryException {
        node.accept(this, buffer);
    }

    /**
     * Dumps a query node tree to the string <code>buffer</code>.
     * @param node the root node of a query tree.
     * @param buffer a string buffer where to dump the tree structure.
     * @throws RepositoryException
     */
    public static void dump(QueryNode node, StringBuffer buffer) throws RepositoryException {
        new QueryTreeDump(node, buffer);
    }

    public Object visit(QueryRootNode node, Object data) throws RepositoryException {
        StringBuffer buffer = (StringBuffer) data;
        buffer.append("+ Root node");
        buffer.append("\n");
        // select properties
        Name[] select = node.getSelectProperties();
        buffer.append("+ Select properties: ");
        if (select.length == 0) {
            buffer.append("*");
        } else {
            String comma = "";
            for (int i = 0; i < select.length; i++) {
                buffer.append(comma);
                buffer.append(select[i].toString());
                comma = ", ";
            }
        }
        buffer.append("\n");
        // path
        traverse(new QueryNode[]{node.getLocationNode()}, buffer);
        // order by
        OrderQueryNode order = node.getOrderNode();
        if (order != null) {
            traverse(new QueryNode[]{order}, buffer);
        }
        return buffer;
    }

    public Object visit(OrQueryNode node, Object data) throws RepositoryException {
        StringBuffer buffer = (StringBuffer) data;
        buffer.append(PADDING, 0, indent);
        buffer.append("+ OrQueryNode");
        buffer.append("\n");
        traverse(node.getOperands(), buffer);
        return buffer;
    }

    public Object visit(AndQueryNode node, Object data) throws RepositoryException {
        StringBuffer buffer = (StringBuffer) data;
        buffer.append(PADDING, 0, indent);
        buffer.append("+ AndQueryNode");
        buffer.append("\n");
        traverse(node.getOperands(), buffer);
        return buffer;
    }

    public Object visit(NotQueryNode node, Object data) throws RepositoryException {
        StringBuffer buffer = (StringBuffer) data;
        buffer.append(PADDING, 0, indent);
        buffer.append("+ NotQueryNode");
        buffer.append("\n");
        traverse(node.getOperands(), buffer);
        return buffer;
    }

    public Object visit(ExactQueryNode node, Object data) {
        StringBuffer buffer = (StringBuffer) data;
        buffer.append(PADDING, 0, indent);
        buffer.append("+ ExactQueryNode: ");
        buffer.append(" Prop=").append(node.getPropertyName());
        buffer.append(" Value=").append(node.getValue());
        buffer.append("\n");
        return buffer;
    }

    public Object visit(NodeTypeQueryNode node, Object data) {
        StringBuffer buffer = (StringBuffer) data;
        buffer.append(PADDING, 0, indent);
        buffer.append("+ NodeTypeQueryNode: ");
        buffer.append(" Prop=").append(node.getPropertyName());
        buffer.append(" Value=").append(node.getValue());
        buffer.append("\n");
        return buffer;
    }

    public Object visit(TextsearchQueryNode node, Object data) {
        StringBuffer buffer = (StringBuffer) data;
        buffer.append(PADDING, 0, indent);
        buffer.append("+ TextsearchQueryNode: ");
        buffer.append(" Path=");
        Path relPath = node.getRelativePath();
        if (relPath == null) {
            buffer.append(".");
        } else {
            Path.Element[] elements = relPath.getElements();
            String slash = "";
            for (int i = 0; i < elements.length; i++) {
                buffer.append(slash);
                slash = "/";
                if (node.getReferencesProperty() && i == elements.length - 1) {
                    buffer.append("@");
                }
                buffer.append(elements[i]);
            }
        }
        buffer.append(" Query=").append(node.getQuery());
        buffer.append("\n");
        return buffer;
    }

    public Object visit(PathQueryNode node, Object data) throws RepositoryException {
        StringBuffer buffer = (StringBuffer) data;
        buffer.append(PADDING, 0, indent);
        buffer.append("+ PathQueryNode");
        buffer.append("\n");
        traverse(node.getOperands(), buffer);
        return buffer;
    }

    public Object visit(LocationStepQueryNode node, Object data) throws RepositoryException {
        StringBuffer buffer = (StringBuffer) data;
        buffer.append(PADDING, 0, indent);
        buffer.append("+ LocationStepQueryNode: ");
        buffer.append(" NodeTest=");
        if (node.getNameTest() == null) {
            buffer.append("*");
        } else {
            buffer.append(node.getNameTest());
        }
        buffer.append(" Descendants=").append(node.getIncludeDescendants());
        buffer.append(" Index=");
        if (node.getIndex() == LocationStepQueryNode.NONE) {
            buffer.append("NONE");
        } else if (node.getIndex() == LocationStepQueryNode.LAST) {
            buffer.append("last()");
        } else {
            buffer.append(node.getIndex());
        }
        buffer.append("\n");
        traverse(node.getOperands(), buffer);
        return buffer;
    }

    public Object visit(RelationQueryNode node, Object data) throws RepositoryException {
        StringBuffer buffer = (StringBuffer) data;
        buffer.append(PADDING, 0, indent);
        buffer.append("+ RelationQueryNode: Op: ");
        buffer.append(QueryConstants.OPERATION_NAMES.getName(node.getOperation()));
        buffer.append(" Prop=[");
        PathQueryNode relPath = node.getRelativePath();
        if (relPath == null) {
            buffer.append(relPath);
        } else {
            visit(relPath, buffer);
        }
        buffer.append("] Type=").append(QueryConstants.TYPE_NAMES.getName(node.getValueType()));
        if (node.getValueType() == QueryConstants.TYPE_DATE) {
            buffer.append(" Value=").append(node.getDateValue());
        } else if (node.getValueType() == QueryConstants.TYPE_DOUBLE) {
            buffer.append(" Value=").append(node.getDoubleValue());
        } else if (node.getValueType() == QueryConstants.TYPE_LONG) {
            buffer.append(" Value=").append(node.getLongValue());
        } else if (node.getValueType() == QueryConstants.TYPE_POSITION) {
            buffer.append(" Value=").append(node.getPositionValue());
        } else if (node.getValueType() == QueryConstants.TYPE_STRING) {
            buffer.append(" Value=").append(node.getStringValue());
        } else if (node.getValueType() == QueryConstants.TYPE_TIMESTAMP) {
            buffer.append(" Value=").append(node.getDateValue());
        }

        buffer.append("\n");
        traverse(node.getOperands(), buffer);
        return buffer;
    }

    public Object visit(OrderQueryNode node, Object data) {
        StringBuffer buffer = (StringBuffer) data;
        buffer.append(PADDING, 0, indent);
        buffer.append("+ OrderQueryNode");
        buffer.append("\n");
        OrderQueryNode.OrderSpec[] specs = node.getOrderSpecs();
        for (int i = 0; i < specs.length; i++) {
            buffer.append(PADDING, 0, indent);
            buffer.append("  ");
            appendPath(specs[i].getPropertyPath(), buffer);
            buffer.append(" asc=").append(specs[i].isAscending());
            buffer.append("\n");
        }
        return buffer;
    }

    public Object visit(DerefQueryNode node, Object data) throws RepositoryException {
        StringBuffer buffer = (StringBuffer) data;
        buffer.append(PADDING, 0, indent);
        buffer.append("+ DerefQueryNode: ");
        buffer.append(" NodeTest=");
        if (node.getNameTest() == null) {
            buffer.append("*");
        } else {
            buffer.append(node.getNameTest());
        }
        buffer.append(" Descendants=").append(node.getIncludeDescendants());
        buffer.append(" Index=");
        if (node.getIndex() == LocationStepQueryNode.NONE) {
            buffer.append("NONE");
        } else if (node.getIndex() == LocationStepQueryNode.LAST) {
            buffer.append("last()");
        } else {
            buffer.append(node.getIndex());
        }
        buffer.append("\n");
        traverse(node.getOperands(), buffer);
        return buffer;
    }

    public Object visit(PropertyFunctionQueryNode node, Object data) {
        StringBuffer buffer = (StringBuffer) data;
        buffer.append(PADDING, 0, indent);
        buffer.append("+ PropertyFunctionQueryNode: ");
        buffer.append(node.getFunctionName());
        buffer.append("()\n");
        return buffer;
    }

    private void traverse(QueryNode[] node, StringBuffer buffer) throws RepositoryException {
        indent += 2;
        if (indent > PADDING.length) {
            char[] tmp = new char[indent * 2];
            Arrays.fill(tmp, PADDING_CHAR);
            PADDING = tmp;
        }
        for (int i = 0; i < node.length; i++) {
            node[i].accept(this, buffer);
        }
        indent -= 2;
    }

    /**
     * Appends the relative path to the <code>buffer</code> using '/' as the
     * delimiter for path elements.
     *
     * @param relPath a relative path.
     * @param buffer the buffer where to append the path.
     */
    private static void appendPath(Path relPath, StringBuffer buffer) {
        Path.Element[] elements = relPath.getElements();
        String slash = "";
        for (int i = 0; i < elements.length; i++) {
            buffer.append(slash);
            slash = "/";
            buffer.append(elements[i]);
        }
    }
}
