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

import javax.jcr.Session;

import org.apache.commons.chain.Command;
import org.apache.commons.chain.Context;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.standalone.cli.CommandHelper;

/**
 * Logout from the current working <code>Repository</code>
 */
public class Logout implements Command {
    /** logger */
    private static Log log = LogFactory.getLog(Logout.class);


    /**
     * {@inheritDoc}
     */
    public boolean execute(Context ctx) throws Exception {
        Session s = CommandHelper.getSession(ctx);
        if (log.isDebugEnabled()) {
            log.debug("logging out user " + s.getUserID());
        }
        s.logout();
        CommandHelper.setCurrentNode(ctx, null);
        CommandHelper.setSession(ctx, null);
        return false;
    }
}
