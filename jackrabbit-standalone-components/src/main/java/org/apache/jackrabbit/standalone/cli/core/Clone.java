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

import javax.jcr.Workspace;

import org.apache.commons.chain.Command;
import org.apache.commons.chain.Context;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.standalone.cli.CommandHelper;

/**
 * Clone the given <code>Node</code> to another <code>Workspace</code>
 */
public class Clone implements Command {
    /** logger */
    private static Log log = LogFactory.getLog(Clone.class);

    // ---------------------------- < keys >
    /** Source workspace */
    private String srcWorkspaceKey = "scrWorkspace";

    /** target workspace */
    private String srcAbsPathKey = "srcAbsPath";

    /** source node */
    private String destAbsPathKey = "destAbsPath";

    /** target node */
    private String removeExistingKey = "removeExisting";

    /**
     * {@inheritDoc}
     */
    public boolean execute(Context ctx) throws Exception {
        String srcWorkspace = (String) ctx.get(this.srcWorkspaceKey);
        String srcAbsPath = (String) ctx.get(this.srcAbsPathKey);
        String destAbsPath = (String) ctx.get(this.destAbsPathKey);
        Boolean removeExisting = Boolean.valueOf((String) ctx
            .get(this.removeExistingKey));

        Workspace w = CommandHelper.getSession(ctx).getWorkspace();

        if (log.isDebugEnabled()) {
            log.debug("cloning node. from [" + srcWorkspace + ":" + srcAbsPath
                    + "] to [" + w.getName() + ":" + destAbsPath + "]");
        }

        w.clone(srcWorkspace, srcAbsPath, destAbsPath, removeExisting
            .booleanValue());

        return false;
    }

    /**
     * @return the destination absolute path key
     */
    public String getDestAbsPathKey() {
        return destAbsPathKey;
    }

    /**
     * Sets the destination absolute path key
     * @param destAbsPathKey
     *        the destination absolute path key
     */
    public void setDestAbsPathKey(String destAbsPathKey) {
        this.destAbsPathKey = destAbsPathKey;
    }

    /**
     * @return the remove existing key option
     */
    public String getRemoveExistingKey() {
        return removeExistingKey;
    }

    /**
     * Set the remove existing key option
     * @param removeExistingKey
     *        the remove existing key option
     */
    public void setRemoveExistingKey(String removeExistingKey) {
        this.removeExistingKey = removeExistingKey;
    }

    /**
     * @return the source absolute path key
     */
    public String getSrcAbsPathKey() {
        return srcAbsPathKey;
    }

    /**
     * Sets the source absolute path key
     * @param srcAbsPathKey
     *        the source absolute path key
     */
    public void setSrcAbsPathKey(String srcAbsPathKey) {
        this.srcAbsPathKey = srcAbsPathKey;
    }

    /**
     * @return the source <code>Workspace</code> key
     */
    public String getSrcWorkspaceKey() {
        return srcWorkspaceKey;
    }

    /**
     * Sets the source <code>Workspace</code> key
     * @param srcWorkspaceKey
     *        the source <code>Workspace</code> key
     */
    public void setSrcWorkspaceKey(String srcWorkspaceKey) {
        this.srcWorkspaceKey = srcWorkspaceKey;
    }
}
