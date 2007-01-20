/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.lite;

import javax.jcr.NamespaceRegistry;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeTypeManager;

import org.apache.jackrabbit.base.BaseWorkspace;

/**
 * TODO
 */
public class LiteWorkspace extends BaseWorkspace implements Workspace {

    private Session session;

    private NamespaceRegistry namespaceRegistry;

    private NodeTypeManager nodeTypeManager;

    public Session getSession() {
        return session;
    }

    protected void setSession(Session session) {
        this.session = session;
    }

    public NamespaceRegistry getNamespaceRegistry() {
        return namespaceRegistry;
    }

    protected void setNamespaceRegistry(NamespaceRegistry namespaceRegistry) {
        this.namespaceRegistry = namespaceRegistry;
    }

    public NodeTypeManager getNodeTypeManager() {
        return nodeTypeManager;
    }

    protected void setNodeTypeManager(NodeTypeManager nodeTypeManager) {
        this.nodeTypeManager = nodeTypeManager;
    }

}
