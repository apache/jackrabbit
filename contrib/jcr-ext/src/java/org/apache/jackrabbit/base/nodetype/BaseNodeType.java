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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.jcr.Value;
import javax.jcr.nodetype.ItemDef;
import javax.jcr.nodetype.NodeDef;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDef;

/**
 * TODO
 */
public class BaseNodeType implements NodeType {

    /** {@inheritDoc} */
    public String getName() {
        return null;
    }

    /** {@inheritDoc} */
    public boolean isMixin() {
        return false;
    }

    /** {@inheritDoc} */
    public boolean hasOrderableChildNodes() {
        return false;
    }

    /** {@inheritDoc} */
    public String getPrimaryItemName() {
        return null;
    }

    /** {@inheritDoc} */
    public NodeType[] getSupertypes() {
        Set defs = new HashSet();

        NodeType[] types = getDeclaredSupertypes();
        for (int i = 0; i < types.length; i++) {
            defs.addAll(Arrays.asList(types[i].getSupertypes()));
        }
        defs.addAll(Arrays.asList(types));

        return (NodeType[]) defs.toArray(new NodeType[0]);
    }

    /** {@inheritDoc} */
    public NodeType[] getDeclaredSupertypes() {
        return new NodeType[0];
    }

    /** {@inheritDoc} */
    public boolean isNodeType(String nodeTypeName) {
        if (nodeTypeName.equals(getName())) {
            return true;
        } else {
            NodeType[] types = getSupertypes();
            for (int i = 0; i < types.length; i++) {
                if (nodeTypeName.equals(types[i].getName())) {
                    return true;
                }
            }
            return false;
        }
    }

    /** {@inheritDoc} */
    public PropertyDef[] getPropertyDefs() {
        Set defs = new HashSet();

        NodeType[] types = getSupertypes();
        for (int i = 0; i < types.length; i++) {
            defs.addAll(Arrays.asList(types[i].getPropertyDefs()));
        }
        defs.addAll(Arrays.asList(getDeclaredPropertyDefs()));

        return (PropertyDef[]) defs.toArray(new PropertyDef[0]);
    }

    /** {@inheritDoc} */
    public PropertyDef[] getDeclaredPropertyDefs() {
        return new PropertyDef[0];
    }

    /** {@inheritDoc} */
    public NodeDef[] getChildNodeDefs() {
        Set defs = new HashSet();

        NodeType[] types = getSupertypes();
        for (int i = 0; i < types.length; i++) {
            defs.addAll(Arrays.asList(types[i].getChildNodeDefs()));
        }
        defs.addAll(Arrays.asList(getDeclaredChildNodeDefs()));

        return (NodeDef[]) defs.toArray(new NodeDef[0]);
    }

    /** {@inheritDoc} */
    public NodeDef[] getDeclaredChildNodeDefs() {
        return new NodeDef[0];
    }

    protected PropertyDef getPropertyDef(String propertyName) {
        PropertyDef[] defs = getPropertyDefs();
        for (int i = 0; i < defs.length; i++) {
            if (propertyName.equals(defs[i].getName())) {
                return defs[i];
            }
        }
        return null;
    }

    /** {@inheritDoc} */
    public boolean canSetProperty(String propertyName, Value value) {
        PropertyDef def = getPropertyDef(propertyName);
        if (def == null) {
            def = getPropertyDef("*");
        }
        if (def == null || def.isMultiple()) {
            return false;
        } else {
            return true; // TODO check constraints!
        }
    }

    /** {@inheritDoc} */
    public boolean canSetProperty(String propertyName, Value[] values) {
        PropertyDef def = getPropertyDef(propertyName);
        if (def == null) {
            def = getPropertyDef("*");
        }
        if (def == null || !def.isMultiple()) {
            return false;
        } else {
            return true; // TODO check constraints!
        }
    }

    protected NodeDef getChildNodeDef(String childNodeName) {
        NodeDef[] defs = getChildNodeDefs();
        for (int i = 0; i < defs.length; i++) {
            if (childNodeName.equals(defs[i].getName())) {
                return defs[i];
            }
        }
        return null;
    }

    /** {@inheritDoc} */
    public boolean canAddChildNode(String childNodeName) {
        NodeDef def = getChildNodeDef(childNodeName);
        if (def == null) {
            def = getChildNodeDef("*");
        }
        return def != null;
    }

    /** {@inheritDoc} */
    public boolean canAddChildNode(String childNodeName, String nodeTypeName) {
        NodeDef def = getChildNodeDef(childNodeName);
        if (def == null) {
            def = getChildNodeDef("*");
        }
        if (def == null) {
            return false;
        }

        NodeType[] types = def.getRequiredPrimaryTypes();
        for (int i = 0; i < types.length; i++) {
            if (types[i].isNodeType(nodeTypeName)) {
                return true;
            }
        }
        return types.length == 0;
    }

    /** {@inheritDoc} */
    public boolean canRemoveItem(String itemName) {
        ItemDef def = getPropertyDef(itemName);
        if (def == null) {
            def = getChildNodeDef(itemName);
        }
        if (def == null) {
            return true;
        } else {
            return def.isMandatory();
        }
    }

}
