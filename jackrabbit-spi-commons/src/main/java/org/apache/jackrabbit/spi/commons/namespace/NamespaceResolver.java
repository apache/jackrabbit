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

/**
 * Interface for resolving namespace URIs and prefixes. Unlike the JCR
 * {@link javax.jcr.NamespaceRegistry} interface, this interface contains
 * no functionality other than the basic namespace URI and prefix resolution
 * methods. This interface is therefore used internally in many places where
 * the full namespace registry is either not available or some other mechanism
 * is used for resolving namespaces.
 */
public interface NamespaceResolver {

    /**
     * Returns the URI to which the given prefix is mapped.
     *
     * @param prefix namespace prefix
     * @return the namespace URI to which the given prefix is mapped.
     * @throws NamespaceException if the prefix is unknown.
     */
    String getURI(String prefix) throws NamespaceException;

    /**
     * Returns the prefix which is mapped to the given URI.
     *
     * @param uri namespace URI
     * @return the prefix mapped to the given URI.
     * @throws NamespaceException if the URI is unknown.
     */
    String getPrefix(String uri) throws NamespaceException;
}
