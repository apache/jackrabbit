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
package org.apache.jackrabbit.core.security.principal;

import org.apache.jackrabbit.api.security.principal.GroupPrincipal;
import org.apache.jackrabbit.api.security.principal.JackrabbitPrincipal;

import java.security.Principal;
import java.util.Enumeration;

/**
 * The EveryonePrincipal contains all principals (excluding itself).
 */
public final class EveryonePrincipal implements GroupPrincipal, JackrabbitPrincipal {

    public static final String NAME = "everyone";
    private static final EveryonePrincipal INSTANCE = new EveryonePrincipal();

    private EveryonePrincipal() { }

    public static EveryonePrincipal getInstance() {
        return INSTANCE;
    }

    //----------------------------------------------------------< Principal >---
    public String getName() {
        return NAME;
    }

    //--------------------------------------------------------------< Group >---
    public boolean addMember(Principal user) {
        return false;
    }

    public boolean removeMember(Principal user) {
        throw new UnsupportedOperationException("Cannot remove a member from the everyone group.");
    }

    public boolean isMember(Principal member) {
        return !member.equals(this);
    }

    public Enumeration<? extends Principal> members() {
        throw new UnsupportedOperationException("Not implemented.");
    }

    //-------------------------------------------------------------< Object >---

    @Override
    public int hashCode() {
        return NAME.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this || obj instanceof EveryonePrincipal) {
            return true;
        } else if (obj instanceof JackrabbitPrincipal) {
            return NAME.equals(((JackrabbitPrincipal) obj).getName());
        }
        return false;
    }
}
