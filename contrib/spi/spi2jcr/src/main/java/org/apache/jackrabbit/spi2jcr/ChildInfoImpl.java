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

import org.apache.jackrabbit.spi.ChildInfo;
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.name.NamespaceResolver;
import org.apache.jackrabbit.name.NameFormat;
import org.apache.jackrabbit.name.NameException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;

/**
 * <code>ChildInfoImpl</code> implements a <code>ChildInfo</code> and provides
 * information about a child node.
 */
class ChildInfoImpl implements ChildInfo {

    /**
     * The name of this child info.
     */
    private final QName name;

    /**
     * The unique id for this child info or <code>null</code> if it does not
     * have a unique id.
     */
    private final String uniqueId;

    /**
     * 1-based index of this child info.
     */
    private final int index;

    /**
     * Creates a new <code>ChildInfoImpl</code> for <code>node</code>.
     *
     * @param node       the JCR node.
     * @param nsResolver the namespace resolver in use.
     * @throws RepositoryException if an error occurs while reading from
     *                             <code>node</code>.
     */
    public ChildInfoImpl(Node node, NamespaceResolver nsResolver) throws RepositoryException {
        try {
            this.name = NameFormat.parse(node.getName(), nsResolver);
        } catch (NameException e) {
            throw new RepositoryException(e.getMessage(), e);
        }
        String uuid = null;
        try {
            uuid = node.getUUID();
        } catch (UnsupportedRepositoryOperationException e) {
            // not referenceable
        }
        this.uniqueId = uuid;
        this.index = node.getIndex();
    }

    /**
     * {@inheritDoc}
     */
    public QName getName() {
        return name;
    }

    /**
     * {@inheritDoc}
     */
    public String getUniqueID() {
        return uniqueId;
    }

    /**
     * {@inheritDoc}
     */
    public int getIndex() {
        return index;
    }
}
