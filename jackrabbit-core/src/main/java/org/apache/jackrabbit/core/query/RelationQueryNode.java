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

import java.util.Date;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.conversion.MalformedPathException;
import org.apache.jackrabbit.name.NameFactoryImpl;
import org.apache.jackrabbit.name.PathBuilder;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;

/**
 * Implements a query node that defines property value relation.
 */
public class RelationQueryNode extends NAryQueryNode implements QueryConstants {

    /**
     * Acts as an syntetic placeholder for a location step that matches any
     * name. This is required becase a JCR path does not allow a Name with
     * a single '*' (star) character.
     */
    public static final Name STAR_NAME_TEST = NameFactoryImpl.getInstance().create(Name.NS_REP_URI, "__star__");

    /**
     * The relative path to the property.
     */
    private Path relPath;

    /**
     * If <code>true</code> this relation query node contains a value preceded
     * with an unary minus.
     */
    private boolean unaryMinus;

    /**
     * The <code>long</code> value of the relation if this is a query is of type
     * <code>long</code>
     */
    private long valueLong;

    /**
     * The <code>int</code> value of the position index.
     */
    private int valuePosition;

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
     * The operation type of this relation. One of the operation values defined
     * in {@link QueryConstants}.
     */
    private int operation;

    /**
     * The value type of this relation. One of {@link #TYPE_DATE}, {@link
     * #TYPE_DOUBLE}, {@link #TYPE_LONG}, {@link #TYPE_STRING}, {@link #TYPE_POSITION}.
     */
    private int type;

    /**
     * Creates a new <code>RelationQueryNode</code> without a type nor value
     * assigned.
     *
     * @param parent the parent node for this query node.
     */
    protected RelationQueryNode(QueryNode parent, int operation) {
        super(parent);
        this.operation = operation;
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
        return QueryNode.TYPE_RELATION;
    }

    /**
     * If <code>b</code> is <code>true</code> then the value in this relation
     * node contains a receding unary minus.
     *
     * @param b <code>true</code> if this relation contains a unary minus.
     */
    public void setUnaryMinus(boolean b) {
        unaryMinus = b;
    }

    /**
     * Returns the type of the value.
     *
     * @return the type of the value.
     */
    public int getValueType() {
        return type;
    }

    /**
     * Returns the name of the property in this relation query node. Please
     * note that this method does not return the full relative path that
     * reference the property to match, but only the name of the final name
     * element of the path returned by {@link #getRelativePath()}.
     *
     * @return the name of the property in this relation query node.
     * @deprecated Use {@link #getRelativePath()} instead.
     */
    public Name getProperty() {
        return relPath == null ? null : relPath.getNameElement().getName();
    }

    /**
     * Sets a new property name for this relation query node.
     *
     * @param name the new property name.
     * @deprecated Use {@link #setRelativePath(Path)} instead.
     */
    public void setProperty(Name name) {
        PathBuilder builder = new PathBuilder();
        builder.addLast(name);
        try {
            this.relPath = builder.getPath();
        } catch (MalformedPathException e) {
            // path is always valid
        }
    }

    /**
     * @return the relative path that references the property in this relation.
     */
    public Path getRelativePath() {
        return relPath;
    }

    /**
     * Sets the relative path to the property in this relation.
     *
     * @param relPath the relative path to a property.
     * @throws IllegalArgumentException if <code>relPath</code> is absolute.
     */
    public void setRelativePath(Path relPath) {
        if (relPath != null && relPath.isAbsolute()) {
            throw new IllegalArgumentException("relPath must be relative");
        }
        this.relPath = relPath;
    }

    /**
     * Adds a path element to the existing relative path. To add a path element
     * which matches all node names use {@link #STAR_NAME_TEST}.
     *
     * @param element the path element to append.
     */
    public void addPathElement(Path.Element element) {
        PathBuilder builder = new PathBuilder();
        if (relPath != null) {
            builder.addAll(relPath.getElements());
        }
        builder.addLast(element);
        try {
            relPath = builder.getPath();
        } 
        catch (MalformedPathException e) {
            // path is always valid
        }
        // try to normalize the path
        try {
          relPath = relPath.getNormalizedPath();
        } catch (RepositoryException e) {
            // just keep the original in that case
        }
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
     * Sets a new value of type <code>long</code>.
     *
     * @param value the new value.
     */
    public void setLongValue(long value) {
        valueLong = unaryMinus ? -value : value;
        type = TYPE_LONG;
    }

    /**
     * Returns the <code>int</code> position index value if this relation is
     * of type {@link #TYPE_POSITION}.
     * @return the position index value.
     */
    public int getPositionValue() {
        return valuePosition;
    }

    /**
     * Sets a new value for the position index.
     *
     * @param value the new value.
     */
    public void setPositionValue(int value) {
        valuePosition = value;
        type = TYPE_POSITION;
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
     * Sets a new value of type <code>double</code>.
     *
     * @param value the new value.
     */
    public void setDoubleValue(double value) {
        valueDouble = unaryMinus ? -value : value;
        type = TYPE_DOUBLE;
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
     * Sets a new value of type <code>String</code>.
     *
     * @param value the new value.
     */
    public void setStringValue(String value) {
        valueString = value;
        type = TYPE_STRING;
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
     * Sets a new value of type <code>Date</code>.
     *
     * @param value the new value.
     */
    public void setDateValue(Date value) {
        valueDate = value;
        type = TYPE_DATE;
    }

    /**
     * Returns the operation type.
     *
     * @return the operation type.
     */
    public int getOperation() {
        return operation;
    }

    /**
     * @inheritDoc
     */
    public boolean equals(Object obj) {
        if (obj instanceof RelationQueryNode) {
            RelationQueryNode other = (RelationQueryNode) obj;
            return type == other.type
                    && (valueDate == null ? other.valueDate == null : valueDate.equals(other.valueDate))
                    && valueDouble == other.valueDouble
                    && valueLong == other.valueLong
                    && valuePosition == other.valuePosition
                    && (valueString == null ? other.valueString == null : valueString.equals(other.valueString))
                    && (relPath == null ? other.relPath== null : relPath.equals(other.relPath));
        }
        return false;
    }
}
