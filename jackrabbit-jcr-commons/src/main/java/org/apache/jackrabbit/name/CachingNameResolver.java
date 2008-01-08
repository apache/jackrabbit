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
package org.apache.jackrabbit.name;

import javax.jcr.NamespaceException;

/**
 * Name resolver decorator that uses a generational cache to speed up
 * parsing and formatting of JCR names. Uncached names are resolved using
 * the underlying decorated name resolver.
 *
 * @deprecated Use the CachingNameResolver class from 
 *             the org.apache.jackrabbit.spi.commons.conversion package of
 *             the jackrabbit-spi-commons component.
 */
public class CachingNameResolver implements NameResolver {

    /**
     * Decorated name resolver.
     */
    private final NameResolver resolver;

    /**
     * Generational cache.
     */
    private final GenerationalCache cache;

    /**
     * Creates a caching decorator for the given name resolver. The given
     * generational cache is used for caching.
     *
     * @param resolver decorated name resolver
     * @param cache generational cache
     */
    public CachingNameResolver(NameResolver resolver, GenerationalCache cache) {
        this.resolver = resolver;
        this.cache = cache;
    }

    /**
     * Creates a caching decorator for the given name resolver.
     *
     * @param resolver name resolver
     */
    public CachingNameResolver(NameResolver resolver) {
        this(resolver, new GenerationalCache());
    }

    //--------------------------------------------------------< NameResolver >

    /**
     * Returns the qualified name for the given prefixed JCR name. The name
     * is first looked up form the generational cache and the call gets
     * delegated to the decorated name resolver only if the cache misses.
     *
     * @param name prefixed JCR name
     * @return qualified name
     * @throws NameException if the JCR name format is invalid
     * @throws NamespaceException if the namespace prefix can not be resolved
     */
    public QName getQName(String name)
            throws NameException, NamespaceException {
        QName qname = (QName) cache.get(name);
        if (qname == null) {
            qname = resolver.getQName(name);
            cache.put(name, qname);
        }
        return qname;
    }


    /**
     * Returns the prefixed JCR name for the given qualified name. The name
     * is first looked up form the generational cache and the call gets
     * delegated to the decorated name resolver only if the cache misses.
     *
     * @param qname qualified name
     * @return prefixed JCR name
     * @throws NamespaceException if the namespace URI can not be resolved
     */
    public String getJCRName(QName qname) throws NamespaceException {
        String name = (String) cache.get(qname);
        if (name == null) {
            name = resolver.getJCRName(qname);
            cache.put(qname, name);
        }
        return name;
    }

}
