/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.chain.command;

import javax.jcr.Repository;

import org.apache.commons.chain.Command;
import org.apache.commons.chain.Context;
import org.apache.jackrabbit.chain.CtxHelper;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.config.RepositoryConfig;

/**
 * Start Jackrabbit. <br>
 * Note that this command doesn't check whether there's a Jackrabbit
 * instance already running with the same configuration. Remember that two
 * jackrabbit instances using the same persistence storage might lead to a
 * corrupt Repository.<br>
 */
public class StartJackrabbit implements Command
{
    /** config file */
    private String config;

    /** home folder */
    private String home;

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.commons.chain.Command#execute(org.apache.commons.chain.Context)
     */
    public boolean execute(Context ctx) throws Exception
    {
        RepositoryConfig conf = RepositoryConfig.create(config, home);
        Repository repo = RepositoryImpl.create(conf);
        CtxHelper.setRepository(ctx, repo);
        return false;
    }

    /**
     * @return Returns the config.
     */
    public String getConfig()
    {
        return config;
    }

    /**
     * @param config
     *            The config to set.
     */
    public void setConfig(String config)
    {
        this.config = config;
    }

    /**
     * @return Returns the home.
     */
    public String getHome()
    {
        return home;
    }

    /**
     * @param home
     *            The home to set.
     */
    public void setHome(String home)
    {
        this.home = home;
    }
}
