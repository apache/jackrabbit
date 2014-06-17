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
package org.apache.jackrabbit.spi.commons.query.qom;

import javax.jcr.query.qom.Column;

import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;

/**
 * <code>ColumnImpl</code>...
 */
public class ColumnImpl extends AbstractQOMNode implements Column {

    /**
     * Empty <code>ColumnImpl</code> array.
     */
    public static final ColumnImpl[] EMPTY_ARRAY = new ColumnImpl[0];

    /**
     * The name of the selector.
     */
    private final Name selectorName;

    /**
     * The name of the property.
     */
    private final Name propertyName;

    /**
     * The name of the column.
     */
    private final String columnName;

    ColumnImpl(NamePathResolver resolver,
               Name selectorName,
               Name propertyName,
               String columnName) {
        super(resolver);
        this.selectorName = selectorName;
        this.propertyName = propertyName;
        this.columnName = columnName;
    }

    /**
     * Gets the name of the selector.
     *
     * @return the selector name; non-null
     */
    public Name getSelectorQName() {
        return selectorName;
    }

    /**
     * Gets the name of the property.
     *
     * @return the property name, or null to include a column for each
     *         single-value non-residual property of the selector's node type
     */
    public Name getPropertyQName() {
        return propertyName;
    }

    //---------------------------< Column >-------------------------------------

    /**
     * Gets the name of the selector.
     *
     * @return the selector name; non-null
     */
    public String getSelectorName() {
        return getJCRName(selectorName);
    }

    /**
     * Gets the name of the property.
     *
     * @return the property name, or null to include a column for each
     *         single-value non-residual property of the selector's node type
     */
    public String getPropertyName() {
        return getJCRName(propertyName);
    }

    /**
     * Gets the column name.
     * <p>
     *
     * @return the column name; must be null if <code>getPropertyName</code> is
     *         null and non-null otherwise
     */
    public String getColumnName() {
        return columnName;
    }

    //------------------------< AbstractQOMNode >-------------------------------

    /**
     * Accepts a <code>visitor</code> and calls the appropriate visit method
     * depending on the type of this QOM node.
     *
     * @param visitor the visitor.
     */
    public Object accept(QOMTreeVisitor visitor, Object data) throws Exception {
        return visitor.visit(this, data);
    }

    //------------------------< Object >----------------------------------------

    public String toString() {
        if (propertyName != null) {
            return getSelectorName() + "." + getPropertyName()
                + " AS " + getColumnName();
        } else {
            return getSelectorName() + ".*";
        }
    }

}
