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
package org.apache.jackrabbit.server.io;

import org.apache.commons.chain.Command;
import org.apache.commons.chain.Context;
import org.apache.jackrabbit.JcrConstants;

/**
 * This Class implements an abstract command
 */
public abstract class AbstractCommand implements Command, JcrConstants {

    /**
     * the id of the command
     */
    private String id;

    /**
     * flag, indicating if this command is enabled
     */
    private boolean enabled = true;

    /**
     * Executes this command by calling {@link #execute(AbstractContext)} if
     * this command is not disabled by the context properties.
     *
     * @param context the (import) context.
     * @return the return value of the delegated method or false;
     * @throws Exception in an error occurrs
     */
    final public boolean execute(Context context) throws Exception {
        if (context instanceof AbstractContext) {
            AbstractContext ctx = (AbstractContext) context;
            if (!ctx.isCommandEnabled(getId(), enabled)) {
                // ignore if this command is disabled by context property
                return false;
            }
            return execute(ctx);
        } else {
            return false;
        }
    }

    /**
     * Sets the enabled flag.
     * @param enabled
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Executes this command
     *
     * @param context
     * @return
     * @throws Exception
     */
    public abstract boolean execute(AbstractContext context) throws Exception;

    /**
     * Gets the id of this command
     * @return
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the id of this command
     * @param id
     */
    public void setId(String id) {
        this.id = id;
    }
}
