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
package org.apache.jackrabbit.core.search;

/**
 * Implements a query node that defines an exact match of a property and a
 * value.
 */
public class ExactQueryNode extends QueryNode {

    /**
     * The name of the property to match
     */
    private final String property;

    /**
     * The value of the property to match
     */
    private final String value;

    /**
     * Creates a new <code>ExactQueryNode</code> instance.
     *
     * @param parent   the parent node for this <code>ExactQueryNode</code>.
     * @param property the name of the property to match.
     * @param value    the value of the property to match.
     */
    public ExactQueryNode(QueryNode parent, String property, String value) {
        super(parent);
        if (parent == null) {
            throw new NullPointerException("parent");
        }
        this.property = property;
        this.value = value;
    }

    /**
     * @see QueryNode#accept(org.apache.jackrabbit.core.search.QueryNodeVisitor, java.lang.Object)
     */
    public Object accept(QueryNodeVisitor visitor, Object data) {
        return visitor.visit(this, data);
    }

    /**
     * Returns the name of the property to match.
     *
     * @return the name of the property to match.
     */
    public String getPropertyName() {
        return property;
    }

    /**
     * Returns the value of the property to match.
     *
     * @return the value of the property to match.
     */
    public String getValue() {
        return value;
    }

    /**
     * Returns a JCRQL representation for this query node.
     *
     * @return a JCRQL representation for this query node.
     */
    public String toJCRQLString() {
        if (property.indexOf(' ') > -1) {
            return "\"" + property + "\"=\"" + value + "\"";
        }
        return property + "=\"" + value + "\"";
    }

    /**
     * Returns a JCR SQL representation for this query node.
     *
     * @return a JCR SQL representation for this query node.
     */
    public String toJCRSQLString() {
        return "\"" + property + "\"='" + value + "'";
    }

    /**
     * Returns an XPath representation for this query node.
     *
     * @return an XPath representation for this query node.
     */
    public String toXPathString() {
        // todo use encoding for property name
        return "@" + property + "='" + value.replaceAll("'", "''") + "'";
    }
}
