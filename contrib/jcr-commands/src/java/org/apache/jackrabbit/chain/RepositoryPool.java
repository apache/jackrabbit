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
package org.apache.jackrabbit.chain;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.Repository;

/**
 * Repository pool
 */
public class RepositoryPool
{
    /** singleton */
    private static RepositoryPool singleton = new RepositoryPool();

    /** cache of repository instances */
    private Map cache = Collections.synchronizedMap(new HashMap());

    /**
     * Private Constructor
     */
    private RepositoryPool()
    {
        super();
    }

    public synchronized Repository get(String conf, String home)
    {
        return (Repository) cache.get(getKey(conf, home));
    }

    public synchronized void put(String conf, String home, Repository repo)
    {
        if (cache.containsKey(getKey(conf, home)))
        {
            throw new IllegalArgumentException(
                "There's already a repository for the given key. Remove it first.");
        }
        cache.put(getKey(conf, home), repo);
    }

    private String getKey(String conf, String home)
    {
        return "conf:" + conf + "-home:" + home;
    }

    public static RepositoryPool getInstance()
    {
        return singleton;
    }

}
