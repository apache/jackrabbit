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

import org.apache.commons.chain.Command;
import org.apache.commons.chain.Context;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.standalone.cli.CommandHelper;

/**
 * Lock the given <code>Node</code>
 */
public class Lock implements Command {
    /** logger */
    private static Log log = LogFactory.getLog(Lock.class);

    // ---------------------------- < keys >
    /** Node path key */
    private String pathKey = "path";

    /**
     * depth lock
     */
    private String deepKey = "deep";

    /**
     * Session scoped lock <br>
     * Key that refers to a <code>Boolean</code> context variable
     */
    private String sessionScopedKey = "sessionScoped";

    /**
     * {@inheritDoc}
     */
    public boolean execute(Context ctx) throws Exception {
        String path = (String) ctx.get(this.pathKey);
        boolean deep = Boolean.valueOf((String) ctx.get(this.deepKey))
            .booleanValue();
        boolean sessionScoped = Boolean.valueOf(
            (String) ctx.get(this.sessionScopedKey)).booleanValue();
        if (log.isDebugEnabled()) {
            log.debug("locking node at " + path + " deep=" + deep
                    + " sessionScoped=" + sessionScoped);
        }
        CommandHelper.getNode(ctx, path).lock(deep, sessionScoped);
        return false;
    }

    /**
     * @return deep key
     */
    public String getDeepKey() {
        return deepKey;
    }

    /**
     * @param deepKey
     *        deep key to set
     */
    public void setDeepKey(String deepKey) {
        this.deepKey = deepKey;
    }

    /**
     * @return the session scoped key
     */
    public String getSessionScopedKey() {
        return sessionScopedKey;
    }

    /**
     * @param sessionScopedKey
     *        the session scoped key to set
     */
    public void setSessionScopedKey(String sessionScopedKey) {
        this.sessionScopedKey = sessionScopedKey;
    }

    /**
     * @return the source path key
     */
    public String getPathKey() {
        return pathKey;
    }

    /**
     * @param srcPathKey
     *        the source path key to set
     */
    public void setPathKey(String srcPathKey) {
        this.pathKey = srcPathKey;
    }
}
