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

import javax.jcr.Node;

/**
 * This Class implements a import command that adds a child node to the
 * current node.
 */
public class AddNodeCommand extends AbstractCommand {

    /**
     * the nodetype to be added.
     */
    private String nodeType = NT_UNSTRUCTURED;

    /**
     * Creates a new AddNodeCommand
     */
    public AddNodeCommand() {
    }

    /**
     * Creates a new AddNodeCommand with the given node type.
     *
     * @param nodeType the node type of the node to be added.
     */
    public AddNodeCommand(String nodeType) {
        this.nodeType = nodeType;
    }

    /**
     * Returns the node type of the new node to be added.
     *
     * @return the node type
     */
    public String getNodeType() {
        return nodeType;
    }

    /**
     * Sets the node type of the new node to be added.
     *
     * @param nodeType the node type
     */
    public void setNodeType(String nodeType) {
        this.nodeType = nodeType;
    }

    /**
     * Executes this command by delegating to {@link #execute(ImportContext)} if
     * the context has the correct class.
     *
     * @param context the (import) context.
     * @return <code>false</code>.
     * @throws Exception if an error occurrs.
     */
    public boolean execute(AbstractContext context) throws Exception {
        if (context instanceof ImportContext) {
            return execute((ImportContext) context);
        } else {
            return false;
        }
    }

    /**
     * Executes this command. It adds a node, using the systemid of the import
     * context and the given node type unless the respective chils node
     * already exists. In either case, the current node of the import context
     * is set to this new node.
     *
     * @param context the import context
     * @return <code>false</code>
     * @throws Exception in an error occurrs
     * @see ImportContext#getSystemId()
     * @see ImportContext#getNode()
     */
    public boolean execute(ImportContext context) throws Exception {
        Node parentNode = context.getNode();
        if (parentNode.hasNode(context.getSystemId())) {
            context.setNode(parentNode.getNode(context.getSystemId()));
        } else {
            context.setNode(parentNode.addNode(context.getSystemId(), nodeType));
        }
        return false;
    }

}
