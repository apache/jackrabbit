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

import java.util.Iterator;

import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.NoSuchItemStateException;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PersistenceManager;
import org.apache.jackrabbit.sanitycheck.SanityCheckException;
import org.apache.jackrabbit.sanitycheck.inconsistency.impl.NoSuchParentInconsistency;

/**
 * Checks that all the referenced parents exists in the PersistenceManager
 */
public class CheckParentsExist extends AbstractNodeCheck
{
    /**
     * @inheritDoc
     */
    protected void internalExecute(
        NodeState node,
        SanityCheckContext ctx) throws SanityCheckException
    {
        PersistenceManager pm = ctx.getPersistenceManager();
        Iterator iter = node.getParentUUIDs().iterator() ;
        while (iter.hasNext())
        {
            String uuid = (String) iter.next();
            try {
                pm.load(NodeId.valueOf(uuid));
            } catch (NoSuchItemStateException ve)
            {
                NoSuchParentInconsistency inc = new NoSuchParentInconsistency();
                inc.setPersistenceManager(pm);
                inc.setPersistenceManagerName(ctx.getPersistenceManagerName());
                inc.setNode(node);
                inc.setParentUUID(uuid);
                ctx.addInconsistency(inc);
            } catch (ItemStateException ise)
            {
                throw new SanityCheckException(
                    "An error while running the check.",
                    ise);
            }
            
        }
    }
}
