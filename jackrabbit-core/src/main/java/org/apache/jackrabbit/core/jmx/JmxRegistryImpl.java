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
package org.apache.jackrabbit.core.jmx;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.jackrabbit.core.jmx.query.QueryStatManager;
import org.apache.jackrabbit.core.jmx.query.QueryStatManagerImpl;
import org.apache.jackrabbit.core.jmx.query.QueryStatManagerImplMBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JmxRegistry default implementation
 * 
 */
public class JmxRegistryImpl implements JmxRegistry {

    private static Logger log = LoggerFactory.getLogger(JmxRegistryImpl.class);

    private MBeanServer server;

    private List<ObjectName> registry = new ArrayList<ObjectName>();

    private QueryStatManagerImpl queryStatManager;

    public JmxRegistryImpl() {
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.jackrabbit.core.jmx.JmxRegistry#start()
     */
    public void start() {
        server = ManagementFactory.getPlatformMBeanServer();
        try {
            queryStatManager = new QueryStatManagerImpl();
            register(queryStatManager, new ObjectName(
                    QueryStatManagerImplMBean.NAME));
            log.info("Started JMX Registry");
        } catch (Exception e) {
            queryStatManager = null;
            log.error("Unable to start JMX Registry", e);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.jackrabbit.core.jmx.JmxRegistry#stop()
     */
    public void stop() {
        if (server == null) {
            return;
        }

        for (ObjectName o : registry) {
            try {
                server.unregisterMBean(o);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        registry.clear();
        server = null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.jackrabbit.core.jmx.JmxRegistry#register(org.apache.jackrabbit
     * .core.jmx.JackrabbitMBean, javax.management.ObjectName)
     */
    public void register(JackrabbitBaseMBean bean, ObjectName name)
            throws Exception {
        this.server.registerMBean(bean, name);
        this.registry.add(name);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.jackrabbit.core.jmx.JmxRegistry#unregister(javax.management
     * .ObjectName)
     */
    public void unregister(ObjectName name) throws Exception {
        if (server == null || !server.isRegistered(name)) {
            return;
        }
        registry.remove(name);
        server.unregisterMBean(name);
    }

    public QueryStatManager getQueryStatManager() {
        return queryStatManager;
    }
}
