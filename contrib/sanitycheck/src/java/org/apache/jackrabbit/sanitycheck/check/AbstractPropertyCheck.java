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

import org.apache.jackrabbit.core.PropertyId;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.sanitycheck.SanityCheckException;

/**
 * Superclass of property checks. It assumens the tree is consistent.
 */
public abstract class AbstractPropertyCheck extends AbstractNodeCheck
{

    /**
     * @inheritDoc
     */
    protected void internalExecute(NodeState node, SanityCheckContext ctx)
            throws SanityCheckException
    {
        Iterator propsIter = node.getPropertyEntries().iterator();
        while (propsIter.hasNext())
        {
            NodeState.PropertyEntry propEntry = (NodeState.PropertyEntry) propsIter.next();
            try
            {
                PropertyState property = ctx.getPersistenceManager().load(
                    new PropertyId(node.getUUID(), propEntry.getName()));
                // perform PropertyState SanityCheck
                internalExecute(node, property, ctx);
            } catch (ItemStateException e)
            {
                throw new SanityCheckException(
                    "Unable o traverse tree. Run a tree check before this check.",
                    e);
            }
        }
    }

    /**
     * Checks the given property
     * 
     * @param node
     * @param property
     * @param ctx
     * @throws SanityCheckException
     */
    protected abstract void internalExecute(
        NodeState node,
        PropertyState property,
        SanityCheckContext ctx) throws SanityCheckException;

}
