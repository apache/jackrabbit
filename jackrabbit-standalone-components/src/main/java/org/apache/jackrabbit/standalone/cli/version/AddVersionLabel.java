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
 * Add a label to the given <code>Version</code>
 */
public class AddVersionLabel implements Command {
    /** logger */
    private static Log log = LogFactory.getLog(AddVersionLabel.class);

    // ---------------------------- < keys >
    /** node path */
    private String pathKey = "path";

    /** version name key */
    private String versionKey = "version";

    /** version label key */
    private String labelKey = "label";

    /** move label key */
    private String moveLabelKey = "moveLabel";

    /**
     * {@inheritDoc}
     */
    public boolean execute(Context ctx) throws Exception {
        String path = (String) ctx.get(this.pathKey);
        String versionName = (String) ctx.get(this.versionKey);
        boolean moveLabel = Boolean
            .valueOf((String) ctx.get(this.moveLabelKey)).booleanValue();
        String versionLabel = (String) ctx.get(this.labelKey);
        if (log.isDebugEnabled()) {
            log.debug("Add label " + versionLabel + " to version  "
                    + versionName + " of node at " + path);
        }
        CommandHelper.getNode(ctx, path).getVersionHistory().addVersionLabel(
            versionName, versionLabel, moveLabel);
        return false;
    }

    /**
     * @return the move label key
     */
    public String getMoveLabelKey() {
        return moveLabelKey;
    }

    /**
     * @param moveLabelKey
     *        the move label key to set
     */
    public void setMoveLabelKey(String moveLabelKey) {
        this.moveLabelKey = moveLabelKey;
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
     * @return the version label key
     */
    public String getLabelKey() {
        return labelKey;
    }

    /**
     * @param versionLabelKey
     *        the version label key to set
     */
    public void setLabelKey(String versionLabelKey) {
        this.labelKey = versionLabelKey;
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
