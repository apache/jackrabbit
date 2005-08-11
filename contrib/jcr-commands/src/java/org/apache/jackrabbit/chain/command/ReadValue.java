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

import javax.jcr.Property;

import org.apache.commons.chain.Command;
import org.apache.commons.chain.Context;
import org.apache.jackrabbit.chain.CtxHelper;

/**
 * Read the Value of the given property and store it under the given context
 * attribute. <br>
 * The Command attributes are set from the specified literal values, or from the
 * context attributes stored under the given keys.
 */
public class ReadValue implements Command
{

    // ---------------------------- < literals >

    /** property path */
    private String path;

    /** value index */
    private String index;

    // ---------------------------- < keys >

    /** property path key */
    private String pathKey;

    /** value index key */
    private String indexKey;

    /** destination context attribute */
    private String toKey;

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.commons.chain.Command#execute(org.apache.commons.chain.Context)
     */
    public boolean execute(Context ctx) throws Exception
    {
        String path = CtxHelper.getAttr(this.path, this.pathKey, ctx);
        int index = CtxHelper.getIntAttr(this.index, this.indexKey, 0,
            ctx);

        Property p = (Property) CtxHelper.getItem(ctx, path);

        if (p.getDefinition().isMultiple())
        {
            ctx.put(this.toKey, p.getValues()[index].getString());
        } else
        {
            ctx.put(this.toKey, p.getValue().getString());
        }
        return false;
    }

    /**
     * @return Returns the index.
     */
    public String getIndex()
    {
        return index;
    }

    /**
     * @param index
     *            The index to set.
     */
    public void setIndex(String index)
    {
        this.index = index;
    }

    /**
     * @return Returns the indexKey.
     */
    public String getIndexKey()
    {
        return indexKey;
    }

    /**
     * @param indexKey
     *            Set the context attribute key for the index attribute.
     */
    public void setIndexKey(String indexKey)
    {
        this.indexKey = indexKey;
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

    /**
     * @return Returns the toKey.
     */
    public String getToKey()
    {
        return toKey;
    }

    /**
     * @param toKey
     *            The toKey to set.
     */
    public void setToKey(String toKey)
    {
        this.toKey = toKey;
    }
}
