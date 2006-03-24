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


require_once 'PHPCR/Node.php';
require_once 'PHPCR/version/VersionHistory.php';
require_once 'PHPCR/RepositoryException.php';


/**
 * A <code>Version</code> object wraps an <code>nt:version</code> node. It
 * provides convenient access to version information.
 *
 * @author Markus Nix <mnix@mayflower.de>
 * @package phpcr
 * @subpackage version
 */
interface Version extends Node
{
    /**
     * Returns the <code>VersionHistory</code> that contains this <code>Version</code>.
     * @return the <code>VersionHistory</code> that contains this <code>Version</code>.
     * @throws RepositoryException if an error occurs.
     */
     public function getContainingHistory();

    /**
     * Returns the date this version was created. This corresponds to the value
     * of the <code>jcr:created</code> property in the <code>nt:version</code>
     * node that represents this version.
     *
     * @return date
     * @throws RepositoryException if an error occurs.
     */
    public function getCreated();

    /**
     * Returns the successor versions of this version. This corresponds to
     * returning all the <code>nt:version</code> nodes referenced by the
     * <code>jcr:successors</code> multi-value property in the
     * <code>nt:version</code> node that represents this version.
     *
     * @return a <code>Version</code> array.
     * @throws RepositoryException if an error occurs.
     */
    public function getSuccessors();

    /**
     * Returns the predecessor versions of this version. This corresponds to
     * returning all the <code>nt:version</code> nodes whose
     * <code>jcr:successors</code> property includes a reference to the
     * <code>nt:version</code> node that represents this version.
     *
     * @return a <code>Version</code> array.
     * @throws RepositoryException if an error occurs.
     */
    public function getPredecessors();
}

?>
