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
package org.apache.jackrabbit.core.search;

/**
 * Implements a query node that defines a path restriction.
 */
public class PathQueryNode extends NAryQueryNode {

    /**
     * Empty step node array.
     */
    private static final LocationStepQueryNode[] EMPTY = new LocationStepQueryNode[0];

    public PathQueryNode(QueryNode parent) {
        super(parent);
    }

    /**
     * @see QueryNode#accept(org.apache.jackrabbit.core.search.QueryNodeVisitor, java.lang.Object)
     */
    public Object accept(QueryNodeVisitor visitor, Object data) {
        return visitor.visit(this, data);
    }

    /**
     * Adds a path step to this <code>PathQueryNode</code>.
     * @param step the step to add.
     */
    public void addPathStep(LocationStepQueryNode step) {
        addOperand(step);
    }

    /**
     * Returns an array of all currently set location step nodes.
     * @return an array of all currently set location step nodes.
     */
    public LocationStepQueryNode[] getPathSteps() {
        if (operands == null) {
            return EMPTY;
        } else {
            return (LocationStepQueryNode[]) operands.toArray(new LocationStepQueryNode[operands.size()]);
        }
    }

    /**
     * Returns a JCRQL representation for this query node.
     *
     * @return a JCRQL representation for this query node.
     */
    public String toJCRQLString() {
        StringBuffer sb = new StringBuffer("LOCATION \"");
        LocationStepQueryNode[] steps = getPathSteps();
        for (int i = 0; i < steps.length; i++) {
            if (steps[i].getNameTest() == null
                    || steps[i].getNameTest().length() > 0) {
                sb.append('/');
            }
            sb.append(steps[i].toJCRQLString());
        }
        sb.append('"');
        return sb.toString();
    }


    /**
     * Returns a JCR SQL representation for this query node.
     *
     * @return a JCR SQL representation for this query node.
     */
    public String toJCRSQLString() {
        // FIXME implement namespace awareness
        StringBuffer sb = new StringBuffer("\"jcr:path\"='");
        LocationStepQueryNode[] steps = getPathSteps();
        for (int i = 0; i < steps.length; i++) {
            if (steps[i].getNameTest() == null
                    || steps[i].getNameTest().length() > 0) {
                sb.append('/');
            }
            sb.append(steps[i].toJCRSQLString());
        }
        sb.append('\'');
        return sb.toString();
    }

    /**
     * Returns an XPath representation for this query node.
     *
     * @return an XPath representation for this query node.
     */
    public String toXPathString() {
        StringBuffer sb = new StringBuffer();
        LocationStepQueryNode[] steps = getPathSteps();
        for (int i = 0; i < steps.length; i++) {
            if (steps[i].getNameTest() == null
                    || steps[i].getNameTest().length() > 0) {
                sb.append('/');
            }
            sb.append(steps[i].toXPathString());
        }
        return sb.toString();
    }
}
