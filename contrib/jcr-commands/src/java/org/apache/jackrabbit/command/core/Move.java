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

import java.util.ResourceBundle;

import javax.jcr.Workspace;

import org.apache.commons.chain.Command;
import org.apache.commons.chain.Context;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.command.CommandHelper;

/**
 * Move a Node.
 */
public class Move implements Command
{
    /** resource bundle */
    private static ResourceBundle bundle = CommandHelper.getBundle();

    /** logger */
    private static Log log = LogFactory.getLog(Move.class);

    // ---------------------------- < keys >
    /** source path */
    private String srcAbsPathKey = "srcAbsPath";

    /** destination path */
    private String destAbsPathKey = "destAbsPath";

    /**
     * @inheritDoc
     */
    public boolean execute(Context ctx) throws Exception
    {
        String srcAbsPath = (String) ctx.get(this.srcAbsPathKey);
        String destAbsPath = (String) ctx.get(this.destAbsPathKey);

        if (!srcAbsPath.startsWith("/") || !destAbsPath.startsWith("/"))
        {
            throw new IllegalArgumentException(
                bundle.getString("exception.illegalargument")
                + ". " + 
                bundle.getString("exception.only.absolute.path") + ".");
        }

        Workspace w = CommandHelper.getSession(ctx).getWorkspace();

        if (log.isDebugEnabled())
        {
            log.debug("moving node from " + srcAbsPath + " to " + destAbsPath);
        }

        w.move(srcAbsPath, destAbsPath);

        return false;
    }

    /**
     * @return Returns the destAbsPathKey.
     */
    public String getDestAbsPathKey()
    {
        return destAbsPathKey;
    }

    /**
     * @param destAbsPathKey
     *            The destAbsPathKey to set.
     */
    public void setDestAbsPathKey(String destAbsPathKey)
    {
        this.destAbsPathKey = destAbsPathKey;
    }

    /**
     * @return Returns the srcAbsPathKey.
     */
    public String getSrcAbsPathKey()
    {
        return srcAbsPathKey;
    }

    /**
     * @param srcAbsPathKey
     *            The srcAbsPathKey to set.
     */
    public void setSrcAbsPathKey(String srcAbsPathKey)
    {
        this.srcAbsPathKey = srcAbsPathKey;
    }
}
