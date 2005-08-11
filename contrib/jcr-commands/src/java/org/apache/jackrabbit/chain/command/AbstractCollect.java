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
import java.util.ResourceBundle;

import javax.jcr.Node;

import org.apache.commons.chain.Command;
import org.apache.commons.chain.Context;
import org.apache.jackrabbit.chain.CtxHelper;
import org.apache.jackrabbit.util.ChildrenCollectorFilter;

/**
 * Collect the children items and store them under the given key. <br>
 * The Command attributes are set from the specified literal values, or from the
 * context attributes stored under the given keys.
 * 
 */
public abstract class AbstractCollect implements Command
{
    /** Resource bundle */
    protected ResourceBundle bundle = ResourceBundle.getBundle(this.getClass()
        .getPackage().getName()
            + ".resources");

    // ---------------------------- < literals >

    /** depth literal value */
    private String depth;

    /** name pattern literal value */
    private String namePattern;

    // ---------------------------- < keys >

    /** context attribute key for the depth attribute */
    private String depthKey;

    /** context attribute key for the name pattern attribute */
    private String namePatternKey;

    /** context attribute key for the destination attribute */
    private String toKey;

    /**
     * @inheritDoc
     */
    public final boolean execute(Context ctx) throws Exception
    {
        if (this.toKey == null || this.toKey.length() == 0)
        {
            throw new IllegalStateException("target variable is not set");
        }

        Node node = CtxHelper.getCurrentNode(ctx);

        String namePattern = CtxHelper.getAttr(this.namePattern,
            this.namePatternKey, ctx);

        int depth = CtxHelper.getIntAttr(this.depth,
            this.depthKey, 1, ctx);

        if (namePattern == null || namePattern.length() == 0)
        {
            namePattern = "*";
        }

        Collection items = new ArrayList();
        ChildrenCollectorFilter collector = new ChildrenCollectorFilter(
            namePattern, items, isCollectNodes(), isCollectProperties(),
            depth);
        collector.visit(node);
        ctx.put(this.toKey, items.iterator());

        return false;
    }

    /**
     * @return Returns the depth.
     */
    public String getDepth()
    {
        return depth;
    }

    /**
     * @param depth
     *            The depth to set.
     */
    public void setDepth(String depth)
    {
        this.depth = depth;
    }

    /**
     * @return Returns the depthKey.
     */
    public String getDepthKey()
    {
        return depthKey;
    }

    /**
     * @param depthKey
     *            Set the context attribute key for the depth attribute
     */
    public void setDepthKey(String depthKey)
    {
        this.depthKey = depthKey;
    }

    /**
     * @return Returns the namePattern.
     */
    public String getNamePattern()
    {
        return namePattern;
    }

    /**
     * @param namePattern
     *            The namePattern to set.
     */
    public void setNamePattern(String namePattern)
    {
        this.namePattern = namePattern;
    }

    /**
     * @return Returns the namePatternKey.
     */
    public String getNamePatternKey()
    {
        return namePatternKey;
    }

    /**
     * @param namePatternKey
     *            context attribute key for the name pattern attribute
     */
    public void setNamePatternKey(String namePatternKey)
    {
        this.namePatternKey = namePatternKey;
    }

    /**
     * @return Returns the toKey.
     */
    public String getToKey()
    {
        return toKey;
    }

    /**
     * @param toKey
     *            context attribute key for the destination attribute
     */
    public void setToKey(String toKey)
    {
        this.toKey = toKey;
    }

    /** Collect nodes flag */
    protected abstract boolean isCollectNodes();

    /** Collect properties flag */
    protected abstract boolean isCollectProperties();
}
