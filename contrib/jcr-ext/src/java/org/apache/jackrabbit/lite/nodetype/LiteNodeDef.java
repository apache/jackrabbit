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
package org.apache.jackrabbit.lite.nodetype;

import javax.jcr.Session;
import javax.jcr.nodetype.NodeDef;
import javax.jcr.nodetype.NodeType;

import org.apache.jackrabbit.name.Name;

/**
 * TODO
 */
public class LiteNodeDef extends LiteItemDef implements NodeDef {

    public LiteNodeDef(
            Session session, Name name, NodeType declaringNodeType,
            int onParentVersion, boolean autoCreate,
            boolean mandatory, boolean isProtected) {
        super(session, name, declaringNodeType,
                onParentVersion, autoCreate, mandatory, isProtected);
    }

    /** {@inheritDoc} */
    public NodeType[] getRequiredPrimaryTypes() {
        // TODO Auto-generated method stub
        return null;
    }

    /** {@inheritDoc} */
    public NodeType getDefaultPrimaryType() {
        // TODO Auto-generated method stub
        return null;
    }

    /** {@inheritDoc} */
    public boolean allowSameNameSibs() {
        // TODO Auto-generated method stub
        return false;
    }

}
