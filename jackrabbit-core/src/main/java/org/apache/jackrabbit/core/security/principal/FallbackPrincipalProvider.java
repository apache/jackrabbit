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

import java.security.Principal;
import java.util.Properties;

import javax.jcr.Session;

import org.apache.jackrabbit.api.security.principal.PrincipalIterator;

/**
 * The <code>FallbackPrincipalProvider</code> is used to provide any desired
 * principal. It is used to defined ACE for principals that are not known to
 * the repository yet or that were deleted.
 */
public class FallbackPrincipalProvider implements PrincipalProvider {

    /**
     * name of the "disabled" option.
     */
    public static final String OPTION_DISABLED = "disabled";

    /**
     * If <code>true</code> this principal provider is disabled.
     */
    private boolean disabled;

    /**
     * {@inheritDoc}
     *
     * @return a {@link UnknownPrincipal} with the given name.
     */
    public Principal getPrincipal(String principalName) {
        return disabled ? null : new UnknownPrincipal(principalName);
    }

    /**
     * {@inheritDoc}
     *
     * @return an empty principal iterator
     */
    public PrincipalIterator findPrincipals(String simpleFilter) {
        return PrincipalIteratorAdapter.EMPTY;
    }

    /**
     * {@inheritDoc}
     *
     * @return an empty principal iterator
     */
    public PrincipalIterator findPrincipals(String simpleFilter, int searchType) {
        return PrincipalIteratorAdapter.EMPTY;
    }

    /**
     * {@inheritDoc}
     *
     * @return an empty principal iterator
     */
    public PrincipalIterator getPrincipals(int searchType) {
        return PrincipalIteratorAdapter.EMPTY;
    }

    /**
     * {@inheritDoc}
     *
     * @return an empty principal iterator
     */
    public PrincipalIterator getGroupMembership(Principal principal) {
        return PrincipalIteratorAdapter.EMPTY;
    }

    /**
     * {@inheritDoc}
     */
    public void init(Properties options) {
        disabled = "true".equals(options.get(OPTION_DISABLED));
    }

    /**
     * {@inheritDoc}
     */
    public void close() {
    }

    /**
     * {@inheritDoc}
     *
     * @return <code>true</code>
     */
    public boolean canReadPrincipal(Session session, Principal principalToRead) {
        return true;
    }
}