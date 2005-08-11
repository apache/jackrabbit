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
import javax.jcr.Session;

import org.apache.commons.chain.Command;
import org.apache.commons.chain.Context;
import org.apache.jackrabbit.chain.CtxHelper;

/**
 * Rename a node
 */
public class Rename implements Command
{
    // ---------------------------- < literals >
    /** source path */
    private String from;

    /** destination path */
    private String to;

    // ---------------------------- < keys >
    /** source path */
    private String fromKey;

    /** destination path */
    private String toKey;

    /**
     * @return Returns the from.
     */
    public String getFrom()
    {
        return from;
    }

    /**
     * @param from
     *            The from to set.
     */
    public void setFrom(String from)
    {
        this.from = from;
    }

    /**
     * @return Returns the fromKey.
     */
    public String getFromKey()
    {
        return fromKey;
    }

    /**
     * @param fromKey
     *            The fromKey to set.
     */
    public void setFromKey(String fromKey)
    {
        this.fromKey = fromKey;
    }

    /**
     * @return Returns the to.
     */
    public String getTo()
    {
        return to;
    }

    /**
     * @param to
     *            The to to set.
     */
    public void setTo(String to)
    {
        this.to = to;
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

    /**
     * @inheritDoc
     */
    public boolean execute(Context ctx) throws Exception
    {
        String from = CtxHelper.getAttr(this.from, this.fromKey, ctx);
        String to = CtxHelper.getAttr(this.to, this.toKey, ctx);

        Session s = CtxHelper.getSession(ctx);

        Node n = CtxHelper.getCurrentNode(ctx);

        Node nodeFrom = n.getNode(from);

        if (nodeFrom.getDepth() == 1)
        {
            s.move(nodeFrom.getPath(), "/" + to);
        } else
        {
            s.move(nodeFrom.getPath(), nodeFrom.getParent().getPath() + "/"
                    + to);
        }

        return false;
    }
}
