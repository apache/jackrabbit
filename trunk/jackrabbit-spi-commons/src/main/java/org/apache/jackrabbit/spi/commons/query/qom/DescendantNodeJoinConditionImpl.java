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

import javax.jcr.query.qom.DescendantNodeJoinCondition;

import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.Name;

/**
 * <code>DescendantNodeJoinConditionImpl</code>...
 */
public class DescendantNodeJoinConditionImpl
        extends JoinConditionImpl
        implements DescendantNodeJoinCondition {

    /**
     * Name of the descendant selector.
     */
    private final Name descendantSelectorName;

    /**
     * Name of the ancestor selector.
     */
    private final Name ancestorSelectorName;

    DescendantNodeJoinConditionImpl(NamePathResolver resolver,
                                    Name descendantSelectorName,
                                    Name ancestorSelectorName) {
        super(resolver);
        this.descendantSelectorName = descendantSelectorName;
        this.ancestorSelectorName = ancestorSelectorName;
    }

    /**
     * Gets the name of the descendant selector.
     *
     * @return the selector name; non-null
     */
    public String getDescendantSelectorName() {
        return getJCRName(descendantSelectorName);
    }

    /**
     * Gets the name of the ancestor selector.
     *
     * @return the selector name; non-null
     */
    public String getAncestorSelectorName() {
        return getJCRName(ancestorSelectorName);
    }

    /**
     * Gets the name of the descendant selector.
     *
     * @return the selector name; non-null
     */
    public Name getDescendantSelectorQName() {
        return descendantSelectorName;
    }

    /**
     * Gets the name of the ancestor selector.
     *
     * @return the selector name; non-null
     */
    public Name getAncestorSelectorQName() {
        return ancestorSelectorName;
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
        String descendant = getDescendantSelectorName();
        String ancestor = getAncestorSelectorName();
        return "ISDESCENDANTNODE(" + descendant + ", " + ancestor + ")";
    }

}
