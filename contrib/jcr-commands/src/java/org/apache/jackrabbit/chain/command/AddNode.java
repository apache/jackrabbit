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
package org.apache.jackrabbit.chain.command;

import javax.jcr.Node;

import org.apache.commons.chain.Command;
import org.apache.commons.chain.Context;
import org.apache.jackrabbit.chain.CtxHelper;

/**
 * Add a node to the current working node. <br>
 * The Command attributes are set from the specified literal values, or from the
 * context attributes stored under the given keys.
 */
public class AddNode implements Command
{
    
    // ---------------------------- < literals >    
    /** Node type */
    private String nodeType;

    /** Node name */
    private String nodeName;

    // ---------------------------- < keys >    
    /** Node type key */
    private String nodeTypeKey;

    /** Node name key */
    private String nodeNameKey;

    /**
     * @inheritDoc
     */
    public boolean execute(Context ctx) throws Exception
    {
        Node node = CtxHelper.getCurrentNode(ctx);

        String nodeType = CtxHelper.getAttr(this.nodeType,
            this.nodeTypeKey, ctx);

        String name = CtxHelper.getAttr(this.nodeName, this.nodeNameKey, ctx);

        if (nodeType == null)
        {
            node.addNode(name);
        } else
        {
            node.addNode(name, nodeType);
        }
        return false;
    }

    /**
     * @return Returns the name.
     */
    public String getNodeName()
    {
        return nodeName;
    }

    /**
     * @param name
     *            The literal name to set.
     */
    public void setNodeName(String name)
    {
        this.nodeName = name;
    }

    /**
     * @return Returns the type.
     */
    public String getNodeType()
    {
        return nodeType;
    }

    /**
     * @param type
     *            The literal note type to set.
     */
    public void setNodeType(String type)
    {
        this.nodeType = type;
    }

    /**
     * @return Returns the nameKey.
     */
    public String getNodeNameKey()
    {
        return nodeNameKey;
    }

    /**
     * @param nameKey
     *            Set the context attribute key for the name attribute.
     */
    public void setNodeNameKey(String nameKey)
    {
        this.nodeNameKey = nameKey;
    }

    /**
     * @return Returns the nodeTypeKey.
     */
    public String getNodeTypeKey()
    {
        return nodeTypeKey;
    }

    /**
     * @param nodeTypeKey
     *            Set the context attribute key for the node type attribute.
     */
    public void setNodeTypeKey(String nodeTypeKey)
    {
        this.nodeTypeKey = nodeTypeKey;
    }

}
