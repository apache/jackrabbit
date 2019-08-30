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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.api.security.principal.GroupPrincipal;
import org.apache.jackrabbit.api.security.principal.ItemBasedPrincipal;
import org.apache.jackrabbit.api.security.principal.JackrabbitPrincipal;
import org.apache.jackrabbit.api.security.principal.PrincipalIterator;
import org.apache.jackrabbit.api.security.principal.PrincipalManager;

/**
 * This principal manager implementation uses the {@link DefaultPrincipalProvider}
 * in order to dispatch the respective requests and assemble the required
 * data. It is bound to a session and therefore obliges the access restrictions
 * of the respective subject.
 */
public class PrincipalManagerImpl implements PrincipalManager {

    /** the Session this manager has been created for*/
    private final Session session;

    /** the principalProviders */
    private final PrincipalProvider[] providers;

    private boolean closed;

    /**
     * Creates a new default principal manager implementation.
     *
     * @param session the underlying session
     * @param providers the providers
     */
    public PrincipalManagerImpl(Session session, PrincipalProvider[] providers) {
        this.session = session;
        this.providers = providers;
        closed = false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasPrincipal(String principalName) {
        return internalGetPrincipal(principalName) != null;
    }

    /**
     * {@inheritDoc}
     */
    public Principal getPrincipal(String principalName) {
        return internalGetPrincipal(principalName);
    }

    /**
     * {@inheritDoc}
     */
    public PrincipalIterator findPrincipals(String simpleFilter) {
        checkIsValid();
        List<CheckedIteratorEntry> entries = new ArrayList<CheckedIteratorEntry>(providers.length);
        for (PrincipalProvider pp : providers) {
            PrincipalIterator it = pp.findPrincipals(simpleFilter);
            if (it.hasNext()) {
                entries.add(new CheckedIteratorEntry(it, pp));
            }
        }
        return new CheckedPrincipalIterator(entries);
    }

    /**
     * {@inheritDoc}
     */
    public PrincipalIterator findPrincipals(String simpleFilter, int searchType) {
        checkIsValid();
        List<CheckedIteratorEntry> entries = new ArrayList<CheckedIteratorEntry>(providers.length);
        for (PrincipalProvider pp : providers) {
            PrincipalIterator it = pp.findPrincipals(simpleFilter, searchType);
            if (it.hasNext()) {
                entries.add(new CheckedIteratorEntry(it, pp));
            }
        }
        return new CheckedPrincipalIterator(entries);
    }

    /**
     * {@inheritDoc}
     * @param searchType
     */
    public PrincipalIterator getPrincipals(int searchType) {
        checkIsValid();
        List<CheckedIteratorEntry> entries = new ArrayList<CheckedIteratorEntry>(providers.length);
        for (PrincipalProvider pp : providers) {
            PrincipalIterator it = pp.getPrincipals(searchType);
            if (it.hasNext()) {
                entries.add(new CheckedIteratorEntry(it, pp));
            }
        }
        return new CheckedPrincipalIterator(entries);
    }

    /**
     * {@inheritDoc}
     */
    public PrincipalIterator getGroupMembership(Principal principal) {
        checkIsValid();
        List<CheckedIteratorEntry> entries =  new ArrayList<CheckedIteratorEntry>(providers.length + 1);
        for (PrincipalProvider pp : providers) {
            PrincipalIterator groups = pp.getGroupMembership(principal);
            if (groups.hasNext()) {
                entries.add(new CheckedIteratorEntry(groups, pp));
            }
        }
        // additional entry for the 'everyone' group
        if (!(principal instanceof EveryonePrincipal)) {
            Iterator<Principal> it = Collections.singletonList(getEveryone()).iterator();
            entries.add(new CheckedIteratorEntry(it, null));
        }
        return new CheckedPrincipalIterator(entries);
    }

    /**
     * {@inheritDoc}
     */
    public Principal getEveryone() {
        checkIsValid();
        Principal everyone = getPrincipal(EveryonePrincipal.NAME);
        if (everyone == null) {
            everyone = EveryonePrincipal.getInstance();
        }
        return everyone;
    }

    //--------------------------------------------------------------------------
    /**
     * Check if the instance has been closed.
     *
     * @throws IllegalStateException if this instance was closed.
     */
    private void checkIsValid() {
        if (closed) {
            throw new IllegalStateException("PrincipalManagerImpl instance has been closed.");
        }
    }

    /**
     * @param principalName the name of the principal
     * @return The principal with the given name or <code>null</code> if none
     * of the providers knows that principal of if the Session is not allowed
     * to see it.
     */
    private Principal internalGetPrincipal(String principalName) {
        checkIsValid();
        for (PrincipalProvider provider : providers) {
            Principal principal = provider.getPrincipal(principalName);
            if (principal != null && provider.canReadPrincipal(session, principal)) {
                return disguise(principal, provider);
            }
        }
        // nothing found or not allowed to see it.
        return null;
    }

    /**
     * @param principal the principal
     * @param provider the provider
     * @return A group that only reveals those members that are visible to the
     * current session or the specified principal if its not a group or the
     * everyone principal.
     */
    private Principal disguise(Principal principal, PrincipalProvider provider) {
        if (!GroupPrincipals.isGroup(principal) || principal instanceof EveryonePrincipal) {
            // nothing to do.
            return principal;
        }
        // make sure all groups except for the 'everyone' group expose only
        // principals visible to the session.
        if (principal instanceof ItemBasedPrincipal) {
            return new ItemBasedCheckedGroup(principal, provider);
        } else {
            return new CheckedGroup(principal, provider);
        }
    }

    //--------------------------------------------------------------------------
    /**
     * An implementation of the <code>Group</code> interface that wraps another
     * Group and makes sure, that all exposed members are visible to the Session
     * the <code>PrincipalManager</code> has been built for. This is required
     * due to the fact, that the principal provider is not bound to a particular
     * Session object.
     */
    private class CheckedGroup implements GroupPrincipal, JackrabbitPrincipal {

        final Principal delegatee;
        private final PrincipalProvider provider;

        private CheckedGroup(Principal delegatee, PrincipalProvider provider) {
            this.delegatee = delegatee;
            this.provider = provider;
        }

        public boolean addMember(Principal user) {
            throw new UnsupportedOperationException("Not implemented");
        }

        public boolean removeMember(Principal user) {
            throw new UnsupportedOperationException("Not implemented");
        }

        @Override
        public boolean isMember(Principal member) {
            return GroupPrincipals.isMember(delegatee, member);
        }

        public Enumeration<? extends Principal> members() {
            Iterator<? extends Principal> it = Collections.list(GroupPrincipals.members(delegatee)).iterator();
            final PrincipalIterator members = new CheckedPrincipalIterator(it, provider);
            return new Enumeration<Principal>() {
                public boolean hasMoreElements() {
                    return members.hasNext();
                }
                public Principal nextElement() {
                    return members.nextPrincipal();
                }
            };
        }

        public String getName() {
            return delegatee.getName();
        }

        //---------------------------------------------------------< Object >---
        @Override
        public int hashCode() {
            return delegatee.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            return delegatee.equals(obj instanceof CheckedGroup ? ((CheckedGroup) obj).delegatee : obj);
        }
    }

    /**
     * Same as {@link CheckedGroup} but wrapping an ItemBasePrincipal.
     */
    private class ItemBasedCheckedGroup extends CheckedGroup implements ItemBasedPrincipal {

        private ItemBasedCheckedGroup(Principal delegatee, PrincipalProvider provider) {
            super(delegatee, provider);
            if (!(delegatee instanceof ItemBasedPrincipal)) {
                throw new IllegalArgumentException();
            }
        }

        public String getPath() throws RepositoryException {
            return ((ItemBasedPrincipal) delegatee).getPath();
        }
    }

    //--------------------------------------------------------------------------
    /**
     * A PrincipalIterator implementation that tests for each principal
     * in the passed base iterators whether it is visible to the Session
     * the PrincipalManager has been built for. A principal that is not
     * accessible is skipped during the iteration.
     */
    private class CheckedPrincipalIterator extends AbstractPrincipalIterator {

        private final List<CheckedIteratorEntry> entries;

        private CheckedPrincipalIterator(Iterator<? extends Principal> it, PrincipalProvider provider) {
            entries = new ArrayList<CheckedIteratorEntry>(1);
            entries.add(new CheckedIteratorEntry(it, provider));
            next = seekNext();
        }

        private CheckedPrincipalIterator(List<CheckedIteratorEntry> entries) {
            this.entries = new ArrayList<CheckedIteratorEntry>(entries);
            next = seekNext();
        }

        /**
         * @see org.apache.jackrabbit.core.security.principal.AbstractPrincipalIterator#seekNext()
         */
        @Override
        protected final Principal seekNext() {
            while (!entries.isEmpty()) {
                // first test if current iterator has more elements
                CheckedIteratorEntry current = entries.get(0);
                Iterator<? extends Principal> iterator = current.iterator;
                while (iterator.hasNext()) {
                    Principal chk = iterator.next();
                    if (current.provider == null ||
                        current.provider.canReadPrincipal(session, chk)) {
                        return disguise(chk, current.provider);
                    }
                }
                // no more elements in current iterator -> move to next iterator.
                entries.remove(0);
            }
            return null;
        }
    }

    //--------------------------------------------------------------------------
    /**
     *
     */
    private static class CheckedIteratorEntry {

        private final PrincipalProvider provider;
        private final Iterator<? extends Principal> iterator;

        private CheckedIteratorEntry(Iterator<? extends Principal> iterator, PrincipalProvider provider) {
            this.iterator = iterator;
            this.provider = provider;
        }
    }
}
