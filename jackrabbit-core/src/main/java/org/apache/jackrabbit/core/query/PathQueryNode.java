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
package org.apache.jackrabbit.core.query;

/**
 * Implements a query node that defines a path restriction.
 */
public class PathQueryNode extends NAryQueryNode {

    /**
     * Flag indicating whether this path is absolute.
     */
    private boolean absolute = false;

    /**
     * Empty step node array.
     */
    private static final LocationStepQueryNode[] EMPTY = new LocationStepQueryNode[0];

    /**
     * Creates a relative <code>PathQueryNode</code> with no location steps.
     *
     * @param parent the parent query node.
     */
    public PathQueryNode(QueryNode parent) {
        super(parent);
    }

    /**
     * {@inheritDoc}
     */
    public Object accept(QueryNodeVisitor visitor, Object data) {
        return visitor.visit(this, data);
    }

    /**
     * Returns the type of this node.
     *
     * @return the type of this node.
     */
    public int getType() {
        return QueryNode.TYPE_PATH;
    }

    /**
     * Adds a path step to this <code>PathQueryNode</code>.
     *
     * @param step the step to add.
     */
    public void addPathStep(LocationStepQueryNode step) {
        addOperand(step);
    }

    /**
     * Returns an array of all currently set location step nodes.
     *
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
     * If <code>absolute</code> is <code>true</code> sets this
     * <code>PathQueryNode</code> to an absolute path. If <code>absolute</code>
     * is <code>false</code> this path is considered relative.
     *
     * @param absolute sets the absolute property to this new value.
     */
    public void setAbsolute(boolean absolute) {
        this.absolute = absolute;
    }

    /**
     * Returns <code>true</code> if this is an absolute path; <code>false</code>
     * otherwise.
     *
     * @return <code>true</code> if this is an absolute path; <code>false</code>
     *         otherwise.
     */
    public boolean isAbsolute() {
        return absolute;
    }

    /**
     * @inheritDoc
     */
    public boolean equals(Object obj) {
        if (obj instanceof PathQueryNode) {
            PathQueryNode other = (PathQueryNode) obj;
            return super.equals(obj) && absolute == other.absolute;
        }
        return false;
    }
}
