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

import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;

import org.apache.commons.chain.Command;
import org.apache.commons.chain.Context;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.standalone.cli.CommandHelper;

/**
 * Remove the <code>Version</code> from the <code>VersionHistory</code> that
 * match the given label
 */
public class RemoveVersionByLabel implements Command {
    /** logger */
    private static Log log = LogFactory.getLog(RemoveVersionByLabel.class);

    // ---------------------------- < keys >
    /** node path */
    private String pathKey = "path";

    /** version label key */
    private String labelKey = "label";

    /**
     * {@inheritDoc}
     */
    public boolean execute(Context ctx) throws Exception {
        String path = (String) ctx.get(this.pathKey);
        String label = (String) ctx.get(this.labelKey);
        if (log.isDebugEnabled()) {
            log.debug("Remove version with label " + label + " from node "
                    + path);
        }
        VersionHistory vh = CommandHelper.getNode(ctx, path)
            .getVersionHistory();
        Version v = vh.getVersionByLabel(label);
        vh.removeVersion(v.getName());
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
     * @return the version name key
     */
    public String getLabelKey() {
        return labelKey;
    }

    /**
     * @param versionNameKey
     *        the version name key to set
     */
    public void setLabelKey(String versionNameKey) {
        this.labelKey = versionNameKey;
    }
}
