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

import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.name.IllegalNameException;
import org.apache.jackrabbit.name.UnknownPrefixException;
import org.apache.jackrabbit.name.NoPrefixDeclaredException;
import org.apache.jackrabbit.name.NamespaceResolver;
import org.apache.jackrabbit.name.NamespaceListener;
import org.apache.jackrabbit.name.AbstractNamespaceResolver;
import org.apache.jackrabbit.name.Path;
import org.apache.jackrabbit.name.MalformedPathException;
import org.apache.jackrabbit.name.PathFormat;
import org.apache.commons.collections.map.LRUMap;

import javax.jcr.NamespaceException;
import java.util.Map;

/**
 * Implements a {@link NamespaceResolver} that caches QName to resolved jcr names
 * and vice versa. The cache is invalidated when a namespace uri to prefix
 * mapping is changed.
 */
class CachingNamespaceResolver
        implements NamespaceResolver, NamespaceListener {

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
     * @inheritDoc
     */
    public synchronized QName getQName(String name)
            throws IllegalNameException, UnknownPrefixException {
        QName qName = (QName) jcrNameToQName.get(name);
        if (qName == null) {
            qName = QName.fromJCRName(name, this);
            jcrNameToQName.put(name, qName);
        }
        return qName;
    }

    /**
     * @inheritDoc
     */
    public synchronized String getJCRName(QName name)
            throws NoPrefixDeclaredException {
        String jcrName = (String) qnameToJCRName.get(name);
        if (jcrName == null) {
            jcrName = name.toJCRName(this);
            qnameToJCRName.put(name, jcrName);
        }
        return jcrName;
    }

    /**
     * @inheritDoc
     * As currently paths are not cached, the call is delegated to
     * {@link PathFormat#parse(String, NamespaceResolver)}.
     */
    public Path getQPath(String jcrPath) throws MalformedPathException {
        return PathFormat.parse(jcrPath, this);
    }

    /**
     * @inheritDoc
     * As currently paths are not cached, the call is delegated to
     * {@link PathFormat#format(Path, NamespaceResolver)}.
     */
    public String getJCRPath(Path qPath) throws NoPrefixDeclaredException {
        return PathFormat.format(qPath, this);
    }

    /**
     * Disposes this <code>CachingNamespaceResolver</code>.
     */
    public void dispose() {
        base.removeListener(this);
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
        qnameToJCRName.clear();
        jcrNameToQName.clear();
    }

    /**
     * @inheritDoc
     * Invalidates all cached mappings.
     */
    public void namespaceRemoved(String uri) {
        qnameToJCRName.clear();
        jcrNameToQName.clear();
    }
}
