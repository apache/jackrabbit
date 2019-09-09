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
package org.apache.jackrabbit.core.security.authorization;

import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.core.ItemImpl;

import javax.jcr.RepositoryException;

import java.security.Principal;
import java.util.Set;

/**
 * <code>AccessControlUtils</code>...
 */
public interface AccessControlUtils {

    /**
     * Test if the specified path points to an item that defines AC
     * information.
     *
     * @param absPath Path to an item.
     * @return true if the item at the specified <code>absPath</code> contains
     * access control information.
     * @throws RepositoryException If an error occurs.
     */
    boolean isAcItem(Path absPath) throws RepositoryException;

    /**
     * Test if the specified path points to an item that defines AC
     * information and consequently should be considered protected.
     *
     * @param item An item.
     * @return true if the item at the specified <code>item</code> defines
     * access control related information is should therefore be considered
     * protected.
     * @throws RepositoryException If an error occurs.
     */
    boolean isAcItem(ItemImpl item) throws RepositoryException;

    /**
     * Test if the specified set of principals contains an admin or system
     * principal.
     *
     * @param principals A set of principals.
     * @return true if the specified set of principals contains an
     * <code>AdminPrincipal</code> or a <code>SystemPrincipal</code>.
     */
    boolean isAdminOrSystem(Set<Principal> principals);

    /**
     * Test if if the specified set of principals will have read-only permissions
     * only. False otherwise (or if it cannot be determined from the principal
     * set only).
     *
     * @param principals A set of principals.
     * @return true if the specified set of principals will only be granted
     * read permission on all items.
     */
    boolean isReadOnly(Set<Principal> principals);

}