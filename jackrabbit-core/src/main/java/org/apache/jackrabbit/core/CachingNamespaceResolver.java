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
package org.apache.jackrabbit.core;

import org.apache.commons.collections.map.LRUMap;
import org.apache.jackrabbit.name.AbstractNamespaceResolver;
import org.apache.jackrabbit.name.IllegalNameException;
import org.apache.jackrabbit.name.NameFormat;
import org.apache.jackrabbit.name.NamespaceListener;
import org.apache.jackrabbit.name.NamespaceResolver;
import org.apache.jackrabbit.name.NoPrefixDeclaredException;
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.name.NameCache;
import org.apache.jackrabbit.name.UnknownPrefixException;

import javax.jcr.NamespaceException;
import java.util.Map;

/**
 * Implements a {@link NamespaceResolver} that caches QName to resolved jcr names
 * and vice versa. The cache is invalidated when a namespace uri to prefix
 * mapping is changed.
 */
class CachingNamespaceResolver
        implements NamespaceResolver, NamespaceListener, NameCache {

    /**
     * The base namespace resolver.
     */
    private final AbstractNamespaceResolver base;

    /**
     * Maps QName instances to resolved jcr name Strings.
     */
    private final Map qnameToJCRName;

    /**
     * Maps resolved jcr name Strings to QName instances.
     */
    private final Map jcrNameToQName;

    /**
     * Creates a new <code>CachingNamespaceResolver</code>.
     *
     * @param base      a base namespace resolver with support for listener
     *                  registration.
     * @param cacheSize number of mappings this resolver may cache.
     */
    public CachingNamespaceResolver(AbstractNamespaceResolver base, int cacheSize) {
        this.base = base;
        qnameToJCRName = new LRUMap(cacheSize);
        jcrNameToQName = new LRUMap(cacheSize);
        this.base.addListener(this);
    }

    /**
     * @inheritDoc
     */
    public String getURI(String prefix) throws NamespaceException {
        return base.getURI(prefix);
    }

    /**
     * @inheritDoc
     */
    public String getPrefix(String uri) throws NamespaceException {
        return base.getPrefix(uri);
    }

    /**
     * @deprecated use {@link NameFormat#parse(String, NamespaceResolver)}
     */
    public synchronized QName getQName(String name)
            throws IllegalNameException, UnknownPrefixException {
        return NameFormat.parse(name, this);
    }

    /**
     * @deprecated use {@link NameFormat#format(QName, NamespaceResolver)}
     */
    public synchronized String getJCRName(QName name)
            throws NoPrefixDeclaredException {
        return NameFormat.format(name, this);
    }

    /**
     * Disposes this <code>CachingNamespaceResolver</code>.
     */
    public void dispose() {
        base.removeListener(this);
    }

    //------------------------------------------------------------< NameCache >

    /**
     * @inheritDoc
     */
    public synchronized QName retrieveName(String jcrName) {
        return (QName) jcrNameToQName.get(jcrName);
    }

    /**
     * @inheritDoc
     */
    public synchronized String retrieveName(QName name) {
        return (String) qnameToJCRName.get(name);
    }

    /**
     * @inheritDoc
     */
    public synchronized void cacheName(String jcrName, QName name) {
        qnameToJCRName.put(name, jcrName);
        jcrNameToQName.put(jcrName, name);
    }

    /**
     * @inheritDoc
     */
    public synchronized void evictAllNames() {
        qnameToJCRName.clear();
        jcrNameToQName.clear();
    }

    //----------------------------------------------------< NamespaceListener >

    /**
     * @inheritDoc
     */
    public void namespaceAdded(String prefix, String uri) {
        // since it is a new namespace there's no need to flush the
        // cached mappings
    }

    /**
     * @inheritDoc
     * Invalidates all cached mappings.
     */
    public void namespaceRemapped(String oldPrefix, String newPrefix, String uri) {
        evictAllNames();
    }

    /**
     * @inheritDoc
     * Invalidates all cached mappings.
     */
    public void namespaceRemoved(String uri) {
        evictAllNames();
    }
}
