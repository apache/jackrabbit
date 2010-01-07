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
 * Merge
 */
public class Merge implements Command {
    /** logger */
    private static Log log = LogFactory.getLog(Merge.class);

    // ---------------------------- < keys >
    /** node path */
    private String pathKey = "path";

    /** source workspace key */
    private String srcWorkspaceKey = "srcWorkspace";

    /** best effort key */
    private String bestEffortKey = "bestEffort";

    /**
     * {@inheritDoc}
     */
    public boolean execute(Context ctx) throws Exception {
        String path = (String) ctx.get(this.pathKey);
        String srcWorkspace = (String) ctx.get(this.srcWorkspaceKey);
        boolean bestEffort = Boolean.valueOf(
            (String) ctx.get(this.bestEffortKey)).booleanValue();
        if (log.isDebugEnabled()) {
            log.debug("merging node at " + path + " from workspace "
                    + srcWorkspace + " besteffort=" + bestEffort);
        }
        CommandHelper.getNode(ctx, path).merge(srcWorkspace, bestEffort);
        return false;
    }

    /**
     * @return the best effort key
     */
    public String getBestEffortKey() {
        return bestEffortKey;
    }

    /**
     * @param bestEffortKey
     *        the best effort key to set
     */
    public void setBestEffortKey(String bestEffortKey) {
        this.bestEffortKey = bestEffortKey;
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
     * @return the source <code>Workspace</code> key
     */
    public String getSrcWorkspaceKey() {
        return srcWorkspaceKey;
    }

    /**
     * @param srcWorkspaceKey
     *        the source <code>Workspace</code> key to set
     */
    public void setSrcWorkspaceKey(String srcWorkspaceKey) {
        this.srcWorkspaceKey = srcWorkspaceKey;
    }
}
