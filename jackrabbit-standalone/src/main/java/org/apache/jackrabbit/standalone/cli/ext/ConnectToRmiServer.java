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
package org.apache.jackrabbit.standalone.cli.ext;

import javax.jcr.Repository;

import org.apache.commons.chain.Command;
import org.apache.commons.chain.Context;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.rmi.client.ClientRepositoryFactory;
import org.apache.jackrabbit.standalone.cli.CommandHelper;

/**
 * Connect to a JCR-RMI server
 */
public class ConnectToRmiServer implements Command {
    /** logger */
    private static Log log = LogFactory.getLog(ConnectToRmiServer.class);

    // ---------------------------- < keys >
    /** url key */
    private String urlKey = "url";

    /**
     * {@inheritDoc}
     */
    public boolean execute(Context ctx) throws Exception {
        String url = (String) ctx.get(this.urlKey);
        if (log.isDebugEnabled()) {
            log.debug("connecting to jcr-rmi server at " + url);
        }
        ClientRepositoryFactory factory = new ClientRepositoryFactory();
        Repository repository = factory.getRepository(url);
        CommandHelper.setRepository(ctx, repository, "jcr-rmi " + url);
        return false;
    }

    /**
     * @return the url key
     */
    public String getUrlKey() {
        return urlKey;
    }

    /**
     * @param urlKey
     *            the url key to set
     */
    public void setUrlKey(String urlKey) {
        this.urlKey = urlKey;
    }
}
