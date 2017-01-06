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

import javax.jcr.Item;
import javax.jcr.Session;

import org.apache.commons.chain.Command;
import org.apache.commons.chain.Context;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.standalone.cli.CommandHelper;

/**
 * Rename a <code>Node</code><br>
 * The persistent flag indicates whether to use the session or the workspace 
 * to perform the move command
 */
public class Rename implements Command {
    /** logger */
    private static Log log = LogFactory.getLog(Rename.class);

    // ---------------------------- < keys >
    /** source path */
    private String srcPathKey = "srcPath";

    /** destination path */
    private String destPathKey = "destPath";

    /** persistent key */
    private String persistentKey = "persistent";

    /**
     * {@inheritDoc}
     */
    public boolean execute(Context ctx) throws Exception {
        String from = (String) ctx.get(this.srcPathKey);
        String to = (String) ctx.get(this.destPathKey);

        boolean persistent = Boolean.valueOf(
                (String) ctx.get(this.persistentKey)).booleanValue();

        if (log.isDebugEnabled()) {
            log.debug("renaming node from " + from + " to " + to);
        }

        Session s = CommandHelper.getSession(ctx);
        Item itemFrom = CommandHelper.getItem(ctx, from);
        if (itemFrom.getDepth() == 1) {
            if (persistent) {
                s.getWorkspace().move(itemFrom.getPath(), "/" + to);
            } else {
                s.move(itemFrom.getPath(), "/" + to);
            }
        } else {
            if (persistent) {
                s.getWorkspace().move(itemFrom.getPath(),
                        itemFrom.getParent().getPath() + "/" + to);
            } else {
                s.move(itemFrom.getPath(), itemFrom.getParent().getPath() + "/"
                        + to);
            }
        }

        return false;
    }

    /**
     * @return the destination path key
     */
    public String getDestPathKey() {
        return destPathKey;
    }

    /**
     * @param destPathKey
     *            the destination path key to set
     */
    public void setDestPathKey(String destPathKey) {
        this.destPathKey = destPathKey;
    }

    /**
     * @return the source path key
     */
    public String getSrcPathKey() {
        return srcPathKey;
    }

    /**
     * @param srcPathKey
     *            the source path key to set
     */
    public void setSrcPathKey(String srcPathKey) {
        this.srcPathKey = srcPathKey;
    }

    public String getPersistentKey() {
        return persistentKey;
    }

    public void setPersistentKey(String persistentKey) {
        this.persistentKey = persistentKey;
    }
}
