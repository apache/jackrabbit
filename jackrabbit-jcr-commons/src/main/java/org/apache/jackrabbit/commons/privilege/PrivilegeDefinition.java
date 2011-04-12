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
package org.apache.jackrabbit.commons.privilege;

import java.util.Arrays;

/**
 * <code>PrivilegeDefinition</code>
 */
public class PrivilegeDefinition {

    private final String name;
    private final boolean isAbstract;
    private final String[] declaredAggregateNames;

    public PrivilegeDefinition(String name, boolean isAbstract, String[] declaredAggregateNames) {
        this.name = name;
        this.isAbstract = isAbstract;
        this.declaredAggregateNames = declaredAggregateNames == null ? new String[0] : declaredAggregateNames;
    }

    public String getName() {
        return name;
    }

    public boolean isAbstract() {
        return isAbstract;
    }

    public String[] getDeclaredAggregateNames() {
        return declaredAggregateNames;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PrivilegeDefinition that = (PrivilegeDefinition) o;

        if (isAbstract != that.isAbstract) return false;
        if (!Arrays.equals(declaredAggregateNames, that.declaredAggregateNames)) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (isAbstract ? 1 : 0);
        result = 31 * result + (declaredAggregateNames != null ? Arrays.hashCode(declaredAggregateNames) : 0);
        return result;
    }
}