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

import org.apache.commons.chain.Command;
import org.apache.commons.chain.Context;
import org.apache.jackrabbit.chain.CtxHelper;

/**
 * Removes the item at the given path. <br>
 * The Command attributes are set from the specified literal values, or from the
 * context attributes stored under the given keys.
 */
public class RemoveItem implements Command
{

    // ---------------------------- < literals >

    /** path to the current node */
    private String path;

    // ---------------------------- < keys >

    /** context attribute key for the path attribute */
    private String pathKey;

    /**
     * @inheritDoc
     */
    public boolean execute(Context ctx) throws Exception
    {
        String path = CtxHelper.getAttr(this.path, this.pathKey, ctx);
        CtxHelper.getItem(ctx, path).remove();
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
     *            Set the context attribute key for the path attribute.
     */
    public void setPathKey(String pathKey)
    {
        this.pathKey = pathKey;
    }
}
