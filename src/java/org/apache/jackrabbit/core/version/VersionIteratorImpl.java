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

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.version.Version;
import javax.jcr.version.VersionIterator;
import java.util.*;

/**
 * This Class implements a VersionIterator that iterates over a version
 * graph following the successor nodes. When this iterator is created, it gathers
 * the id's of the versions and returns them when iterating. please note, that
 * a version can be deleted while traversing this iterator and the 'nextVesion'
 * would produce a  ConcurrentModificationException.
 */
public class VersionIteratorImpl implements VersionIterator {

    /**
     * the id's of the versions to return
     */
    private LinkedList versions = new LinkedList();

    /**
     * the current position
     */
    private int pos = 0;

    /**
     * the session for wrapping the versions
     */
    private final Session session;

    /**
     * Creates a new VersionIterator that iterates over the version tree,
     * starting the root node.
     *
     * @param rootVersion
     */
    public VersionIteratorImpl(Session session, InternalVersion rootVersion) {
        this.session = session;

        addVersion(rootVersion);
    }

    /**
     * @see VersionIterator#nextVersion()
     */
    public Version nextVersion() {
        if (versions.isEmpty()) {
            throw new NoSuchElementException();
        }
        String id = (String) versions.removeFirst();
        pos++;

        try {
            return (Version) session.getNodeByUUID(id);
        } catch (RepositoryException e) {
            throw new ConcurrentModificationException("Unable to provide element: " + e.toString());
        }
    }

    /**
     * @see VersionIterator#skip(long)
     */
    public void skip(long skipNum) {
        while (skipNum > 0) {
            skipNum--;
            nextVersion();
        }
    }

    /**
     * @see VersionIterator#getSize()
     */
    public long getSize() {
        return versions.size();
    }

    /**
     * @see VersionIterator#getPos()
     */
    public long getPos() {
        return pos;
    }

    /**
     * @throws UnsupportedOperationException since this operation is not supported
     * @see VersionIterator#remove()
     */
    public void remove() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * @see VersionIterator#hasNext()
     */
    public boolean hasNext() {
        return !versions.isEmpty();
    }

    /**
     * @see VersionIterator#next()
     */
    public Object next() {
        return nextVersion();
    }

    /**
     * Adds the version 'v' to the list of versions to return and then calls
     * it self recursively with all the verions prodecessors.
     * @param v
     */
    private synchronized void addVersion(InternalVersion v) {
        String id = v.getId();
        if (!versions.contains(id)) {
            versions.add(id);
            InternalVersion[] vs = v.getSuccessors();
            for (int i=0; i<vs.length; i++) {
                addVersion(vs[i]);
            }
        }
    }
}
