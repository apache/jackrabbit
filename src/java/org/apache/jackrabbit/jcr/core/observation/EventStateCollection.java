/*
 * Copyright 2004 The Apache Software Foundation.
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
package org.apache.jackrabbit.jcr.core.observation;

import org.apache.log4j.Logger;
import org.apache.jackrabbit.jcr.core.state.ItemState;
import org.apache.jackrabbit.jcr.core.state.NodeState;
import org.apache.jackrabbit.jcr.core.state.PropertyState;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * The <code>EventStateCollection</code> class implements how {@link EventState}
 * objects are created based on the {@link org.apache.jackrabbit.jcr.core.state.ItemState}s
 * passed to the {@link #createEventStates} method.
 *
 * @author mreutegg
 * @version $Revision: 1.5 $
 */
final public class EventStateCollection {

    /**
     * Logger instance for this class
     */
    private static Logger log = Logger.getLogger(EventStateCollection.class);

    /**
     * List of events
     */
    private final List events = new ArrayList();

    /**
     * The session that created these events
     */
    private final Session session;

    /**
     * Creates a new empty <code>EventStateCollection</code>.
     *
     * @param session the session that created these events.
     */
    public EventStateCollection(Session session) {
	this.session = session;
    }

    /**
     * Creates {@link EventState}s for the passed {@link org.apache.jackrabbit.jcr.core.state.ItemState state}
     * instance.
     *
     * @param state the transient <code>ItemState</code> for whom
     *              to create {@link EventState}s.
     * @throws RepositoryException if an error occurs.
     */
    public void createEventStates(ItemState state)
	    throws RepositoryException {
	int status = state.getStatus();

	if (status == ItemState.STATUS_EXISTING_MODIFIED ||
		status == ItemState.STATUS_NEW) {

	    if (state.isNode()) {

		// 1) check added properties
		NodeState currentNode = (NodeState) state;
		List addedProperties = currentNode.getAddedPropertyEntries();
		for (Iterator it = addedProperties.iterator(); it.hasNext();) {
		    NodeState.PropertyEntry prop = (NodeState.PropertyEntry) it.next();
		    events.add(EventState.PropertyAdded(currentNode.getUUID(),
			    prop.getName(),
			    session));
		}

		// 2) check removed properties
		List removedProperties = currentNode.getRemovedPropertyEntries();
		for (Iterator it = removedProperties.iterator(); it.hasNext();) {
		    NodeState.PropertyEntry prop = (NodeState.PropertyEntry) it.next();
		    events.add(EventState.PropertyRemoved(currentNode.getUUID(),
			    prop.getName(),
			    session));
		}

		// 3) check for added nodes
		List addedNodes = currentNode.getAddedChildNodeEntries();
		for (Iterator it = addedNodes.iterator(); it.hasNext();) {
		    NodeState.ChildNodeEntry child = (NodeState.ChildNodeEntry) it.next();
		    events.add(EventState.ChildNodeAdded(currentNode.getUUID(),
			    child.getName(),
			    session));
		}

		// 4) check for removed nodes
		List removedNodes = currentNode.getRemovedChildNodeEntries();
		for (Iterator it = removedNodes.iterator(); it.hasNext();) {
		    NodeState.ChildNodeEntry child = (NodeState.ChildNodeEntry) it.next();
		    events.add(EventState.ChildNodeRemoved(currentNode.getUUID(),
			    child.getName(),
			    session));
		}
	    } else {
		// only add property changed event if property is existing
		if (state.getStatus() == ItemState.STATUS_EXISTING_MODIFIED) {
		    events.add(EventState.PropertyChanged(state.getParentUUID(),
			    ((PropertyState) state).getName(),
			    session));
		}
	    }
	}
    }

    /**
     * Returns an iterator over {@link EventState} instance.
     *
     * @return an iterator over {@link EventState} instance.
     */
    Iterator iterator() {
	return events.iterator();
    }
}
