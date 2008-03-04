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
package org.apache.jackrabbit.core;

import java.util.HashMap;
import java.util.Iterator;

import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Audit logger for some workspace, logging all events observed. The logger
 * used is obtained by asking the central <code>LoggerFactory</code> for
 * a logger named <code>audit.<i>workspace</i></code>. If such a logger has not
 * been declared, the standard logger will be used.
 * <p/>The output format of this logger is as follows:
 * <pre>[counter] type path (userid@workspace)</pre>
 * <p/>The following bean properties are available:
 * <ul>
 * <li><code>workspace</code>: workspace name. </li>
 * <li><code>granularity</code>: logging granularity. If the value is <i>property</i>
 * all events are logged. If the value is <i>node</i>, events associated with
 * properties are consolidated and logged as node modification events.</li>
 * </ul>
 */
public class WorkspaceAuditLogger implements EventListener {
    
    /**
     * Logger.
     */
    private static final Logger log = LoggerFactory.getLogger(WorkspaceAuditLogger.class);
    
    /**
     * Logger name prefix.
     */
    private static final String LOGGER_NAME_PREFIX = "audit.";
    
    /**
     * Node granularity.
     */
    private static final String NODE_GRANULARITY = "node";

    /**
     * Property granularity.
     */
    private static final String PROPERTY_GRANULARITY = "property";

    /**
     * Workspace name.
     */
    private String workspace;
    
    /**
     * Granularity.
     */
    private String granularity;
    
    /**
     * Event processor based on granularity.
     */
    private EventListener processor;

    /**
     * Private audit logger.
     */
    private Logger audit;
    
    /**
     * Event counter.
     */
    protected int counter;
    
    /**
     * Initialize this audit logger.
     */
    protected void init() {
        audit = createLogger(getWorkspace());
        processor = createEventProcessor(getGranularity());
    }
    
    /**
     * Create the logger associated with this audit's workspace.
     */
    protected Logger createLogger(String workspace) {
        return LoggerFactory.getLogger(LOGGER_NAME_PREFIX + workspace);
    }
    
    /**
     * Create the event processor associated with this audit's granularity.
     * 
     * @param granularity granularity
     * @return event listener
     */
    protected EventListener createEventProcessor(String granularity) {
        if (PROPERTY_GRANULARITY.equals(granularity)) {
            return new FullEventLogger();
        }
        if (NODE_GRANULARITY.equals(granularity) || granularity == null) {
            return new ConsolidateLogger();
        }
        log.warn("Granularity unknown: " + granularity);
        return new ConsolidateLogger();
    }
    
    /* (non-Javadoc)
     * @see javax.jcr.observation.EventListener#onEvent(javax.jcr.observation.EventIterator)
     * 
     * Initialize the audit log and event processor if this is the first event 
     * received and pass the events on.
     */
    public synchronized final void onEvent(EventIterator events) {
        if (audit == null) {
            init();
        }
        counter++;
        processor.onEvent(events);
    }
    
    /**
     * Log an event, given type, path and user ID.
     * @param type type
     * @param path path
     * @param userID user ID
     */
    protected void log(String type, String path, String userID) {
        StringBuffer buf = new StringBuffer();
        buf.append('[');
        buf.append(String.valueOf(counter));
        buf.append("] ");
        buf.append(type);
        buf.append(" ");
        buf.append(path);
        buf.append(" (");
        buf.append(userID + "@" + workspace);
        buf.append(")");
        audit.info(buf.toString());
    }
    
    /**
     * Return a event type's string representation. In this implementation,
     * this is either <code>ADD</code>, <code>DEL</code>, <code>MOD</code>
     * 
     * @param type event type
     * @return string representation
     */
    protected String typeToString(int type) {
        switch (type) {
        case Event.NODE_ADDED:
            return "ADD";
        case Event.NODE_REMOVED:
            return "DEL";
        case Event.PROPERTY_ADDED:
            return "add";
        case Event.PROPERTY_REMOVED:
            return "del";
        case Event.PROPERTY_CHANGED:
            return "mod";
        }
        return "???";
    }
    
    /**
     * Return a flag indicating whether an event is associated with a property.
     * @param evt event
     * @return <code>true</code> if the event is associated with a property;
     *         <code>false</code> otherwise.
     */
    public static final boolean isPropertyEvent(Event evt) {
        switch (evt.getType()) {
        case Event.NODE_ADDED:
        case Event.NODE_REMOVED:
            return false;
        case Event.PROPERTY_ADDED:
        case Event.PROPERTY_CHANGED:
        case Event.PROPERTY_REMOVED:
            return true;
        }
        return false;
    }
    
    /**
     * Bean getters.
     */
    public String getWorkspace() {
        return workspace;
    }
    
    public String getGranularity() {
        return granularity;
    }

    /**
     * Bean setters.
     */
    public void setWorkspace(String workspace) {
        this.workspace = workspace;
    }

    public void setGranularity(String granularity) {
        this.granularity = granularity;
    }

    /**
     * Event processor that will log all events.
     */
    private class FullEventLogger implements EventListener {

        /* (non-Javadoc)
         * @see javax.jcr.observation.EventListener#onEvent(javax.jcr.observation.EventIterator)
         * 
         * Log every event.
         */
        public void onEvent(EventIterator events) {
            while (events.hasNext()) {
                Event event = events.nextEvent();
                String path = "?";
                
                try {
                    path = event.getPath();
                } catch (RepositoryException e) {
                    // ignore
                }
                log(typeToString(event.getType()), path, event.getUserID());
            }
        }
    }
    
    /**
     * Event processor that will log node events and consolidate property
     * events.
     */
    private class ConsolidateLogger implements EventListener {
        
        /**
         * Internal structure class that holds node path and user ID. 
         */
        class NodeModified {
            public final String path;
            public final String userID;
            
            NodeModified(String path, String userID) {
                this.path = path;
                this.userID = userID;
            }
        }
        
        /* (non-Javadoc)
         * @see javax.jcr.observation.EventListener#onEvent(javax.jcr.observation.EventIterator)
         * 
         * Consolidate property events, logging a 'node modified' event at the
         * end if the node itself has neither been added nor removed.
         */
        public void onEvent(EventIterator events) {
            HashMap modifiedNodes = new HashMap();
            
            while (events.hasNext()) {
                Event event = events.nextEvent();
                String path = "?";
                
                try {
                    path = event.getPath();
                    if (isPropertyEvent(event)) {
                        String parentPath = path;
                        int index = parentPath.lastIndexOf('/');
                        if (index > 0) {
                            parentPath = parentPath.substring(0, index);
                        }
                        NodeModified nme = new NodeModified(
                                parentPath, event.getUserID());
                        modifiedNodes.put(parentPath, nme);
                        continue;
                    } else {
                        modifiedNodes.remove(path);
                    }
                } catch (RepositoryException e) { 
                    // ignore
                }
                log(typeToString(event.getType()), path, event.getUserID());
            }
            Iterator iter = modifiedNodes.values().iterator();
            while (iter.hasNext()) {
                NodeModified nme = (NodeModified) iter.next();
                log("MOD", nme.path, nme.userID);
            }
        }
    }

}