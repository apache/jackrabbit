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
package org.apache.jackrabbit.sanitycheck.fix;

import java.util.Iterator;

import org.apache.commons.chain.Command;
import org.apache.commons.chain.Context;
import org.apache.jackrabbit.core.QName;
import org.apache.jackrabbit.core.state.ChangeLog;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.sanitycheck.SanityCheckException;
import org.apache.jackrabbit.sanitycheck.inconsistency.ChildNodeEntryInconsistency;

/**
 * Fixes a <code>ChildNodeEntryInconsistency</code> by removing a the child node entry.
 */
public class RemoveChildNodeEntry implements Command
{
    
    /**
     * @inheritDoc
     */
    public boolean execute(Context ctx) throws Exception
    {
        FixContext fCtx = (FixContext) ctx ;
        ChildNodeEntryInconsistency inc = (ChildNodeEntryInconsistency) fCtx.getInconsistency() ; 
        Iterator iter = inc.getNode().getChildNodeEntries().iterator();
        while (iter.hasNext())
        {
            NodeState.ChildNodeEntry entry = (NodeState.ChildNodeEntry) iter
                    .next();
            if (entry.getUUID().equals(inc.getChild()))
            {
                QName name = entry.getName();
                int index = entry.getIndex();
                inc.getNode().removeChildNodeEntry(name, index);
                ChangeLog changeLog = new ChangeLog();
                changeLog.modified(inc.getNode());
                try
                {
                    inc.getPersistenceManager().store(changeLog);
                    return true ;
                } catch (ItemStateException e)
                {
                    throw new SanityCheckException(
                            "Unable to store repaired state", e);
                }
            }
        }
        throw new SanityCheckException(
                "Unable to repair. Unknown reason.");
    }
    
}
