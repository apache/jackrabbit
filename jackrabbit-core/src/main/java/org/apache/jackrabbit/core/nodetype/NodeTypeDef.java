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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.jcr.PropertyType;

import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.QNodeDefinition;
import org.apache.jackrabbit.spi.QNodeTypeDefinition;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.QValueConstraint;
import org.apache.jackrabbit.spi.commons.QNodeTypeDefinitionImpl;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;

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
    private boolean queryable;

    private boolean abstractStatus;
    private Name primaryItemName;

    private Set<QPropertyDefinition> propDefs;
    private Set<QNodeDefinition> nodeDefs;
    private Set<Name> dependencies;

    /**
     * Default constructor.
     */
    public NodeTypeDef() {
        dependencies = null;
        name = null;
        primaryItemName = null;
        supertypes = Name.EMPTY_ARRAY;
        mixin = false;
        orderableChildNodes = false;
        abstractStatus = false;
        queryable = true;
        nodeDefs = new HashSet<QNodeDefinition>();
        propDefs = new HashSet<QPropertyDefinition>();
    }

    /**
     * Creates a node type def from a spi QNodeTypeDefinition
     * @param def definition
     */
    public NodeTypeDef(QNodeTypeDefinition def) {
        name = def.getName();
        primaryItemName = def.getPrimaryItemName();
        supertypes = def.getSupertypes();
        mixin = def.isMixin();
        orderableChildNodes = def.hasOrderableChildNodes();
        abstractStatus = def.isAbstract();
        queryable = def.isQueryable();
        nodeDefs = new HashSet<QNodeDefinition>();
        for (QNodeDefinition nd: def.getChildNodeDefs()) {
            nodeDefs.add(nd);
        }
        propDefs = new HashSet<QPropertyDefinition>();
        for (QPropertyDefinition pd: def.getPropertyDefs()) {
            propDefs.add(pd);
        }
    }

    /**
     * Returns the QNodeTypeDefintion for this NodeTypeDef
     * @return the QNodeTypeDefintion
     */
    public QNodeTypeDefinition getQNodeTypeDefinition() {
        return new QNodeTypeDefinitionImpl(
                getName(),
                getSupertypes(),
                null,
                isMixin(),
                isAbstract(),
                isQueryable(),
                hasOrderableChildNodes(),
                getPrimaryItemName(),
                propDefs.toArray(new QPropertyDefinition[propDefs.size()]),
                nodeDefs.toArray(new QNodeDefinition[nodeDefs.size()])
        );
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
            dependencies = new HashSet<Name>();
            // supertypes
            dependencies.addAll(Arrays.asList(supertypes));
            // child node definitions
            for (QNodeDefinition nd: nodeDefs) {
                // default primary type
                Name ntName = nd.getDefaultPrimaryType();
                if (ntName != null && !name.equals(ntName)) {
                    dependencies.add(ntName);
                }
                // required primary type
                Name[] ntNames = nd.getRequiredPrimaryTypes();
                for (Name ntName1 : ntNames) {
                    if (ntName1 != null && !name.equals(ntName1)) {
                        dependencies.add(ntName1);
                    }
                }
            }
            // property definitions
            for (QPropertyDefinition pd : propDefs) {
                // [WEAK]REFERENCE value constraints
                if (pd.getRequiredType() == PropertyType.REFERENCE
                        || pd.getRequiredType() == PropertyType.WEAKREFERENCE) {
                    QValueConstraint[] ca = pd.getValueConstraints();
                    if (ca != null) {
                        for (QValueConstraint aCa : ca) {
                            Name rcName = NameFactoryImpl.getInstance().create(aCa.getString());
                            if (!name.equals(rcName)) {
                                dependencies.add(rcName);
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
            SortedSet<Name> types = new TreeSet<Name>();
            types.addAll(Arrays.asList(names));
            supertypes = types.toArray(new Name[types.size()]);
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
     * Sets the 'queryable' flag.
     *
     * @param queryable flag
     */
    public void setQueryable(boolean queryable) {
        this.queryable = queryable;
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
    public void setPropertyDefs(QPropertyDefinition[] defs) {
        resetDependencies();
        propDefs.clear();
        propDefs.addAll(Arrays.asList(defs));
    }

    /**
     * Sets the child node definitions.
     *
     * @param defs An array of <code>QNodeDefinition</code> objects
     */
    public void setChildNodeDefs(QNodeDefinition[] defs) {
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
     * Returns the value of the 'queryable' flag.
     *
     * @return true if this node type is queryable; false otherwise.
     */
    public boolean isQueryable() {
        return queryable;
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
    public QPropertyDefinition[] getPropertyDefs() {
        if (propDefs.isEmpty()) {
            return QPropertyDefinition.EMPTY_ARRAY;
        }
        return propDefs.toArray(new QPropertyDefinition[propDefs.size()]);
    }

    /**
     * Returns an array containing the child node definitions or
     * <code>null</code> if not set.
     *
     * @return an array containing the child node definitions or
     *         <code>null</code> if not set.
     */
    public QNodeDefinition[] getChildNodeDefs() {
        if (nodeDefs.isEmpty()) {
            return QNodeDefinition.EMPTY_ARRAY;
        }
        return nodeDefs.toArray(new QNodeDefinition[nodeDefs.size()]);
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
        clone.queryable = queryable;
        clone.nodeDefs = new HashSet<QNodeDefinition>();
        // todo: itemdefs should be cloned as well, since mutable
        clone.nodeDefs = new HashSet<QNodeDefinition>(nodeDefs);
        clone.propDefs = new HashSet<QPropertyDefinition>(propDefs);
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
                    && queryable == other.queryable
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
