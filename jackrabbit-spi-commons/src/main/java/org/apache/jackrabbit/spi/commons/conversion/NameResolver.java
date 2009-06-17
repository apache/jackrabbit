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

import javax.jcr.NamespaceException;

/**
 * Resolver for JCR name Strings and {@link Name} objects.
 */
public interface NameResolver {

    /**
     * Returns the <code>Name</code> for the given JCR name String.
     *
     * @param name A JCR name String.
     * @return A <code>Name</code> object.
     * @throws IllegalNameException if the JCR name format is invalid
     * @throws NamespaceException if the namespace prefix can not be resolved
     */
    Name getQName(String name) throws IllegalNameException, NamespaceException;

    /**
     * Returns the qualified JCR name String for the given <code>Name</code> object.
     *
     * @param name A <code>Name</code> object.
     * @return The qualified JCR name String consisting of
     * <code>prefix:localName</code> or
     * <code>localName</code> in case of the empty namespace.
     * @throws NamespaceException if the namespace URI can not be resolved
     */
    String getJCRName(Name name) throws NamespaceException;

}
