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
package org.apache.jackrabbit.spi2jcr;

import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.conversion.NameException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.NamespaceException;

/**
 * <code>ChildInfoImpl</code> implements a <code>ChildInfo</code> and provides
 * information about a child node.
 */
class ChildInfoImpl extends org.apache.jackrabbit.spi.commons.ChildInfoImpl {

    /**
     * Creates a new <code>ChildInfoImpl</code> for <code>node</code>.
     *
     * @param node       the JCR node.
     * @param resolver
     * @throws RepositoryException    if an error occurs while reading from
     *                                <code>node</code>.
     * @throws NameException   if the <code>node</code> name is illegal.
     * @throws NamespaceException if the name of the <code>node</code>
     *                                contains a prefix not known to
     *                                <code>nsResolver</code>.
     */
    public ChildInfoImpl(Node node, NamePathResolver resolver)
            throws NamespaceException, NameException, RepositoryException {
        super(resolver.getQName(node.getName()),
                getUniqueId(node), node.getIndex());
    }

    /**
     * @param node the JCR node.
     * @return the unique id for the <code>node</code> or <code>null</code> if
     *         the node does not have a unique id.
     * @throws RepositoryException if an error occurs while reading the unique
     *                             id.
     */
    private static String getUniqueId(Node node) throws RepositoryException {
        String uuid = null;
        try {
            uuid = node.getUUID();
        } catch (UnsupportedRepositoryOperationException e) {
            // not referenceable
        }
        return uuid;
    }
}
