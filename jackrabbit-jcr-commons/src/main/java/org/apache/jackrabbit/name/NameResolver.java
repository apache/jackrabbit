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
package org.apache.jackrabbit.name;

import javax.jcr.NamespaceException;

/**
 * Resolver for prefixed JCR names and namespace-qualified
 * {@link QName QNames}.
 *
 * @deprecated Use the NameResolver interface from
 *             the org.apache.jackrabbit.spi.commons.conversion package of
 *             the jackrabbit-spi-commons component.
 */
public interface NameResolver {

    /**
     * Returns the qualified name for the given prefixed JCR name.
     *
     * @param name prefixed JCR name
     * @return qualified name
     * @throws NameException if the JCR name format is invalid
     * @throws NamespaceException if the namespace prefix can not be resolved
     */
    QName getQName(String name) throws NameException, NamespaceException;

    /**
     * Returns the prefixed JCR name for the given qualified name.
     *
     * @param name qualified name
     * @return prefixed JCR name
     * @throws NamespaceException if the namespace URI can not be resolved
     */
    String getJCRName(QName name) throws NamespaceException;

}
