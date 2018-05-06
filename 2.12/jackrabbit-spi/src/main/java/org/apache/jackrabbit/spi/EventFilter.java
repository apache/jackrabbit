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
package org.apache.jackrabbit.spi;

import java.io.Serializable;

/**
 * An <code>EventFilter</code> is applied to the events as generated on the
 * repository server. Event filter instances can be created with {@link
 * RepositoryService#createEventFilter(SessionInfo, int,
 * Path, boolean, String[], Name[], boolean)}.
 * Some repository implementations may
 * also support event filters that are directly instantiated by the client.
 */
public interface EventFilter extends Serializable {

    /**
     * If an implementation returns <code>true</code> the <code>event</code>
     * will be included in the event bundle returned by {@link
     * RepositoryService#getEvents(Subscription, long)}. A return
     * value of <code>false</code> indicates that the client is not interested
     * in the <code>event</code>.
     *
     * @param event   the event in question.
     * @param isLocal flag indicating whether this is a local event.
     * @return <code>true</code> if the event is accepted by the filter;
     *         <code>false</code> otherwise.
     */
    public boolean accept(Event event, boolean isLocal);
}
