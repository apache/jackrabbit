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

import javax.jcr.query.qom.SameNodeJoinCondition;

import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;

/**
 * <code>SameNodeJoinConditionImpl</code>...
 */
public class SameNodeJoinConditionImpl
        extends JoinConditionImpl
        implements SameNodeJoinCondition {

    /**
     * The name of the first selector.
     */
    private final Name selector1Name;

    /**
     * The name of the second selector.
     */
    private final Name selector2Name;

    /**
     * The path relative to the second selector.
     */
    private final Path selector2Path;

    SameNodeJoinConditionImpl(NamePathResolver resolver,
                              Name selector1Name,
                              Name selector2Name,
                              Path selector2Path) {
        super(resolver);
        this.selector1Name = selector1Name;
        this.selector2Name = selector2Name;
        this.selector2Path = selector2Path;
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
     * Gets the name of the second selector.
     *
     * @return the selector name; non-null
     */
    public String getSelector2Name() {
        return getJCRName(selector2Name);
    }

    /**
     * Gets the path relative to the second selector.
     *
     * @return the relative path, or null for none
     */
    public String getSelector2Path() {
        return getJCRPath(selector2Path);
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
     * Gets the path relative to the second selector.
     *
     * @return the relative path, or null for none
     */
    public Path getSelector2QPath() {
        return selector2Path;
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
        builder.append("ISSAMENODE(");
        builder.append(getSelector1Name());
        builder.append(", ");
        builder.append(getSelector2Name());
        if (selector2Path != null) {
            builder.append(", ");
            builder.append(quote(selector2Path));
        }
        builder.append(")");
        return builder.toString();
    }


}
