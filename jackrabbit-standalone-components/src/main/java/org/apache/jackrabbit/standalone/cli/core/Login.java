/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.standalone.cli.core;

import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import jline.ConsoleReader;

import org.apache.commons.chain.Command;
import org.apache.commons.chain.Context;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.standalone.cli.CommandHelper;

/**
 * Login to the current working <code>Repository</code>
 */
public class Login implements Command {
    /** logger */
    private static Log log = LogFactory.getLog(Login.class);

    // ---------------------------- < keys >
    /** user key */
    private String userKey = "user";

    /** password key */
    private String passwordKey = "password";

    /** workspace key */
    private String workspaceKey = "workspace";

    /**
     * {@inheritDoc}
     */
    public boolean execute(Context ctx) throws Exception {
        String anon = "anonymous";

        String user = (String) ctx.get(this.userKey);
        String password = (String) ctx.get(this.passwordKey);
        String workspace = (String) ctx.get(this.workspaceKey);

        if (user == null) {
            user = anon;
        }

        if (password == null || (password.equals(anon) && !user.equals(anon))) {
            ConsoleReader reader = new ConsoleReader();
            password = reader.readLine("Password: ", (char) 0);
        }
        
        if (log.isDebugEnabled()) {
            log.debug("logging in as " + user);
        }

        Session session = null;
        Repository repo = CommandHelper.getRepository(ctx);

        Credentials credentials = new SimpleCredentials(user, password
            .toCharArray());

        if (workspace == null) {
            session = repo.login(credentials);
        } else {
            session = repo.login(credentials, workspace);
        }
        CommandHelper.setSession(ctx, session);
        CommandHelper.setCurrentNode(ctx, session.getRootNode());
        return false;
    }

    /**
     * @return the password key
     */
    public String getPasswordKey() {
        return passwordKey;
    }

    /**
     * @param passwordKey
     *        the password key to set
     */
    public void setPasswordKey(String passwordKey) {
        this.passwordKey = passwordKey;
    }

    /**
     * @return the user key.
     */
    public String getUserKey() {
        return userKey;
    }

    /**
     * @param userKey
     *        the user key to set
     */
    public void setUserKey(String userKey) {
        this.userKey = userKey;
    }

    /**
     * @return the <code>Workspace</code>.
     */
    public String getWorkspaceKey() {
        return workspaceKey;
    }

    /**
     * @param workspaceKey
     *        the <code>Workspace</code> key to set
     */
    public void setWorkspaceKey(String workspaceKey) {
        this.workspaceKey = workspaceKey;
    }
}
