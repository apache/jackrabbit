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
package org.apache.jackrabbit.core.config;

import java.util.Iterator;
import java.util.Properties;

import org.apache.commons.collections.BeanMap;

/**
 * Bean configuration class. BeanConfig instances contain the class name
 * and property information required to instantiate a class that conforms
 * with the JavaBean conventions.
 */
public class BeanConfig {

    /**
     * The class name of the configured bean.
     */
    private final String className;

    /**
     * The initial properties of the configured bean.
     */
    private final Properties properties;

    /**
     * Creates a bean configuration. Note that a copy of the given
     * bean properties is stored as a part of the created configuration
     * object. Thus the caller is free to modify the given properties
     * once the configuration object has been created.
     *
     * @param className class name of the bean
     * @param properties initial properties of the bean
     */
    protected BeanConfig(String className, Properties properties) {
        this.className = className;
        this.properties = new Properties(properties);
    }

    /**
     * Copies a bean configuration.
     *
     * @param config the configuration to be copied
     */
    protected BeanConfig(BeanConfig config) {
        this(config.getClassName(), config.getParameters());
    }

    /**
     * Returns the class name of the configured bean.
     *
     * @return class name of the bean
     */
    public String getClassName() {
        return className;
    }

    /**
     * Returns the initial properties of the configured bean.
     *
     * @return initial properties of the bean
     */
    public Properties getParameters() {
        return properties;
    }

    /**
     * Creates a new instance of the configured bean class.
     *
     * @return new bean instance
     * @throws ClassNotFoundException if the bean class is not found
     * @throws InstantiationException if the bean could not be instantiated
     * @throws IllegalAccessException if the bean methods are protected
     */
    public Object newInstance()
            throws ClassNotFoundException, InstantiationException,
            IllegalAccessException {
        Object object = Class.forName(className).newInstance();
        BeanMap map = new BeanMap(object);
        Iterator iterator = map.keyIterator();
        while (iterator.hasNext()) {
            String name = (String) iterator.next();
            String value = properties.getProperty(name);
            if (value != null) {
                map.put(name, properties.getProperty(name));
            }
        }
        return object;
    }

}
