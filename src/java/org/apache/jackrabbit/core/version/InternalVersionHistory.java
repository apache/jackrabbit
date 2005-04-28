/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.core.version;

import org.apache.jackrabbit.core.QName;

import javax.jcr.version.VersionException;
import javax.jcr.RepositoryException;
import java.util.Iterator;

/**
 * This interface defines the internal version history.
 */
public interface InternalVersionHistory extends InternalVersionItem {

    /**
     * Equivalalent to {@link javax.jcr.version.VersionHistory#getRootVersion()}.
     *
     * @see javax.jcr.version.VersionHistory#getRootVersion()
     */
    InternalVersion getRootVersion();

    /**
     * Equivalalent to {@link javax.jcr.version.VersionHistory#getVersion(java.lang.String)}.
     *
     * @see javax.jcr.version.VersionHistory#getVersion(java.lang.String)
     */
    InternalVersion getVersion(QName versionName) throws VersionException;

    /**
     * Checks if the version with the given name exists in this version history.
     *
     * @param versionName the name of the version
     * @return <code>true</code> if the version exists;
     *         <code>false</code> otherwise.
     */
    boolean hasVersion(QName versionName);

    /**
     * Checks if the version for the given uuid exists in this history.
     *
     * @param uuid the uuid of the version
     * @return <code>true</code> if the version exists;
     *         <code>false</code> otherwise.
     */
    boolean hasVersion(String uuid);

    /**
     * Returns the version with the given uuid or <code>null</code> if the
     * respective version does not exist.
     *
     * @param uuid the uuid of the version
     * @return the internal version ot <code>null</code>
     */
    InternalVersion getVersion(String uuid);

    /**
     * Equivalalent to {@link javax.jcr.version.VersionHistory#getVersionByLabel(java.lang.String)}
     * but returns <code>null</code> if the version does not exists.
     *
     * @see javax.jcr.version.VersionHistory#getVersionByLabel(java.lang.String)
     */
    InternalVersion getVersionByLabel(QName label);

    /**
     * Removes the indicated version from this VersionHistory. If the specified
     * vesion does not exist, if it specifies the root version or if it is
     * referenced by any node e.g. as base version, a VersionException is thrown.
     * <p/>
     * all successors of the removed version become successors of the
     * predecessors of the removed version and vice versa. then, the entire
     * version node and all its subnodes are removed.
     *
     * @param versionName the name of the version to be removed
     * @throws RepositoryException if an error occurrs.
     */
    void removeVersion(QName versionName) throws RepositoryException;

    /**
     * Sets the version <code>label</code> to the given <code>version</code>.
     * If the label is already assigned to another version, a VersionException is
     * thrown unless <code>move</code> is <code>true</code>. If <code>version</code>
     * is <code>null</code>, the label is removed from the respective version.
     * In either case, the version the label was previously assigned to is returned,
     * or <code>null</code> of the label was not moved.
     *
     * @param version the name of the version
     * @param label the label to assgign
     * @param move  flag what to do by collisions
     * @return the version that was previously assigned by this label or <code>null</code>.
     * @throws VersionException
     */
    InternalVersion setVersionLabel(QName version, QName label, boolean move)
            throws VersionException;

    /**
     * Returns an iterator over all versions (not ordered yet), including the
     * root version.
     *
     * @return an iterator over {@link InternalVersion} objects.
     */
    Iterator getVersions();

    /**
     * Returns the number of versions in this version history.
     *
     * @return the number of versions, including the root version.
     */
    int getNumVersions();

    /**
     * Returns the UUID of the versionable node that this history belongs to.
     *
     * @return the UUID of the versionable node.
     */
    String getVersionableUUID();

    /**
     * Returns a string  iterator over all version labels that exist in this
     * version history
     *
     * @return
     */
    QName[] getVersionLabels();

    /**
     * Returns the UUID of the version labels node
     *
     * @return
     */
    String getVersionLabelsUUID();
}
