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
package org.apache.jackrabbit.server.io;

import org.apache.commons.chain.Command;
import org.apache.commons.chain.Context;
import org.apache.jackrabbit.JCRConstants;

import javax.jcr.Node;

/**
 * This Class implements a import command that adds a mixin node type to the
 * current node.
 */
public class AddMixinCommand implements Command, JCRConstants {

    /**
     * the mixin node type to add
     */
    private String nodeType = null;

    /**
     * Creates a new AddMixinCommand
     */
    public AddMixinCommand() {
    }

    /**
     * Creates a new AddMixinCommand with the given node type.
     * @param nodeType the node type to add as mixin.
     */
    public AddMixinCommand(String nodeType) {
        this.nodeType = nodeType;
    }

    /**
     * Returns the node type parameter
     * @return the node type.
     */
    public String getNodeType() {
        return nodeType;
    }

    /**
     * Sets the node type that is to be added to the current node.
     * @param nodeType the mixin node type
     */
    public void setNodeType(String nodeType) {
        this.nodeType = nodeType;
    }

    /**
     * Executes this command by delegating to {@link #execute(ImportContext)} if
     * the context has the correct class.
     * @param context the (import) context.
     * @return <code>false</code>.
     * @throws Exception if an error occurrs.
     */
    public boolean execute(Context context) throws Exception {
        if (context instanceof ImportContext) {
            return execute((ImportContext) context);
        } else {
            return false;
        }
    }

    /**
     * Adds the mixin nodetype to the current import node.
     * @param context the import context.
     * @return <code>false</code>
     * @throws Exception if an error occurrs.
     */
    public boolean execute(ImportContext context) throws Exception {
        Node parentNode = context.getNode();
        if (nodeType == null) {
            throw new IllegalArgumentException("AddMixinCommand needs 'nodeType' attribute.");
        }
        parentNode.addMixin(nodeType);
        return false;
    }
}
