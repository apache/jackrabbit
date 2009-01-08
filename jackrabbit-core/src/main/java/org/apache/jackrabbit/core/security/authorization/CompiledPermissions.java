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

import javax.jcr.RepositoryException;

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
     * if the given <code>absPath</code> denotes an existing <code>Node</code>,
     * otherwise it returns zero.
     *
     * @param absPath Absolute path to a <code>Node</code>.
     * @return the granted privileges at <code>absPath</code> or zero if
     * the path does not denote an existing <code>Node</code>.
     * @throws RepositoryException if an error occurs
     */
    int getPrivileges(Path absPath) throws RepositoryException;

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
}
