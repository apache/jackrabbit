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
package org.apache.jackrabbit.command.version;

import org.apache.commons.chain.Command;
import org.apache.commons.chain.Context;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.command.CommandHelper;

/**
 * Restore a node to the version with the specified label
 */
public class RestoreByLabel implements Command
{
	/** logger */
	private static Log log = LogFactory.getLog(RestoreByLabel.class);

	// ---------------------------- < keys >
	/** node path */
	private String pathKey = "path";

	/** version name key */
	private String labelKey = "label";

	/** remove existing node key */
	private String removeExistingKey = "removeExisting";

	/**
	 * @inheritDoc
	 */
	public boolean execute(Context ctx) throws Exception
	{
		String path = (String) ctx.get(this.pathKey);
		String versionLabel = (String) ctx.get(this.labelKey);
		boolean removeExisting = Boolean.valueOf(
				(String) ctx.get(this.removeExistingKey)).booleanValue();
		if (log.isDebugEnabled())
		{
			log.debug("restoring node at " + path + " to version label "
					+ versionLabel + " removeexisting=" + removeExisting);
		}
		CommandHelper.getNode(ctx, path).restoreByLabel(versionLabel,
				removeExisting);
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

	/**
	 * @return Returns the removeExistingKey.
	 */
	public String getRemoveExistingKey()
	{
		return removeExistingKey;
	}

	/**
	 * @param removeExistingKey
	 *            The removeExistingKey to set.
	 */
	public void setRemoveExistingKey(String removeExistingKey)
	{
		this.removeExistingKey = removeExistingKey;
	}

	/**
	 * @return Returns the versionNameKey.
	 */
	public String getLabelKey()
	{
		return labelKey;
	}

	/**
	 * @param versionNameKey
	 *            The versionNameKey to set.
	 */
	public void setLabelKey(String versionNameKey)
	{
		this.labelKey = versionNameKey;
	}
}
