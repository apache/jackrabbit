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
package org.apache.jackrabbit.spi.commons.nodetype;

import java.util.Arrays;
import java.util.Set;
import java.util.HashSet;

import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.QNodeDefinition;
import org.apache.jackrabbit.spi.commons.QNodeDefinitionImpl;
import org.apache.jackrabbit.spi.commons.name.NameConstants;

/**
 * A builder for a {@link QNodeDefinition}.
 */
public class QNodeDefinitionBuilder extends QItemDefinitionBuilder {

    private Name defaultPrimaryType;
    private Set<Name> requiredPrimaryTypes = new HashSet<Name>();
    private boolean allowsSameNameSiblings;

    /**
     * @param name the name of the default primary type of the node definition
     *             being built.
     */
    public void setDefaultPrimaryType(Name name) {
        defaultPrimaryType = name;
    }

    /**
     * @return the name of the default primary type of the node definition being
     *         built.
     */
    public Name getDefaultPrimaryType() {
        return defaultPrimaryType;
    }

    /**
     * Adds a required primary type of the node definition being built.
     *
     * @param name the name of a required primary type.
     */
    public void addRequiredPrimaryType(Name name) {
        requiredPrimaryTypes.add(name);
    }

    /**
     * @param names array of names of the required primary types of the node
     *              definition being built.
     */
    public void setRequiredPrimaryTypes(Name[] names) {
        requiredPrimaryTypes.clear();
        if (names != null) {
            requiredPrimaryTypes.addAll(Arrays.asList(names));
        }
    }

    /**
     * @return array of names of the required primary types of the node
     *         definition being built.
     */
    public Name[] getRequiredPrimaryTypes() {
        if (requiredPrimaryTypes.isEmpty()) {
            return new Name[]{NameConstants.NT_BASE};
        } else {
            return requiredPrimaryTypes.toArray(new Name[requiredPrimaryTypes.size()]);
        }
    }

    /**
     * @param allowSns true if building a node definition with same name
     *                 siblings, false otherwise.
     */
    public void setAllowsSameNameSiblings(boolean allowSns) {
        allowsSameNameSiblings = allowSns;
    }

    /**
     * @return true if building a node definition with same name siblings, false
     *         otherwise.
     */
    public boolean getAllowsSameNameSiblings() {
        return allowsSameNameSiblings;
    }

    /**
     * Creates a new {@link QNodeDefinition} instance based on the state of this
     * builder.
     *
     * @return a new {@link QNodeDefinition} instance.
     * @throws IllegalStateException if the instance has not the necessary
     *                               information to build the QNodeDefinition
     *                               instance.
     */
    public QNodeDefinition build() throws IllegalStateException {
        return new QNodeDefinitionImpl(getName(), getDeclaringNodeType(),
                getAutoCreated(), getMandatory(), getOnParentVersion(),
                getProtected(), getDefaultPrimaryType(),
                getRequiredPrimaryTypes(), getAllowsSameNameSiblings());
    }

}
