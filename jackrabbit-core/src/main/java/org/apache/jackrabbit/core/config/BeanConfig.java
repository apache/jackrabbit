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

import org.apache.commons.collections.BeanMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Properties;

/**
 * Bean configuration class. BeanConfig instances contain the class name
 * and property information required to instantiate a class that conforms
 * with the JavaBean conventions.
 */
public class BeanConfig {

    private static Logger log = LoggerFactory.getLogger(BeanConfig.class);

    /** The default class loader used by all instances of this class */
    private static ClassLoader defaultClassLoader =
        BeanConfig.class.getClassLoader();

    /**
     * The current class loader used by this instance to create instances of
     * configured classes.
     */
    private ClassLoader classLoader = getDefaultClassLoader();

    /**
     * The class name of the configured bean.
     */
    private final String className;

    /**
     * The initial properties of the configured bean.
     */
    private final Properties properties;

    /**
     * Flag to validate the configured bean property names against
     * the configured bean class. By default this is <code>true</code>
     * to prevent incorrect property names. However, in some cases this
     * validation should not be performed as client classes may access
     * the configuration parameters directly through the
     * {@link #getParameters()} method.
     *
     * @see <a href="https://issues.apache.org/jira/browse/JCR-1920">JCR-1920</a>
     */
    private boolean validate = true;

    /**
     * Creates a bean configuration. Note that a copy of the given
     * bean properties is stored as a part of the created configuration
     * object. Thus the caller is free to modify the given properties
     * once the configuration object has been created.
     *
     * @param className class name of the bean
     * @param properties initial properties of the bean
     */
    public BeanConfig(String className, Properties properties) {
        this.className = className;
        this.properties = (Properties) properties.clone();
    }

    /**
     * Copies a bean configuration.
     *
     * @param config the configuration to be copied
     */
    public BeanConfig(BeanConfig config) {
        this(config.getClassName(), config.getParameters());
    }

    /**
     * Allows subclasses to control whether the configured bean property
     * names should be validated.
     *
     * @param validate flag to validate the configured property names
     */
    protected void setValidate(boolean validate) {
        this.validate = validate;
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
     * @throws ConfigurationException on bean configuration errors
     */
    public Object newInstance() throws ConfigurationException {
        try {
            // Instantiate the object using the default constructor
            Class objectClass =
                Class.forName(getClassName(), true, getClassLoader());
            Object object = objectClass.newInstance();

            // Set all configured bean properties
            BeanMap map = new BeanMap(object);
            Iterator iterator = map.keyIterator();
            while (iterator.hasNext()) {
                String name = (String) iterator.next();
                String value = properties.getProperty(name);
                if (value != null) {
                    map.put(name, properties.getProperty(name));
                }
            }

            if (validate) {
                // Check that no invalid property names were configured
                Iterator it = properties.keySet().iterator();
                while (it.hasNext()) {
                    String key = (String) it.next();
                    if (!map.containsKey(key) && properties.getProperty(key) != null) {
                        String msg =
                            "Configured class " + object.getClass().getName()
                            + " does not contain the property " + key
                            + ". Please fix the repository configuration.";
                        log.error(msg);
                        throw new ConfigurationException(msg);
                    }
                }
            }

            return object;
        } catch (ClassNotFoundException e) {
            throw new ConfigurationException(
                    "Configured bean implementation class " + getClassName()
                    + " was not found.", e);
        } catch (InstantiationException e) {
            throw new ConfigurationException(
                    "Configured bean implementation class " + getClassName()
                    + " can not be instantiated.", e);
        } catch (IllegalAccessException e) {
            throw new ConfigurationException(
                    "Configured bean implementation class " + getClassName()
                    + " is protected.", e);
        }
    }

    //---------- Configurable class loader support ----------------------------

    /**
     * Returns the current <code>ClassLoader</code> used to instantiate objects
     * in the {@link #newInstance()} method.
     *
     * @see #newInstance()
     * @see #setClassLoader(ClassLoader)
     * @see #getDefaultClassLoader()
     * @see #setDefaultClassLoader(ClassLoader)
     */
    public ClassLoader getClassLoader() {
        return classLoader;
    }

    /**
     * Sets the <code>ClassLoader</code> used to instantiate objects in the
     * {@link #newInstance()} method.
     *
     * @param classLoader The class loader to set on this instance. If this is
     *      <code>null</code> the system class loader will be used, which may
     *      lead to unexpected class loading failures.
     *
     * @see #newInstance()
     * @see #getClassLoader()
     * @see #getDefaultClassLoader()
     * @see #setDefaultClassLoader(ClassLoader)
     */
    public void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    /**
     * Returns the current <code>ClassLoader</code> used for new instances of
     * this class as the loader used to instantiate objects in the
     * {@link #newInstance()} method.
     *
     * @see #newInstance()
     * @see #getClassLoader()
     * @see #setClassLoader(ClassLoader)
     * @see #setDefaultClassLoader(ClassLoader)
     */
    public static ClassLoader getDefaultClassLoader() {
        return defaultClassLoader;
    }

    /**
     * Sets the <code>ClassLoader</code> used for new instances of this class as
     * the loader to instantiate objects in the {@link #newInstance()} method.
     *
     * @param classLoader The class loader to set as the default class loader.
     *      If this is <code>null</code> the system class loader will be used,
     *      which may lead to unexpected class loading failures.
     *
     * @see #newInstance()
     * @see #getClassLoader()
     * @see #setClassLoader(ClassLoader)
     * @see #getDefaultClassLoader()
     */
    public static void setDefaultClassLoader(ClassLoader classLoader) {
        defaultClassLoader = classLoader;
    }
}
