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
package org.apache.jackrabbit.core.query.qom;

import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.name.NamePathResolver;

import org.apache.jackrabbit.core.query.jsr283.qom.PropertyExistence;

/**
 * <code>PropertyExistenceImpl</code>...
 */
public class PropertyExistenceImpl
        extends ConstraintImpl
        implements PropertyExistence {

    /**
     * The name of the selector against which to apply this constraint.
     */
    private final QName selectorName;

    /**
     * The name of the property.
     */
    private final QName propertyName;

    PropertyExistenceImpl(NamePathResolver resolver,
                          QName selectorName,
                          QName propertyName) {
        super(resolver);
        this.selectorName = selectorName;
        this.propertyName = propertyName;
    }

    /**
     * Gets the name of the selector against which to apply this constraint.
     *
     * @return the selector name; non-null
     */
    public QName getSelectorQName() {
        return selectorName;
    }

    /**
     * Gets the name of the property.
     *
     * @return the property name; non-null
     */
    public QName getPropertyQName() {
        return propertyName;
    }

    //------------------------------< PropertyExistence >-----------------------

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
     * @return the property name; non-null
     */
    public String getPropertyName() {
        return getJCRName(propertyName);
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
}
