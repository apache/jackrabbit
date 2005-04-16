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

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeDef;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDef;

import org.apache.jackrabbit.base.nodetype.BaseNodeType;
import org.apache.jackrabbit.name.Name;

/**
 * TODO
 */
public class LiteNodeType extends BaseNodeType {

    private final Session session;

    private final Name name;

    private final NodeType[] supertypes;

    private final NodeDef[] childNodeDefs;

    private final PropertyDef[] propertyDefs;

    private final Name primaryItemName;

    private final boolean orderableChildNodes;

    private final boolean mixin;

    public LiteNodeType(
            Session session, Name name, NodeType[] supertypes,
            NodeDef[] childNodeDefs, PropertyDef[] propertyDefs,
            Name primaryItemName, boolean orderableChildNodes, boolean mixin) {
        this.session = session;
        this.name = name;
        this.supertypes = supertypes;
        this.childNodeDefs = childNodeDefs;
        this.propertyDefs = propertyDefs;
        this.primaryItemName = primaryItemName;
        this.orderableChildNodes = orderableChildNodes;
        this.mixin = mixin;
    }

    public String getName() {
        try {
            return name.toJCRName(session);
        } catch (RepositoryException e) {
            throw new IllegalStateException(e.getMessage());
        }
    }

    public NodeType[] getDeclaredSupertypes() {
        NodeType[] types = new NodeType[supertypes.length];
        for (int i = 0; i < supertypes.length; i++) {
            types[i] = supertypes[i];
        }
        return types;
    }

    public NodeDef[] getDeclaredChildNodeDefs() {
        NodeDef[] defs = new NodeDef[childNodeDefs.length];
        for (int i = 0; i < childNodeDefs.length; i++) {
            defs[i] = childNodeDefs[i];
        }
        return defs;
    }

    public PropertyDef[] getDeclaredPropertyDefs() {
        PropertyDef[] defs = new PropertyDef[propertyDefs.length];
        for (int i = 0; i < propertyDefs.length; i++) {
            defs[i] = propertyDefs[i];
        }
        return defs;
    }

    public String getPrimaryItemName() {
        try {
            return primaryItemName.toJCRName(session);
        } catch (RepositoryException e) {
            throw new IllegalStateException(e.getMessage());
        }
    }

    public boolean hasOrderableChildNodes() {
        return orderableChildNodes;
    }

    public boolean isMixin() {
        return mixin;
    }

}
