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
package org.apache.jackrabbit.core;

import org.apache.jackrabbit.core.config.SearchConfig;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.state.ItemStateManager;
import org.apache.jackrabbit.core.observation.EventImpl;
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.name.AbstractNamespaceResolver;
import org.apache.jackrabbit.name.NoPrefixDeclaredException;

import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;
import javax.jcr.NamespaceException;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.Event;
import java.util.NoSuchElementException;

/**
 * <code>SystemSearchManager</code> implements a search manager for the jcr:system
 * tree of a workspace.
 */
public class SystemSearchManager extends SearchManager {

    /**
     * The namespace registry of the repository.
     */
    private final NamespaceRegistry nsReg;

    /**
     * A system search manager will only index content that is located under
     * /jcr:system
     * @inheritDoc
     */
    public SystemSearchManager(SearchConfig config,
                 NamespaceRegistry nsReg,
                 NodeTypeRegistry ntReg,
                 ItemStateManager itemMgr,
                 NodeId rootNodeId) throws RepositoryException {
        super(config, nsReg, ntReg, itemMgr, rootNodeId, null, null);
        this.nsReg = nsReg;
    }

    /**
     * Overwrites the implementation of the base class and filters out events
     * that are not under /jcr:system.
     *
     * @param events the original events.
     */
    public void onEvent(final EventIterator events) {

        // todo FIXME use namespace resolver of session that registered this
        // SearchManager as listener?
        String jcrSystem = "";
        try {
            jcrSystem = QName.JCR_SYSTEM.toJCRName(new AbstractNamespaceResolver() {
                public String getURI(String prefix) throws NamespaceException {
                    try {
                        return nsReg.getURI(prefix);
                    } catch (RepositoryException e) {
                        throw new NamespaceException(e.getMessage());
                    }
                }

                public String getPrefix(String uri) throws NamespaceException {
                    try {
                        return nsReg.getPrefix(uri);
                    } catch (RepositoryException e) {
                        throw new NamespaceException(e.getMessage());
                    }
                }
            });
        } catch (NoPrefixDeclaredException e) {
            // will never happen
        }
        final String jcrSystemPath = "/" + jcrSystem;

        super.onEvent(new EventIterator() {

            /**
             * The next pre-fetched event. <code>null</code> if no more
             * events are available.
             */
            private Event nextEvent;

            /**
             * Current position of this event iterator.
             */
            private long position = 0;

            {
                fetchNext();
            }

            /**
             * @inheritDoc
             */
            public Event nextEvent() {
                if (nextEvent != null) {
                    Event tmp = nextEvent;
                    fetchNext();
                    return tmp;
                } else {
                    throw new NoSuchElementException();
                }
            }

            /**
             * @inheritDoc
             */
            public void skip(long skipNum) {
                while (skipNum-- > 0) {
                    nextEvent();
                }
            }

            /**
             * @return always -1
             */
            public long getSize() {
                return -1;
            }

            /**
             * @inheritDoc
             */
            public long getPosition() {
                return position;
            }

            /**
             * @exception UnsupportedOperationException always.
             */
            public void remove() {
                throw new UnsupportedOperationException("remove");
            }

            /**
             * @inheritDoc
             */
            public boolean hasNext() {
                return nextEvent != null;
            }

            /**
             * @inheritDoc
             */
            public Object next() {
                return nextEvent();
            }

            /**
             * Sets the next event.
             */
            private void fetchNext() {
                nextEvent = null;
                while (nextEvent == null && events.hasNext()) {
                    EventImpl tmp = (EventImpl) events.nextEvent();
                    try {
                        if (tmp.getPath().startsWith(jcrSystemPath)) {
                            nextEvent = tmp;
                        }
                    } catch (RepositoryException e) {
                        // ignore and try next
                    }
                }
            }
        });
    }
}
