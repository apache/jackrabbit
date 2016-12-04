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

import javax.jcr.NamespaceException;

import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.commons.conversion.IllegalNameException;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceResolver;

/**
 * <code>AbstractNamespaceMappings</code> is the base class for index internal
 * namespace mappings.
 */
public abstract class AbstractNamespaceMappings
        implements NamespaceMappings, NamespaceResolver {

    /**
     * The name resolver used to translate the <code>Name</code>s to JCR name strings.
     */
    private final NamePathResolver resolver;

    public AbstractNamespaceMappings() {
        this.resolver = NamePathResolverImpl.create(this);
    }

    //----------------------------< NamespaceMappings >-------------------------

    /**
     * {@inheritDoc}
     */
    public String translateName(Name qName)
            throws IllegalNameException {
        try {
            return resolver.getJCRName(qName);
        } catch (NamespaceException e) {
            // should never happen actually, because we create yet unknown
            // uri mappings on the fly.
            throw new IllegalNameException("Internal error.", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public String translatePath(Path path) throws IllegalNameException {
        try {
            return resolver.getJCRPath(path);
        } catch (NamespaceException e) {
            // should never happen actually, because we create yet unknown
            // uri mappings on the fly.
            throw new IllegalNameException("Internal error.", e);
        }
    }

}
