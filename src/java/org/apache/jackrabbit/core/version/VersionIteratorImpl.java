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
import javax.jcr.version.Version;
import javax.jcr.version.VersionIterator;
import java.util.NoSuchElementException;
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
     * Creates a new VersionIterator that iterates over the version tree,
     * starting the root node.
     *
     * @param rootVersion
     */
    public VersionIteratorImpl(Version rootVersion) throws RepositoryException {
        successors.push(rootVersion);
    }

    /**
     * @see VersionIterator#nextVersion()
     */
    public Version nextVersion() {
        if (successors.isEmpty()) {
            throw new NoSuchElementException();
        }
        Version ret = (Version) successors.peek();
        pos++;
        try {
            push(ret.getSuccessors());
        } catch (RepositoryException e) {
            // ignore
        }
        return ret;
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
    private void push(Version[] versions) {
        for (int i = 0; i < versions.length; i++) {
            successors.push(versions[i]);
        }
    }
}
