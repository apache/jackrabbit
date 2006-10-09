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
import org.apache.jackrabbit.core.PropertyId;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.NoSuchItemStateException;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PersistenceManager;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.sanitycheck.inconsistency.impl.NoSuchChildInconsistency;
import org.apache.jackrabbit.sanitycheck.inconsistency.impl.NoSuchPropertyInconsistency;
import org.apache.jackrabbit.sanitycheck.inconsistency.impl.UnableToReadNodeInconsistency;
import org.apache.jackrabbit.sanitycheck.inconsistency.impl.UnableToReadPropertyInconsistency;

/**
 * Checks the tree integrity by checking the existence of every child node and
 * property. If any inconsistency is found the context processing is completed
 * and no further checks will be performed.
 */
public class CheckTree implements Command
{
    /**
     * @inheritDoc
     */
    public boolean execute(Context ctx) throws Exception
    {
        SanityCheckContext sanityCtx = (SanityCheckContext) ctx;
        PersistenceManager pm = sanityCtx.getPersistenceManager();
        NodeState root = pm.load(NodeId.valueOf(sanityCtx.getRootUUID()));
        int incs = sanityCtx.getInconsistencies().size();
        execute(root, sanityCtx);
        if (sanityCtx.getInconsistencies().size() > incs)
        {
            return true;
        } else
        {
            return false;
        }
    }

    /**
     * 
     * @param node
     * @param ctx
     * @throws Exception
     */
    private void execute(NodeState node, SanityCheckContext ctx)
            throws Exception
    {
        PersistenceManager pm = ctx.getPersistenceManager();
        Iterator propsIter = node.getPropertyEntries().iterator();
        while (propsIter.hasNext())
        {
            NodeState.PropertyEntry propEntry = (NodeState.PropertyEntry) propsIter.next();
            try
            {
                PropertyState property = pm.load(new PropertyId(
                    node.getUUID(),
                    propEntry.getName()));
            } catch (NoSuchItemStateException e)
            {
                // Property doesn't exist
                NoSuchPropertyInconsistency inc = new NoSuchPropertyInconsistency();
                inc.setPersistenceManager(pm);
                inc.setPersistenceManagerName(ctx.getPersistenceManagerName());
                inc.setNode(node);
                inc.setPropertyEntry(propEntry.getName());
                ctx.addInconsistency(inc);
            } catch (ItemStateException e)
            {
                // Unable to retrieve node
                UnableToReadPropertyInconsistency inc = new UnableToReadPropertyInconsistency() ;
                inc.setPersistenceManager(pm);
                inc.setPersistenceManagerName(ctx.getPersistenceManagerName()) ;
                inc.setNode(node);
                inc.setPropertyEntry(propEntry.getName());
                ctx.addInconsistency(inc);


            }
        }

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
                    // node doesn't exist
                    NoSuchChildInconsistency inc = new NoSuchChildInconsistency();
                    inc.setNode(node);
                    inc.setChild(childEntry.getUUID());
                    inc.setPersistenceManager(pm);
                    inc.setPersistenceManagerName(ctx.getPersistenceManagerName());
                    ctx.addInconsistency(inc);
                } catch (ItemStateException e)
                {
                    // Unable to retrieve node
                    UnableToReadNodeInconsistency inc = new UnableToReadNodeInconsistency() ;
                    inc.setPersistenceManager(pm);
                    inc.setPersistenceManagerName(ctx.getPersistenceManagerName()) ;
                    inc.setNode(node);
                    inc.setChild(childEntry.getUUID()) ;
                    ctx.addInconsistency(inc);
                }
            }
        }
    }

}
