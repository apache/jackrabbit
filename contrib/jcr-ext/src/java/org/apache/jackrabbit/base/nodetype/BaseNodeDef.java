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
package org.apache.jackrabbit.base.nodetype;

import javax.jcr.nodetype.NodeDef;
import javax.jcr.nodetype.NodeType;

/**
 * TODO
 */
public class BaseNodeDef extends BaseItemDef implements NodeDef {

    /** {@inheritDoc} */
    public NodeType[] getRequiredPrimaryTypes() {
        return new NodeType[0];
    }

    /** {@inheritDoc} */
    public NodeType getDefaultPrimaryType() {
        return null;
    }

    /** {@inheritDoc} */
    public boolean allowSameNameSibs() {
        return false;
    }

}
