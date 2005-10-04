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
package org.apache.jackrabbit.command.ext;

import java.util.ResourceBundle;

import javax.jcr.Workspace;

import org.apache.commons.chain.Command;
import org.apache.commons.chain.Context;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.command.CommandHelper;
import org.apache.jackrabbit.core.WorkspaceImpl;

/**
 * Creates a Workspace.<br>
 * Note that this Command uses Jackrabbit specific API.
 */
public class CreateWorkspace implements Command
{
	/** bundle */
	ResourceBundle bundle = CommandHelper.getBundle();

	/** logger */
	private static Log log = LogFactory.getLog(CreateWorkspace.class);

	// ---------------------------- < keys >
	/** workspace name key */
	private String nameKey = "name";

	public boolean execute(Context ctx) throws Exception
	{
		String name = (String) ctx.get(this.nameKey);
		if (log.isDebugEnabled())
		{
			log.debug("creating workspace for name " + name);
		}
		Workspace w = CommandHelper.getSession(ctx).getWorkspace();
		if (w instanceof WorkspaceImpl == false)
		{
			throw new IllegalStateException(bundle
					.getString("phrase.jackrabbit.command"));
		}

		WorkspaceImpl jrw = (WorkspaceImpl) w;
		jrw.createWorkspace(name);
		return false;
	}

	public String getNameKey()
	{
		return nameKey;
	}

	public void setNameKey(String nameKey)
	{
		this.nameKey = nameKey;
	}

}
