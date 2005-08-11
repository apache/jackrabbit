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
import org.apache.jackrabbit.chain.CtxHelper;
import org.apache.jackrabbit.chain.JcrCommandException;

/**
 * Login command. <br>
 * The Command attributes are set from the specified literal values, or from the
 * context attributes stored under the given keys.
 */
public class Login implements Command
{

    // ---------------------------- < literals >

    /** user */
    private String user;

    /** password */
    private String password;

    /** workspace */
    private String workspace;

    // ---------------------------- < keys >
    /** user key */
    private String userKey;

    /** password key */
    private String passwordKey;

    /** workspace key */
    private String workspaceKey;

    /**
     * @inheritDoc
     */
    public boolean execute(Context ctx) throws Exception
    {
        if (CtxHelper.getSession(ctx) != null)
        {
            throw new JcrCommandException("already.logged.in");
        }

        String anon = "anonymous";

        String user = CtxHelper.getAttr(this.user, this.userKey, anon,
            ctx);

        String password = CtxHelper.getAttr(this.password,
            this.passwordKey, anon, ctx);

        String workspace = CtxHelper.getAttr(this.workspace,
            this.workspaceKey, ctx);

        Session session = null;
        Repository repo = CtxHelper.getRepository(ctx);

        if (repo == null)
        {
            throw new JcrCommandException("repository.not.in.context");
        }

        Credentials credentials = new SimpleCredentials(user, password
            .toCharArray());

        if (workspace == null)
        {
            session = repo.login(credentials);
        } else
        {
            session = repo.login(credentials, workspace);
        }
        CtxHelper.setSession(ctx, session);
        CtxHelper.setCurrentNode(ctx, session.getRootNode());
        return false;
    }

    /**
     * @return Returns the password.
     */
    public String getPassword()
    {
        return password;
    }

    /**
     * @param password
     *            The password to set.
     */
    public void setPassword(String password)
    {
        this.password = password;
    }

    /**
     * @return Returns the user.
     */
    public String getUser()
    {
        return user;
    }

    /**
     * @param user
     *            The user to set.
     */
    public void setUser(String user)
    {
        this.user = user;
    }

    /**
     * @return Returns the workspace.
     */
    public String getWorkspace()
    {
        return workspace;
    }

    /**
     * @param workspace
     *            The workspace to set.
     */
    public void setWorkspace(String workspace)
    {
        this.workspace = workspace;
    }

    /**
     * @return Returns the passwordKey.
     */
    public String getPasswordKey()
    {
        return passwordKey;
    }

    /**
     * @param passwordKey
     *            Set the context attribute key for the password attribute.
     */
    public void setPasswordKey(String passwordKey)
    {
        this.passwordKey = passwordKey;
    }

    /**
     * @return Returns the userKey.
     */
    public String getUserKey()
    {
        return userKey;
    }

    /**
     * @param userKey
     *            Set the context attribute key for the user attribute.
     */
    public void setUserKey(String userKey)
    {
        this.userKey = userKey;
    }

    /**
     * @return Returns the workspaceKey.
     */
    public String getWorkspaceKey()
    {
        return workspaceKey;
    }

    /**
     * @param workspaceKey
     *            Set the context attribute key for the workspace attribute.
     */
    public void setWorkspaceKey(String workspaceKey)
    {
        this.workspaceKey = workspaceKey;
    }
}
