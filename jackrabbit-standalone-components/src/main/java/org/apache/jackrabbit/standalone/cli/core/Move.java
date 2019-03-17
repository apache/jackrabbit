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

import java.util.ResourceBundle;

import javax.jcr.Item;
import javax.jcr.Session;

import org.apache.commons.chain.Command;
import org.apache.commons.chain.Context;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.standalone.cli.CommandHelper;

/**
 * Move a <code>Node</code>
 */
public class Move implements Command {
    /** resource bundle */
    private static ResourceBundle bundle = CommandHelper.getBundle();

    /** logger */
    private static Log log = LogFactory.getLog(Move.class);

    // ---------------------------- < keys >
    /** source path */
    private String srcAbsPathKey = "srcAbsPath";

    /** destination path */
    private String destAbsPathKey = "destAbsPath";

    /** persistent key */
    private String persistentKey = "persistent";

    /**
     * {@inheritDoc}
     */
    public boolean execute(Context ctx) throws Exception {
        String srcAbsPath = (String) ctx.get(this.srcAbsPathKey);
        String destAbsPath = (String) ctx.get(this.destAbsPathKey);

        boolean persistent = Boolean.valueOf(
                (String) ctx.get(this.persistentKey)).booleanValue();

        if (!srcAbsPath.startsWith("/") || !destAbsPath.startsWith("/")) {
            throw new IllegalArgumentException(bundle
                    .getString("exception.illegalargument")
                    + ". "
                    + bundle.getString("exception.only.absolute.path")
                    + ".");
        }

        Session s = CommandHelper.getSession(ctx);

        if (log.isDebugEnabled()) {
            log.debug("moving node from " + srcAbsPath + " to " + destAbsPath);
        }

        if (destAbsPath.endsWith("/")) {
            Item source = s.getItem(srcAbsPath);
            destAbsPath = destAbsPath + source.getName();
        }

        if (persistent) {
            s.getWorkspace().move(srcAbsPath, destAbsPath);
        } else {
            s.move(srcAbsPath, destAbsPath);
        }

        return false;
    }

    /**
     * @return the destintation absolute path key
     */
    public String getDestAbsPathKey() {
        return destAbsPathKey;
    }

    /**
     * @param destAbsPathKey
     *            the destintation absolute path key to set
     */
    public void setDestAbsPathKey(String destAbsPathKey) {
        this.destAbsPathKey = destAbsPathKey;
    }

    /**
     * @return the source absolute path key
     */
    public String getSrcAbsPathKey() {
        return srcAbsPathKey;
    }

    /**
     * @param srcAbsPathKey
     *            the source absolute path key to set
     */
    public void setSrcAbsPathKey(String srcAbsPathKey) {
        this.srcAbsPathKey = srcAbsPathKey;
    }

    public String getPersistentKey() {
        return persistentKey;
    }

    public void setPersistentKey(String persistentKey) {
        this.persistentKey = persistentKey;
    }
}
