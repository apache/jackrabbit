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
package org.apache.jackrabbit.jcr2spi.security.authorization;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.jcr.NamespaceException;
import javax.jcr.RepositoryException;
import javax.jcr.security.Privilege;

import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.PrivilegeDefinition;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;

public class PrivilegeImpl implements Privilege {

    private final PrivilegeDefinition definition;

    private final Privilege[] declaredAggregates;
    private final Privilege[] aggregates;
    private static final Privilege[] EMPTY_ARRAY = new Privilege[0];

    private final NamePathResolver npResolver;

    public PrivilegeImpl(PrivilegeDefinition definition, PrivilegeDefinition[] allDefs, NamePathResolver npResolver) throws RepositoryException {
        this.definition = definition;
        this.npResolver = npResolver;

        Set<Name> set = definition.getDeclaredAggregateNames();
        Name[] declAggrNames = set.toArray(new Name[set.size()]);
        if (declAggrNames.length == 0) {
            declaredAggregates = EMPTY_ARRAY;
            aggregates = EMPTY_ARRAY;
        } else {
            declaredAggregates = new Privilege[declAggrNames.length];
            for (int i = 0; i < declAggrNames.length; i++) {
                for (PrivilegeDefinition def : allDefs) {
                    if (def.getName().equals(declAggrNames[i])) {
                        declaredAggregates[i] = new PrivilegeImpl(def, allDefs, npResolver);
                    }
                }
            }

            Set<Privilege> aggr = new HashSet<Privilege>();
            for (Privilege decl : declaredAggregates) {
                aggr.add(decl);
                if (decl.isAggregate()) {
                    aggr.addAll(Arrays.asList(decl.getAggregatePrivileges()));
                }
            }
            aggregates = aggr.toArray(new Privilege[aggr.size()]);
        }
    }

    /**
     * @see Privilege#getName()
     */
    public String getName() {
        try {
            return npResolver.getJCRName(definition.getName());
        } catch (NamespaceException e) {
            // should not occur -> return internal name representation.
            return definition.getName().toString();
        }
    }

    /**
     * @see Privilege#isAbstract()
     */
    public boolean isAbstract() {
        return definition.isAbstract();
    }

    /**
     * @see Privilege#isAggregate()
     */
    public boolean isAggregate() {
        return declaredAggregates.length > 0;
    }

    /**
     * @see Privilege#getDeclaredAggregatePrivileges()
     */
    public Privilege[] getDeclaredAggregatePrivileges() {
        return declaredAggregates;
    }

    /**
     * @see Privilege#getAggregatePrivileges()
     */
    public Privilege[] getAggregatePrivileges() {
        return aggregates;
    }

    //---------------------------------------------------------< Object >---
    @Override
    public String toString() {
        return getName();
    }

    @Override
    public int hashCode() {
        return definition.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof PrivilegeImpl) {
            PrivilegeImpl other = (PrivilegeImpl) obj;
            return definition.equals(other.definition);
        }
        return false;
    }
}
