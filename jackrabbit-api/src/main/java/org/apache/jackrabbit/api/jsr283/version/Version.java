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

import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 * A <code>Version</code> object wraps an <code>nt:version</code> node. It
 * provides convenient access to version information.
 */
public interface Version extends javax.jcr.version.Version {

    /**
     * Assuming that this <code>Version</code> object was acquired through a <code>Workspace</code>
     * <code>W</code> and is within the <code>VersionHistory</code> <code>H</code>,
     * this method returns the successor of this version along the same line of descent
     * as is returned by <code>H.getAllLinearVersions()</code> where <code>H</code>
     * was also acquired through <code>W</code>.
     * <p>
     * Note that under simple versioing the behavior of this method is equivalent to
     * getting the unique successor (if any) of this version.
     *
     * @see VersionHistory#getAllLinearVersions
     * @return a <code>Version</code> or <code>null</code> if no linear successor exists.
     * @throws RepositoryException if an error occurs.
     */
    public Version getLinearSuccessor() throws RepositoryException;

    /**
     * Assuming that this <code>Version</code> object was acquired through a <code>Workspace</code>
     * <code>W</code> and is within the <code>VersionHistory</code> <code>H</code>,
     * this method returns the predecessor of this version along the same line of descent
     * as is returned by <code>H.getAllLinearVersions()</code> where <code>H</code>
     * was also acquired through <code>W</code>.
     * <p>
     * Note that under simple versioning the behavior of this method is equivalent to
     * getting the unique predecessor (if any) of this version.
     *
     * @see VersionHistory#getAllLinearVersions
     * @return a <code>Version</code> or <code>null</code> if no linear predecessor exists.
     * @throws RepositoryException if an error occurs.
     */
    public javax.jcr.version.Version getLinearPredecessor() throws RepositoryException;

    /**
     * Returns the frozen node of this version.
     *
     * @return a <code>Node</code> object
     * @throws RepositoryException if an error occurs.
     * @since JCR 2.0
     */
    public Node getFrozenNode() throws RepositoryException;
}