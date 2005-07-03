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

import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.commons.chain.Command;
import org.apache.commons.chain.Context;
import org.apache.jackrabbit.chain.ContextHelper;

/**
 * Login command.
 */
public class Login implements Command {
	/** user */
	private String user;

	/** password */
	private String password;

	/** workspace */
	private String workspace;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.commons.chain.Command#execute(org.apache.commons.chain.Context)
	 */
	public boolean execute(Context ctx) throws Exception {
		Session session = null;
		Repository repo = ContextHelper.getRepository(ctx);

        Credentials credentials = new SimpleCredentials(user, password
				.toCharArray());

		if (this.workspace == null) {
			session = repo.login(credentials);
		} else {
			session = repo.login(credentials, workspace);
		}
		ContextHelper.setSession(ctx, session);
		ContextHelper.setCurrentNode(ctx, session.getRootNode());
		return false;
	}

	/**
	 * @return Returns the password.
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * @param password
	 *            The password to set.
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * @return Returns the user.
	 */
	public String getUser() {
		return user;
	}

	/**
	 * @param user
	 *            The user to set.
	 */
	public void setUser(String user) {
		this.user = user;
	}

	/**
	 * @return Returns the workspace.
	 */
	public String getWorkspace() {
		return workspace;
	}

	/**
	 * @param workspace
	 *            The workspace to set.
	 */
	public void setWorkspace(String workspace) {
		this.workspace = workspace;
	}
}
