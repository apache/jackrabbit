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

/**
 * Event channel used to transmit update operations.
 */
public interface UpdateEventChannel {

    /**
     * Called when an a update operation has been created.
     *
     * @param update update operation
     * @throws ClusterException if an error occurs writing to the event channel.
     */
    void updateCreated(Update update) throws ClusterException;

    /**
     * Called when an a update operation has been prepared.
     *
     * @param update update operation
     * @throws ClusterException if an error occurs writing to the event channel.
     */
    void updatePrepared(Update update) throws ClusterException;

    /**
     * Called when an a update operation has been committed.
     *
     * @param update update operation
     * @param path the change path
     */
    void updateCommitted(Update update, String path);

    /**
     * Called when an a update operation has been cancelled.
     *
     * @param update update operation
     */
    void updateCancelled(Update update);

    /**
     * Set listener that will receive information about incoming, external update events.
     *
     * @param listener update event listener
     */
    void setListener(UpdateEventListener listener);

}
