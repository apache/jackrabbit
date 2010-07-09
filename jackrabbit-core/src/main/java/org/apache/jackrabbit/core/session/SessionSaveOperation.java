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
package org.apache.jackrabbit.core.session;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.core.id.NodeId;

public class SessionSaveOperation implements SessionOperation {

    public void perform(SessionContext context) throws RepositoryException {
        NodeId id;
        // JCR-2425: check whether session is allowed to read root node
        if (context.getSessionImpl().hasPermission("/", Session.ACTION_READ)) {
            id = context.getRootNodeId();
        } else {
            id = context.getItemStateManager().getIdOfRootTransientNodeState();
        }
        context.getItemManager().getItem(id).save();
    }

}