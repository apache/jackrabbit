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
package org.apache.jackrabbit.spi;

import org.apache.jackrabbit.name.QName;

import java.util.Collection;

import javax.jcr.nodetype.NodeType;

/**
 * <code>QNodeTypeDefinition</code> is the qualified SPI representation of a
 * {@link javax.jcr.nodetype.NodeType node type}. It refers to qualified names
 * only and is therefore independant of session-specific namespace mappings.
 * 
 * @see javax.jcr.nodetype.NodeType
 */
public interface QNodeTypeDefinition {

    /**
     * Returns the name of the node type being defined or
     * <code>null</code> if not set.
     *
     * @return the name of the node type or <code>null</code> if not set.
     */
    public QName getQName();

    /**
     * Returns an array containing the names of the supertypes. If no
     * supertypes have been specified, then an empty array is returned
     * for mixin types and the <code>nt:base</code> primary type and
     * an array containing just <code>nt:base<code> for other primary types.
     * <p>
     * The returned array must not be modified by the application.
     *
     * @return an array of supertype names
     */
    public QName[] getSupertypes();

    /**
     * Returns the value of the mixin flag.
     *
     * @return true if this is a mixin node type; false otherwise.
     */
    public boolean isMixin();

    /**
     * Returns the value of the orderableChildNodes flag.
     *
     * @return true if nodes of this node type can have orderable child nodes; false otherwise.
     */
    public boolean hasOrderableChildNodes();

    /**
     * Returns the name of the primary item (one of the child items of the
     * node's of this node type) or <code>null</code> if not set.
     *
     * @return the name of the primary item or <code>null</code> if not set.
     */
    public QName getPrimaryItemName();

    /**
     * Returns an array containing the property definitions or
     * <code>null</code> if not set.
     *
     * @return an array containing the property definitions or
     *         <code>null</code> if not set.
     */
    public QPropertyDefinition[] getPropertyDefs();

    /**
     * Returns an array containing the child node definitions or
     * <code>null</code> if not set.
     *
     * @return an array containing the child node definitions or
     *         <code>null</code> if not set.
     */
    public QNodeDefinition[] getChildNodeDefs();

    /**
     * Returns a collection of node type <code>QName</code>s that are being
     * referenced by <i>this</i> node type definition (e.g. as supertypes, as
     * required/default primary types in child node definitions, as REFERENCE
     * value constraints in property definitions).
     * <p/>
     * Note that self-references (e.g. a child node definition that specifies
     * the declaring node type as the default primary type) are not considered
     * dependencies.
     *
     * @return a collection of node type <code>QName</code>s
     */
    public Collection getDependencies();
}
