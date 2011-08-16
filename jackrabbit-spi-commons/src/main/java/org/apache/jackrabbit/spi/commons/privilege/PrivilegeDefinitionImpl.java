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
package org.apache.jackrabbit.spi.commons.privilege;

import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.PrivilegeDefinition;

import java.util.Collections;
import java.util.Set;

/**
 * <code>PrivilegeDefinition</code>
 */
public class PrivilegeDefinitionImpl implements PrivilegeDefinition {

    private final Name name;
    private final boolean isAbstract;
    private final Set<Name> declaredAggregateNames;

    public PrivilegeDefinitionImpl(Name name, boolean isAbstract, Set<Name> declaredAggregateNames) {
        this.name = name;
        this.isAbstract = isAbstract;
        this.declaredAggregateNames = declaredAggregateNames == null ? Collections.<Name>emptySet() : Collections.unmodifiableSet(declaredAggregateNames);
    }

    //------------------------------------------------< PrivilegeDefinition >---
    /**
     * @see PrivilegeDefinition#getName()
     */
    public Name getName() {
        return name;
    }

    /**
     * @see PrivilegeDefinition#isAbstract()
     */
    public boolean isAbstract() {
        return isAbstract;
    }

    /**
     * @see PrivilegeDefinition#getDeclaredAggregateNames()
     */
    public Set<Name> getDeclaredAggregateNames() {
        return declaredAggregateNames;
    }

    //-------------------------------------------------------------< Object >---
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PrivilegeDefinitionImpl that = (PrivilegeDefinitionImpl) o;

        if (isAbstract != that.isAbstract) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        return declaredAggregateNames.equals(that.declaredAggregateNames);

    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (isAbstract ? 1 : 0);
        result = 31 * result + (declaredAggregateNames != null ? declaredAggregateNames.hashCode() : 0);
        return result;
    }
}