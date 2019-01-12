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

import javax.jcr.Node;

import org.apache.commons.chain.Command;
import org.apache.commons.chain.Context;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.standalone.cli.CommandHelper;

/**
 * Set the order of the given <code>Node</code>
 */
public class OrderBefore implements Command {
    /** logger */
    private static Log log = LogFactory.getLog(OrderBefore.class);

    // ---------------------------- < keys >
    /** node path */
    private String parentPathKey = "parentPath";

    /** source path */
    private String srcChildKey = "srcChild";

    /** destination path */
    private String destChildKey = "destChild";

    /**
     * {@inheritDoc}
     */
    public boolean execute(Context ctx) throws Exception {
        String parentPath = (String) ctx.get(this.parentPathKey);
        Node n = CommandHelper.getNode(ctx, parentPath);

        String srcChildPath = (String) ctx.get(this.srcChildKey);
        String destChildPath = (String) ctx.get(this.destChildKey);

        if (log.isDebugEnabled()) {
            log
                .debug("ordering before. from " + n.getPath() + "/"
                        + srcChildPath + " to " + n.getPath() + "/"
                        + destChildPath);
        }

        n.orderBefore(srcChildPath, destChildPath);

        return false;
    }

    /**
     * @return the destination child key
     */
    public String getDestChildKey() {
        return destChildKey;
    }

    /**
     * @param destChildRelPathKey
     *        the destination child key to set
     */
    public void setDestChildKey(String destChildRelPathKey) {
        this.destChildKey = destChildRelPathKey;
    }

    /**
     * @return the source child key
     */
    public String getSrcChildKey() {
        return srcChildKey;
    }

    /**
     * @param srcChildRelPathKey
     *        the source child key to set
     */
    public void setSrcChildKey(String srcChildRelPathKey) {
        this.srcChildKey = srcChildRelPathKey;
    }

    /**
     * @return the parent path key
     */
    public String getParentPathKey() {
        return parentPathKey;
    }

    /**
     * @param parentPathKey
     *        the parent path key to set
     */
    public void setParentPathKey(String parentPathKey) {
        this.parentPathKey = parentPathKey;
    }
}
