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

import org.apache.jackrabbit.core.state.ChangeLog;
import org.apache.jackrabbit.core.observation.EventStateCollection;

/**
 * Event channel used to transmit update operations.
 */
public interface UpdateEventChannel {

    /**
     * Called when an a update operation has been created.
     *
     * @param changes changes
     * @param esc events as they will be delivered on success
     */
    public void updateCreated(ChangeLog changes, EventStateCollection esc);

    /**
     * Called when an a update operation has been prepared.
     */
    public void updatePrepared();

    /**
     * Called when an a update operation has been committed.
     */
    public void updateCommitted();

    /**
     * Called when an a update operation has been cancelled.
     */
    public void updateCancelled();

    /**
     * Set listener that will receive information about incoming, external update events.
     *
     * @param listener update event listener
     */
    public void setListener(UpdateEventListener listener);
}
