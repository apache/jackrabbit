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
package org.apache.jackrabbit.chain.command.info;

import java.util.Iterator;

import javax.jcr.NodeIterator;
import javax.jcr.PropertyIterator;

import org.apache.commons.chain.Context;
import org.apache.jackrabbit.chain.CtxHelper;

/**
 * Ls superclass
 */
public abstract class AbstractLs extends AbstractInfoCommand
{
    /** long width */
    protected int longWidth = 10;

    /** max items to list */
    protected int defaultMaxItems = 100;

    private String maxItemsKey ;

    private boolean path;

    protected void printFooter(Context ctx, Iterator iter)
    {
        CtxHelper.getOutput(ctx).println() ;
        CtxHelper.getOutput(ctx).println(bundle.getString("total"));
        if (iter instanceof NodeIterator)
        {
            printFooter(ctx, (NodeIterator) iter);
        } else if (iter instanceof PropertyIterator)
        {
            printFooter(ctx, (PropertyIterator) iter);
        }
    }

    private void printFooter(Context ctx, NodeIterator iter)
    {
        CtxHelper.getOutput(ctx).println(
            iter.getSize() + " " + bundle.getString("nodes"));
    }

    private void printFooter(Context ctx, PropertyIterator iter)
    {
        CtxHelper.getOutput(ctx).println(
            iter.getSize() + " " + bundle.getString("properties"));
    }

    public int getDefaultMaxItems()
    {
        return defaultMaxItems;
    }

    public void setDefaultMaxItems(int maxItems)
    {
        this.defaultMaxItems = maxItems;
    }

    /**
     * @return Returns the path.
     */
    public boolean isPath()
    {
        return path;
    }

    /**
     * @param path
     *            The path to set.
     */
    public void setPath(boolean path)
    {
        this.path = path;
    }

    /**
     * @return Returns the maxItemsKey.
     */
    public String getMaxItemsKey()
    {
        return maxItemsKey;
    }

    /**
     * @param maxItemsKey
     *            The maxItemsKey to set.
     */
    public void setMaxItemsKey(String maxItemsKey)
    {
        this.maxItemsKey = maxItemsKey;
    }

    protected int getMaxItems(Context ctx)
    {
        return CtxHelper.getIntAttr(null, maxItemsKey, defaultMaxItems, ctx);
    }
}
