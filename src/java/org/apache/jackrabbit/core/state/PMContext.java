/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.core.state;

import org.apache.jackrabbit.core.config.WorkspaceConfig;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;

import javax.jcr.NamespaceRegistry;

/**
 * A <code>PMContext</code> is used to provide context information for a
 * <code>PersistenceManager</code>.
 *
 * @see PersistenceManager#init(PMContext)
 */
public class PMContext {

    /**
     * workspace configuration
     */
    private final WorkspaceConfig wspConfig;

    /**
     * namespace registry
     */
    private final NamespaceRegistry nsReg;

    /**
     * node type registry
     */
    private final NodeTypeRegistry ntReg;

    /**
     * uuid of the root node
     */
    private final String rootNodeUUID;

    /**
     * Creates a new <code>PMContext</code>.
     *
     * @param wspConfig    configuration of workspace
     * @param rootNodeUUID uuid of the root node
     * @param nsReg        namespace registry
     * @param ntReg        node type registry
     */
    public PMContext(WorkspaceConfig wspConfig,
                     String rootNodeUUID,
                     NamespaceRegistry nsReg,
                     NodeTypeRegistry ntReg) {
        this.wspConfig = wspConfig;
        this.rootNodeUUID = rootNodeUUID;
        this.nsReg = nsReg;
        this.ntReg = ntReg;
    }

    /**
     * Returns the workspace configuration
     *
     * @return the workspace configuration
     */
    public WorkspaceConfig getWorkspaceConfig() {
        return wspConfig;
    }

    /**
     * Returns the uuid of the root node
     *
     * @return the uuid of the root node
     */
    public String getRootNodeUUID() {
        return rootNodeUUID;
    }

    /**
     * Returns the namespace registry
     *
     * @return the namespace registry
     */
    public NamespaceRegistry getNamespaceRegistry() {
        return nsReg;
    }

    /**
     * Returns the node type registry
     *
     * @return the node type registry
     */
    public NodeTypeRegistry getNodeTypeRegistry() {
        return ntReg;
    }
}
