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
import org.apache.jackrabbit.core.id.ItemId;

import javax.jcr.RepositoryException;
import javax.jcr.security.Privilege;
import java.util.Collections;
import java.util.Set;

/**
 * <code>CompiledPermissions</code> represents the evaluation of an
 * <code>AccessControlPolicy</code> that applies for a given set of
 * <code>Principal</code>s (normally obtained from the Subject
 * of a Session).
 */
public interface CompiledPermissions {

    /**
     * Indicate to this <code>CompiledPermissions</code> object that it is
     * not used any more.
     */
    void close();

    /**
     * Returns <code>true</code> if the specified permissions are granted
     * on the item identified by the given <code>path</code>.
     *
     * @param absPath Absolute path pointing to an item. If the item does
     * not exist yet (asking for 'add-node' and 'set-property' permission),
     * it's direct ancestor must exist.
     * @param permissions A combination of one or more of permission constants
     * defined by {@link Permission} encoded as a bitmask value
     * @return <code>true</code> if the specified permissions are granted,
     * <code>false</code> otherwise.
     * @throws RepositoryException if an error occurs.
     */
    boolean grants(Path absPath, int permissions) throws RepositoryException;

    /**
     * Returns the <code>Privilege</code> bits granted by the underlying policy
     * if the given <code>absPath</code>.
     *
     * @param absPath Absolute path to a <code>Node</code>.
     * @return the granted privileges at <code>absPath</code>.
     * @throws RepositoryException if an error occurs
     * @deprecated Use {@link #getPrivilegeSet(Path)} instead.
     */
    int getPrivileges(Path absPath) throws RepositoryException;

    /**
     * Returns <code>true</code> if the given privileges are granted at the
     * specified <code>absPath</code>.
     *
     * @param absPath
     * @param privileges
     * @return <code>true</code> if the given privileges are granted at the
     * specified <code>absPath</code>.
     * @throws RepositoryException
     */
    boolean hasPrivileges(Path absPath, Privilege... privileges) throws RepositoryException;

    /**
     * Returns the <code>Privilege</code>s granted by the underlying policy
     * at the given <code>absPath</code>.
     *
     * @param absPath Absolute path to a <code>Node</code>.
     * @return the granted privileges at <code>absPath</code>.
     * @throws RepositoryException if an error occurs
     */
    Set<Privilege> getPrivilegeSet(Path absPath) throws RepositoryException;

    /**
     * Returns <code>true</code> if READ permission is granted everywhere.
     * This method acts as shortcut for {@link #grants(Path, int)} where
     * permissions is {@link Permission#READ} and allows to shorten the
     * evaluation time given the fact that a check for READ permission is
     * considered to be the most frequent test.
     *
     * @return <code>true</code> if the READ permission is granted everywhere.
     * @throws RepositoryException if an error occurs
     */
    boolean canReadAll() throws RepositoryException;

    /**
     * Returns <code>true</code> if READ permission is granted for the
     * <i>existing</i> item with the given <code>Path</code> and/or
     * <code>ItemId</code>.
     * This method acts as shortcut for {@link #grants(Path, int)} where
     * permissions is {@link Permission#READ} and allows to shorten the
     * evaluation time given the fact that a check for READ permissions is
     * considered to be the most frequent test.<br>
     * If both Path and ItemId are not <code>null</code> it is left to the
     * implementation which parameter to use.n
     *
     * @param itemPath The path to the item or <code>null</code> if the ID
     * should be used to determine the READ permission.
     * @param itemId The itemId or <code>null</code> if the path should be
     * used to determine the READ permission.
     * @return <code>true</code> if the READ permission is granted.
     * @throws RepositoryException If no item exists with the specified path or
     * itemId or if some other error occurs.
     */
    boolean canRead(Path itemPath, ItemId itemId) throws RepositoryException;

    /**
     * Static implementation of a <code>CompiledPermissions</code> that doesn't
     * grant any permissions at all.
     */
    public static final CompiledPermissions NO_PERMISSION = new CompiledPermissions() {
        public void close() {
            //nop
        }
        public boolean grants(Path absPath, int permissions) {
            // deny everything
            return false;
        }
        public int getPrivileges(Path absPath) {
            return PrivilegeRegistry.NO_PRIVILEGE;
        }

        public boolean hasPrivileges(Path absPath, Privilege... privileges) throws RepositoryException {
            return false;
        }
        public Set<Privilege> getPrivilegeSet(Path absPath) throws RepositoryException {
            return Collections.emptySet();
        }

        public boolean canReadAll() {
            return false;
        }
        public boolean canRead(Path itemPath, ItemId itemId) throws RepositoryException {
            return false;
        }
    };
}
