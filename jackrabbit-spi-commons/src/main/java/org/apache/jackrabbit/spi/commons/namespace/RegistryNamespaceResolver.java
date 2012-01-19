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

import javax.jcr.NamespaceException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;

/**
 * Namespace resolver based on the repository-wide namespace mappings
 * stored in a namespace registry.
 */
public class RegistryNamespaceResolver implements NamespaceResolver {

    /**
     * Namespace registry
     */
    private final NamespaceRegistry registry;

    /**
     * Creates a new namespace resolver based on the given namespace registry.
     * 
     * @param registry namespace registry
     */
    public RegistryNamespaceResolver(NamespaceRegistry registry) {
        this.registry = registry;
    }

    public String getPrefix(String uri) throws NamespaceException {
        try {
            return registry.getPrefix(uri);
        } catch (RepositoryException e) {
            if (!(e instanceof NamespaceException)) {
                e = new NamespaceException(
                        "Failed to resolve namespace URI: " + uri, e);
            }
            throw (NamespaceException) e;
        }
    }

    public String getURI(String prefix) throws NamespaceException {
        try {
            return registry.getURI(prefix);
        } catch (RepositoryException e) {
            if (!(e instanceof NamespaceException)) {
                e = new NamespaceException(
                        "Failed to resolve namespace prefix: " + prefix, e);
            }
            throw (NamespaceException) e;
        }
    }
}
