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
 * A <code>AnonymousPrincipal</code> ...
 */
public final class AnonymousPrincipal implements Principal, Serializable {

    private static final String ANONYMOUS_NAME = "anonymous";

    /**
     * Creates an <code>AnonymousPrincipal</code>.
     */
    public AnonymousPrincipal() {
    }

    //-------------------------------------------------------------< Object >---
    @Override
    public String toString() {
        return ("AnonymousPrincipal");
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof AnonymousPrincipal) {
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return ANONYMOUS_NAME.hashCode();
    }

    //----------------------------------------------------------< Principal >---
    /**
     * @return Always returns "anonymous"
     * @see Principal#getName()
     */
    public String getName() {
        return ANONYMOUS_NAME;
    }
}
