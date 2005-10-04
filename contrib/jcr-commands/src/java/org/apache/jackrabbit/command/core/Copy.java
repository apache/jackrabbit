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

import javax.jcr.Workspace;

import org.apache.commons.chain.Command;
import org.apache.commons.chain.Context;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.command.CommandHelper;

/**
 * Copy a Node. If the source workspace is null it will create a copy of the
 * given node from the current working workspace.
 */
public class Copy implements Command
{
	/** logger */
	private static Log log = LogFactory.getLog(Copy.class);

	// ---------------------------- < keys >
	/**
	 * Source workspace.
	 */
	private String srcWorkspaceKey = "srcWorkspace";

	/** source path */
	private String srcAbsPathKey = "srcAbsPath";

	/** destination path */
	private String destAbsPathKey = "destAbsPath";

	/**
	 * @inheritDoc
	 */
	public boolean execute(Context ctx) throws Exception
	{
		String srcWorkspace = (String) ctx.get(this.srcWorkspaceKey);
		String srcAbsPath = (String) ctx.get(this.srcAbsPathKey);
		String destAbsPath = (String) ctx.get(this.destAbsPathKey);

		Workspace w = CommandHelper.getSession(ctx).getWorkspace();
		
		if (srcWorkspace == null)
		{
			srcWorkspace = w.getName();
		}

		if (log.isDebugEnabled())
		{
			log.debug("copying node from [" + srcWorkspace + ":" + srcAbsPath
					+ "] to [" + w.getName() + ":" + destAbsPath + "]");
		}

		w.copy(srcWorkspace, srcAbsPath, destAbsPath);

		return false;
	}

	public String getDestAbsPathKey()
	{
		return destAbsPathKey;
	}

	public void setDestAbsPathKey(String destAbsPathKey)
	{
		this.destAbsPathKey = destAbsPathKey;
	}

	public String getSrcAbsPathKey()
	{
		return srcAbsPathKey;
	}

	public void setSrcAbsPathKey(String srcAbsPathKey)
	{
		this.srcAbsPathKey = srcAbsPathKey;
	}

	public String getSrcWorkspaceKey()
	{
		return srcWorkspaceKey;
	}

	public void setSrcWorkspaceKey(String srcWorkspaceKey)
	{
		this.srcWorkspaceKey = srcWorkspaceKey;
	}
}
