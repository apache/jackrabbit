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

import javax.jcr.NamespaceException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.Session;

import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.apache.jackrabbit.spi.commons.name.PathFactoryImpl;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceResolver;
import org.apache.jackrabbit.spi.commons.namespace.RegistryNamespaceResolver;
import org.apache.jackrabbit.spi.commons.namespace.SessionNamespaceResolver;

/**
 * <code>DefaultNamePathResolver</code>...
 */
public class DefaultNamePathResolver implements NamePathResolver {

    private final NameResolver nResolver;

    private final PathResolver pResolver;

    public DefaultNamePathResolver(NamespaceResolver nsResolver) {
        this(nsResolver, false);
    }

    public DefaultNamePathResolver(Session session) {
        this(new SessionNamespaceResolver(session), ((session instanceof IdentifierResolver)? (IdentifierResolver) session : null), false);
    }

    public DefaultNamePathResolver(NamespaceRegistry registry) {
        this(new RegistryNamespaceResolver(registry));
    }

    public DefaultNamePathResolver(NamespaceResolver nsResolver, boolean enableCaching) {
        this(nsResolver, null, enableCaching);
    }

    public DefaultNamePathResolver(NamespaceResolver nsResolver, IdentifierResolver idResolver, boolean enableCaching) {
        NameResolver nr = new ParsingNameResolver(NameFactoryImpl.getInstance(), nsResolver);
        PathResolver pr = new ParsingPathResolver(PathFactoryImpl.getInstance(), nr, idResolver);
        if (enableCaching) {
            this.nResolver = new CachingNameResolver(nr);
            this.pResolver = new CachingPathResolver(pr);
        } else {
            this.nResolver = nr;
            this.pResolver = pr;
        }
    }

    public DefaultNamePathResolver(NameResolver nResolver, PathResolver pResolver) {
        this.nResolver = nResolver;
        this.pResolver = pResolver;
    }

    public Name getQName(String name) throws IllegalNameException, NamespaceException {
        return nResolver.getQName(name);
    }

    public String getJCRName(Name name) throws NamespaceException {
        return nResolver.getJCRName(name);
    }

    public Path getQPath(String path) throws MalformedPathException, IllegalNameException, NamespaceException {
        return pResolver.getQPath(path);
    }

    public Path getQPath(String path, boolean normalizeIdentifier) throws MalformedPathException, IllegalNameException, NamespaceException {
        return pResolver.getQPath(path, normalizeIdentifier);
    }

    public String getJCRPath(Path path) throws NamespaceException {
        return pResolver.getJCRPath(path);
    }
}