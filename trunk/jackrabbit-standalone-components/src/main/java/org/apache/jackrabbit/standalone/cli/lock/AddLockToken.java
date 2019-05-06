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
package org.apache.jackrabbit.standalone.cli.lock;

import javax.jcr.Session;

import org.apache.commons.chain.Command;
import org.apache.commons.chain.Context;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.standalone.cli.CommandHelper;

/**
 * Add the given <code>Lock</code> token to the current <code>Session</code>
 */
public class AddLockToken implements Command {
    /** logger */
    private static Log log = LogFactory.getLog(AddLockToken.class);

    // ---------------------------- < keys >
    /**
     * token
     */
    private String tokenKey = "token";

    /**
     * {@inheritDoc}
     */
    public boolean execute(Context ctx) throws Exception {
        String token = (String) ctx.get(this.tokenKey);
        if (log.isDebugEnabled()) {
            log
                .debug("Adding lock token " + token
                        + " to the current session.");
        }
        Session s = CommandHelper.getSession(ctx);
        s.addLockToken(token);
        return false;
    }

    /**
     * @return the token key
     */
    public String getTokenKey() {
        return tokenKey;
    }

    /**
     * @param tokenKey
     *        the token key to set
     */
    public void setTokenKey(String tokenKey) {
        this.tokenKey = tokenKey;
    }
}
