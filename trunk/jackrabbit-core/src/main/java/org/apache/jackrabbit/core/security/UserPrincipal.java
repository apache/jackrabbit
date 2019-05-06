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
package org.apache.jackrabbit.core.security;

import java.io.Serializable;
import java.security.Principal;

/**
 * A <code>UserPrincipal</code> ...
 */
public class UserPrincipal implements Principal, Serializable {

    private final String name;

    /**
     * Creates a <code>UserPrincipal</code> with the given name.
     *
     * @param name the name of this principal
     * @throws IllegalArgumentException if <code>name</code> is <code>null</code>.
     */
    public UserPrincipal(String name) throws IllegalArgumentException {
        if (name == null) {
            throw new IllegalArgumentException("name can not be null");
        }
        this.name = name;
    }

    //-------------------------------------------------------------< Object >---
    @Override
    public String toString() {
        return ("UserPrincipal: " + name);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof UserPrincipal) {
            UserPrincipal other = (UserPrincipal) obj;
            return name.equals(other.name);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    //----------------------------------------------------------< Principal >---
    /**
     * {@inheritDoc}
     */
    public String getName() {
        return name;
    }
}
