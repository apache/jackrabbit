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

import javax.jcr.Item;
import javax.jcr.Session;

import org.apache.commons.chain.Command;
import org.apache.commons.chain.Context;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.command.CommandHelper;

/**
 * Refresh the session to reflect the current saved state. <br>
 * If the path is set then only that Item is refreshed.
 */
public class Refresh implements Command
{
	/** logger */
	private static Log log = LogFactory.getLog(Refresh.class);

	// ---------------------------- < keys >
	/** keep changes key */
	private String keepChangesKey = "keepChanges";

	/** path to the node to refresh key */
	private String pathKey = "path";

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
		boolean keepChanges = Boolean.valueOf(
				(String) ctx.get(this.keepChangesKey)).booleanValue();

		String path = (String) ctx.get(this.pathKey);

		if (log.isDebugEnabled())
		{
			log.debug("refreshing. from node " + path);
		}

		if (path == null)
		{
			Session s = CommandHelper.getSession(ctx);
			s.refresh(keepChanges);
		} else
		{
			Item i = CommandHelper.getItem(ctx, path);
			i.refresh(keepChanges);
		}

		return false;
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
