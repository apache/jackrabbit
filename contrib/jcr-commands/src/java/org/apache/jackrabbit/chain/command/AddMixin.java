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
 * Add a mixin to the given node
 */
public class AddMixin implements Command
{
    // ---------------------------- < literals >
    /** node path */
    private String path;

    /** mixin name */
    private String mixinName;

    // ---------------------------- < keys >
    /** node path */
    private String pathKey;

    /** mixin name */
    private String mixinNameKey;

    /**
     * @return Returns the mixinName.
     */
    public String getMixinName()
    {
        return mixinName;
    }

    /**
     * @param mixinName
     *            The mixinName to set.
     */
    public void setMixinName(String mixinName)
    {
        this.mixinName = mixinName;
    }

    /**
     * @return Returns the mixinNameKey.
     */
    public String getMixinNameKey()
    {
        return mixinNameKey;
    }

    /**
     * @param mixinNameKey
     *            The mixinNameKey to set.
     */
    public void setMixinNameKey(String mixinNameKey)
    {
        this.mixinNameKey = mixinNameKey;
    }

    /**
     * @return Returns the path.
     */
    public String getPath()
    {
        return path;
    }

    /**
     * @param path
     *            The path to set.
     */
    public void setPath(String path)
    {
        this.path = path;
    }

    /**
     * @return Returns the pathKey.
     */
    public String getPathKey()
    {
        return pathKey;
    }

    /**
     * @param pathKey
     *            The pathKey to set.
     */
    public void setPathKey(String pathKey)
    {
        this.pathKey = pathKey;
    }

    /**
     * @inheritDoc
     */
    public boolean execute(Context ctx) throws Exception
    {
        String path = CtxHelper.getAttr(this.path, this.pathKey, ctx);
        String mixin = CtxHelper
            .getAttr(this.mixinName, this.mixinNameKey, ctx);

        Node n = CtxHelper.getNode(ctx, path);
        n.addMixin(mixin);

        return false;
    }
}
