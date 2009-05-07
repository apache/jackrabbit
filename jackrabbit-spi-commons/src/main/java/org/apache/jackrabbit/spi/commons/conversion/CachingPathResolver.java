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
package org.apache.jackrabbit.spi.commons.conversion;

import org.apache.jackrabbit.spi.Path;

import javax.jcr.NamespaceException;

/**
 * Path resolver decorator that uses a generational cache to speed up
 * parsing and formatting of JCR paths. Uncached paths are resolved using
 * the underlying decorated path resolver.
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
     * @throws MalformedPathException if the JCR path format is invalid
     * @throws IllegalNameException if any of the JCR names contained in the path are invalid.
     * @throws NamespaceException if a namespace prefix can not be resolved
     */
    public Path getQPath(String path) throws MalformedPathException, IllegalNameException, NamespaceException {
        return getQPath(path, true);
    }

    /**
     * @see PathResolver#getQPath(String, boolean) 
     */
    public Path getQPath(String path, boolean normalizeIdentifier) throws MalformedPathException, IllegalNameException, NamespaceException {
        Path qpath;
        /*
         * Jcr paths consisting of an identifier segment have 2 different
         * path object representations depending on the given resolution flag:
         * 1) a normalized absolute path if resolveIdentifier is true
         * 2) a path denoting an identifier if resolveIdentifier is false.
         * The latter are not cached in order not to return a wrong resolution
         * when calling getQPath with the same identifier-jcr-path.
         */
        if (path.startsWith("[") && !normalizeIdentifier) {
            qpath = resolver.getQPath(path, normalizeIdentifier);
        } else {
            qpath = (Path) cache.get(path);
            if (qpath == null) {
                qpath = resolver.getQPath(path, normalizeIdentifier);
                cache.put(path, qpath);
            }
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
