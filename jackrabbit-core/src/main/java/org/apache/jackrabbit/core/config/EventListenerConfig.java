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
package org.apache.jackrabbit.core.config;

import java.util.Properties;

import javax.jcr.observation.EventListener;

/**
 * Event listener configuration. This bean configuration class
 * is used to create configured event listener objects.
 */
public class EventListenerConfig extends BeanConfig {

    /**
     * Event types.
     */
    private int eventTypes;
    
    /**
     * Absolute path.
     */
    private String absPath;
    
    /**
     * Deep flag.
     */
    private boolean isDeep;
    
    /**
     * UUID array.
     */
    private String[] uuid;
    
    /**
     * Node type name array.
     */
    private String[] nodeTypeName;
    
    /**
     * NoLocal flag.
     */
    private boolean noLocal;
    
    /**
     * Creates a new event listener configuration.
     *
     * @param className  the class name of the event listener implementation.
     * @param parameters configuration parameters.
     */
    public EventListenerConfig(String className, Properties parameters) {
        super(className, parameters);
    }

    /**
     * @return a new event listener instance based on this configuration.
     * @throws ConfigurationException on bean configuration errors.
     */
    public EventListener createEventListener() throws ConfigurationException {
        return (EventListener) newInstance();
    }

    /**
     * Bean getters
     */
    public int getEventTypes() {
        return eventTypes;
    }

    public String getAbsPath() {
        return absPath;
    }

    public boolean isDeep() {
        return isDeep;
    }

    public String[] getUUID() {
        return uuid;
    }

    public String[] getNodeTypeName() {
        return nodeTypeName;
    }

    public boolean isNoLocal() {
        return noLocal;
    }

    /**
     * Bean setters.
     */
    public void setEventTypes(int eventTypes) {
        this.eventTypes = eventTypes;
    }

    public void setAbsPath(String absPath) {
        this.absPath = absPath;
    }

    public void setDeep(boolean isDeep) {
        this.isDeep = isDeep;
    }

    public void setUUID(String[] uuid) {
        this.uuid = uuid;
    }

    public void setNodeTypeName(String[] nodeTypeName) {
        this.nodeTypeName = nodeTypeName;
    }

    public void setNoLocal(boolean noLocal) {
        this.noLocal = noLocal;
    }
}
