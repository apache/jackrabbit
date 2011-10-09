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

import javax.management.ObjectName;

import org.apache.jackrabbit.core.jmx.JackrabbitBaseMBean;

/**
 * JMX Mbean dynamic registration service
 * 
 */
public interface JmxRegistry {

    /**
     * starts the service
     */
    void start();

    /**
     * stops the service
     */
    void stop();

    /**
     * Registers a new MBEan under the given name
     * 
     * @param bean
     *            to be registered
     * @param name
     * @throws Exception
     */
    void register(JackrabbitBaseMBean bean, ObjectName name) throws Exception;

    /**
     * Unregisters a bean
     * 
     * @param name
     * @throws Exception
     */
    void unregister(ObjectName name) throws Exception;

    void enableCoreStatJmx();

    void enableQueryStatJmx();

}
