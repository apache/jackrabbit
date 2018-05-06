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

import javax.jcr.query.qom.FullTextSearch;
import javax.jcr.query.qom.StaticOperand;

import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;

/**
 * <code>FullTextSearchImpl</code>...
 */
public class FullTextSearchImpl
        extends ConstraintImpl
        implements FullTextSearch {

    /**
     * Name of the selector against which to apply this constraint
     */
    private final Name selectorName;

    /**
     * Name of the property.
     */
    private final Name propertyName;

    /**
     * Full text search expression.
     */
    private final StaticOperand fullTextSearchExpression;

    FullTextSearchImpl(NamePathResolver resolver,
                       Name selectorName,
                       Name propertyName,
                       StaticOperand fullTextSearchExpression) {
        super(resolver);
        this.selectorName = selectorName;
        this.propertyName = propertyName;
        this.fullTextSearchExpression = fullTextSearchExpression;
    }

    /**
     * Gets the name of the selector against which to apply this constraint.
     *
     * @return the selector name; non-null
     */
    public Name getSelectorQName() {
        return selectorName;
    }

    /**
     * Gets the name of the property.
     *
     * @return the property name if the full-text search scope is a property,
     *         otherwise null if the full-text search scope is the node (or node
     *         subtree, in some implementations).
     */
    public Name getPropertyQName() {
        return propertyName;
    }

    //--------------------------< FullTextSearch >------------------------------

    /**
     * Gets the name of the selector against which to apply this constraint.
     *
     * @return the selector name; non-null
     */
    public String getSelectorName() {
        return getJCRName(selectorName);
    }

    /**
     * Gets the name of the property.
     *
     * @return the property name if the full-text search scope is a property,
     *         otherwise null if the full-text search scope is the node (or node
     *         subtree, in some implementations).
     */
    public String getPropertyName() {
        return getJCRName(propertyName);
    }

    /**
     * Gets the full-text search expression.
     *
     * @return the full-text search expression; non-null
     */
    public StaticOperand getFullTextSearchExpression() {
        return fullTextSearchExpression;
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
        StringBuilder builder = new StringBuilder();
        builder.append("CONTAINS(");
        builder.append(getSelectorName());
        if (propertyName != null) {
            builder.append(".");
            builder.append(quote(propertyName));
            builder.append(", ");
        } else {
            builder.append(".*, ");
        }
        builder.append(getFullTextSearchExpression());
        builder.append(")");
        return builder.toString();
    }

}
