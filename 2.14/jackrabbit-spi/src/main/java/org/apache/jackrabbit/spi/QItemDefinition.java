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
package org.apache.jackrabbit.spi;

import javax.jcr.nodetype.ItemDefinition;

/**
 * <code>QItemDefinition</code> is the SPI representation of
 * an {@link ItemDefinition item definition}. It refers to <code>Name</code>s
 * only and is thus isolated from session-specific namespace mappings.
 *
 * @see javax.jcr.nodetype.ItemDefinition
 */
public interface QItemDefinition {

    /**
     * Empty array of <code>QItemDefinition</code>.
     */
    public static final QItemDefinition[] EMPTY_ARRAY = new QItemDefinition[0];

    /**
     * Gets the name of the child item.
     *
     * @return the name of the child item.
     */
    public Name getName();

    /**
     * Gets the name of the declaring node type.
     *
     * @return the name of the declaring node type.
     */
    public Name getDeclaringNodeType();

    /**
     * Determines whether the item is 'autoCreated'.
     *
     * @return the 'autoCreated' flag.
     */
    public boolean isAutoCreated();

    /**
     * Gets the 'onParentVersion' attribute of the item.
     *
     * @return the 'onParentVersion' attribute.
     */
    public int getOnParentVersion();

    /**
     * Determines whether the item is 'protected'.
     *
     * @return the 'protected' flag.
     */
    public boolean isProtected();

    /**
     * Determines whether the item is 'mandatory'.
     *
     * @return the 'mandatory' flag.
     */
    public boolean isMandatory();

    /**
     * Determines whether this item definition defines a residual set of
     * child items.
     *
     * @return <code>true</code> if this definition defines a residual set;
     *         <code>false</code> otherwise.
     */
    public boolean definesResidual();

    /**
     * Determines whether this item definition defines a node.
     *
     * @return <code>true</code> if this is a node definition;
     *         <code>false</code> otherwise (i.e. it is a property definition).
     */
    public boolean definesNode();
}
