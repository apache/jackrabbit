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
package org.apache.jackrabbit.spi.commons.namespace;

import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;

/**
 * Provides default implementations for the methods:
 * <ul>
 * <li>{@link #addListener(NamespaceListener)}</li>
 * <li>{@link #removeListener(NamespaceListener)}</li>
 * </ul>
 * Subclasses may overwrite those methods with more efficient implementations
 * e.g. using caching. This class also adds optional support for
 * {@link NamespaceListener}s. To enable listener support call the constructor
 * with <code>supportListeners</code> set to <code>true</code>. The default
 * constructor will not enable listener support and all listener related
 * methods will throw an {@link UnsupportedOperationException} in that case.
 *
 * @deprecated https://issues.apache.org/jira/browse/JCR-1700
 */
public abstract class AbstractNamespaceResolver implements NamespaceResolver {

    private final Set listeners;

    /**
     * Creates a <code>AbstractNamespaceResolver</code> without listener
     * support.
     */
    public AbstractNamespaceResolver() {
        this(false);
    }

    /**
     * Creates a <code>AbstractNamespaceResolver</code> with listener support if
     * <code>supportListeners</code> is set to <code>true</code>.
     *
     * @param supportListeners if <code>true</code> listener are supported by
     *                         this instance.
     */
    public AbstractNamespaceResolver(boolean supportListeners) {
        if (supportListeners) {
            listeners = new HashSet();
        } else {
            listeners = null;
        }
    }

    //--------------------------------------------< NamespaceListener support >

    /**
     * Registers <code>listener</code> to get notifications when namespace
     * mappings change.
     *
     * @param listener the listener to register.
     * @throws UnsupportedOperationException if listener support is not enabled
     *                                       for this <code>AbstractNamespaceResolver</code>.
     */
    public void addListener(NamespaceListener listener) {
        if (listeners == null) {
            throw new UnsupportedOperationException("addListener");
        }
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    /**
     * Removes the <code>listener</code> from this <code>NamespaceRegistery</code>.
     *
     * @param listener the listener to remove.
     * @throws UnsupportedOperationException if listener support is not enabled
     *                                       for this <code>AbstractNamespaceResolver</code>.
     */
    public void removeListener(NamespaceListener listener) {
        if (listeners == null) {
            throw new UnsupportedOperationException("removeListener");
        }
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    /**
     * Notifies the listeners that a new namespace <code>uri</code> has been
     * added and mapped to <code>prefix</code>.
     *
     * @param prefix the prefix.
     * @param uri    the namespace uri.
     */
    protected void notifyNamespaceAdded(String prefix, String uri) {
        if (listeners == null) {
            throw new UnsupportedOperationException("notifyNamespaceAdded");
        }
        // addition is infrequent compared to listener registration
        // -> use copy-on-read
        NamespaceListener[] currentListeners;
        synchronized (listeners) {
            int i = 0;
            currentListeners = new NamespaceListener[listeners.size()];
            for (Iterator it = listeners.iterator(); it.hasNext();) {
                currentListeners[i++] = (NamespaceListener) it.next();
            }
        }
        for (int i = 0; i < currentListeners.length; i++) {
            currentListeners[i].namespaceAdded(prefix, uri);
        }
    }

    /**
     * Notifies listeners that an existing namespace uri has been remapped
     * to a new prefix.
     *
     * @param oldPrefix the old prefix.
     * @param newPrefix the new prefix.
     * @param uri the associated namespace uri.
     */
    protected void notifyNamespaceRemapped(String oldPrefix,
                                           String newPrefix,
                                           String uri) {
        if (listeners == null) {
            throw new UnsupportedOperationException("notifyNamespaceRemapped");
        }
        // remapping is infrequent compared to listener registration
        // -> use copy-on-read
        NamespaceListener[] currentListeners;
        synchronized (listeners) {
            int i = 0;
            currentListeners = new NamespaceListener[listeners.size()];
            for (Iterator it = listeners.iterator(); it.hasNext();) {
                currentListeners[i++] = (NamespaceListener) it.next();
            }
        }
        for (int i = 0; i < currentListeners.length; i++) {
            currentListeners[i].namespaceRemapped(oldPrefix, newPrefix, uri);
        }
    }

    /**
     * Notifies the listeners that the namespace with the given <code>uri</code>
     * has been removed from the mapping.
     *
     * @param uri the namespace uri.
     * @see NamespaceListener#namespaceRemoved(String)
     */
    protected void notifyNamespaceRemoved(String uri) {
        if (listeners == null) {
            throw new UnsupportedOperationException("notifyNamespaceRemapped");
        }
        // removal is infrequent compared to listener registration
        // -> use copy-on-read
        NamespaceListener[] currentListeners;
        synchronized (listeners) {
            int i = 0;
            currentListeners = new NamespaceListener[listeners.size()];
            for (Iterator it = listeners.iterator(); it.hasNext();) {
                currentListeners[i++] = (NamespaceListener) it.next();
            }
        }
        for (int i = 0; i < currentListeners.length; i++) {
            currentListeners[i].namespaceRemoved(uri);
        }
    }
}
