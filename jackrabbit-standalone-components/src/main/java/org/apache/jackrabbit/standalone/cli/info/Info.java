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
package org.apache.jackrabbit.standalone.cli.info;

import java.io.PrintWriter;

import javax.jcr.Session;

import org.apache.commons.chain.Command;
import org.apache.commons.chain.Context;
import org.apache.jackrabbit.standalone.cli.CommandException;
import org.apache.jackrabbit.standalone.cli.CommandHelper;

public class Info implements Command {

    public boolean execute(Context ctx) throws Exception {
        PrintWriter out = CommandHelper.getOutput(ctx);

        out.println();

        try {
            CommandHelper.getRepository(ctx);
        } catch (CommandException e) {
            out.println("No connection to a repository.");
            return false;
        }

        out.println("Repository: " + CommandHelper.getRepositoryAddress(ctx));

        Session session;
        String currentPath;
        try {
            session = CommandHelper.getSession(ctx);
            currentPath = CommandHelper.getCurrentNode(ctx).getPath();
        } catch (CommandException e) {
            out.println("Not logged in / no session.");
            return false;
        }

        out.println("User      : " + session.getUserID());
        out.println("Workspace : " + session.getWorkspace().getName());
        out.println("Node      : " + currentPath);

        out.println();

        if (session.isLive()) {
            out.println("Session is live.");
        } else {
            out.println("Session is not live.");
        }

        if (session.hasPendingChanges()) {
            out.println("Session has pending changes.");
        } else {
            out.println("Session has no changes.");
        }

        return false;
    }

}
