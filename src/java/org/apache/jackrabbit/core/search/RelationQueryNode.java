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

import javax.jcr.util.ISO8601;
import java.util.Date;
import java.util.Calendar;
import java.util.TimeZone;

/**
 * Implements a query node that defines property value relation.
 *
 * @author Marcel Reutegger
 * @version $Revision:  $, $Date:  $
 */
public class RelationQueryNode extends QueryNode implements Constants {

    /** The name of the property */
    private String property;

    /**
     * The <code>long</code> value of the relation if this is a query is of type
     * <code>long</code>
     */
    private long valueLong;

    /**
     * The <code>double</code> value of the relation if this is a query is of
     * type <code>double</code>
     */
    private double valueDouble;

    /**
     * The <code>String</code> value of the relation if this is a query is of
     * type <code>String</code>
     */
    private String valueString;

    /**
     * The <code>Date</code> value of the relation if this is a query is of type
     * <code>Date</code>
     */
    private Date valueDate;

    /**
     * The operation type of this relation. One of {@link #OPERATION_EQ},
     * {@link #OPERATION_GE}, {@link #OPERATION_GT}, {@link #OPERATION_LE},
     * {@link #OPERATION_LIKE}, {@link #OPERATION_LT}, {@link #OPERATION_NE}.
     */
    private int operation;

    /**
     * The value type of this relation. One of {@link #TYPE_DATE}, {@link
     * #TYPE_DOUBLE}, {@link #TYPE_LONG}, {@link #TYPE_STRING}.
     */
    private int type;

    /**
     * Creates a new <code>RelationQueryNode</code> with a <code>long</code>
     * <code>value</code> and an <code>operation</code> type.
     *
     * @param parent    the parent node for this query node.
     * @param property  the name of a property.
     * @param value     a property value
     * @param operation the type of the relation.
     */
    public RelationQueryNode(QueryNode parent, String property, long value, int operation) {
	super(parent);
	this.property = property;
	this.valueLong = value;
	this.operation = operation;
	this.type = TYPE_LONG;
    }

    /**
     * Creates a new <code>RelationQueryNode</code> with a <code>double</code>
     * <code>value</code> and an <code>operation</code> type.
     *
     * @param parent    the parent node for this query node.
     * @param property  the name of a property.
     * @param value     a property value
     * @param operation the type of the relation.
     */
    public RelationQueryNode(QueryNode parent, String property, double value, int operation) {
	super(parent);
	this.property = property;
	this.valueDouble = value;
	this.operation = operation;
	this.type = TYPE_DOUBLE;
    }

    /**
     * Creates a new <code>RelationQueryNode</code> with a <code>Date</code>
     * <code>value</code> and an <code>operation</code> type.
     *
     * @param parent    the parent node for this query node.
     * @param property  the name of a property.
     * @param value     a property value
     * @param operation the type of the relation.
     */
    public RelationQueryNode(QueryNode parent, String property, Date value, int operation) {
	super(parent);
	this.property = property;
	this.valueDate = value;
	this.operation = operation;
	this.type = TYPE_DATE;
    }

    /**
     * Creates a new <code>RelationQueryNode</code> with a <code>String</code>
     * <code>value</code> and an <code>operation</code> type.
     *
     * @param parent    the parent node for this query node.
     * @param property  the name of a property.
     * @param value     a property value
     * @param operation the type of the relation.
     */
    public RelationQueryNode(QueryNode parent, String property, String value, int operation) {
	super(parent);
	this.property = property;
	this.valueString = value;
	this.operation = operation;
	this.type = TYPE_STRING;
    }

    /**
     * @see QueryNode#accept(org.apache.jackrabbit.core.search.QueryNodeVisitor, java.lang.Object)
     */
    public Object accept(QueryNodeVisitor visitor, Object data) {
	return visitor.visit(this, data);
    }

    /**
     * Returns the type of the value.
     * @return the type of the value.
     */
    public int getType() {
	return type;
    }

    /**
     * Returns the name of the property in this relation query node.
     * @return the name of the property in this relation query node.
     */
    public String getProperty() {
	return property;
    }

    /**
     * Returns the <code>long</code> value if this relation if of type
     * {@link #TYPE_LONG}.
     *
     * @return the <code>long</code> value.
     */
    public long getLongValue() {
	return valueLong;
    }

    /**
     * Returns the <code>double</code> value if this relation if of type
     * {@link #TYPE_DOUBLE}.
     *
     * @return the <code>double</code> value.
     */
    public double getDoubleValue() {
	return valueDouble;
    }

    /**
     * Returns the <code>String</code> value if this relation if of type
     * {@link #TYPE_STRING}.
     *
     * @return the <code>String</code> value.
     */
    public String getStringValue() {
	return valueString;
    }

    /**
     * Returns the <code>Date</code> value if this relation if of type
     * {@link #TYPE_DATE}.
     *
     * @return the <code>Date</code> value.
     */
    public Date getDateValue() {
	return valueDate;
    }

    /**
     * Returns the operation type.
     * @return the operation type.
     */
    public int getOperation() {
	return operation;
    }

    /**
     * Returns a JCRQL representation for this query node.
     * @return a JCRQL representation for this query node.
     */
    public String toJCRQLString() {
	StringBuffer sb = new StringBuffer();
	if (property.indexOf(' ') > -1) {
	    sb.append("\"" + property + "\"");
	} else {
	    sb.append(property);
	}

	if (operation == OPERATION_EQ) {
	    sb.append("=");
	} else if (operation == OPERATION_GE) {
	    sb.append(">=");
	} else if (operation == OPERATION_GT) {
	    sb.append(">");
	} else if (operation == OPERATION_LE) {
	    sb.append("<=");
	} else if (operation == OPERATION_LIKE) {
	    sb.append(" LIKE ");
	} else if (operation == OPERATION_LT) {
	    sb.append("<");
	} else if (operation == OPERATION_NE) {
	    sb.append("<>");
	} else {
	    throw new RuntimeException("invalid operation: " + operation);
	}


	if (type == TYPE_LONG) {
	    sb.append(valueLong);
	} else if (type == TYPE_DOUBLE) {
	    sb.append(valueDouble);
	} else if (type == TYPE_STRING) {
	    sb.append("\"").append(valueString).append("\"");
	} else if (type == TYPE_DATE) {
	    Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
	    cal.setTime(valueDate);
	    sb.append(ISO8601.format(cal));
	} else {
	   throw new RuntimeException("Invalid type: " + type);
	}
	return sb.toString();
    }

    /**
     * Returns an XPath representation for this query node.
     * @return an XPath representation for this query node.
     */
    public String toXPathString() {
	// todo implement
	return "";
    }
}
