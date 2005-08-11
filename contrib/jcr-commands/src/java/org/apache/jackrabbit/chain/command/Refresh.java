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

import javax.jcr.Item;
import javax.jcr.Session;

import org.apache.commons.chain.Command;
import org.apache.commons.chain.Context;
import org.apache.jackrabbit.chain.CtxHelper;

/**
 * Refresh the session to reflect the current saved state. <br>
 * If the path is set then only that Item is refreshed.
 */
public class Refresh implements Command
{
    // ---------------------------- < literals >
    /** keep changes */
    private String keepChanges;

    /** path to the node to refresh */
    private String path;

    // ---------------------------- < keys >
    /** keep changes key */
    private String keepChangesKey;

    /** path to the node to refresh key */
    private String pathKey;

    /**
     * @return Returns the keepChanges.
     */
    public String getKeepChanges()
    {
        return keepChanges;
    }

    /**
     * @param keepChanges
     *            The keepChanges to set.
     */
    public void setKeepChanges(String keepChanges)
    {
        this.keepChanges = keepChanges;
    }

    /**
     * @return Returns the keepChangesKey.
     */
    public String getKeepChangesKey()
    {
        return keepChangesKey;
    }

    /**
     * @param keepChangesKey
     *            The keepChangesKey to set.
     */
    public void setKeepChangesKey(String keepChangesKey)
    {
        this.keepChangesKey = keepChangesKey;
    }

    /**
     * @inheritDoc
     */
    public boolean execute(Context ctx) throws Exception
    {
        boolean keepChanges = CtxHelper.getBooleanAttr(this.keepChanges,
            this.keepChangesKey, true, ctx);

        String path = CtxHelper.getAttr(this.path, this.pathKey, ctx);
        if (path == null)
        {
            Session s = CtxHelper.getSession(ctx);
            s.refresh(keepChanges);
        } else
        {
            Item i = CtxHelper.getItem(ctx, path);
            i.refresh(keepChanges);
        }

        return false;
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
}
