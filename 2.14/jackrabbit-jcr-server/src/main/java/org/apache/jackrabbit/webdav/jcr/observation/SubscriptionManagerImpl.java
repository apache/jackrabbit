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
package org.apache.jackrabbit.webdav.jcr.observation;

import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavResourceLocator;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.transaction.TransactionResource;
import org.apache.jackrabbit.webdav.jcr.JcrDavException;
import org.apache.jackrabbit.webdav.jcr.JcrDavSession;
import org.apache.jackrabbit.webdav.jcr.transaction.TransactionListener;
import org.apache.jackrabbit.webdav.observation.EventDiscovery;
import org.apache.jackrabbit.webdav.observation.ObservationResource;
import org.apache.jackrabbit.webdav.observation.Subscription;
import org.apache.jackrabbit.webdav.observation.SubscriptionDiscovery;
import org.apache.jackrabbit.webdav.observation.SubscriptionInfo;
import org.apache.jackrabbit.webdav.observation.SubscriptionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Document;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.ObservationManager;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

/**
 * <code>SubscriptionManager</code> collects all subscriptions requested, handles
 * the subscription timeout and provides METHODS to discover subscriptions
 * present on a given resource as well as events for an specific subscription.
 */
// todo: make sure all expired subscriptions are removed!
public class SubscriptionManagerImpl implements SubscriptionManager, TransactionListener {

    private static Logger log = LoggerFactory.getLogger(SubscriptionManagerImpl.class);

    /**
     * Map containing all {@link org.apache.jackrabbit.webdav.observation.Subscription subscriptions}.
     */
    private final SubscriptionMap subscriptions = new SubscriptionMap();

    private final Map<String, List<TransactionListener>> transactionListenerById = new HashMap<String, List<TransactionListener>>();

    /**
     * Retrieve the {@link org.apache.jackrabbit.webdav.observation.SubscriptionDiscovery}
     * object for the given resource. Note, that the discovery object will be empty
     * if there are no subscriptions present.<br>
     * Note that all subscriptions present on the given resource are returned.
     * However, the subscription id will not be visible in order to avoid abuse
     * by clients not having registered the subscription originally.
     *
     * @param resource
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

        Subscription subscription;
        if (subscriptionId == null) {
            // new subscription
            SubscriptionImpl newSubs = new SubscriptionImpl(info, resource);
            registerSubscription(newSubs, resource);

            // adjust references to this subscription
            subscriptions.put(newSubs.getSubscriptionId(), newSubs);
            resource.getSession().addReference(newSubs.getSubscriptionId());
            subscription = newSubs;
        } else {
            // refresh/modify existing one
            SubscriptionImpl existing = validate(subscriptionId, resource);
            existing.setInfo(info);
            registerSubscription(existing, resource);

            subscription = new WrappedSubscription(existing);
        }
        return subscription;
    }

    /**
     * Register the event listener defined by the given subscription to the
     * repository's observation manager.
     *
     * @param subscription
     * @param resource
     * @throws DavException
     */
    private void registerSubscription(SubscriptionImpl subscription,
                                      ObservationResource resource) throws DavException {
        try {
            Session session = getRepositorySession(resource);
            ObservationManager oMgr = session.getWorkspace().getObservationManager();
            String itemPath = subscription.getLocator().getRepositoryPath();
            oMgr.addEventListener(subscription, subscription.getJcrEventTypes(),
                    itemPath, subscription.isDeep(),
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
        unregisterSubscription(subs, resource);
    }

    /**
     * Remove the event listener defined by the specified subscription from
     * the repository's observation manager and clean up the references present
     * on the <code>DavSession</code>.
     *
     * @param subscription
     * @param resource
     * @throws DavException
     */
    private void unregisterSubscription(SubscriptionImpl subscription,
                                        ObservationResource resource) throws DavException {
        try {
            Session session = getRepositorySession(resource);
            session.getWorkspace().getObservationManager().removeEventListener(subscription);
            String sId = subscription.getSubscriptionId();

            // clean up any references
            subscriptions.remove(sId);
            resource.getSession().removeReference(sId);

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
     * @param timeout timeout in milliseconds
     * @param resource
     * @return object encapsulating the events.
     */
    public EventDiscovery poll(String subscriptionId, long timeout, ObservationResource resource)
            throws DavException {

        SubscriptionImpl subs = validate(subscriptionId, resource);
        return subs.discoverEvents(timeout);
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
     * @throws DavException if an error occurred while retrieving the <code>Subscription</code>
     */
    private SubscriptionImpl validate(String subscriptionId, ObservationResource resource)
            throws DavException {

        SubscriptionImpl subs;
        if (subscriptions.contains(subscriptionId)) {
            subs = subscriptions.get(subscriptionId);
            if (!subs.isSubscribedToResource(resource)) {
                throw new DavException(DavServletResponse.SC_PRECONDITION_FAILED, "Attempt to operate on subscription with invalid resource path.");
            }
            if (subs.isExpired()) {
                unregisterSubscription(subs, resource);
                throw new DavException(DavServletResponse.SC_PRECONDITION_FAILED, "Attempt to  operate on expired subscription.");
            }
            return subs;
        } else {
            throw new DavException(DavServletResponse.SC_PRECONDITION_FAILED, "Attempt to modify or to poll for non-existing subscription.");
        }
    }

    /**
     * @param resource
     * @return JCR session
     */
    private static Session getRepositorySession(ObservationResource resource) throws DavException {
        return JcrDavSession.getRepositorySession(resource.getSession());
    }

    //---------------------------< TransactionListener >------------------------

    /**
     * {@inheritDoc}
     */
    public synchronized void beforeCommit(TransactionResource resource,
                                          String lockToken) {
        // suspend regular subscriptions during a commit
        List<TransactionListener> transactionListeners = new ArrayList<TransactionListener>();
        for (Iterator<SubscriptionImpl> it = subscriptions.iterator(); it.hasNext(); ) {
            SubscriptionImpl sub = it.next();
            TransactionListener tl = sub.createTransactionListener();
            tl.beforeCommit(resource, lockToken);
            transactionListeners.add(tl);
        }
        transactionListenerById.put(lockToken, transactionListeners);
    }

    /**
     * {@inheritDoc}
     */
    public void afterCommit(TransactionResource resource, String lockToken, boolean success) {
        List<TransactionListener> transactionListeners = transactionListenerById.remove(lockToken);
        if (transactionListeners != null) {
            for (TransactionListener txListener : transactionListeners) {
                txListener.afterCommit(resource, lockToken, success);
            }
        }
    }

    //----------------------------------------------< private inner classes >---
    /**
     * Private inner class wrapping around an <code>Subscription</code> as
     * present in the internal map. This allows to hide the subscription Id
     * from other sessions, that did create the subscription.
     */
    private static class WrappedSubscription implements Subscription {

        private final Subscription delegatee;

        private WrappedSubscription(Subscription subsc) {
            this.delegatee = subsc;
        }

        public String getSubscriptionId() {
            // always return null, since the subscription id must not be exposed
            // but to the client, that created the subscription.
            return null;
        }

        public Element toXml(Document document) {
            return delegatee.toXml(document);
        }

        public boolean eventsProvideNodeTypeInformation() {
            return delegatee.eventsProvideNodeTypeInformation();
        }

        public boolean eventsProvideNoLocalFlag() {
            return delegatee.eventsProvideNoLocalFlag();
        }
    }

    /**
     * Private inner class <code>SubscriptionMap</code> that allows for quick
     * access by resource path as well as by subscription id.
     */
    private class SubscriptionMap {

        private HashMap<String, SubscriptionImpl> subscriptions = new HashMap<String, SubscriptionImpl>();
        private HashMap<DavResourceLocator, Set<String>> ids = new HashMap<DavResourceLocator, Set<String>>();

        private boolean contains(String subscriptionId) {
            return subscriptions.containsKey(subscriptionId);
        }

        private SubscriptionImpl get(String subscriptionId) {
            return subscriptions.get(subscriptionId);
        }

        private Iterator<SubscriptionImpl> iterator() {
            return subscriptions.values().iterator();
        }

        private void put(String subscriptionId, SubscriptionImpl subscription) {
            subscriptions.put(subscriptionId, subscription);
            DavResourceLocator key = subscription.getLocator();
            Set<String> idSet;
            if (ids.containsKey(key)) {
                idSet = ids.get(key);
            } else {
                idSet = new HashSet<String>();
                ids.put(key, idSet);
            }
            if (!idSet.contains(subscriptionId)) {
                idSet.add(subscriptionId);
            }
        }

        private void remove(String subscriptionId) {
            SubscriptionImpl sub = subscriptions.remove(subscriptionId);
            ids.get(sub.getLocator()).remove(subscriptionId);
        }

        private Subscription[] getByPath(DavResourceLocator locator) {
            Set<String> idSet = ids.get(locator);
            if (idSet != null && !idSet.isEmpty()) {
                Subscription[] subsForResource = new Subscription[idSet.size()];
                int i = 0;
                for (String id : idSet) {
                    SubscriptionImpl s = subscriptions.get(id);
                    subsForResource[i] = new WrappedSubscription(s);
                    i++;
                }
                return subsForResource;
            } else {
                return new Subscription[0];
            }
        }
    }
}
