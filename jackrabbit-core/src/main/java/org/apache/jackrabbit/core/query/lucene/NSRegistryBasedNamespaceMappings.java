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
package org.apache.jackrabbit.core.query.lucene;

import org.apache.jackrabbit.spi.commons.conversion.IllegalNameException;
import org.apache.jackrabbit.spi.commons.conversion.NameResolver;
import org.apache.jackrabbit.core.NamespaceRegistryImpl;
import org.apache.jackrabbit.spi.commons.namespace.AbstractNamespaceResolver;
import org.apache.jackrabbit.spi.Name;

import javax.jcr.NamespaceException;

/**
 * <code>NSRegistryBasedNamespaceMappings</code> implements a namespace mapping
 * based on the stable index prefix provided by the namespace registry.
 */
public class NSRegistryBasedNamespaceMappings
        extends AbstractNamespaceResolver
        implements NamespaceMappings {

    /**
     * The namespace registry.
     */
    private final NamespaceRegistryImpl nsReg;

        /**
     * The name resolver used to translate the qualified name to JCR name
     */
    private final NameResolver nameResolver;

    /**
     * Creates a new <code>NSRegistryBasedNamespaceMappings</code>.
     *
     * @param nsReg the namespace registry of the repository.
     */
    NSRegistryBasedNamespaceMappings(NamespaceRegistryImpl nsReg) {
        this.nsReg = nsReg;
        this.nameResolver = NamePathResolverImpl.create(this);
    }

    //-------------------------------< NamespaceResolver >----------------------

    /**
     * {@inheritDoc}
     */
    public String getURI(String prefix) throws NamespaceException {
        try {
            int index = Integer.parseInt(prefix);
            return nsReg.getURI(index);
        } catch (NumberFormatException e) {
            throw new NamespaceException("Unknown prefix: " + prefix);
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getPrefix(String uri) throws NamespaceException {
        return String.valueOf(nsReg.getURIIndex(uri));
    }

    //-------------------------------< NamespaceMappings >----------------------

    /**
     * {@inheritDoc}
     */
    public String translatePropertyName(Name qName)
            throws IllegalNameException {
        try {
            return nameResolver.getJCRName(qName);
        } catch (NamespaceException e) {
            // should never happen actually, there is always a stable index
            // prefix for a known namespace uri
            throw new IllegalNameException("Internal error.", e);
        }
    }
}
