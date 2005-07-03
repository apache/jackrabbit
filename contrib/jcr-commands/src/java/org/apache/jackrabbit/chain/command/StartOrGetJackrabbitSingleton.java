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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.chain.ContextHelper;
import org.apache.jackrabbit.chain.RepositoryPool;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.config.RepositoryConfig;

/**
 * <p>
 * Gets a Jackrabbit instance from the RepositoryPool and put in the Commons
 * Chain Context. If there's no Repository for the given config then it will
 * create a new instance and will add it to the pool.
 * </p>
 * <p>
 * This is the recommended implementation to use in JMeter. It will create only
 * one Jackrabbit instance on the first call and will reuse it in all the JMeter
 * app lifecycle.
 * </p>
 */
public class StartOrGetJackrabbitSingleton implements Command
{
    /** logger */
    private static Log log = LogFactory.getLog(StartOrGetJackrabbitSingleton.class);

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
        try
        {
            RepositoryPool pool = RepositoryPool.getInstance();
            synchronized (pool)
            {
                Repository repo = pool.get(config, home);
                if (repo == null)
                {
                    String msg = "Starting Jakrabbit instance";
                    System.out.println(msg);
                    log.info(msg);
                    RepositoryConfig conf = RepositoryConfig.create(config, home);
                    repo = RepositoryImpl.create(conf);
                    pool.put(config, home, repo);
                }
                ContextHelper.setRepository(ctx, repo);
            }
        } catch (Exception e)
        {
            e.printStackTrace();
            throw e;
        }
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
