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

import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.observation.EventState;
import org.apache.jackrabbit.name.Path;
import org.apache.jackrabbit.name.QName;

import java.util.Set;
import java.util.Collection;

/**
 * Listener interface on a journal that gets called back for records that should be processed.
 */
public interface RecordProcessor {

    /**
     * Invoked when a record starts.
     *
     * @param workspace workspace, may be <code>null</code>
     */
    public void start(String workspace);

    /**
     * Process an update operation.
     *
     * @param operation operation to process
     */
    public void process(ItemOperation operation);

    /**
     * Process an event.
     *
     * @param event event
     */
    public void process(EventState event);

    /**
     * Process a lock operation.
     *
     * @param nodeId node id
     * @param isDeep flag indicating whether lock is deep
     * @param owner lock owner
     */
    public void process(NodeId nodeId, boolean isDeep, String owner);

    /**
     * Process an unlock operation.
     *
     * @param nodeId node id
     */
    public void process(NodeId nodeId);

    /**
     * Process a namespace operation.
     *
     * @param oldPrefix old prefix. if <code>null</code> this is a fresh mapping
     * @param newPrefix new prefix. if <code>null</code> this is an unmap operation
     * @param uri uri to map prefix to
     */
    public void process(String oldPrefix, String newPrefix, String uri);

    /**
     * Process one or more node type registrations.
     *
     * @param ntDefs node type definition
     */
    public void process(Collection ntDefs);

    /**
     * Invoked when a record ends.
     */
    public void end();
}
