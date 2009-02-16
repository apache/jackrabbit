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

import org.apache.jackrabbit.spi.commons.namespace.NamespaceResolver;

/**
 * Dummy namespace resolver for use in unit testing. This namespace resolver
 * maps each valid XML prefix string to the same string as the namespace URI
 * and vice versa.
 */
public class DummyNamespaceResolver implements NamespaceResolver {

    /**
     * Returns the given prefix.
     *
     * @param prefix namespace prefix
     * @return the given prefix
     */
    public String getURI(String prefix) throws NamespaceException {
        return prefix;
    }

    /**
     * Returns the given namespace URI as the corresponding prefix.
     *
     * @param uri namespace URI
     * @return the given URI
     */
    public String getPrefix(String uri) {
        return uri;
    }
}
