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
package org.apache.jackrabbit.core.version;

import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.SessionImpl;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.version.Version;
import javax.jcr.version.VersionIterator;
import java.util.ConcurrentModificationException;
import java.util.LinkedList;
import java.util.NoSuchElementException;

/**
 * This Class implements a VersionIterator that iterates over a version
 * graph following the successor nodes. When this iterator is created, it gathers
 * the id's of the versions and returns them when iterating. please note, that
 * a version can be deleted while traversing this iterator and the 'nextVersion'
 * would produce a  ConcurrentModificationException.
 * <p>
 * If this iterator is initialized with a base version, it will only iterate
 * on the versions of a single line of decent from the given root version to the
 * base version.
 */
class VersionIteratorImpl implements VersionIterator {

    /**
     * the id's of the versions to return
     */
    private final LinkedList<NodeId> versions = new LinkedList<NodeId>();

    /**
     * the current position
     */
    private int pos = 0;

    /**
     * the session for wrapping the versions
     */
    private final SessionImpl session;

    /**
     * The number of versions available.
     */
    private final long size;

    /**
     * Creates a new VersionIterator that iterates over the version tree,
     * starting the root node.
     *
     * @param session repository session
     * @param rootVersion the root version
     */
    public VersionIteratorImpl(Session session, InternalVersion rootVersion) {
        this(session, rootVersion, null);
    }

    /**
     * Creates a new VersionIterator that iterates over a single line of decent
     * of all versions starting at the root version and ending at the given
     * base version
     *
     * @param session repository session
     * @param rootVersion the root version
     * @param baseVersion the ending base version
     */
    public VersionIteratorImpl(Session session, InternalVersion rootVersion,
                               InternalVersion baseVersion) {
        this.session = (SessionImpl) session;
        if (baseVersion == null) {
            collectAllVersions(rootVersion);
        } else {
            collectLinearVersions(baseVersion);
        }
        // retrieve initial size, since size of the list is not stable
        size = versions.size();
    }

    /**
     * {@inheritDoc}
     */
    public Version nextVersion() {
        if (versions.isEmpty()) {
            throw new NoSuchElementException();
        }
        NodeId id = versions.removeFirst();
        pos++;

        try {
            return (Version) session.getNodeById(id);
        } catch (RepositoryException e) {
            throw new ConcurrentModificationException("Unable to provide element: " + e.toString());
        }
    }

    /**
     * {@inheritDoc}
     */
    public void skip(long skipNum) {
        while (skipNum > 0) {
            skipNum--;
            nextVersion();
        }
    }

    /**
     * {@inheritDoc}
     */
    public long getSize() {
        return size;
    }

    /**
     * {@inheritDoc}
     */
    public long getPosition() {
        return pos;
    }

    /**
     * {@inheritDoc}
     * @throws UnsupportedOperationException since this operation is not supported
     */
    public void remove() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasNext() {
        return !versions.isEmpty();
    }

    /**
     * {@inheritDoc}
     */
    public Object next() {
        return nextVersion();
    }

    /**
     * Adds the version subtree starting at <code>root</code> to the internal
     * set of versions.
     *
     * @param root the root version
     */
    private synchronized void collectAllVersions(InternalVersion root) {
        LinkedList<InternalVersion> workQueue = new LinkedList<InternalVersion>();
        workQueue.add(root);
        while (!workQueue.isEmpty()) {
            InternalVersion currentVersion = workQueue.removeFirst();
            NodeId id = currentVersion.getId();
            if (!versions.contains(id)) {
                versions.add(id);
                workQueue.addAll(currentVersion.getSuccessors());
            }
        }

    }

    /**
     * Adds all versions of a single line of decent starting from the root
     * version to the <code>base</code> version.
     *
     * @param base the base version
     */
    private synchronized void collectLinearVersions(InternalVersion base) {
        while (base != null) {
            versions.addFirst(base.getId());
            InternalVersion[] preds = base.getPredecessors();
            if (preds.length == 0) {
                base = null;
            } else {
                base = preds[0];
            }
        }
    }
}
