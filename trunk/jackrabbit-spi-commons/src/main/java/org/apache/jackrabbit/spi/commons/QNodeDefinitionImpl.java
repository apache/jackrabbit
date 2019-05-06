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
package org.apache.jackrabbit.spi.commons;

import org.apache.jackrabbit.spi.QNodeDefinition;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.conversion.NameException;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.name.NameConstants;

import java.util.Arrays;
import java.util.TreeSet;
import java.util.Set;
import java.util.HashSet;

import javax.jcr.NamespaceException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;

/**
 * <code>QNodeDefinitionImpl</code> implements a <code>QNodeDefinition</code>.
 */
public class QNodeDefinitionImpl extends QItemDefinitionImpl implements QNodeDefinition {

    private static final long serialVersionUID = -3671882394577685657L;

    /**
     * The name of the default primary type.
     */
    private final Name defaultPrimaryType;
    /**
     * The names of the required primary types.
     */
    private final Set<Name> requiredPrimaryTypes = new HashSet<Name>();
    /**
     * The 'allowsSameNameSiblings' flag.
     */
    private final boolean allowsSameNameSiblings;

    /**
     * Copy constructor.
     *
     * @param nodeDef some other node definition.
     */
    public QNodeDefinitionImpl(QNodeDefinition nodeDef) {
        this(nodeDef.getName(), nodeDef.getDeclaringNodeType(),
                nodeDef.isAutoCreated(), nodeDef.isMandatory(),
                nodeDef.getOnParentVersion(), nodeDef.isProtected(),
                nodeDef.getDefaultPrimaryType(),
                nodeDef.getRequiredPrimaryTypes(),
                nodeDef.allowsSameNameSiblings());
    }

    /**
     * Creates a new SPI node definition based on a JCR NodeDefinition.
     *
     * @param name              the name of the child item.
     * @param declaringNodeType the declaring node type
     * @param isAutoCreated     if this item is auto created.
     * @param isMandatory       if this is a mandatory item.
     * @param onParentVersion   the on parent version behaviour.
     * @param isProtected       if this item is protected.
     * @param defaultPrimaryType the default primary type name
     * @param requiredPrimaryTypes the required primary type name
     * @param allowsSameNameSiblings if this node allows SNS
     */
    public QNodeDefinitionImpl(Name name, Name declaringNodeType,
                               boolean isAutoCreated, boolean isMandatory,
                               int onParentVersion, boolean isProtected,
                               Name defaultPrimaryType, Name[] requiredPrimaryTypes,
                               boolean allowsSameNameSiblings) {
        super(name, declaringNodeType, isAutoCreated, isMandatory,
                onParentVersion, isProtected);
        this.defaultPrimaryType = defaultPrimaryType;
        this.requiredPrimaryTypes.addAll(Arrays.asList(requiredPrimaryTypes));
        // sanitize field value
        if (this.requiredPrimaryTypes.isEmpty()) {
            this.requiredPrimaryTypes.add(NameConstants.NT_BASE);
        }
        this.allowsSameNameSiblings = allowsSameNameSiblings;
    }

    /**
     * Creates a new node definition based on a JCR <code>NodeDefinition</code>.
     *
     * @param nodeDef  the node definition.
     * @param resolver the name/path resolver of the session that provided the
     *                 node definition
     * @throws NameException      if <code>nodeDef</code> contains an illegal
     *                            name.
     * @throws NamespaceException if <code>nodeDef</code> contains a name with
     *                            an namespace prefix that is unknown to
     *                            <code>resolver</code>.
     */
    public QNodeDefinitionImpl(NodeDefinition nodeDef,
                               NamePathResolver resolver)
            throws NameException, NamespaceException {
        this(nodeDef.getName().equals(NameConstants.ANY_NAME.getLocalName()) ? NameConstants.ANY_NAME : resolver.getQName(nodeDef.getName()),
                nodeDef.getDeclaringNodeType() != null ? resolver.getQName(nodeDef.getDeclaringNodeType().getName()) : null,
                nodeDef.isAutoCreated(), nodeDef.isMandatory(),
                nodeDef.getOnParentVersion(), nodeDef.isProtected(),
                nodeDef.getDefaultPrimaryType() != null ? resolver.getQName(nodeDef.getDefaultPrimaryType().getName()) : null,
                getNodeTypeNames(nodeDef.getRequiredPrimaryTypes(), resolver),
                nodeDef.allowsSameNameSiblings());
    }

    //----------------------------------------------------< QNodeDefinition >---
    /**
     * {@inheritDoc}
     */
    public Name getDefaultPrimaryType() {
        return defaultPrimaryType;
    }

    /**
     * {@inheritDoc}
     */
    public Name[] getRequiredPrimaryTypes() {
        return requiredPrimaryTypes.toArray(new Name[requiredPrimaryTypes.size()]);
    }

    /**
     * {@inheritDoc}
     */
    public boolean allowsSameNameSiblings() {
        return allowsSameNameSiblings;
    }

    /**
     * {@inheritDoc}
     *
     * @return always <code>true</code>
     */
    public boolean definesNode() {
        return true;
    }

    //-------------------------------------------------------------< Object >---
    /**
     * Compares two node definitions for equality. Returns <code>true</code>
     * if the given object is a node definition and has the same attributes
     * as this node definition.
     *
     * @param obj the object to compare this node definition with
     * @return <code>true</code> if the object is equal to this node definition,
     *         <code>false</code> otherwise
     * @see Object#equals(Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof QNodeDefinition) {
            QNodeDefinition other = (QNodeDefinition) obj;
            return super.equals(obj)
                    && requiredPrimaryTypes.equals(new HashSet<Name>(
                            Arrays.asList(other.getRequiredPrimaryTypes())))
                    && (defaultPrimaryType == null
                            ? other.getDefaultPrimaryType() == null
                            : defaultPrimaryType.equals(other.getDefaultPrimaryType()))
                    && allowsSameNameSiblings == other.allowsSameNameSiblings();
        }
        return false;
    }

    /**
     * Overwrites {@link QItemDefinitionImpl#hashCode()}.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        if (hashCode == 0) {
            int h = super.hashCode();
            h = 37 * h + (defaultPrimaryType == null ? 0 : defaultPrimaryType.hashCode());
            h = 37 * h + requiredPrimaryTypes.hashCode();
            h = 37 * h + (allowsSameNameSiblings ? 11 : 43);
            hashCode = h;
        }
        return hashCode;

    }

    //-----------------------------------------------------------< internal >---
    /**
     * Returns the names of the passed node types using the namespace resolver
     * to parse the names.
     *
     * @param nt       the node types
     * @param resolver the name/path resolver of the session that provided
     *                 <code>nt</code>.
     * @return the names of the node types.
     * @throws NameException      if a node type returns an illegal name.
     * @throws NamespaceException if the name of a node type contains a prefix
     *                            that is not known to <code>resolver</code>.
     */
    private static Name[] getNodeTypeNames(NodeType[] nt,
                                           NamePathResolver resolver)
            throws NameException, NamespaceException {
        Name[] names = new Name[nt.length];
        for (int i = 0; i < nt.length; i++) {
            Name ntName = resolver.getQName(nt[i].getName());
            names[i] = ntName;
        }
        return names;
    }
}
