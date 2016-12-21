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

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;

/**
 * Defines a location step for querying the path of a node.
 * <p>
 * <code>
 * /foo  -&gt; descendants = false, nameTest = foo<br>
 * //foo -&gt; descendants = true, nameTest = foo<br>
 * //*   -&gt; descendants = true, nameTest = null<br>
 * /*    -&gt; descendants = false, nameTest = null<br>
 * /     -&gt; descendants = false, nameTest = ""
 * </code>
 */
public class LocationStepQueryNode extends NAryQueryNode<QueryNode> {

    /** Constant value for position index = last() */
    public static final int LAST = Integer.MIN_VALUE;

    /** Constant value to indicate no position index */
    public static final int NONE = Integer.MIN_VALUE + 1;

    /**
     * The empty name used in matching the root node. This is an implementation
     * specific constant as the empty name is not a valid JCR name.
     * TODO: The root location step should be refactored somehow
     */
    public static final Name EMPTY_NAME = NameFactoryImpl.getInstance().create("", "");
    
    /** Empty <code>QueryNode</code> array for us as return value */
    private static final QueryNode[] EMPTY = new QueryNode[0];

    /**
     * Name test for this location step. A <code>null</code> value indicates
     * a '*' name test.
     */
    private Name nameTest;

    /**
     * If set to <code>true</code> this location step uses the descendant-or-self
     * axis.
     */
    private boolean includeDescendants;

    /**
     * The context position <code>index</code>. Initially {@link #NONE}.
     */
    private int index = NONE;

    /**
     * Creates a new <code>LocationStepQueryNode</code> that matches only the
     * empty name (the repository root). The created location step uses only the
     * child axis.
     *
     * @param parent the parent of this query node.
     */
    protected LocationStepQueryNode(QueryNode parent) {
        super(parent);
        this.nameTest = EMPTY_NAME;
        this.includeDescendants = false;
    }

    /**
     * Returns the label of the node for this location step, or <code>null</code>
     * if the name test is '*'.
     * @return the label of the node for this location step.
     */
    public Name getNameTest() {
        return nameTest;
    }

    /**
     * Sets a new name test.
     * @param nameTest the name test or <code>null</code> to match all names.
     */
    public void setNameTest(Name nameTest) {
        this.nameTest = nameTest;
    }

    /**
     * Returns <code>true</code> if this location step uses the
     * descendant-or-self axis, <code>false</code> if this step uses the child
     * axis.
     * @return <code>true</code> if this step uses the descendant-or-self axis.
     */
    public boolean getIncludeDescendants() {
        return includeDescendants;
    }

    /**
     * Sets a new value for the includeDescendants property.
     * @param include the new value.
     * @see #getIncludeDescendants()
     */
    public void setIncludeDescendants(boolean include) {
        this.includeDescendants = include;
    }

    /**
     * Adds a predicate node to this location step.
     * @param predicate the node to add.
     */
    public void addPredicate(QueryNode predicate) {
        addOperand(predicate);
    }

    /**
     * Returns the predicate nodes for this location step. This method may
     * also return a position predicate.
     * @return the predicate nodes or an empty array if there are no predicates
     *   for this location step.
     */
    public QueryNode[] getPredicates() {
        if (operands == null) {
            return EMPTY;
        } else {
            return operands.toArray(new QueryNode[operands.size()]);
        }
    }

    /**
     * Sets the position index for this step. A value of {@link #NONE} indicates
     * that this location step has no position index assigned. That is, the
     * step selects all same name siblings.
     * @param index the position index.
     */
    public void setIndex(int index) {
        this.index = index;
    }

    /**
     * Returns the position index for this step. A value of {@link #NONE} indicates
     * that this location step has no position index assigned. That is, the
     * step selects all same name siblings.
     * @return the position index for this step.
     */
    public int getIndex() {
        return index;
    }

    /**
     * {@inheritDoc}
     * @throws RepositoryException
     */
    public Object accept(QueryNodeVisitor visitor, Object data) throws RepositoryException {
        return visitor.visit(this, data);
    }

    /**
     * {@inheritDoc}
     */
    public int getType() {
        return QueryNode.TYPE_LOCATION;
    }

    /**
     * {@inheritDoc}
     */
    public boolean equals(Object obj) {
        if (obj instanceof LocationStepQueryNode) {
            LocationStepQueryNode other = (LocationStepQueryNode) obj;
            return super.equals(other)
                    && includeDescendants == other.includeDescendants
                    && index == other.index
                    && (nameTest == null ? other.nameTest == null : nameTest.equals(other.nameTest));
        }
        return false;
    }
}
