/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
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
package org.apache.jackrabbit.core.nodetype;

import org.apache.jackrabbit.core.QName;
import org.apache.jackrabbit.core.Constants;

/**
 * This interface defines the base for item definitions.
 */
public interface ItemDef  {

    /**
     * '*' denoting residual child item definition
     */
    static final QName ANY_NAME = new QName(Constants.NS_DEFAULT_URI, "*");

    /**
     * Returns the name of this item def.
     * @return the name of this item def.
     */
    QName getName();

    /**
     * Returns the name of the declaring node type.
     * @return the name of the declaring node type.
     */
    QName getDeclaringNodeType();

    /**
     * Returns the 'autoCreated' flag.
     * @return the 'autoCreated' flag.
     */
    boolean isAutoCreated();

    /**
     * Returns the 'onParentVersion' attribute.
     * @return the 'onParentVersion' attribute.
     */
    int getOnParentVersion();

    /**
     * Returns the 'protected' flag.
     * @return the 'protected' flag.
     */
    boolean isProtected();

    /**
     * Returns the 'mandatory' flag.
     * @return the 'mandatory' flag.
     */
    boolean isMandatory();

    /**
     * Checks if this item definition is a residual definition.
     * @return <code>true</code> if this is a residual definition;
     *         <code>false</code> otherwise.
     */
    boolean definesResidual();

    /**
     * Checks if this is a node definition.
     * @return <code>true</code> if this is a node definition;
     *         <code>false</code> otherwise, i.e. this is a property definition.
     */
    boolean definesNode();
}
