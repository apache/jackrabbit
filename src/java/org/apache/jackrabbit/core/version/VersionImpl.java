/*
 * Copyright 2004 The Apache Software Foundation.
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

import org.apache.jackrabbit.core.NodeImpl;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.version.Version;
import java.util.Calendar;

/**
 * This Class implements a Version that extends the node interface
 */
public class VersionImpl extends NodeWrapper implements Version {

    /**
     * the internal version
     */
    protected final InternalVersion version;

    /**
     * Creates a new version implementation
     *
     * @param session
     * @param version
     * @throws RepositoryException
     */
    protected VersionImpl(Session session, InternalVersion version)
            throws RepositoryException {
        super((NodeImpl) session.getNodeByUUID(version.getUUID()));
        this.version = version;
    }

    /**
     * @see Version#getCreated()
     */
    public Calendar getCreated() throws RepositoryException {
        return version.getCreated();
    }

    /**
     * @see Version#getVersionLabels()
     */
    public String[] getVersionLabels() throws RepositoryException {
        return version.internalGetLabels();
    }

    public void addVersionLabel(String label) throws RepositoryException {
        version.getVersionHistory().addVersionLabel(version, label, false);
    }

    public void removeVersionLabel(String label) throws RepositoryException {
        version.getVersionHistory().removeVersionLabel(label);
    }

    /**
     * @see Version#getSuccessors()
     */
    public Version[] getSuccessors() throws RepositoryException {
        // need to wrap it around proper node
        InternalVersion[] suc = version.getSuccessors();
        Version[] ret = new Version[suc.length];
        for (int i = 0; i < suc.length; i++) {
            ret[i] = new VersionImpl(unwrap().getSession(), suc[i]);
        }
        return ret;
    }

    /**
     * @see Version#getPredecessors()
     */
    public Version[] getPredecessors() throws RepositoryException {
        // need to wrap it around proper node
        InternalVersion[] pred = version.getPredecessors();
        Version[] ret = new Version[pred.length];
        for (int i = 0; i < pred.length; i++) {
            ret[i] = new VersionImpl(unwrap().getSession(), pred[i]);
        }
        return ret;
    }

    /**
     * @see javax.jcr.Node#getUUID()
     */
    public String getUUID() throws UnsupportedRepositoryOperationException, RepositoryException {
        return version.getUUID();
    }

    /**
     * Returns the internal version
     *
     * @return
     */
    public InternalVersion getInternalVersion() {
        return version;
    }

    /**
     * Returns the forzen node of this version
     *
     * @return
     * @throws RepositoryException
     */
    public InternalFrozenNode getFrozenNode() throws RepositoryException {
        return version.getFrozenNode();
    }

}
