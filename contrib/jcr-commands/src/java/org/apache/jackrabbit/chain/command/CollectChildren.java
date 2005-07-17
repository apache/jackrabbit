/*
 * Copyright 2002-2004 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */
package org.apache.jackrabbit.chain.command;

import java.util.ArrayList;
import java.util.Collection;

import javax.jcr.Node;

import org.apache.commons.chain.Command;
import org.apache.commons.chain.Context;
import org.apache.jackrabbit.chain.ContextHelper;
import org.apache.jackrabbit.util.ChildrenCollectorFilter;

/**
 * Collects the children nodes and stores them under 
 * the given key
 */
public class CollectChildren implements Command
{
    /** depth */
    private int depth = 1;

    /** name pattern */
    private String namePattern = "*";

    /** key to store the Traverser in the chain context */
    private String target = "children";

    public boolean execute(Context ctx) throws Exception
    {
        if (target == null || target.length() == 0)
        {
            throw new IllegalStateException("target variable is not set");
        }

        Node node = ContextHelper.getCurrentNode(ctx);
        
        if (this.namePattern == null || this.namePattern.length()==0)
        {
            this.namePattern = "*";
        }
        
        if (this.depth==1 && this.namePattern.equals("*")) {
            ctx.put(target, node.getNodes()) ;
        } else {
            Collection nodes = new ArrayList();
            ChildrenCollectorFilter collector = new ChildrenCollectorFilter(
                namePattern, nodes, true, false, this.depth);
            collector.visit(node);
            ctx.put(target, nodes.iterator());
        }
        return false;
    }

    public int getDepth()
    {
        return depth;
    }

    public void setDepth(int depth)
    {
        this.depth = depth;
    }

    public String getTarget()
    {
        return target;
    }

    public void setTarget(String target)
    {
        this.target = target;
    }

    public String getNamePattern()
    {
        return namePattern;
    }

    public void setNamePattern(String namePattern)
    {
        this.namePattern = namePattern;
    }
}
