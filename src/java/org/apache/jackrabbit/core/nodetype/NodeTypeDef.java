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

import javax.jcr.PropertyType;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.Arrays;

/**
 * A <code>NodeTypeDef</code> holds the definition of a node type.
 */
public class NodeTypeDef implements Cloneable {

    private QName name;
    private QName[] supertypes;
    private boolean mixin;
    private boolean orderableChildNodes;
    private PropDef[] propDefs;
    private ChildNodeDef[] nodeDefs;
    private Set dependencies;

    /**
     * Default constructor.
     */
    public NodeTypeDef() {
        dependencies = null;
        name = null;
        nodeDefs = new ChildNodeDef[0];
        propDefs = new PropDef[0];
        supertypes = new QName[0];
        mixin = false;
        orderableChildNodes = false;
    }

    public Object clone() throws CloneNotSupportedException {
        // create a shallow copy
        NodeTypeDef clone = (NodeTypeDef) super.clone();
        // clear dependencies (will be lazily built)
        clone.resetDependencies();
        return clone;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof NodeTypeDef) {
            NodeTypeDef other = (NodeTypeDef) obj;
            return (name == null ? other.name == null : name.equals(other.name))
                    && Arrays.equals(supertypes, other.supertypes)
                    && mixin == other.mixin
                    && orderableChildNodes == other.orderableChildNodes
                    && Arrays.equals(propDefs, other.propDefs)
                    && Arrays.equals(nodeDefs, other.nodeDefs);
        }
        return false;
    }

    /**
     * Returns a collection of node type <code>QName</code>s that are being
     * referenced by <i>this</i> node type definition (e.g. as supertypes, as
     * required/default primary types in child node definitions, as REFERENCE
     * value constraints in property definitions).
     * <p/>
     * Note that self-references (e.g. a child node definition that specifies
     * the declaring node type as the default primary type) are not considered 
     * dependencies.
     *
     * @return a collection of node type <code>QName</code>s
     */
    public Collection getDependencies() {
        if (dependencies == null) {
            dependencies = new HashSet();
            // supertypes
            for (int i = 0; i < supertypes.length; i++) {
                dependencies.add(supertypes[i]);
            }
            // child node definitions
            for (int i = 0; i < nodeDefs.length; i++) {
                // default primary type
                QName ntName = nodeDefs[i].getDefaultPrimaryType();
                if (ntName != null && !name.equals(ntName)) {
                    dependencies.add(ntName);
                }
                // required primary type
                QName[] ntNames = nodeDefs[i].getRequiredPrimaryTypes();
                for (int j = 0; j < ntNames.length; j++) {
                    if (ntNames[j] != null && !name.equals(ntNames[j])) {
                        dependencies.add(ntNames[j]);
                    }
                }
            }
            // property definitions
            for (int i = 0; i < propDefs.length; i++) {
                // REFERENCE value constraints
                if (propDefs[i].getRequiredType() == PropertyType.REFERENCE) {
                    ValueConstraint[] ca = propDefs[i].getValueConstraints();
                    if (ca != null) {
                        for (int j = 0; j < ca.length; j++) {
                            ReferenceConstraint rc = (ReferenceConstraint) ca[j];
                            if (!name.equals(rc.getNodeTypeName())) {
                                dependencies.add(rc.getNodeTypeName());
                            }
                        }
                    }
                }
            }
        }
        return dependencies;
    }

    private void resetDependencies() {
        dependencies = null;
    }

    /**
     * Sets the name of the node type being defined.
     *
     * @param name The name of the node type.
     */
    public void setName(QName name) {
        this.name = name;
    }

    /**
     * Sets the supertypes.
     *
     * @param names the names of the supertypes.
     */
    public void setSupertypes(QName[] names) {
        resetDependencies();
        supertypes = names;
    }

    /**
     * Sets the mixin flag.
     *
     * @param mixin flag
     */
    public void setMixin(boolean mixin) {
        this.mixin = mixin;
    }

    /**
     * Sets the orderableChildNodes flag.
     *
     * @param orderableChildNodes flag
     */
    public void setOrderableChildNodes(boolean orderableChildNodes) {
        this.orderableChildNodes = orderableChildNodes;
    }

    /**
     * Sets the property definitions.
     *
     * @param defs An array of <code>PropertyDef</code> objects.
     */
    public void setPropertyDefs(PropDef[] defs) {
        propDefs = defs;
    }

    /**
     * Sets the child node definitions.
     *
     * @param defs An array of <code>ChildNodeDef</code> objects
     */
    public void setChildNodeDefs(ChildNodeDef[] defs) {
        resetDependencies();
        nodeDefs = defs;
    }

    /**
     * Returns the name of the node type being defined or
     * <code>null</code> if not set.
     *
     * @return the name of the node type or <code>null</code> if not set.
     */
    public QName getName() {
        return name;
    }

    /**
     * Returns an array containing the names of the supertypes or
     * <code>null</code> if not set.
     *
     * @return an array listing the names of the supertypes or
     *         <code>null</code> if not set.
     */
    public QName[] getSupertypes() {
        return supertypes;
    }

    /**
     * Returns the value of the mixin flag.
     *
     * @return true if this is a mixin node type; false otherwise.
     */
    public boolean isMixin() {
        return mixin;
    }

    /**
     * Returns the value of the orderableChildNodes flag.
     *
     * @return true if nodes of this node type can have orderable child nodes; false otherwise.
     */
    public boolean hasOrderableChildNodes() {
        return orderableChildNodes;
    }

    /**
     * Returns an array containing the property definitions or
     * <code>null</code> if not set.
     *
     * @return an array containing the property definitions or
     *         <code>null</code> if not set.
     */
    public PropDef[] getPropertyDefs() {
        return propDefs;
    }

    /**
     * Returns an array containing the child node definitions or
     * <code>null</code> if not set.
     *
     * @return an array containing the child node definitions or
     *         <code>null</code> if not set.
     */
    public ChildNodeDef[] getChildNodeDefs() {
        return nodeDefs;
    }
}
