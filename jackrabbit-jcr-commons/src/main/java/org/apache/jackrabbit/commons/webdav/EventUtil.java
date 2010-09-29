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
package org.apache.jackrabbit.commons.webdav;

import javax.jcr.observation.Event;
import java.util.HashMap;
import java.util.Map;

/**
 * <code>EventConstants</code>...
 */
public abstract class EventUtil {


    /**
     * Element representing the 'nodeadded' event type.
     * @see javax.jcr.observation.Event#NODE_ADDED
     */
    public static final String EVENT_NODEADDED = "nodeadded";

    /**
     * Element representing the 'noderemoved' event type.
     * @see javax.jcr.observation.Event#NODE_REMOVED
     */
    public static final String EVENT_NODEREMOVED = "noderemoved";

    /**
     * Element representing the 'propertyadded' event type.
     * @see javax.jcr.observation.Event#PROPERTY_ADDED
     */
    public static final String EVENT_PROPERTYADDED = "propertyadded";

    /**
     * Element representing the 'propertyremoved' event type.
     * @see javax.jcr.observation.Event#PROPERTY_REMOVED
     */
    public static final String EVENT_PROPERTYREMOVED = "propertyremoved";

    /**
     * Element representing the 'propertychanged' event type.
     * @see javax.jcr.observation.Event#PROPERTY_CHANGED
     */
    public static final String EVENT_PROPERTYCHANGED = "propertychanged";

    /**
     * Element representing the 'nodemoved' event type.
     * @see javax.jcr.observation.Event#NODE_MOVED
     */
    public static final String EVENT_NODEMOVED = "nodemoved";

    /**
     * Element representing the 'persist' event type.
     * @see javax.jcr.observation.Event#PERSIST
     */
    public static final String EVENT_PERSIST = "persist";

    /**
     * String array listing the xml local names of all type of jcr events.
     */
    public static final String[] EVENT_ALL = new String[] {
            EVENT_NODEADDED,
            EVENT_NODEREMOVED,
            EVENT_PROPERTYADDED,
            EVENT_PROPERTYREMOVED,
            EVENT_PROPERTYCHANGED,
            EVENT_NODEMOVED,
            EVENT_PERSIST};

    private static Map<String, Integer> NAME_TO_JCR = new HashMap<String, Integer>();
    static {
        NAME_TO_JCR.put(EVENT_NODEADDED, Event.NODE_ADDED);
        NAME_TO_JCR.put(EVENT_NODEREMOVED, Event.NODE_REMOVED);
        NAME_TO_JCR.put(EVENT_PROPERTYADDED, Event.PROPERTY_ADDED);
        NAME_TO_JCR.put(EVENT_PROPERTYREMOVED, Event.PROPERTY_REMOVED);
        NAME_TO_JCR.put(EVENT_PROPERTYCHANGED, Event.PROPERTY_CHANGED);
        NAME_TO_JCR.put(EVENT_NODEMOVED, Event.NODE_MOVED);
        NAME_TO_JCR.put(EVENT_PERSIST, Event.PERSIST);

    }

    /**
     * Tests if the specified eventName can be mapped to a JCR event type.
     * 
     * @param eventName
     * @return true if the specified eventName can be mapped to a JCR event type.
     */
    public static boolean isValidEventName(String eventName) {
        return NAME_TO_JCR.containsKey(eventName);
    }

    /**
     * Static utility method to convert the localName of a <code>EventType</code>
     * as present in the Xml body into the corresponding JCR event constant defined by
     * {@link javax.jcr.observation.Event}.
     *
     * @param eventName
     * @return Any of the event types defined by {@link Event} or <code>null</code>.
     * @throws IllegalArgumentException if the specified evenName is invalid.
     */
    public static int getJcrEventType(String eventName) {
        if (NAME_TO_JCR.containsKey(eventName)) {
            return NAME_TO_JCR.get(eventName);
        } else {
            throw new IllegalArgumentException("Invalid eventName : " + eventName);
        }
    }

    /**
     * Static utility method to retrieve a String representation of the type
     * defined by a {@link javax.jcr.observation.Event JCR event}.
     *
     * @param jcrEventType
     * @return Event name of the given JCR event type.
     * @throws IllegalArgumentException if the given int does not represent a
     * valid type constants as defined by {@link Event}.<br>
     * Valid values are
     * <ul>
     * <li>{@link Event#NODE_ADDED}</li>
     * <li>{@link Event#NODE_REMOVED}</li>
     * <li>{@link Event#PROPERTY_ADDED}</li>
     * <li>{@link Event#PROPERTY_REMOVED}</li>
     * <li>{@link Event#PROPERTY_CHANGED}</li>
     * <li>{@link Event#NODE_MOVED}</li>
     * <li>{@link Event#PERSIST}</li>
     * </ul>
     */
    public static String getEventName(int jcrEventType) {
        String eventName;
        switch (jcrEventType) {
            case Event.NODE_ADDED:
                eventName = EVENT_NODEADDED;
                break;
            case Event.NODE_REMOVED:
                eventName = EVENT_NODEREMOVED;
                break;
            case Event.PROPERTY_ADDED:
                eventName = EVENT_PROPERTYADDED;
                break;
            case Event.PROPERTY_CHANGED:
                eventName = EVENT_PROPERTYCHANGED;
                break;
            case Event.PROPERTY_REMOVED:
                eventName = EVENT_PROPERTYREMOVED;
                break;
            case Event.NODE_MOVED:
                eventName = EVENT_NODEMOVED;
                break;
            case Event.PERSIST:
                eventName = EVENT_PERSIST;
                break;
            default: // no default
                throw new IllegalArgumentException("Invalid JCR event type: " + jcrEventType);
        }
        return eventName;
    }

}