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

/**
 * Operation to persist transient changes in a session.
 */
public class SessionSaveOperation implements SessionWriteOperation<Object> {

    /**
     * Persists transient changes by delegating to the save() method of the
     * root node (or the parent of transient changes if access to the root
     * node is not available to this session).
     */
    public Object perform(SessionContext context) throws RepositoryException {
        NodeId id;
        // JCR-2425: check whether session is allowed to read root node
        if (context.getSessionImpl().hasPermission("/", Session.ACTION_READ)) {
            id = context.getRootNodeId();
        } else {
            id = context.getItemStateManager().getIdOfRootTransientNodeState();
        }
        context.getItemManager().getItem(id).save();
        return this;
    }

    //--------------------------------------------------------------< Object >

    /**
     * Returns a string representation of this operation.
     */
    public String toString() {
        return "session.save()";
    }

}