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

import org.apache.commons.collections4.iterators.IteratorChain;
import org.apache.jackrabbit.api.security.principal.PrincipalIterator;
import org.apache.jackrabbit.api.security.principal.PrincipalManager;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.observation.SynchronousEventListener;
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
import java.security.Principal;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;

/**
 * Provides principals for the users contained within the Repository.
 * <p>
 * Each {@link Authorizable} accessible via {@link UserManager}
 * is respected and the provider serves {@link Authorizable#getPrincipal()
 * Principal}s retrieved from those <code>Authorizable</code> objects.
 * <p>
 * In addition this provider exposes the <i>everyone</i> principal, which has no
 * content (user/group) representation.
 * <p>
 * Unless explicitly configured (see {@link #NEGATIVE_ENTRY_KEY negative entry
 * option} this implementation of the <code>PrincipalProvider</code> interface
 * caches both positive and negative (null) results of the {@link #providePrincipal}
 * method. The cache is kept up to date by observation listening to creation
 * and removal of users and groups.
 * <p>
 * Membership cache:<br>
 * In addition to the caching provided by <code>AbstractPrincipalProvider</code>
 * this implementation keeps an extra membership cache, which is notified in
 * case of changes made to the members of any group.
 */
public class DefaultPrincipalProvider extends AbstractPrincipalProvider implements SynchronousEventListener {

    /**
     * the default logger
     */
    private static Logger log = LoggerFactory.getLogger(DefaultPrincipalProvider.class);

    /**
     * Principal-Base of this Provider
     */
    private final UserManagerImpl userManager;

    private final EveryonePrincipal everyonePrincipal;
    private final String pPrincipalName;

    /**
     * Creates a new DefaultPrincipalProvider reading the principals from the
     * storage below the given security root node.
     *
     * @param systemSession for repository access.
     * @param systemUserManager Used to retrieve the principals.
     * @throws RepositoryException if an error accessing the repository occurs.
     */
    public DefaultPrincipalProvider(Session systemSession,
                                    UserManagerImpl systemUserManager) throws RepositoryException {

        this.userManager = systemUserManager;
        everyonePrincipal = EveryonePrincipal.getInstance();

        String[] ntNames = new String[1];
        if (systemSession instanceof SessionImpl) {
            NameResolver resolver = (NameResolver) systemSession;
            ntNames[0] = resolver.getJCRName(UserManagerImpl.NT_REP_AUTHORIZABLE_FOLDER);
            pPrincipalName = resolver.getJCRName(UserManagerImpl.P_PRINCIPAL_NAME);
        } else {
            ntNames[0] = "rep:AuthorizableFolder";
            pPrincipalName = "rep:principalName";
        }

        String groupPath = userManager.getGroupsPath();
        String userPath = userManager.getUsersPath();
        String targetPath = groupPath;
        while (!Text.isDescendantOrEqual(targetPath, userPath)) {
            targetPath = Text.getRelativeParent(targetPath, 1);
        }
        systemSession.getWorkspace().getObservationManager().addEventListener(this,
                Event.NODE_ADDED | Event.NODE_REMOVED, targetPath, true, null, ntNames, false);
    }

    //------------------------------------------< AbstractPrincipalProvider >---
    /**
     * {@inheritDoc}
     * <p>
     * This implementation uses the user and node resolver to find the
     * appropriate nodes.
     */
    @Override
    protected Principal providePrincipal(String principalName) {
        try {
            Principal principal = new PrincipalImpl(principalName);
            Authorizable ath = userManager.getAuthorizable(principal);
            if (ath != null) {
                return ath.getPrincipal();
            } else if (EveryonePrincipal.NAME.equals(principalName)) {
                return everyonePrincipal;
            }
        } catch (RepositoryException e) {
            log.error("Failed to access Authorizable for Principal " + principalName, e);
        }
        return null;
    }

    /**
     * Sets the {@link #NEGATIVE_ENTRY_KEY} option value to <code>true</code> if
     * it isn't included yet in the passed options, before calling the init
     * method of the base class.
     * 
     * @param options
     */
    @Override
    public void init(Properties options) {
        if (!options.containsKey(NEGATIVE_ENTRY_KEY)) {
            options.put(NEGATIVE_ENTRY_KEY, "true");
        }
        super.init(options);
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
    @SuppressWarnings({ "rawtypes", "unchecked" })
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
        Set<Principal> mship = collectGroupMembership(userPrincipal);
        // make sure everyone-group is not missing
        if (!mship.contains(everyonePrincipal) && everyonePrincipal.isMember(userPrincipal)) {
            mship.add(everyonePrincipal);
        }
        return new PrincipalIteratorAdapter(mship);

    }

    /**
     * @see PrincipalProvider#close()
     */
    @Override
    public synchronized void close() {
        super.close();
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
            if (sImpl.isAdmin() || sImpl.isSystem()) {
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
                    if (everyonePrincipal.equals(p)) {
                        addEveryone = false;
                    }
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
