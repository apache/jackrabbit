/*
 * Copyright 2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
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
package org.apache.jackrabbit.webdav.spi.observation;

import org.apache.log4j.Logger;
import org.apache.jackrabbit.webdav.*;
import org.apache.jackrabbit.webdav.spi.JcrDavException;
import org.apache.jackrabbit.webdav.observation.*;

import javax.jcr.RepositoryException;
import javax.jcr.observation.ObservationManager;
import java.util.*;

/**
 * <code>SubscriptionManager</code> collects all subscriptions requested, handles
 * the subscription timeout and provides METHODS to discover subscriptions
 * present on a given resource as well as events for an specific subscription.
 *
 * @todo make sure all expired subscriptions are removed!
 */
public class SubscriptionManagerImpl implements SubscriptionManager {

    private static Logger log = Logger.getLogger(SubscriptionManager.class);

    /**
     * Map containing all {@link org.apache.jackrabbit.webdav.observation.Subscription subscriptions}.
     */
    private final SubscriptionMap subscriptions = new SubscriptionMap();

    /**
     * Retrieve the {@link org.apache.jackrabbit.webdav.observation.SubscriptionDiscovery} object for the given
     * resource. Note, that the discovery object will be empty if there are
     * no subscriptions present.
     *
     * @param resource
     * @todo is it correct to return subscriptions made by another session?
     */
    public SubscriptionDiscovery getSubscriptionDiscovery(ObservationResource resource) {
        Subscription[] subsForResource = subscriptions.getByPath(resource.getLocator());
        return new SubscriptionDiscovery(subsForResource);
    }

    /**
     * Create a new <code>Subscription</code> or update an existing <code>Subscription</code>
     * and add it as eventlistener to the {@link javax.jcr.observation.ObservationManager}.
     *
     * @param info
     * @param subscriptionId
     * @param resource
     * @return <code>Subscription</code> that has been added to the {@link javax.jcr.observation.ObservationManager}
     * @throws DavException if the subscription fails
     */
    public Subscription subscribe(SubscriptionInfo info, String subscriptionId,
                                  ObservationResource resource)
            throws DavException {

        SubscriptionImpl subscription;
        DavSession session = resource.getSession();
        if (subscriptionId == null) {
            // new subscription
            subscription = new SubscriptionImpl(info, resource);
            registerSubscription(subscription, session);

            // ajust references to this subscription
            subscriptions.put(subscription.getSubscriptionId(), subscription);
            session.addReference(subscription.getSubscriptionId());
        } else {
            // refresh/modify existing one
            subscription = validate(subscriptionId, resource);
            subscription.setInfo(info);
            registerSubscription(subscription, session);
        }
        return subscription;
    }

    /**
     * Register the event listener defined by the given subscription to the
     * repository's observation manager.
     *
     * @param subscription
     * @param session
     * @throws DavException
     */
    private void registerSubscription(SubscriptionImpl subscription, DavSession session)
            throws DavException {
        try {
            ObservationManager oMgr = session.getRepositorySession().getWorkspace().getObservationManager();
            oMgr.addEventListener(subscription, subscription.getEventTypes(),
                    subscription.getLocator().getResourcePath(), subscription.isDeep(),
                    subscription.getUuidFilters(),
                    subscription.getNodetypeNameFilters(),
                    subscription.isNoLocal());
        } catch (RepositoryException e) {
            log.error("Unable to register eventlistener: "+e.getMessage());
            throw new JcrDavException(e);
        }
    }

    /**
     * Unsubscribe the <code>Subscription</code> with the given id and remove it
     * from the {@link javax.jcr.observation.ObservationManager} as well as
     * from the internal map.
     *
     * @param subscriptionId
     * @param resource
     * @throws DavException
     */
    public void unsubscribe(String subscriptionId, ObservationResource resource)
            throws DavException {

        SubscriptionImpl subs = validate(subscriptionId, resource);
        unregisterSubscription(subs, resource.getSession());
    }

    /**
     * Remove the event listener defined by the specified subscription from
     * the repository's observation manager.
     *
     * @param subscription
     * @param session
     * @throws DavException
     */
    private void unregisterSubscription(SubscriptionImpl subscription,
                                        DavSession session) throws DavException {
        try {
            session.getRepositorySession().getWorkspace().getObservationManager().removeEventListener(subscription);
            String sId = subscription.getSubscriptionId();

            // clean up any references
            subscriptions.remove(sId);
            session.removeReference(sId);

        } catch (RepositoryException e) {
            log.error("Unable to remove eventlistener: "+e.getMessage());
            throw new JcrDavException(e);
        }
    }

    /**
     * Retrieve all event bundles accumulated since for the subscription specified
     * by the given id.
     *
     * @param subscriptionId
     * @param resource
     * @return object encapsulating the events.
     */
    public EventDiscovery poll(String subscriptionId, ObservationResource resource)
            throws DavException {

        SubscriptionImpl subs = validate(subscriptionId, resource);
        return subs.discoverEvents();
    }

    /**
     * Validate the given subscription id. The validation will fail under the following
     * conditions:<ul>
     * <li>The subscription with the given id does not exist,</li>
     * <li>DavResource path does not match the subscription id,</li>
     * <li>The subscription with the given id is already expired.</li>
     * </ul>
     *
     * @param subscriptionId
     * @param resource
     * @return <code>Subscription</code> with the given id.
     * @throws DavException if an error occured while retrieving the <code>Subscription</code>
     */
    private SubscriptionImpl validate(String subscriptionId, ObservationResource resource)
            throws DavException {

        SubscriptionImpl subs;
        if (subscriptions.contains(subscriptionId)) {
            subs = (SubscriptionImpl) subscriptions.get(subscriptionId);
            if (!subs.isSubscribedToResource(resource)) {
                throw new DavException(DavServletResponse.SC_PRECONDITION_FAILED, "Attempt to operate on subscription with invalid resource path.");
            }
            if (subs.isExpired()) {
                unregisterSubscription(subs, resource.getSession());
                throw new DavException(DavServletResponse.SC_PRECONDITION_FAILED, "Attempt to  operate on expired subscription.");
            }
            return subs;
        } else {
            throw new DavException(DavServletResponse.SC_PRECONDITION_FAILED, "Attempt to modify or to poll for non-existing subscription.");
        }
    }

    /**
     * Private inner class <code>SubscriptionMap</code> that allows for quick
     * access by resource path as well as by subscription id.
     */
    private class SubscriptionMap {

        private HashMap subscriptions = new HashMap();
        private HashMap paths = new HashMap();

        private boolean contains(String subscriptionId) {
            return subscriptions.containsKey(subscriptionId);
        }

        private Subscription get(String subscriptionId) {
            return (Subscription) subscriptions.get(subscriptionId);
        }

        private void put(String subscriptionId, SubscriptionImpl subscription) {
            subscriptions.put(subscriptionId, subscription);
            DavResourceLocator key = subscription.getLocator();
            Set idSet;
            if (paths.containsKey(key)) {
                idSet = (Set) paths.get(key);
            } else {
                idSet = new HashSet();
                paths.put(key, idSet);
            }
            if (!idSet.contains(subscriptionId)) {
                idSet.add(subscriptionId);
            }
        }

        private void remove(String subscriptionId) {
            SubscriptionImpl sub = (SubscriptionImpl) subscriptions.remove(subscriptionId);
            ((Set)paths.get(sub.getLocator())).remove(subscriptionId);
        }

        private Subscription[] getByPath(DavResourceLocator locator) {
            Set idSet = (Set) paths.get(locator);
            if (idSet != null && !idSet.isEmpty()) {
                Iterator idIterator = idSet.iterator();
                Subscription[] subsForResource = new Subscription[idSet.size()];
                int i = 0;
                while (idIterator.hasNext()) {
                    subsForResource[i] = (Subscription) subscriptions.get(idIterator.next());
                }
                return subsForResource;
            } else {
                return new Subscription[0];
            }
        }
    }
}