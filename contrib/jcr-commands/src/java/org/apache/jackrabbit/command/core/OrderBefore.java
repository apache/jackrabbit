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
package org.apache.jackrabbit.command.core;

import javax.jcr.Node;

import org.apache.commons.chain.Command;
import org.apache.commons.chain.Context;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.command.CommandHelper;

/**
 * Sets the order of the given node.
 */
public class OrderBefore implements Command
{
    /** logger */
    private static Log log = LogFactory.getLog(OrderBefore.class);

    // ---------------------------- < keys >
    /** node path */
    private String parentPathKey = "parentPath";

    /** source path */
    private String srcChildKey = "srcChild";

    /** destination path */
    private String destChildKey = "destChild";

    public boolean execute(Context ctx) throws Exception
    {
        String parentPath = (String) ctx.get(this.parentPathKey);
        Node n = CommandHelper.getNode(ctx, parentPath);

        String srcChildPath = (String) ctx.get(this.srcChildKey);
        String destChildPath = (String) ctx.get(this.destChildKey);

        if (log.isDebugEnabled())
        {
            log.debug("ordering before. from " + n.getPath() + "/"
                    + srcChildPath + " to " + n.getPath() + "/"
                    + destChildPath);
        }

        n.orderBefore(srcChildPath, destChildPath);

        return false;
    }

    public String getDestChildKey()
    {
        return destChildKey;
    }

    public void setDestChildKey(String destChildRelPathKey)
    {
        this.destChildKey = destChildRelPathKey;
    }

    public String getSrcChildKey()
    {
        return srcChildKey;
    }

    public void setSrcChildKey(String srcChildRelPathKey)
    {
        this.srcChildKey = srcChildRelPathKey;
    }

    /**
     * @return Returns the parentPathKey.
     */
    public String getParentPathKey()
    {
        return parentPathKey;
    }

    /**
     * @param parentPathKey
     *            The parentPathKey to set.
     */
    public void setParentPathKey(String parentPathKey)
    {
        this.parentPathKey = parentPathKey;
    }
}
