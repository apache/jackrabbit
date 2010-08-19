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

import javax.jcr.query.qom.ChildNodeJoinCondition;

import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.Name;

/**
 * <code>ChildNodeJoinConditionImpl</code>...
 */
public class ChildNodeJoinConditionImpl
        extends JoinConditionImpl
        implements ChildNodeJoinCondition {

    /**
     * The name of the child selector.
     */
    private final Name childSelectorName;

    /**
     * The name of the parent selector.
     */
    private final Name parentSelectorName;

    ChildNodeJoinConditionImpl(NamePathResolver resolver,
                               Name childSelectorName,
                               Name parentSelectorName) {
        super(resolver);
        this.childSelectorName = childSelectorName;
        this.parentSelectorName = parentSelectorName;
    }

    /**
     * Gets the name of the child selector.
     *
     * @return the selector name; non-null
     */
    public String getChildSelectorName() {
        return getJCRName(childSelectorName);
    }

    /**
     * Gets the name of the parent selector.
     *
     * @return the selector name; non-null
     */
    public String getParentSelectorName() {
        return getJCRName(parentSelectorName);
    }

    /**
     * Gets the name of the child selector.
     *
     * @return the selector name; non-null
     */
    public Name getChildSelectorQName() {
        return childSelectorName;
    }

    /**
     * Gets the name of the parent selector.
     *
     * @return the selector name; non-null
     */
    public Name getParentSelectorQName() {
        return parentSelectorName;
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
        String child = getChildSelectorName();
        String parent = getParentSelectorName();
        return "ISCHILDNODE(" + child + ", " + parent + ")";
    }

}
