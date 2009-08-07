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

import java.util.Arrays;
import java.util.TreeSet;

/**
 * <code>QNodeDefinitionImpl</code> implements a <code>QNodeDefinition</code>.
 */
public class QNodeDefinitionImpl extends QItemDefinitionImpl implements QNodeDefinition {

    /**
     * The name of the default primary type.
     */
    private final Name defaultPrimaryType;

    /**
     * The names of the required primary types.
     */
    private final Name[] requiredPrimaryTypes;

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
     * Creates a new qualified node definition based on a JCR NodeDefinition.
     *
     * @param name              the name of the child item.
     * @param declaringNodeType the delaring node type
     * @param isAutoCreated     if this item is auto created.
     * @param isMandatory       if this is a mandatory item.
     * @param onParentVersion   the on parent version behaviour.
     * @param isProtected       if this item is protected.
     */
    public QNodeDefinitionImpl(Name name, Name declaringNodeType,
                               boolean isAutoCreated, boolean isMandatory,
                               int onParentVersion, boolean isProtected,
                               Name defaultPrimaryType, Name[] requiredPrimaryTypes,
                               boolean allowsSameNameSiblings) {
        super(name, declaringNodeType, isAutoCreated, isMandatory,
                onParentVersion, isProtected);
        this.defaultPrimaryType = defaultPrimaryType;
        this.requiredPrimaryTypes = requiredPrimaryTypes;
        this.allowsSameNameSiblings = allowsSameNameSiblings;
    }

    //-------------------------------------------------------< QNodeDefinition >
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
        return requiredPrimaryTypes;
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

    //-------------------------------------------< java.lang.Object overrides >
    /**
     * Compares two node definitions for equality. Returns <code>true</code>
     * if the given object is a node defintion and has the same attributes
     * as this node definition.
     *
     * @param obj the object to compare this node definition with
     * @return <code>true</code> if the object is equal to this node definition,
     *         <code>false</code> otherwise
     * @see Object#equals(Object)
     */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof QNodeDefinition) {
            QNodeDefinition other = (QNodeDefinition) obj;
            return super.equals(obj)
                    && Arrays.equals(requiredPrimaryTypes, other.getRequiredPrimaryTypes())
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
     * @return
     */
    public int hashCode() {
        if (hashCode == 0) {
            // build hashCode (format: <declaringNodeType>/<name>/<requiredPrimaryTypes>)
            StringBuffer sb = new StringBuffer();

            if (getDeclaringNodeType() != null) {
                sb.append(getDeclaringNodeType().toString());
                sb.append('/');
            }
            if (definesResidual()) {
                sb.append('*');
            } else {
                sb.append(getName().toString());
            }
            sb.append('/');
            // set of required node type names, sorted in ascending order
            TreeSet set = new TreeSet();
            Name[] names = getRequiredPrimaryTypes();
            for (int i = 0; i < names.length; i++) {
                set.add(names[i]);
            }
            sb.append(set.toString());

            hashCode = sb.toString().hashCode();
        }
        return hashCode;
    }
}
