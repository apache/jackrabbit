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

import java.util.Date;

/**
 * Implements a query node that defines a range query.
 * The following data types are supported:
 * <ul>
 * <li><code>long</code></li>
 * <li><code>double</code></li>
 * <li><code>String</code></li>
 * <li><code>Date</code></li>
 * </ul>
 * @todo remove this class. not used anymore
 */
public class RangeQueryNode extends QueryNode implements QueryConstants {

    /**
     * Range array for long boundary values
     */
    private long[] rangeLong;

    /**
     * Range array for double boundary values
     */
    private double[] rangeDouble;

    /**
     * Range array for String boundary values
     */
    private String[] rangeString;

    /**
     * Range array for Date boundary values
     */
    private Date[] rangeDate;

    /**
     * The data type of this <code>RangeQueryNode</code>.
     */
    private final int type;

    /**
     * Creates a new range query with <code>long</code> values.
     *
     * @param parent the parent node of this query node.
     * @param lower  lower boundary value.
     * @param upper  upper boundary value.
     */
    public RangeQueryNode(QueryNode parent, long lower, long upper) {
        super(parent);
        rangeLong = new long[]{
            lower, upper
        };
        type = TYPE_LONG;
    }

    /**
     * Creates a new range query with <code>double</code> values.
     *
     * @param parent the parent node of this query node.
     * @param lower  lower boundary value.
     * @param upper  upper boundary value.
     */
    public RangeQueryNode(QueryNode parent, double lower, double upper) {
        super(parent);
        rangeDouble = new double[]{lower, upper};
        type = TYPE_DOUBLE;
    }

    /**
     * Creates a new range query with <code>String</code> values.
     *
     * @param parent the parent node of this query node.
     * @param lower  lower boundary value.
     * @param upper  upper boundary value.
     */
    public RangeQueryNode(QueryNode parent, String lower, String upper) {
        super(parent);
        rangeString = new String[]{lower, upper};
        type = TYPE_STRING;
    }

    /**
     * Creates a new range query with <code>Date</code> values.
     *
     * @param parent the parent node of this query node.
     * @param lower  lower boundary value.
     * @param upper  upper boundary value.
     */
    public RangeQueryNode(QueryNode parent, Date lower, Date upper) {
        super(parent);
        rangeDate = new Date[]{lower, upper};
        type = TYPE_DATE;
    }

    /**
     * @see QueryNode#accept(org.apache.jackrabbit.core.search.QueryNodeVisitor, java.lang.Object)
     */
    public Object accept(QueryNodeVisitor visitor, Object data) {
        return visitor.visit(this, data);
    }

    /**
     * Returns a value array with <code>long</code> boundaries. If this query
     * node is not of type <code>long</code>, <code>null</code> is returned.
     *
     * @return a value array with <code>long</code> boundaries.
     */
    public long[] getLongRange() {
        return rangeLong;
    }

    /**
     * Returns a value array with <code>double</code> boundaries. If this query
     * node is not of type <code>double</code>, <code>null</code> is returned.
     *
     * @return a value array with <code>double</code> boundaries.
     */
    public double[] getDoubleRange() {
        return rangeDouble;
    }

    /**
     * Returns a value array with <code>String</code> boundaries. If this query
     * node is not of type <code>String</code>, <code>null</code> is returned.
     *
     * @return a value array with <code>String</code> boundaries.
     */
    public String[] getStringRange() {
        return rangeString;
    }

    /**
     * Returns a value array with <code>Date</code> boundaries. If this query
     * node is not of type <code>Date</code>, <code>null</code> is returned.
     *
     * @return a value array with <code>Date</code> boundaries.
     */
    public Date[] getDateRange() {
        return rangeDate;
    }

    /**
     * Returns the type of this <code>RangeQueryNode</code>. One of: {@link
     * #TYPE_DATE}, {@link #TYPE_DOUBLE}, {@link #TYPE_LONG}, {@link
     * #TYPE_STRING}
     *
     * @return the type of this <code>RangeQueryNode</code>.
     */
    public int getType() {
        return type;
    }

}
