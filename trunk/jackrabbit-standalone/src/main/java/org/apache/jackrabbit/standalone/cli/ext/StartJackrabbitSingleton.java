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

import java.util.HashMap;
import java.util.Map;

import javax.jcr.Repository;

import org.apache.commons.chain.Command;
import org.apache.commons.chain.Context;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.apache.jackrabbit.standalone.cli.CommandHelper;

/**
 * <p>
 * Get a Jackrabbit instance and put it in the <code>Context</code>.<br>
 * This commands maintains a cache with already created instances.<br>
 * if there's no Repository for the given config and home settings a new
 * instance will be created and cached.
 * </p>
 */
public class StartJackrabbitSingleton implements Command {
    /** logger */
    private static Log log = LogFactory.getLog(StartJackrabbitSingleton.class);

    /** cache */
    private static Map cache = new HashMap();

    /** config file */
    private String configKey = "config";

    /** home folder */
    private String homeKey = "home";

    /**
     * {@inheritDoc}
     */
    public boolean execute(Context ctx) throws Exception {
        String config = (String) ctx.get(this.configKey);
        String home = (String) ctx.get(this.homeKey);

        if (log.isDebugEnabled()) {
            log
                    .debug("starting jackrabbit. config=" + config + " home="
                            + home);
        }

        try {
            synchronized (cache) {
                String key = config + "@" + home;
                Repository repo = (Repository) cache.get(key);
                if (repo == null) {
                    String msg = "Starting Jakrabbit instance";
                    log.info(msg);
                    RepositoryConfig conf = RepositoryConfig.create(config,
                            home);
                    repo = RepositoryImpl.create(conf);
                    cache.put(key, repo);
                }
                CommandHelper.setRepository(ctx, repo, "local singleton " + home);
            }
        } catch (Exception e) {
            log.error("Unable to start jackrabbit", e);
            throw e;
        }
        return false;
    }

    /**
     * @return the config key
     */
    public String getConfigKey() {
        return configKey;
    }

    /**
     * @param configKey
     *            the config key to set
     */
    public void setConfigKey(String configKey) {
        this.configKey = configKey;
    }

    /**
     * @return the home key
     */
    public String getHomeKey() {
        return homeKey;
    }

    /**
     * @param homeKey
     *            the home key to set
     */
    public void setHomeKey(String homeKey) {
        this.homeKey = homeKey;
    }

}
