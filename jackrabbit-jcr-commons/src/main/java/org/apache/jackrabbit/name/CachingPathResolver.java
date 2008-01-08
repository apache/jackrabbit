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
 * Path resolver decorator that uses a generational cache to speed up
 * parsing and formatting of JCR paths. Uncached paths are resolved using
 * the underlying decorated path resolver.
 *
 * @deprecated Use the CachingPathResolver class from 
 *             the org.apache.jackrabbit.spi.commons.conversion package of
 *             the jackrabbit-spi-commons component.
 */
public class CachingPathResolver implements PathResolver {

    /**
     * Decorated path resolver.
     */
    private final PathResolver resolver;

    /**
     * Generational cache.
     */
    private final GenerationalCache cache;

    /**
     * Creates a caching decorator for the given path resolver. The given
     * generational cache is used for caching.
     *
     * @param resolver decorated path resolver
     * @param cache generational cache
     */
    public CachingPathResolver(PathResolver resolver, GenerationalCache cache) {
        this.resolver = resolver;
        this.cache = cache;
    }

    /**
     * Creates a caching decorator for the given path resolver.
     *
     * @param resolver name resolver
     */
    public CachingPathResolver(PathResolver resolver) {
        this(resolver, new GenerationalCache());
    }

    //--------------------------------------------------------< PathResolver >

    /**
     * Returns the qualified path for the given prefixed JCR path. The path
     * is first looked up form the generational cache and the call gets
     * delegated to the decorated path resolver only if the cache misses.
     *
     * @param path prefixed JCR path
     * @return qualified path
     * @throws NameException if the JCR path format is invalid
     * @throws NamespaceException if a namespace prefix can not be resolved
     */
    public Path getQPath(String path) throws NameException, NamespaceException {
        Path qpath = (Path) cache.get(path);
        if (qpath == null) {
            qpath = resolver.getQPath(path);
            cache.put(path, qpath);
        }
        return qpath;
    }


    /**
     * Returns the prefixed JCR path for the given qualified path. The path
     * is first looked up form the generational cache and the call gets
     * delegated to the decorated path resolver only if the cache misses.
     *
     * @param qpath qualified path
     * @return prefixed JCR path
     * @throws NamespaceException if a namespace URI can not be resolved
     */
    public String getJCRPath(Path qpath) throws NamespaceException {
        String path = (String) cache.get(qpath);
        if (path == null) {
            path = resolver.getJCRPath(qpath);
            cache.put(qpath, path);
        }
        return path;
    }

}
