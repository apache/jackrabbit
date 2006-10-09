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
package org.apache.jackrabbit.sanitycheck.check;

import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.chain.Command;
import org.apache.commons.chain.Context;
import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.state.NoSuchItemStateException;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PersistenceManager;
import org.apache.jackrabbit.sanitycheck.SanityCheckException;

/**
 * SanityCheck Superclass. It assumens the tree is consistent.
 */
public abstract class AbstractNodeCheck implements Command
{

    /**
     * @inheritDoc
     */
    public boolean execute(Context ctx) throws Exception
    {
        SanityCheckContext sanityCtx = (SanityCheckContext) ctx;
        PersistenceManager pm = sanityCtx.getPersistenceManager();
        NodeState root = pm.load(NodeId.valueOf(sanityCtx.getRootUUID()));
        execute(root, sanityCtx);
        return false;
    }

    /**
     * Traverses the tree and perform the sanity checks
     * 
     * @param node
     * @param ctx
     * @throws Exception
     */
    private void execute(NodeState node, SanityCheckContext ctx)
            throws Exception
    {
        PersistenceManager pm = ctx.getPersistenceManager();
        // perform NodeState Sanity check
        internalExecute(node, ctx);

        // Recurse if children are present
        Collection children = node.getChildNodeEntries();
        if (children.size() > 0)
        {
            Iterator iter = children.iterator();
            while (iter.hasNext())
            {
                NodeState.ChildNodeEntry childEntry = (NodeState.ChildNodeEntry) iter.next();
                try
                {
                    NodeState child = pm.load(new NodeId(childEntry.getUUID()));
                    this.execute(child, ctx);
                } catch (NoSuchItemStateException e)
                {
                    throw new SanityCheckException(
                        "Unable o traverse tree. Run a tree check before this check.",
                        e);
                }
            }
        }
    }

    /**
     * Sanity check implementation
     * 
     * @param state
     */
    protected abstract void internalExecute(
        NodeState state,
        SanityCheckContext ctx) throws Exception;


}
