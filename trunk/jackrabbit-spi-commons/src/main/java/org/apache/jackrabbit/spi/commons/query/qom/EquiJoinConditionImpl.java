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

import javax.jcr.query.qom.EquiJoinCondition;

import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;

/**
 * <code>EquiJoinConditionImpl</code>...
 */
public class EquiJoinConditionImpl
        extends JoinConditionImpl
        implements EquiJoinCondition {

    /**
     * Name of the first selector.
     */
    private final Name selector1Name;

    /**
     * Property name in the first selector.
     */
    private final Name property1Name;

    /**
     * Name of the second selector.
     */
    private final Name selector2Name;

    /**
     * Property name in the second selector.
     */
    private final Name property2Name;

    EquiJoinConditionImpl(NamePathResolver resolver,
                          Name selector1Name,
                          Name property1Name,
                          Name selector2Name,
                          Name property2Name) {
        super(resolver);
        this.selector1Name = selector1Name;
        this.property1Name = property1Name;
        this.selector2Name = selector2Name;
        this.property2Name = property2Name;
    }

    /**
     * Gets the name of the first selector.
     *
     * @return the selector name; non-null
     */
    public String getSelector1Name() {
        return getJCRName(selector1Name);
    }

    /**
     * Gets the property name in the first selector.
     *
     * @return the property name; non-null
     */
    public String getProperty1Name() {
        return getJCRName(property1Name);
    }

    /**
     * Gets the name of the second selector.
     *
     * @return the selector name; non-null
     */
    public String getSelector2Name() {
        return getJCRName(selector2Name);
    }

    /**
     * Gets the property name in the second selector.
     *
     * @return the property name; non-null
     */
    public String getProperty2Name() {
        return getJCRName(property2Name);
    }

    /**
     * Gets the name of the first selector.
     *
     * @return the selector name; non-null
     */
    public Name getSelector1QName() {
        return selector1Name;
    }

    /**
     * Gets the name of the second selector.
     *
     * @return the selector name; non-null
     */
    public Name getSelector2QName() {
        return selector2Name;
    }

    /**
     * Gets the property name in the first selector.
     *
     * @return the property name; non-null
     */
    public Name getProperty1QName() {
        return property1Name;
    }

    /**
     * Gets the property name in the second selector.
     *
     * @return the property name; non-null
     */
    public Name getProperty2QName() {
        return property2Name;
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
        return getSelector1Name() + "." + quote(getProperty1QName())
            + " = " + getSelector2Name() + "." + quote(getProperty2QName());
    }

}
