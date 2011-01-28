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
package org.apache.jackrabbit.core.cluster;

import java.util.List;

import org.apache.jackrabbit.core.observation.EventState;
import org.apache.jackrabbit.core.state.ChangeLog;

/**
 * Update operation passed in <code>UpdateEventChannel</code>.
 */
public interface Update {

    /**
     * Set an attribute of this update operation. Can be used
     * to remember some setting for a later notification.
     *
     * @param name attribute name
     * @param value attribute value
     */
    void setAttribute(String name, Object value);

    /**
     * Return an attribute of this update operation.
     *
     * @param name attribute name
     * @return attribute value or <code>null</code>
     */
    Object getAttribute(String name);

    /**
     * Return the local changes of this update operation.
     *
     * @return local changes
     */
    ChangeLog getChanges();

    /**
     * Return the collection of events this update operation will
     * generate.
     *
     * @return collection of <code>EventState</code>s
     */
    List<EventState> getEvents();

    /**
     * Returns the timestamp whe this update occurred.
     *
     * @return the timestamp whe this update occurred.
     */
    long getTimestamp();

    /**
     * Returns the user data associated with this update.
     *
     * @return the user data associated with this update.
     */
    String getUserData();
}
