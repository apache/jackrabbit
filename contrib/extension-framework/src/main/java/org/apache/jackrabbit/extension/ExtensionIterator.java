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
package org.apache.jackrabbit.extension;

import java.util.Iterator;
import java.util.NoSuchElementException;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * The <code>ExtensionIterator</code> class implements the iterator
 * over instances of the {@link ExtensionDescriptor}s.
 *
 * @author Felix Meschberger
 */
public class ExtensionIterator implements Iterator {

    /** Default log */
    private static final Log log = LogFactory.getLog(ExtensionIterator.class);

    /**
     * The type of the extension descriptors returned.
     */
    private final ExtensionType type;

    /**
     * The underlying iterator of nodes containing the extension descriptors.
     */
    private final NodeIterator nodes;

    /**
     * The preloaded next <code>EventDescriptor</code>. If <code>null</code>
     * no more descriptors are available in this iterator.
     */
    private ExtensionDescriptor next;

    /**
     * Creates an instance for the given underlying iterator of nodes.
     *
     * @param nodes The underlying <code>NodeIterator</code>.
     */
    /* package */ ExtensionIterator(ExtensionType type, NodeIterator nodes) {
        this.type = type;
        this.nodes = nodes;
        seek();
    }

    /**
     * Returns <code>true</code> if there is at least one more extension
     * descriptor available in this iterator.
     */
    public boolean hasNext() {
        return next != null;
    }

    /**
     * Returns the next available extension descriptor.
     *
     * @throws NoSuchElementException If no more extension descriptors are
     *      available.
     */
    public Object next() {
        return nextExtension();
    }

    /**
     * Returns the next available extension descriptor.
     *
     * @throws NoSuchElementException If no more extension descriptors are
     *      available.
     */
    public ExtensionDescriptor nextExtension() {
        if (next == null) {
            throw new NoSuchElementException("No more Descriptors");
        }

        ExtensionDescriptor toReturn = next;
        seek();
        return toReturn;
    }

    /**
     * Throws <code>UnsupportedOpertationException</code> because this
     * method is not supported by this implementation.
     */
    public void remove() {
        throw new UnsupportedOperationException("remove");
    }

    /**
     * Preload the next <code>ExtensionDescriptor</code> from the next node
     * in the underlying node iterator.
     * <p>
     * If an error occurrs instantiating an extension descriptor for any
     * node in the iterator, the node is ignored and the next node is
     * used. This is repeated until either no more nodes are available in
     * the underlying iterator or an extension descriptor can sucessfully
     * be created.
     */
    private void seek() {
        while (nodes.hasNext()) {
            try {
                Node extNode = nodes.nextNode();
                String name = ExtensionDescriptor.getExtensionName(extNode);
                next = type.getOrCreateExtension(name, extNode);
                return;
            } catch (RepositoryException re) {
                log.warn("Cannot get the extension name", re);
            } catch (ExtensionException ee) {
                log.warn("Cannot create extensions descriptor", ee);
            }
        }

        // fallback if no more nodes
        next = null;
    }
}
