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
package org.apache.jackrabbit.core.jmx.registry;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.jackrabbit.core.jmx.JackrabbitBaseMBean;
import org.apache.jackrabbit.core.jmx.StatManager;
import org.apache.jackrabbit.core.jmx.core.CoreStatManager;
import org.apache.jackrabbit.core.jmx.core.CoreStatManagerMBean;
import org.apache.jackrabbit.core.jmx.query.QueryStatManager;
import org.apache.jackrabbit.core.jmx.query.QueryStatManagerMBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JmxRegistry default implementation
 * 
 */
public class JmxRegistryImpl implements JmxRegistry {

    private static Logger log = LoggerFactory.getLogger(JmxRegistryImpl.class);

    /* JMX */
    private MBeanServer server;

    private final List<ObjectName> registry = new ArrayList<ObjectName>();

    /* Stats */
    private final StatManager statManager;

    public JmxRegistryImpl(final StatManager statManager) {
        this.statManager = statManager;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.jackrabbit.core.jmx.JmxRegistry#start()
     */
    public void start() {
        server = ManagementFactory.getPlatformMBeanServer();
        enableRegistry();
        log.debug("Started JMX Registry.");
    }

    public void enableRegistry() {
        if (server == null) {
            return;
        }
        try {
            register(new JmxRegistryManager(this), new ObjectName(
                    JmxRegistryManagerMBean.NAME));
            log.debug("JMX Registry - registered DynamicRegistry.");
        } catch (Exception e) {
            log.error("JMX Registry - Unable to register DynamicRegistry.", e);
        }
    }

    public void enableCoreStatJmx() {
        if (server == null) {
            return;
        }
        try {
            register(new CoreStatManager(statManager.getCoreStat()),
                    new ObjectName(CoreStatManagerMBean.NAME));
            log.debug("JMX Registry - registered CoreStats.");
        } catch (Exception e) {
            log.error("JMX Registry - Unable to register CoreStats.", e);
        }
    }

    public void enableQueryStatJmx() {
        if (server == null) {
            return;
        }
        try {
            register(new QueryStatManager(statManager.getQueryStat()),
                    new ObjectName(QueryStatManagerMBean.NAME));
            log.debug("JMX Registry - registered QueryStats.");
        } catch (Exception e) {
            log.error("JMX Registry - Unable to register CoreStats.", e);
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

        List<ObjectName> registryCopy = new ArrayList<ObjectName>(registry);
        for (ObjectName o : registryCopy) {
            try {
                unregister(o);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        registry.clear();
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
        if (server == null || server.isRegistered(name)) {
            return;
        }
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

}
