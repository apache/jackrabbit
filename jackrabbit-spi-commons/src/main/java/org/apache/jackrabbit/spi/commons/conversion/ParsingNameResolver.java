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

import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.NameFactory;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceResolver;

import javax.jcr.NamespaceException;

/**
 * Name resolver that parsers and formats prefixed JCR names.
 * A {@link NamespaceResolver} is used for resolving the namespace prefixes.
 */
public class ParsingNameResolver implements NameResolver {

    /**
     * Name factory.
     */
    private final NameFactory nameFactory;

    /**
     * Namespace resolver.
     */
    private final NamespaceResolver resolver;

    /**
     * Creates a parsing name resolver.
     *
     * @param nameFactory the name factory.
     * @param resolver namespace resolver
     */
    public ParsingNameResolver(NameFactory nameFactory, NamespaceResolver resolver) {
        this.nameFactory = nameFactory;
        this.resolver = resolver;
    }

    //--------------------------------------------------------< NameResolver >

    /**
     * Parses the prefixed JCR name and returns the resolved qualified name.
     *
     * @param name prefixed JCR name
     * @return qualified name
     * @throws IllegalNameException if the JCR name format is invalid
     * @throws NamespaceException if the namespace prefix can not be resolved
     */
    public Name getQName(String name) throws IllegalNameException, NamespaceException {
        return NameParser.parse(name, resolver, nameFactory);
    }

    /**
     * Returns the prefixed JCR name for the given qualified name.
     * If the name is in the default namespace, then the local name
     * is returned without a prefix. Otherwise the prefix for the
     * namespace is resolved and used to construct returned the JCR name.
     *
     * @param name qualified name
     * @return prefixed JCR name
     * @throws NamespaceException if the namespace URI can not be resolved
     */
    public String getJCRName(Name name) throws NamespaceException {
        String uri = name.getNamespaceURI();
        if (uri.length() == 0) {
            return name.getLocalName();
        } else {
            return resolver.getPrefix(uri) + ":" + name.getLocalName();
        }
    }
}
