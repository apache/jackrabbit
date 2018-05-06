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
package org.apache.jackrabbit.test;

import java.util.List;
import java.util.LinkedList;
import java.util.Properties;
import java.util.Map;
import java.util.HashMap;
import java.io.InputStream;
import java.io.IOException;

/**
 * <code>RepositoryHelperPoolImpl</code> implements a pool of repository helper
 * instances.
 */
public class RepositoryHelperPoolImpl implements RepositoryHelperPool {

    private static final String PROP_FILE = "repositoryHelperPool.properties";

    private List<RepositoryHelper> helpers = new LinkedList<RepositoryHelper>();

    private static RepositoryHelperPool POOL = null;

    public synchronized static RepositoryHelperPool getInstance() {
        if (POOL == null) {
            POOL = new RepositoryHelperPoolImpl();
        }
        return POOL;
    }

    private RepositoryHelperPoolImpl() {
        InputStream in = RepositoryHelperPoolImpl.class.getClassLoader().getResourceAsStream(PROP_FILE);
        if (in != null) {
            try {
                Properties props = new Properties();
                props.load(in);
                for (int i = 0;; i++) {
                    String prefix = "helper." + i + ".";
                    Map<String, Object> helperProp = new HashMap<String, Object>();
                    for (Map.Entry<Object, Object> entry : props.entrySet()) {
                        String key = (String) entry.getKey();
                        if (key.startsWith(prefix)) {
                            helperProp.put(key.substring(prefix.length()), entry.getValue());
                        }
                    }
                    if (helperProp.isEmpty()) {
                        break;
                    }
                    addHelper(new RepositoryHelper(helperProp));
                }
            } catch (IOException e) {
                // ignore and use default
            } finally {
                try {
                    in.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
        if (helpers.isEmpty()) {
            // use single default repo helper
            addHelper(new RepositoryHelper());
        }
    }

    public synchronized void addHelper(RepositoryHelper helper) {
        helpers.add(helper);
    }

    public synchronized RepositoryHelper borrowHelper()
            throws InterruptedException {
        while (helpers.isEmpty()) {
            wait();
        }
        return helpers.remove(0);
    }

    public synchronized RepositoryHelper[] borrowHelpers() throws InterruptedException {
        while (helpers.isEmpty()) {
            wait();
        }
        try {
            return helpers.toArray(new RepositoryHelper[helpers.size()]);
        } finally {
            helpers.clear();
        }
    }

    public synchronized void returnHelper(RepositoryHelper helper) {
        helpers.add(helper);
        notifyAll();
    }
}
