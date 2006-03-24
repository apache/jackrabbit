<?php

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


require_once 'PHPCR/RepositoryException.php';
require_once 'PHPCR/NamespaceException.php';
require_once 'PHPCR/UnsupportedRepositoryOperationException.php';


/**
 * The <code>Item</code> is the base interface of <code>Node</code>
 * and <code>Property</code>.
 *
 * @author Markus Nix <mnix@mayflower.de>
 * @package phpcr
 */
interface NamespaceRegistry
{
    /**
     * Sets a one-to-one mapping between prefix and URI in the global namespace registry of this repository.
     * Assigning a new prefix to a URI that already exists in the namespace registry erases the old prefix.
     * In general this can almost always be done, though an implementation is free to prevent particular
     * remappings by throwing a <code>NamespaceException</code>.
     * <p>
     * On the other hand, taking a prefix that is already assigned to a URI and re-assigning it to a new URI
     * in effect unregisters that URI. Therefore, the same restrictions apply to this operation as to
     * <code>NamespaceRegistry.unregisterNamespace</code>:
     * <ul>
     * <li>
     * Attempting to re-assign a built-in prefix (<code>jcr</code>, <code>nt</code>, <code>mix</code>,
     * <code>sv</code>) to a new URI will throw a <code>NamespaceException</code>.
     * </li>
     * <li>
     * Attempting to re-assign a prefix that is currently assigned to a URI that is present in content
     * (either within an item name or within the value of a <code>NAME</code> or <code>PATH</code> property)
     * will throw a <code>NamespaceException</code>. This includes prefixes in use within in-content node type definitions.
     * </li>
     * <li>
     * Attempting to assign any prefix beginning with "<code>xml</code>". These prefixes are reserved by the
     * XML specification.
     * </li>
     * <li>
     * An implementation may prevent the re-assignment of any other namespace prefixes for
     * implementation-specific reasons by throwing a <code>NamespaceException</code>.
     * </li>
     * </ul>
     * In a level 1 implementation, this method always throws an
     * <code>UnsupportedRepositoryOperationException</code>.
     * <p/>
     * A <code>RepositoryException</code> is thrown if another error occurs.
     *
     * @param prefix The prefix to be mapped.
     * @param uri The URI to be mapped.
     * @throws NamespaceException if an illegal attempt is made to register a mapping.
     * @throws UnsupportedRepositoryOperationException in a level 1 implementation
     * @throws RepositoryException if another error occurs.
     */
    public function registerNamespace( $prefix, $uri );

    /**
     * Removes a namespace mapping from the registry. The following restriction apply:
     * <ul>
     * <li>
     * Attempting to unregister a built-in namespace (<code>jcr</code>, <code>nt</code>,
     * <code>mix</code>, <code>sv</code>) will throw a <code>NamespaceException</code>.
     * </li>
     * <li>
     * Attempting to unregister a namespace that is currently present in content (either within an
     * item name or within the value of a <code>NAME</code> or <code>PATH</code> property)
     * will throw a <code>NamespaceException</code>. This includes prefixes in use within in-content node type
     * definitions.
     * </li>
     * <li>
     * An attempt to unregister a namespace that is not currently registered will throw a
     * <code>NamespaceException</code>.
     * </li>
     * <li>
     * An implementation may prevent the unregistering of any other namespace for
     * implementation-specific reasons by throwing a <code>NamespaceException</code>.
     * </li>
     * </ul>
     * In a level 1 implementation, this method always throws an
     * <code>UnsupportedRepositoryOperationException</code>.
     * <p/>
     * A <code>RepositoryException</code> is thrown if another error occurs.
     *
     * @param prefix The prefix of the mapping to be removed.
     * @throws NamespaceException if an illegal attempt is made to remove a mapping.
     * @throws UnsupportedRepositoryOperationException in a level 1 implementation
     * @throws RepositoryException if another error occurs.
     */
    public function unregisterNamespace( $prefix );

    /**
     * Returns an array holding all currently registered prefixes.
     * @return a string array
     * @throws RepositoryException if an error occurs.
     */
    public function getPrefixes();

    /**
     * Returns an array holding all currently registered URIs.
     * @return a string array
     * @throws RepositoryException if an error occurs.
     */
    public function getURIs();

    /**
     * Returns the URI to which the given prefix is mapped.
     * @param prefix a string
     * @return a string
     * @throws NamespaceException if the prefix is unknown.
     * @throws RepositoryException is another error occurs
     */
    public function getURI( $prefix );

    /**
     * Returns the prefix to which the given URI is mapped
     *
     * @param uri a string
     * @return a string
     * @throws NamespaceException if the URI is unknown.
     * @throws RepositoryException is another error occurs
     */
    public function getPrefix( $uri );
}

?>