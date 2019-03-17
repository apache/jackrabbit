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
package org.apache.jackrabbit.standalone.cli.version;

import org.apache.commons.chain.Command;
import org.apache.commons.chain.Context;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.standalone.cli.CommandHelper;

/**
 * Restore a <code>Node</code> to the state of the given <code>Version</code>
 */
public class Restore implements Command {
    /** logger */
    private static Log log = LogFactory.getLog(Restore.class);

    // ---------------------------- < keys >
    /** node path */
    private String pathKey = "path";

    /** version name key */
    private String versionKey = "version";

    /** remove existing node key */
    private String removeExistingKey = "removeExisting";

    /**
     * {@inheritDoc}
     */
    public boolean execute(Context ctx) throws Exception {
        String path = (String) ctx.get(this.pathKey);
        String versionName = (String) ctx.get(this.versionKey);
        boolean removeExisting = Boolean.valueOf(
            (String) ctx.get(this.removeExistingKey)).booleanValue();
        if (log.isDebugEnabled()) {
            log.debug("restoring node at " + path + " to version "
                    + versionName + " removeexisting=" + removeExisting);
        }
        CommandHelper.getNode(ctx, path).restore(versionName, removeExisting);
        return false;
    }

    /**
     * @return the path key
     */
    public String getPathKey() {
        return pathKey;
    }

    /**
     * @param pathKey
     *        the path key to set
     */
    public void setPathKey(String pathKey) {
        this.pathKey = pathKey;
    }

    /**
     * @return the remove existing key
     */
    public String getRemoveExistingKey() {
        return removeExistingKey;
    }

    /**
     * @param removeExistingKey
     *        the remove existing key to set
     */
    public void setRemoveExistingKey(String removeExistingKey) {
        this.removeExistingKey = removeExistingKey;
    }

    /**
     * @return the version name key
     */
    public String getVersionKey() {
        return versionKey;
    }

    /**
     * @param versionNameKey
     *        the version name key to set
     */
    public void setVersionKey(String versionNameKey) {
        this.versionKey = versionNameKey;
    }
}
