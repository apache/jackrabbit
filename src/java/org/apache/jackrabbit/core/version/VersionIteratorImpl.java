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
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Stack;

/**
 * This Class implements a VersionIterator that iterates over a version
 * graph following the successor nodes.
 */
public class VersionIteratorImpl implements VersionIterator {

    /**
     * the current position
     */
    private int pos = 0;

    /**
     * the traversal stack
     */
    private Stack successors = new Stack();

    /**
     * the set of versions already returned. due to the topology of the version
     * graph it is possible to reach a version via different paths.
     */
    private Set visited = new HashSet();

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
        successors.push(rootVersion);
    }

    /**
     * @see VersionIterator#nextVersion()
     */
    public Version nextVersion() {
        if (successors.isEmpty()) {
            throw new NoSuchElementException();
        }
        InternalVersion ret = (InternalVersion) successors.pop();
        visited.add(ret);
        pos++;
        push(ret.getSuccessors());

        try {
            return (Version) session.getNodeByUUID(ret.getId());
        } catch (RepositoryException e) {
            throw new NoSuchElementException("Unable to provide element: " + e.toString());
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
        return -1;
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
        return !successors.isEmpty();
    }

    /**
     * @see VersionIterator#next()
     */
    public Object next() {
        return nextVersion();
    }

    /**
     * Pushes the versions on the stack
     *
     * @param versions
     */
    private void push(InternalVersion[] versions) {
        for (int i = 0; i < versions.length; i++) {
            if (!visited.contains(versions[i])) {
                successors.push(versions[i]);
            }
        }
    }
}
