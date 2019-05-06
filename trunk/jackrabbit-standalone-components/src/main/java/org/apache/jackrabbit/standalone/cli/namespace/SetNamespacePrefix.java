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
package org.apache.jackrabbit.standalone.cli.namespace;

import org.apache.commons.chain.Command;
import org.apache.commons.chain.Context;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.standalone.cli.CommandHelper;

/**
 * Sets a namespace prefix
 */
public class SetNamespacePrefix implements Command {
    /** logger */
    private static Log log = LogFactory.getLog(SetNamespacePrefix.class);

    // ---------------------------- < keys >
    /** prefix key */
    private String prefixKey = "prefix";

    /** uri key */
    private String uriKey = "uri";

    /**
     * {@inheritDoc}
     */
    public boolean execute(Context ctx) throws Exception {
        String prefix = (String) ctx.get(this.prefixKey);
        String uri = (String) ctx.get(this.uriKey);
        if (log.isDebugEnabled()) {
            log.debug("setting namespace prefix uri=" + uri + " new prefix=" + prefix);
        }
        CommandHelper.getSession(ctx).setNamespacePrefix(prefix, uri);
        return false;
    }

    /**
     * @return the prefix key.
     */
    public String getPrefixKey() {
        return prefixKey;
    }

    /**
     * @param prefixKey
     *        the prefix key to set
     */
    public void setPrefixKey(String prefixKey) {
        this.prefixKey = prefixKey;
    }

    /**
     * @return the uri key
     */
    public String getUriKey() {
        return uriKey;
    }

    /**
     * @param uriKey
     *        the uri key to set
     */
    public void setUriKey(String uriKey) {
        this.uriKey = uriKey;
    }
}
