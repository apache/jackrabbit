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

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import org.apache.jackrabbit.core.util.db.ConnectionFactory;
import org.apache.jackrabbit.core.util.db.DatabaseAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bean configuration class. BeanConfig instances contain the class name
 * and property information required to instantiate a class that conforms
 * with the JavaBean conventions.
 */
public class BeanConfig {

    private static Logger log = LoggerFactory.getLogger(BeanConfig.class);

    private static final Map<String, String> DEPRECATIONS;

    static {
        try {
            Map<String, String> temp = new HashMap<String, String>();
            Properties props = new Properties();
            InputStream in = BeanConfig.class.getResourceAsStream("deprecated-classes.properties");
            try {
                props.load(in);
            } finally {
                in.close();
            }
            for (Map.Entry<Object, Object> entry : props.entrySet()) {
                temp.put(entry.getKey().toString(), entry.getValue().toString());
            }
            DEPRECATIONS = Collections.unmodifiableMap(temp);
        } catch (IOException e) {
            throw new InternalError("failed to read deprecated classes");
        }
    }

    /** The default class loader used by all instances of this class */
    private static ClassLoader defaultClassLoader =
        BeanConfig.class.getClassLoader();

    /**
     * Factory to create instance from Bean className
     */
    private BeanFactory instanceFactory = new SimpleBeanFactory();

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
     * The repositories {@link ConnectionFactory}.
     */
    private ConnectionFactory connectionFactory = null;

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
        if (DEPRECATIONS.containsKey(className)) {
            String replacement = DEPRECATIONS.get(className);
            log.info("{} is deprecated. Please use {} instead", className, replacement);
            className = replacement;
        }
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
        setConnectionFactory(config.connectionFactory);
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
     * @param connectionFactory the {@link ConnectionFactory} to inject (if possible) in the
     *            {@link #newInstance(Class)} method
     */
    public void setConnectionFactory(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    /**
     *
     * @param instanceFactory the {@link BeanFactory} to use to create bean instance
     */
    public void setInstanceFactory(BeanFactory instanceFactory) {
        this.instanceFactory = instanceFactory;
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
    @SuppressWarnings("unchecked")
    public <T> T newInstance(Class<T> klass) throws ConfigurationException {
        String cname = getClassName();
        // Instantiate the object using the default constructor
        Object instance = instanceFactory.newInstance(klass,this);
        Class<?> objectClass = instance.getClass();

        // Set all configured bean properties
        Map<String, Method> setters = getSetters(objectClass);
        Enumeration<?> enumeration = properties.propertyNames();
        while (enumeration.hasMoreElements()) {
            String name = enumeration.nextElement().toString();
            Method setter = setters.get(name);
            if (setter != null) {
                if (setter.getAnnotation(Deprecated.class) != null) {
                    log.warn("Parameter {} of {} has been deprecated",
                            name, cname);
                }
                String value = properties.getProperty(name);
                setProperty(instance, name, setter, value);
            } else if (validate) {
                throw new ConfigurationException(
                        "Configured class " + cname
                        + " does not contain a property named " + name);
            }
        }

        if (instance instanceof DatabaseAware) {
            ((DatabaseAware) instance).setConnectionFactory(connectionFactory);
        }

        return (T) instance;
    }

    private Map<String, Method> getSetters(Class<?> klass) {
        Map<String, Method> methods = new HashMap<String, Method>();
        for (Method method : klass.getMethods()) {
            String name = method.getName();
            if (name.startsWith("set") && name.length() > 3
                    && Modifier.isPublic(method.getModifiers())
                    && !Modifier.isStatic(method.getModifiers())
                    && Void.TYPE.equals(method.getReturnType())
                    && method.getParameterTypes().length == 1) {
                methods.put(
                        name.substring(3, 4).toLowerCase(Locale.ENGLISH) + name.substring(4),
                        method);
            }
        }
        return methods;
    }

    private void setProperty(
            Object instance, String name, Method setter, String value)
            throws ConfigurationException {
        Class<?> type = setter.getParameterTypes()[0];
        try {
            if (type.isAssignableFrom(String.class)
                || type.isAssignableFrom(Object.class)) {
                setter.invoke(instance, value);
            } else if (type.isAssignableFrom(Boolean.TYPE)
                    || type.isAssignableFrom(Boolean.class)) {
                setter.invoke(instance, Boolean.valueOf(value));
            } else if (type.isAssignableFrom(Integer.TYPE)
                    || type.isAssignableFrom(Integer.class)) {
                setter.invoke(instance, Integer.valueOf(value));
            } else if (type.isAssignableFrom(Long.TYPE)
                    || type.isAssignableFrom(Long.class)) {
                setter.invoke(instance, Long.valueOf(value));
            } else if (type.isAssignableFrom(Double.TYPE)
                    || type.isAssignableFrom(Double.class)) {
                setter.invoke(instance, Double.valueOf(value));
            } else {
                throw new ConfigurationException(
                        "The type (" + type.getName()
                        + ") of property " + name + " of class "
                        + getClassName() + " is not supported");
            }
        } catch (NumberFormatException e) {
            throw new ConfigurationException(
                    "Invalid number format (" + value + ") for property "
                    + name + " of class " + getClassName(), e);
        } catch (InvocationTargetException e) {
            throw new ConfigurationException(
                    "Property " + name + " of class "
                    + getClassName() + " can not be set to \"" + value + "\"",
                    e);
        } catch (IllegalAccessException e) {
            throw new ConfigurationException(
                    "The setter of property " + name
                    + " of class " + getClassName() + " can not be accessed",
                    e);
        } catch (IllegalArgumentException e) {
            throw new ConfigurationException(
                    "Unable to call the setter of property "
                    + name + " of class " + getClassName(), e);
        }
    }

    //---------- Configurable class loader support ----------------------------

    /**
     * Returns the current <code>ClassLoader</code> used to instantiate objects
     * in the {@link #newInstance(Class)} method.
     *
     * @see #setClassLoader(ClassLoader)
     * @see #getDefaultClassLoader()
     * @see #setDefaultClassLoader(ClassLoader)
     */
    public ClassLoader getClassLoader() {
        return classLoader;
    }

    /**
     * Sets the <code>ClassLoader</code> used to instantiate objects in the
     * {@link #newInstance(Class)} method.
     *
     * @param classLoader The class loader to set on this instance. If this is
     *      <code>null</code> the system class loader will be used, which may
     *      lead to unexpected class loading failures.
     *
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
     * {@link #newInstance(Class)} method.
     *
     * @see #getClassLoader()
     * @see #setClassLoader(ClassLoader)
     * @see #setDefaultClassLoader(ClassLoader)
     */
    public static ClassLoader getDefaultClassLoader() {
        return defaultClassLoader;
    }

    /**
     * Sets the <code>ClassLoader</code> used for new instances of this class as
     * the loader to instantiate objects in the {@link #newInstance(Class)} method.
     *
     * @param classLoader The class loader to set as the default class loader.
     *      If this is <code>null</code> the system class loader will be used,
     *      which may lead to unexpected class loading failures.
     *
     * @see #getClassLoader()
     * @see #setClassLoader(ClassLoader)
     * @see #getDefaultClassLoader()
     */
    public static void setDefaultClassLoader(ClassLoader classLoader) {
        defaultClassLoader = classLoader;
    }
}
