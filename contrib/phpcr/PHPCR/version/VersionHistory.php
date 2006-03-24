<?php

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
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


require_once 'PHPCR/RepositoryException.php';
require_once 'PHPCR/version/Version.php';
require_once 'PHPCR/version/VersionIterator.php';
require_once 'PHPCR/ReferentialIntegrityException.php';
require_once 'PHPCR/AccessDeniedException.php';
require_once 'PHPCR/UnsupportedRepositoryOperationException.php';
require_once 'PHPCR/version/VersionException.php';


/**
 * A <code>VersionHistory</code> object wraps an <code>nt:versionHistory</code>
 * node. It provides convenient access to version history information.
 *
 * @author Markus Nix <mnix@mayflower.de>
 * @package phpcr
 * @subpackage version
 */
interface VersionHistory
{
    /**
     * Returns the UUID of the versionable node for which this is the version history.
     *
     * @return the UUID of the versionable node for which this is the version history.
     * @throws RepositoryException if an error occurs.
     */
    public function getVersionableUUID();

    /**
     * Returns the root version of this version history.
     *
     * @return a <code>Version</code> object.
     * @throws RepositoryException if an error occurs.
     */
    public function getRootVersion();

    /**
     * Returns an iterator over all the versions within this version history
     * The order of the returned objects will not necessarily correspond to the
     * order of versions in terms of the successor relation. To traverse the
     * version graph one must traverse the <code>jcr:successor REFERENCE</code>
     * properties starting with the root version. A version history will always
     * have at least one version, the root version. Therefore, this method will
     * always return an iterator of at least size 1.
     *
     * @return a <code>VersionIterator</code> object.
     * @throws RepositoryException if an error occurs.
     */
    public function getAllVersions();

    /**
     * Retrieves a particular version from this version history by version name.
     * <p/>
     * Throws a <code>VersionException</code> if the specified version is not in
     * this version history.
     *
     * @param versionName a version name
     * @return a <code>Version</code> object.
     * @throws VersionException if the specified version is not in this version history.
     * @throws RepositoryException if an error occurs.
     */
    public function getVersion( $versionName );

    /**
     * Retrieves a particular version from this version history by version label.
     * <p/>
     * Throws a <code>VersionException</code> if the specified <code>label</code> is not in
     * this version history.
     *
     * @param label a version label
     * @return a <code>Version</code> object.
     * @throws VersionException if the specified <code>label</code> is not in this version history.
     * @throws RepositoryException if an error occurs.
     */
    public function getVersionByLabel( $label );

    /**
     * Adds the specified label to the specified version. This corresponds to adding a
     * value to the <code>jcr:versionLabels</code> multi-value property of the
     * <code>nt:version</code> node that represents the specified version.
     * <p/>
     * Note that this change is made immediately; there is no need to call <code>save</code>.
     * In fact, since the the version storage is read-only with respect to normal repository
     * methods, <code>save</code> does not even function in this context.
     * <p/>
     * Within a particular version history, a given label may appear a maximum of once.
     * If the specified label is already assigned to a version in this history and
     * <code>moveLabel</code> is <code>true</code> then the label is removed from its
     * current location and added to the version with the specified <code>versionName</code>.
     * If <code>moveLabel</code> is <code>false</code>, then an attempt to add a label that
     * already exists in this version history will throw a <code>VersionException</code>.
     *
     * @param versionName the name of the version to which the label is to be added.
     * @param label the label to be added.
     * @param moveLabel if <code>true</code>, then if <code>label</code> is already assigned to a version in
     * this version history, it is moved to the new version specified; if <code>false</code>, then attempting
     * to assign an already used label will throw a <code>VersionException</code>.
     *
     * @throws VersionException if an attempt is made to add an existing label to a version history
     * and <code>moveLabel</code> is <code>false</code> or if the specifed version does not exisit in
     * this version history.
     * @throws RepositoryException if another error occurs.
     */
    public function addVersionLabel( $versionName, $label, $moveLabel );

    /**
     * Removes the specified label from among the labels of this version history.
     * This corresponds to removing a property from the <code>jcr:versionLabels</code>
     * child node of the <code>nt:versionHistory</code> node that represents this version
     * history.
     * <p/>
     * Note that this change is made immediately; there is no need to call <code>save</code>.
     * In fact, since the the version storage is read-only with respect to normal repository
     * methods, <code>save</code> does not even function in this context.
     * <p/>
     * If a label is specified that does not exist in this version history,
     * a <code>VersionException</code> is thrown.
     *
     * @param label a version label
     * @throws VersionException if the name labvel does not exist in this version history.
     * @throws RepositoryException if another error occurs.
     */
    public function removeVersionLabel( $label );

    /**
     * Returns true if the given version has the given <code>label</code>.
     * @param version a Version object
     * @param label a version label
     * @return a <code>boolean</code>.
     * @throws VersionException if the specified <code>version</code> is not of this version history.
     * @throws RepositoryException if another error occurs.
     *
     */
    public function hasVersionLabel( Version $version, $label );

    /**
     * Returns all version labels of the given <code>version</code> - empty array if none.
     * Throws a <code>VersionException</code> if the specified <code>version</code> is not
     * in this version history.
     * @param version
     * @return a <code>String</code> array containing all the labels of the given version
     * @throws VersionException if the specified <code>version</code> is not in this version history.
     * @throws RepositoryException if another error occurs.
     */
    public function getVersionLabels( Version $version );

    /**
     * Removes the named version from this version history and automatically
     * repairs the version graph. If the version to be removed is <code>V</code>, <code>V</code>'s
     * predecessor set is <code>P</code> and <code>V</code>'s successor set is <code>S</code>, then
     * the version graph is repaired s follows:
     * <ul>
     * <li>For each member of <code>P</code>, remove the reference to <code>V</code> from its
     * successor list and add references to each member of <code>S</code>.
     * <li>For each member of <code>S</code>, remove the reference to <code>V</code> from its
     * predecessor list and add references to each member of <code>P</code>.
     * </ul>
     * Note that this change is made immediately; there is no need to call <code>save</code>.
     * In fact, since the the version storage is read-only with respect to normal repository
     * methods, <code>save</code> does not even function in this context.
     * <p/>
     * A <code>ReferentialIntegrityException</code> will be thrown if the specified version is
     * currently the target of a <code>REFERENCE</code> property elsewhere in the repository
     * (not just in this workspace) and the current <code>Session</code> has read access to
     * that <code>REFERENCE</code> property.
     * <p/>
     * An <code>AccessDeniedException</code> will be thrown if the current <code>Session</code>
     * does not have permission to remove the specified version or if the specified version is
     * currently the target of a <code>REFERENCE</code> property elsewhere in the repository
     * (not just in this workspace) and the current <code>Session</code> does not have read
     * access to that <code>REFERENCE</code> property.
     * <p/>
     * Throws an <code>UnsupportedRepositoryOperationException</code> if this operation is
     * not supported by the implementation.
     * <p/>
     * Throws a <code>VersionException</code> if the named version is not in this <code>VersionHistory</code>.
     *
     * @param versionName the name of a version in this version history.
     * @throws ReferentialIntegrityException if the specified version is currently the target of a
     * <code>REFERENCE</code> property elsewhere in the repository (not necessarily in this workspace)
     * and the current <code>Session</code> has read access to that <code>REFERENCE</code> property.
     * @throws AccessDeniedException if the current Session does not have permission to remove the
     * specified version or if the specified version is currently the target of a <code>REFERENCE</code>
     * property elsewhere in the repository (not just in this workspace) and the current <code>Session</code>
     * does not have read access to that <code>REFERENCE</code> property.
     * @throws UnsupportedRepositoryOperationException if this operation is not supported by the implementation.
     * @throws VersionException if the named version is not in this version history.
     * @throws RepositoryException if another error occurs.
     */
    public function removeVersion( $versionName );
}

?>