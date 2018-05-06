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
     * Returns the <code>Path</code> object for the given JCR path String.
     * The path is first looked up form the generational cache and the call gets
     * delegated to the decorated path resolver only if the cache misses.
     *
     * @param path A JCR path String.
     * @return A <code>Path</code> object.
     * @throws MalformedPathException if the JCR path format is invalid
     * @throws IllegalNameException if any of the JCR names contained in the
     * path are invalid.
     * @throws NamespaceException if a namespace prefix can not be resolved.
     * @see PathResolver#getQPath(String) 
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
     * Returns the JCR path String for the given <code>Path</code>. The path
     * is first looked up form the generational cache and the call gets
     * delegated to the decorated path resolver only if the cache misses.
     *
     * @param path A <code>Path</code> object.
     * @return A JCR path String in the standard form.
     * @throws NamespaceException if a namespace URI can not be resolved.
     * @see PathResolver#getJCRPath(org.apache.jackrabbit.spi.Path)
     */
    public String getJCRPath(Path path) throws NamespaceException {
        String jcrPath = (String) cache.get(path);
        if (jcrPath == null) {
            jcrPath = resolver.getJCRPath(path);
            cache.put(path, jcrPath);
        }
        return jcrPath;
    }
}
