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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.jcr.NamespaceException;

/**
 * Name resolver decorator that uses a generational cache to speed up
 * parsing and formatting of JCR names. Uncached names are resolved using
 * the underlying decorated name resolver.
 * <p>
 * The cache consists of three parts: a long term cache and two generations
 * of recent cache entries. The two generations are used to collect recent new
 * entries, and those entries that are used within two successive generations
 * get promoted to the long term cache. The entries within the long term cache
 * are discarded only when the size of the cache exceeds the given maximum
 * cache size.
 */
public class CachingNameResolver implements NameResolver {

    /**
     * Default maximum cache size.
     */
    private static final int DEFAULT_CACHE_SIZE = 1000;

    /**
     * Divisor used to determine the default generation age from the
     * maximum cache size.
     */
    private static final int DEFAULT_SIZE_AGE_RATIO = 10;

    /**
     * Decorated name resolver.
     */
    private final NameResolver resolver;

    /**
     * Maximum size of the name cache.
     */
    private final int maxSize;

    /**
     * Maximum age of a cache generation.
     */
    private final int maxAge;

    /**
     * Long term name cache. Read only.
     */
    private Map cache = new HashMap();

    /**
     * Old cache generation. Read only.
     */
    private Map old = new HashMap();

    /**
     * Young cache generation. Write only.
     */
    private Map young = new HashMap();

    /**
     * Age of the young cache generation.
     */
    private int age = 0;

    /**
     * Creates a caching name resolver.
     *
     * @param resolver decorated name resolver
     * @param maxSize maximum size of the long term cache
     * @param maxAge maximum age of a cache generation
     */
    public CachingNameResolver(NameResolver resolver, int maxSize, int maxAge) {
        this.resolver = resolver;
        this.maxSize = maxSize;
        this.maxAge = maxAge;
    }

    /**
     * Creates a caching name resolver using the default generation age for
     * the given cache size.
     *
     * @param resolver decorated name resolver
     * @param maxSize maximum size of the long term cache
     */
    public CachingNameResolver(NameResolver resolver, int maxSize) {
        this(resolver, maxSize, maxSize / DEFAULT_SIZE_AGE_RATIO);
    }

    /**
     * Creates a caching name resolver using the default size and
     * generation age.
     *
     * @param resolver decorated name resolver
     */
    public CachingNameResolver(NameResolver resolver) {
        this(resolver, DEFAULT_CACHE_SIZE);
    }

    /**
     * Caches the given key-value pair and increases the age of the current
     * cache generation. When the maximum age of a generation is reached,
     * the following steps are taken:
     * <ol>
     *   <li>The union of the two cache generations is calculated</li>
     *   <li>The union is added to the long term name cache</li>
     *   <li>If the cache size exceeds the maximum, only the union is kept</li>
     *   <li>A new cache generation is started</li>
     * </ol>
     *
     * @param key key of the cache entry
     * @param value value of the cache entry
     */
    private synchronized void cache(Object key, Object value) {
        young.put(key, value);

        if (++age == maxAge) {
            Map union = new HashMap();
            Iterator iterator = old.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry entry = (Map.Entry) iterator.next();
                if (young.containsKey(entry.getKey())) {
                    union.put(entry.getKey(), entry.getValue());
                }
            }

            if (!union.isEmpty()) {
                if (cache.size() + union.size() <= maxSize) {
                    union.putAll(cache);
                }
                cache = union;
            }

            old = young;
            young = new HashMap();
            age = 0;
        }
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
            qname = (QName) old.get(name);
            if (qname == null) {
                qname = resolver.getQName(name);
            }
            cache(name, qname);
        }
        return qname;
    }


    /**
     * Returns the prefixed JCR name for the given qualified name.
     * If the name is in the default namespace, then the local name
     * is returned without a prefix. Otherwise the name is first looked
     * up form the generational cache and the call gets delegated to the
     * decorated name resolver only if the cache misses.
     *
     * @param qname qualified name
     * @return prefixed JCR name
     * @throws NamespaceException if the namespace URI can not be resolved
     */
    public String getJCRName(QName qname) throws NamespaceException {
        String name = (String) cache.get(qname);
        if (name == null) {
            name = (String) old.get(qname);
            if (name == null) {
                name = resolver.getJCRName(qname);
            }
            cache(qname, name);
        }
        return name;
    }

}
