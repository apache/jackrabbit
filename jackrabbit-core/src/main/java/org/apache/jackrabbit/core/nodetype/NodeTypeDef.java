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
package org.apache.jackrabbit.core.nodetype;

import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.name.NameConstants;

import javax.jcr.PropertyType;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * A <code>NodeTypeDef</code> holds the definition of a node type.
 */
public class NodeTypeDef implements Cloneable {

    private Name name;

    /**
     * Ordered array of supertype names. Empty if no supertypes have been
     * specified. Never <code>null</code>.
     */
    private Name[] supertypes;

    private boolean mixin;
    private boolean orderableChildNodes;

    private boolean abstractStatus;
    private Name primaryItemName;

    private HashSet propDefs;
    private HashSet nodeDefs;
    private Set dependencies;

    /**
     * Default constructor.
     */
    public NodeTypeDef() {
        dependencies = null;
        name = null;
        primaryItemName = null;
        nodeDefs = new HashSet();
        propDefs = new HashSet();
        supertypes = Name.EMPTY_ARRAY;
        mixin = false;
        orderableChildNodes = false;
        abstractStatus = false;
    }

    /**
     * Returns a collection of node type <code>Name</code>s that are being
     * referenced by <i>this</i> node type definition (e.g. as supertypes, as
     * required/default primary types in child node definitions, as REFERENCE
     * value constraints in property definitions).
     * <p/>
     * Note that self-references (e.g. a child node definition that specifies
     * the declaring node type as the default primary type) are not considered
     * dependencies.
     *
     * @return a collection of node type <code>Name</code>s
     */
    public Collection getDependencies() {
        if (dependencies == null) {
            dependencies = new HashSet();
            // supertypes
            dependencies.addAll(Arrays.asList(supertypes));
            // child node definitions
            for (Iterator iter = nodeDefs.iterator(); iter.hasNext();) {
                NodeDef nd = (NodeDef) iter.next();
                // default primary type
                Name ntName = nd.getDefaultPrimaryType();
                if (ntName != null && !name.equals(ntName)) {
                    dependencies.add(ntName);
                }
                // required primary type
                Name[] ntNames = nd.getRequiredPrimaryTypes();
                for (int j = 0; j < ntNames.length; j++) {
                    if (ntNames[j] != null && !name.equals(ntNames[j])) {
                        dependencies.add(ntNames[j]);
                    }
                }
            }
            // property definitions
            for (Iterator iter = propDefs.iterator(); iter.hasNext();) {
                PropDef pd = (PropDef) iter.next();
                // REFERENCE value constraints
                if (pd.getRequiredType() == PropertyType.REFERENCE) {
                    ValueConstraint[] ca = pd.getValueConstraints();
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

    //----------------------------------------------------< setters & getters >
    /**
     * Sets the name of the node type being defined.
     *
     * @param name The name of the node type.
     */
    public void setName(Name name) {
        this.name = name;
    }

    /**
     * Sets the supertypes.
     *
     * @param names the names of the supertypes.
     */
    public void setSupertypes(Name[] names) {
        resetDependencies();
        // Optimize common cases (zero or one supertypes)
        if (names.length == 0) {
            supertypes = Name.EMPTY_ARRAY;
        } else if (names.length == 1) {
            supertypes = new Name[] { names[0] };
        } else {
            // Sort and remove duplicates
            SortedSet types = new TreeSet();
            types.addAll(Arrays.asList(names));
            supertypes = (Name[]) types.toArray(new Name[types.size()]);
        }
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
     * Sets the 'abstract' flag.
     *
     * @param abstractStatus flag
     */
    public void setAbstract(boolean abstractStatus) {
        this.abstractStatus = abstractStatus;
    }

    /**
     * Sets the name of the primary item (one of the child items of the node's
     * of this node type)
     *
     * @param primaryItemName The name of the primary item.
     */
    public void setPrimaryItemName(Name primaryItemName) {
        this.primaryItemName = primaryItemName;
    }

    /**
     * Sets the property definitions.
     *
     * @param defs An array of <code>PropertyDef</code> objects.
     */
    public void setPropertyDefs(PropDef[] defs) {
        resetDependencies();
        propDefs.clear();
        propDefs.addAll(Arrays.asList(defs));
    }

    /**
     * Sets the child node definitions.
     *
     * @param defs An array of <code>NodeDef</code> objects
     */
    public void setChildNodeDefs(NodeDef[] defs) {
        resetDependencies();
        nodeDefs.clear();
        nodeDefs.addAll(Arrays.asList(defs));
    }

    /**
     * Returns the name of the node type being defined or
     * <code>null</code> if not set.
     *
     * @return the name of the node type or <code>null</code> if not set.
     */
    public Name getName() {
        return name;
    }

    /**
     * Returns an array containing the names of the supertypes. If no
     * supertypes have been specified, then an empty array is returned
     * for mixin types and the <code>nt:base</code> primary type and
     * an array containing just <code>nt:base<code> for other primary types.
     * <p>
     * The returned array must not be modified by the application.
     *
     * @return a sorted array of supertype names
     */
    public Name[] getSupertypes() {
        if (supertypes.length > 0
                || isMixin() || NameConstants.NT_BASE.equals(getName())) {
            return supertypes;
        } else {
            return new Name[] { NameConstants.NT_BASE };
        }
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
     * Returns the value of the 'abstract' flag.
     *
     * @return true if this node type is abstract; false otherwise.
     */
    public boolean isAbstract() {
        return abstractStatus;
    }

    /**
     * Returns the name of the primary item (one of the child items of the
     * node's of this node type) or <code>null</code> if not set.
     *
     * @return the name of the primary item or <code>null</code> if not set.
     */
    public Name getPrimaryItemName() {
        return primaryItemName;
    }

    /**
     * Returns an array containing the property definitions or
     * <code>null</code> if not set.
     *
     * @return an array containing the property definitions or
     *         <code>null</code> if not set.
     */
    public PropDef[] getPropertyDefs() {
        if (propDefs.isEmpty()) {
            return PropDef.EMPTY_ARRAY;
        }
        return (PropDef[]) propDefs.toArray(new PropDef[propDefs.size()]);
    }

    /**
     * Returns an array containing the child node definitions or
     * <code>null</code> if not set.
     *
     * @return an array containing the child node definitions or
     *         <code>null</code> if not set.
     */
    public NodeDef[] getChildNodeDefs() {
        if (nodeDefs.isEmpty()) {
            return NodeDef.EMPTY_ARRAY;
        }
        return (NodeDef[]) nodeDefs.toArray(new NodeDef[nodeDefs.size()]);
    }

    //-------------------------------------------< java.lang.Object overrides >
    public Object clone() {
        NodeTypeDef clone = new NodeTypeDef();
        clone.name = name;
        clone.primaryItemName = primaryItemName;
        clone.supertypes = supertypes; // immutable, thus ok to share
        clone.mixin = mixin;
        clone.orderableChildNodes = orderableChildNodes;
        clone.abstractStatus = abstractStatus;
        clone.nodeDefs = (HashSet) nodeDefs.clone();
        clone.propDefs = (HashSet) propDefs.clone();
        return clone;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof NodeTypeDef) {
            NodeTypeDef other = (NodeTypeDef) obj;
            return (name == null ? other.name == null : name.equals(other.name))
                    && (primaryItemName == null ? other.primaryItemName == null : primaryItemName.equals(other.primaryItemName))
                    && Arrays.equals(getSupertypes(), other.getSupertypes())
                    && mixin == other.mixin
                    && orderableChildNodes == other.orderableChildNodes
                    && abstractStatus == other.abstractStatus
                    && propDefs.equals(other.propDefs)
                    && nodeDefs.equals(other.nodeDefs);
        }
        return false;
    }

    /**
     * Returns zero to satisfy the Object equals/hashCode contract.
     * This class is mutable and not meant to be used as a hash key.
     *
     * @return always zero
     * @see Object#hashCode()
     */
    public int hashCode() {
        return 0;
    }
}
