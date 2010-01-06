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

import org.apache.commons.collections.iterators.IteratorChain;
import org.apache.commons.collections.map.LRUMap;
import org.apache.jackrabbit.api.security.principal.PrincipalIterator;
import org.apache.jackrabbit.api.security.principal.PrincipalManager;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.security.SystemPrincipal;
import org.apache.jackrabbit.core.security.user.UserManagerImpl;
import org.apache.jackrabbit.spi.commons.conversion.NameResolver;
import org.apache.jackrabbit.util.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import javax.security.auth.Subject;
import java.security.Principal;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Provides principals for the users contained within the Repository.<p/>
 * Each {@link Authorizable} accessible via {@link UserManager}
 * is respected and the provider serves {@link Authorizable#getPrincipal()
 * Principal}s retrieved from those <code>Authorizable</code> objects.
 * <p/>
 * In addition this provider exposes the <i>everyone</i> principal, which has no
 * content (user/group) represention.
 */
public class DefaultPrincipalProvider extends AbstractPrincipalProvider implements EventListener {

    /**
     * the default logger
     */
    private static Logger log = LoggerFactory.getLogger(DefaultPrincipalProvider.class);

    /**
     * a cache for group memberships: maps principal-name to a set of principals
     * representing the members.
     */
    private final Map<String, Set<Principal>> membershipCache;

    /**
     * Principal-Base of this Provider
     */
    private final UserManagerImpl userManager;

    private final EveryonePrincipal everyonePrincipal;

    private final String pMembers;
    private final String pPrincipalName;

    /**
     * Creates a new DefaultPrincipalProvider reading the principals from the
     * storage below the given security root node.
     *
     * @param securitySession for repository access.
     * @param userManager Used to retrieve the principals.
     * @throws RepositoryException if an error accessing the repository occurs.
     */
    public DefaultPrincipalProvider(Session securitySession,
                                    UserManagerImpl userManager) throws RepositoryException {

        this.userManager = userManager;
        everyonePrincipal = EveryonePrincipal.getInstance();
        membershipCache = new LRUMap();

        // listen to modifications of group-membership
        String[] ntNames = new String[1];
        if (securitySession instanceof SessionImpl) {
            NameResolver resolver = (NameResolver) securitySession;
            ntNames[0] = resolver.getJCRName(UserManagerImpl.NT_REP_GROUP);
            pMembers = resolver.getJCRName(UserManagerImpl.P_MEMBERS);
            pPrincipalName = resolver.getJCRName(UserManagerImpl.P_PRINCIPAL_NAME);
        } else {
            ntNames[0] = "rep:Group";
            pMembers = "rep:members";
            pPrincipalName = "rep:principalName";
        }

        String groupPath = userManager.getGroupsPath();
        securitySession.getWorkspace().getObservationManager().addEventListener(this,
                Event.NODE_REMOVED | Event.PROPERTY_ADDED | Event.PROPERTY_CHANGED | Event.PROPERTY_REMOVED,
                groupPath,
                true,
                null,
                ntNames,
                false);
    }

    //------------------------------------------< AbstractPrincipalProvider >---
    /**
     * {@inheritDoc}
     * <p/>
     * This implementation uses the user and node resolver to find the
     * appropriate nodes.
     */
    @Override
    protected Principal providePrincipal(String principalName) {
        // check for 'everyone'
        if (everyonePrincipal.getName().equals(principalName)) {
            return everyonePrincipal;
        }
        try {
            Principal principal = new PrincipalImpl(principalName);
            Authorizable ath = userManager.getAuthorizable(principal);
            if (ath != null) {
                return ath.getPrincipal();
            }
        } catch (RepositoryException e) {
            log.error("Failed to access Authorizable for Principal " + principalName, e);
        }
        return null;
    }

    //--------------------------------------------------< PrincipalProvider >---
    /**
     * @see PrincipalProvider#findPrincipals(String)
     */
    public PrincipalIterator findPrincipals(String simpleFilter) {
        return findPrincipals(simpleFilter, PrincipalManager.SEARCH_TYPE_ALL);
    }

    /**
     * @see PrincipalProvider#findPrincipals(String, int)
     */
    public PrincipalIterator findPrincipals(String simpleFilter, int searchType) {
        checkInitialized();
        switch (searchType) {
            case PrincipalManager.SEARCH_TYPE_GROUP:
                return findGroupPrincipals(simpleFilter);
            case PrincipalManager.SEARCH_TYPE_NOT_GROUP:
                return findUserPrincipals(simpleFilter);
            case PrincipalManager.SEARCH_TYPE_ALL:
                PrincipalIterator[] its = new PrincipalIterator[] {
                        findUserPrincipals(simpleFilter),
                        findGroupPrincipals(simpleFilter)
                };
                return new PrincipalIteratorAdapter(new IteratorChain(its));
            default:
                throw new IllegalArgumentException("Invalid searchType");
        }
    }

    /**
     * @see PrincipalProvider#getPrincipals(int)
     * @param searchType Any of the following search types:
     * <ul>
     * <li>{@link PrincipalManager#SEARCH_TYPE_GROUP}</li>
     * <li>{@link PrincipalManager#SEARCH_TYPE_NOT_GROUP}</li>
     * <li>{@link PrincipalManager#SEARCH_TYPE_ALL}</li>
     * </ul>
     * @see PrincipalProvider#getPrincipals(int)
     */
    public PrincipalIterator getPrincipals(int searchType) {
        return findPrincipals(null, searchType);
    }

    /**
     * @see PrincipalProvider#getGroupMembership(Principal)
     */
    public PrincipalIterator getGroupMembership(Principal userPrincipal) {
        checkInitialized();
        Set<Principal> mship;
        synchronized (membershipCache) {
            mship = membershipCache.get(userPrincipal.getName());
            if (mship == null) {
                // recursively collect group membership
                mship = collectGroupMembership(userPrincipal);

                // make sure everyone-group is not missing
                if (!mship.contains(everyonePrincipal) && everyonePrincipal.isMember(userPrincipal)) {
                    mship.add(everyonePrincipal);
                }
                membershipCache.put(userPrincipal.getName(), mship);
            }
        }
        return new PrincipalIteratorAdapter(mship);

    }

    /**
     * @see PrincipalProvider#close()
     */
    @Override
    public synchronized void close() {
        super.close();
        membershipCache.clear();
    }

    /**
     * @see PrincipalProvider#canReadPrincipal(javax.jcr.Session,java.security.Principal)
     */
    public boolean canReadPrincipal(Session session, Principal principal) {
        checkInitialized();
        // check if the session can read the user/group associated with the
        // given principal
        if (session instanceof SessionImpl) {
            SessionImpl sImpl = (SessionImpl) session;
            Subject subject = sImpl.getSubject();
            if (!subject.getPrincipals(SystemPrincipal.class).isEmpty()
                    || !subject.getPrincipals(AdminPrincipal.class).isEmpty()) {
                return true;
            }
            try {
                UserManager umgr = sImpl.getUserManager();
                return umgr.getAuthorizable(principal) != null;
            } catch (RepositoryException e) {
                log.error("Failed to determine accessibility of Principal {}", principal, e);
            }
        }
        return false;
    }

    //------------------------------------------------------< EventListener >---
    /**
     * @see EventListener#onEvent(EventIterator)
     */
    public void onEvent(EventIterator eventIterator) {
        // superclass: flush all cached
        clearCache();

        // membership cache:
        while (eventIterator.hasNext()) {
            Event ev = eventIterator.nextEvent();
            int type = ev.getType();
            if (type == Event.PROPERTY_ADDED || type == Event.PROPERTY_CHANGED
                    || type == Event.PROPERTY_REMOVED) {
                try {
                    if (pMembers.equals(Text.getName(ev.getPath()))) {
                        synchronized (membershipCache) {
                            membershipCache.clear();
                        }
                        break;
                    }
                } catch (RepositoryException e) {
                    // should never get here
                    log.warn(e.getMessage());
                }
            }
        }
    }

    //--------------------------------------------------------------------------
    /**
     * Recursively collect all Group-principals the specified principal is
     * member of.
     *
     * @param princ Principal for which the group membership will be collected.
     * @return all Group principals the specified <code>princ</code> is member of
     * including inherited membership.
     */
    private Set<Principal> collectGroupMembership(Principal princ) {
        final Set<Principal> membership = new LinkedHashSet<Principal>();
            try {
                final Authorizable auth = userManager.getAuthorizable(princ);
                if (auth != null) {
                    /*
                    make sure the principal is contained in the cache.
                    however, avoid putting the given 'princ' but assert that
                    the cached principal is obtained with the system session
                    used to deliver principals with this provider implementation.
                    */
                    addToCache(auth.getPrincipal());
                    Iterator<Group> itr = auth.memberOf();
                    while (itr.hasNext()) {
                        Group group = itr.next();
                        Principal gp = group.getPrincipal();
                        addToCache(gp);
                        membership.add(gp);
                    }
                } else {
                    log.debug("Cannot find authorizable for principal " + princ.getName());
                }
            } catch (RepositoryException e) {
                log.warn("Failed to determine membership for " + princ.getName(), e.getMessage());
            }
        return membership;
    }

    /**
     * @param simpleFilter Principal name or fragment.
     * @return An iterator over the main principals of the authorizables found
     * by the user manager.
     */
    private PrincipalIterator findUserPrincipals(String simpleFilter) {
        synchronized (userManager) {
            try {
                Iterator<Authorizable> itr = userManager.findAuthorizables(pPrincipalName, simpleFilter, UserManager.SEARCH_TYPE_USER);
                return new PrincipalIteratorImpl(itr, false);
            } catch (RepositoryException e) {
                log.error("Error while searching user principals.", e);
                return PrincipalIteratorAdapter.EMPTY;
            }
        }
    }

    /**
     * @param simpleFilter Principal name or fragment.
     * @return An iterator over the main principals of the authorizables found
     * by the user manager.
     */
    private PrincipalIterator findGroupPrincipals(final String simpleFilter) {
        synchronized (userManager) {
            try {
                Iterator<Authorizable> itr = userManager.findAuthorizables(pPrincipalName, simpleFilter, UserManager.SEARCH_TYPE_GROUP);

                // everyone will not be found by the user manager -> extra test
                boolean addEveryone = everyonePrincipal.getName().matches(".*"+simpleFilter+".*");
                return new PrincipalIteratorImpl(itr, addEveryone);

            } catch (RepositoryException e) {
                log.error("Error while searching group principals.", e);
                return PrincipalIteratorAdapter.EMPTY;
            }
        }
    }

    //--------------------------------------------------------< inner class >---
    /**
     * Extension of AbstractPrincipalIterator that retrieves the next
     * principal from the iterator over authorizables by calling
     * {@link Authorizable#getPrincipal()}.
     */
    private class PrincipalIteratorImpl extends AbstractPrincipalIterator {

        private final Iterator<Authorizable> authorizableItr;
        private boolean addEveryone;

        private PrincipalIteratorImpl(Iterator<Authorizable> authorizableItr, boolean addEveryone) {
            this.authorizableItr = authorizableItr;
            this.addEveryone = addEveryone;

            next = seekNext();
        }

        /**
         * @see org.apache.jackrabbit.core.security.principal.AbstractPrincipalIterator#seekNext()
         */
        @Override
        protected Principal seekNext() {
            while (authorizableItr.hasNext()) {
                try {
                    Principal p = authorizableItr.next().getPrincipal();
                    addToCache(p);
                    return p;
                } catch (RepositoryException e) {
                    // should never get here
                    log.warn("Error while retrieving principal from group -> skip.");
                }
            }

            if (addEveryone) {
                addEveryone = false; // make sure iteration stops
                return everyonePrincipal;
            } else {
                // end of iteration reached
                return null;
            }
        }
    }
}
