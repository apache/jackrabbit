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
package org.apache.jackrabbit.core.security.principal;

import org.apache.jackrabbit.api.security.principal.PrincipalIterator;

import javax.jcr.Session;
import java.security.Principal;
import java.util.Properties;

/**
 * This interface defines methods to provide access to sources of
 * {@link Principal}s. This allows the security framework share any external
 * sources for authorization and authentication, as may be used by a custom
 * {@link javax.security.auth.spi.LoginModule} for example.
 *
 * @see org.apache.jackrabbit.api.security.principal.PrincipalManager for more details about principals, users and groups.
 */
public interface PrincipalProvider {

    /**
     * Returns the principal with the given name if is known to this provider
     *
     * @param principalName the name of the principal to retrieve
     * @return return the requested principal or <code>null</code>
     */
    Principal getPrincipal(String principalName);

    /**
     * Searches for <code>Principal</code>s that match the given String.
     * NOTE: <code>Group</code>s are included in the search result.
     *
     * @param simpleFilter
     * @return
     * @see #findPrincipals(String,int)
     */
    PrincipalIterator findPrincipals(String simpleFilter);

    /**
     * Searches for <code>Principal</code>s that match the given String.
     *
     * @param simpleFilter
     * @param searchType searchType Any of the following constants:
     * <ul>
     * <li>{@link org.apache.jackrabbit.api.security.principal.PrincipalManager#SEARCH_TYPE_ALL}</li>
     * <li>{@link org.apache.jackrabbit.api.security.principal.PrincipalManager#SEARCH_TYPE_GROUP}</li>
     * <li>{@link org.apache.jackrabbit.api.security.principal.PrincipalManager#SEARCH_TYPE_NOT_GROUP}</li>
     * </ul>
     * @return
     * @see #findPrincipals(String)
     */
    PrincipalIterator findPrincipals(String simpleFilter, int searchType);

    /**
     * Returns an iterator over all principals that match the given search type.
     *
     * @return an iterator over all principals that match the given search type.
     * @param searchType searchType Any of the following constants:
     * <ul>
     * <li>{@link org.apache.jackrabbit.api.security.principal.PrincipalManager#SEARCH_TYPE_ALL}</li>
     * <li>{@link org.apache.jackrabbit.api.security.principal.PrincipalManager#SEARCH_TYPE_GROUP}</li>
     * <li>{@link org.apache.jackrabbit.api.security.principal.PrincipalManager#SEARCH_TYPE_NOT_GROUP}</li>
     * </ul>
     */
    PrincipalIterator getPrincipals(int searchType);

    /**
     * Returns an iterator over all group principals for which the given
     * principal is either direct or indirect member of. If a principal is
     * a direct member of a group, then
     * <code>{@link org.apache.jackrabbit.api.security.principal.GroupPrincipal#isMember(Principal)}</code>
     * evaluates to <code>true</code>. A principal is an indirect member of a
     * group if any of its groups (to any degree of separation) is direct member
     * of the group.
     * <p>
     * Example:<br>
     * If Principal is member of Group A, and Group A is member of
     * Group B, this method will return Group A and Group B.
     *
     * @param principal the principal to return it's membership from.
     * @return an iterator returning all groups the given principal is member of.
     */
    PrincipalIterator getGroupMembership(Principal principal);

    /**
     * Initialize this provider.
     *
     * @param options the options that are set
     */
    void init(Properties options);

    /**
     * This is called when a provider is not longer used by the repository.
     * An implementation can then release any resources bound to this
     * provider, eg. disconnect from a back end system.
     */
    void close();

    /**
     * Tests if the provided session is allowed to read the given principal.
     * Since the principal providers do not restrict the access
     * on the principals they provide, this method is used by the PrincipalManger
     * to ensure proper access rights for the client requesting the principals.
     *
     * @param session
     * @param principalToRead The principal to be accessed by the specified subject.
     * @return <code>true</code> if the session is allowed to read the principal;
     * <code>false</code> otherwise.
     */
    boolean canReadPrincipal(Session session, Principal principalToRead);
}
