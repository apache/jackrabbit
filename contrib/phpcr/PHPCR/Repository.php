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


require_once 'PHPCR/LoginException.php';
require_once 'PHPCR/NoSuchWorkspaceException.php';
require_once 'PHPCR/RepositoryException.php';
require_once 'PHPCR/Session.php';


/**
 * The entry point into the content repository.
 * Represents the entry point into the content repository. Typically the object
 * implementing this interface will be acquired from a JNDI-compatible
 * naming and directory service.
 *
 * @author Markus Nix <mnix@mayflower.de>
 * @package phpcr
 */
interface Repository
{
    /**
     * The descriptor key for the version of the specification
     * that this repository implements.
     */
    const SPEC_VERSION_DESC = "jcr.specification.version";

    /**
     * The descriptor key for the name of the specification
     * that this repository implements.
     */
    const SPEC_NAME_DESC = "jcr.specification.name";

    /**
     * The descriptor key for the name of the repository vendor.
     */
    const REP_VENDOR_DESC = "jcr.repository.vendor";

    /**
     * The descriptor key for the URL of the repository vendor.
     */
    const REP_VENDOR_URL_DESC = "jcr.repository.vendor.url";

    /**
     * The descriptor key for the name of this repository implementation.
     */
    const REP_NAME_DESC = "jcr.repository.name";

    /**
     * The descriptor key for the version of this repository implementation.
     */
    const REP_VERSION_DESC = "jcr.repository.version";

    /**
     * The presence of this key indicates that this implementation supports
     * all level 1 features. This key will always be present.
     */
    const LEVEL_1_SUPPORTED = "level.1.supported";

    /**
     * The presence of this key indicates that this implementation supports
     * all level 2 features.
     */
    const LEVEL_2_SUPPORTED = "level.2.supported";

    /**
     * The presence of this key indicates that this implementation supports transactions.
     */
    const OPTION_TRANSACTIONS_SUPPORTED = "option.transactions.supported";

    /**
     * The presence of this key indicates that this implementation supports versioning.
     */
    const OPTION_VERSIONING_SUPPORTED = "option.versioning.supported";

    /**
     * The presence of this key indicates that this implementation supports observation.
     */
    const OPTION_OBSERVATION_SUPPORTED = "option.observation.supported";

    /**
     * The presence of this key indicates that this implementation supports locking.
     */
    const OPTION_LOCKING_SUPPORTED = "option.locking.supported";

    /**
     * The presence of this key indicates that this implementation supports the SQL query language.
     */
    const OPTION_QUERY_SQL_SUPPORTED = "option.query.sql.supported";

    /**
     * The presence of this key indicates that the index position notation for
     * same-name siblings is supported within XPath queries.
     */
    const QUERY_XPATH_POS_INDEX = "query.xpath.pos.index";

    /**
     * The presence of this key indicates that XPath queries return results in document order.
     */
    const QUERY_XPATH_DOC_ORDER = "query.xpath.doc.order";

    /**
     * The presence of this key indicates that SQL queries can SELECT the pseudo-property
     * <code>jcr:path</code>.
     */
    const QUERY_JCRPATH = "query.jcrpath";

    /**
     * The presence of this key indicates that the <code>jcr:score</code> pseudo-property is
     * available in XPath and SQL queries that include a <code>jcrfn:contains</code>
     * (in XPath) or <code>CONTAINS</code> (in SQL) function to do a full-text search.
     */
    const QUERY_JCRSCORE = "query.jcrscore";


    /**
     * Returns a string array holding all descriptor keys available for this implementation.
     * This set must contain at least the built-in keys defined by the string constants in
     * this interface.Used in conjunction with {@link #getDescriptor(String name)}
     * to query information about this repository implementation.
     */
    public function getDescriptorKeys();

    /**
     * Returns the descriptor for the specified key. Used to query information about this
     * repository implementation. The set of available keys can be found by calling
     * {@link #getDescriptorKeys}. If the specifed key is not found, <code>null</code> is returned.
     *
     * @param key a string corresponding to a descriptor for this repsoitory implementation.
     * @return a descriptor string
     */
    public function getDescriptor( $key );

    /**
     * Authenticates the user using the supplied <code>credentials</code>.
     * <p>
     * If <code>workspaceName</code> is recognized as the name of an existing workspace in the repository and
     * authorization to access that workspace is granted, then a new <code>Session</code> object is returned.
     * The format of the string <code>workspaceName</code> depends upon the implementation.
     * <p>
     * If <code>credentials</code> is <code>null</code>, it is assumed that authentication is handled by a
     * mechanism external to the repository itself (for example, through the JAAS framework) and that the
     * repository implementation exists within a context (for example, an application server) that allows
     * it to handle authorization of the request for access to the specified workspace.
     * <p>
     * If <code>workspaceName</code> is <code>null</code>, a default workspace is automatically selected by
     * the repository implementation. This may, for example, be the "home workspace" of the user whose
     * credentials were passed, though this is entirely up to the configuration and implementation of the
     * repository. Alternatively, it may be a "null workspace" that serves only to provide the method
     * {@link Workspace#getAccessibleWorkspaceNames}, allowing the client to select from among available "real"
     * workspaces.
     * <p>
     * If authentication or authorization for the specified workspace fails, a <code>LoginException</code> is
     * thrown.
     * <p>
     * If <code>workspaceName</code> is not recognized, a <code>NoSuchWorkspaceException</code> is thrown.
     *
     * @param credentials   The credentials of the user
     * @param workspaceName the name of a workspace.
     * @return a valid session for the user to access the repository.
     * @throws LoginException  If the login fails.
     * @throws NoSuchWorkspaceException If the specified <code>workspaceName</code> is not recognized.
     * @throws RepositoryException if another error occurs.
     */
    public function login( $credentials = null, $workspaceName = null );
}

?>