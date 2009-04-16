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
package org.apache.jackrabbit.api.jsr283.version;

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.version.VersionIterator;

/**
 * A <code>VersionHistory</code> object wraps an <code>nt:versionHistory</code>
 * node. It provides convenient access to version history information.
 */
public interface VersionHistory extends javax.jcr.version.VersionHistory {

    /**
     * Returns the identifier of the versionable node for which this is the version history.
     *
     * @return the identifier of the versionable node for which this is the version history.
     * @throws RepositoryException if an error occurs.
     * @since JCR 2.0
     */
    public String getVersionableIdentifier() throws RepositoryException;

    /**
     * This method returns an iterator over all the versions in the <i>line of descent</i>
     * from the root version to that base version within this history <i>that is bound to the
     * workspace through which this <code>VersionHistory</code> was accessed</i>.
     * <p>
     * Within a version history <code>H</code>, <code>B</code> is the base version bound
     * to workspace <code>W</code> if and only if there exists a versionable node <code>N</code>
     * in <code>W</code> whose version history is <code>H</code> and <code>B</code> is the base
     * version of <code>N</code>.
     * <p>
     * The <i>line of descent</i> from version <code>V1</code> to <code>V2</code>,
     * where <code>V2</code> is a successor of <code>V1</code>, is the ordered list
     * of versions starting with <code>V1</code> and proceeding through each direct successor to
     * <code>V2</code>.
     * <p>
     * The versions are returned in order of creation date, from oldest to newest.
     * <p>
     * Note that in a simple versioning repository the behavior of this method is
     * equivalent to returning all versions in the version history in order from
     * oldest to newest.
     *
     * @return a <code>VersionIterator</code> object.
     * @throws RepositoryException if an error occurs.
     */
    public VersionIterator getAllLinearVersions() throws RepositoryException;

    /**
     * This method returns all the frozen nodes of all the versions in this verison history
     * in the same order as {@link #getAllLinearVersions}.
     *
     * @return a <code>NodeIterator</code> object.
     * @throws RepositoryException if an error occurs.
     * @since JCR 2.0
     */
    public NodeIterator getAllLinearFrozenNodes() throws RepositoryException;

    /**
     * Returns an iterator over all the frozen nodes of all the versions of
     * this version history. Under simple versioning the order of the returned
     * nodes will be the order of their creation. Under full versioning the
     * order is implementation-dependent.
     *
     * @return a <code>NodeIterator</code> object.
     * @throws RepositoryException if an error occurs.
     * @since JCR 2.0
     */
    public NodeIterator getAllFrozenNodes() throws RepositoryException;


}
