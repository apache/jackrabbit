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
package org.apache.jackrabbit.spi;

import javax.jcr.RepositoryException;
import javax.jcr.Credentials;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Properties;

/**
 * The <code>RepositoryServiceStub</code> is the entry point to the SPI
 * RepositoryService.
 */
public abstract class RepositoryServiceStub {

    public static final String STUB_IMPL_PROPS = "repositoryServiceStubImpl.properties";

    public static final String PROP_PREFIX = "org.apache.jackrabbit.spi";

    public static final String STUB_IMPL_SYS_PROPS = PROP_PREFIX + ".properties";

    public static final String PROP_STUB_IMPL_CLASS = PROP_PREFIX + ".repository_service_stub_impl";

    public static final String PROP_ADMIN_PWD = "admin.pwd";

    public static final String PROP_ADMIN_NAME = "admin.name";

    public static final String PROP_READONLY_PWD = "readonly.pwd";

    public static final String PROP_READONLY_NAME = "readonly.name";

    public static final String PROP_WORKSPACE = "workspacename";

    protected final Properties environment;

    /**
     * Implementations of this class must overwrite this constructor.
     *
     * @param env the environment variables. This parameter must not be null.
     */
    protected RepositoryServiceStub(Properties env) {
        if (env == null) {
            throw new IllegalArgumentException("Parameter 'env' must not be null!");
        }
        environment = env;
    }

    public static RepositoryServiceStub getInstance(Map configuration) throws RepositoryException {
        Properties props = null;
        RepositoryServiceStub stub = null;
        String implProp = System.getProperty(RepositoryServiceStub.STUB_IMPL_SYS_PROPS);
        if (implProp != null) {
            File implPropFile = new File(implProp);
            if (implPropFile.exists()) {
                props = new Properties();
                try {
                    props.load(new FileInputStream(implPropFile));
                } catch (IOException e) {
                    throw new RepositoryException("Unable to load config file: "
                            + implProp + " " + e.toString());
                }
            } else {
                throw new RepositoryException("File does not exist: " + implProp);
            }
        }

        if (props == null) {
            props = new Properties();
            InputStream is = RepositoryServiceStub.class.getClassLoader().getResourceAsStream(RepositoryServiceStub.STUB_IMPL_PROPS);
            if (is != null) {
                try {
                    props.load(is);
                } catch (IOException e) {
                    throw new RepositoryException("Exception reading "
                            + RepositoryServiceStub.STUB_IMPL_PROPS + ": " + e.toString());
                }
            }
        }

        // overlay with configuration parameter
        props.putAll(configuration);

        try {
            String className = props.getProperty(RepositoryServiceStub.PROP_STUB_IMPL_CLASS);
            if (className == null || className.length() == 0) {
                throw new RepositoryException("Property " + RepositoryServiceStub.PROP_STUB_IMPL_CLASS + " not defined!");
            }
            Class stubClass = Class.forName(className);
            Constructor constr = stubClass.getConstructor(new Class[]{Properties.class});
            stub = (RepositoryServiceStub) constr.newInstance(new Object[]{props});
        } catch (ClassCastException e) {
            throw new RepositoryException(e.toString());
        } catch (NoSuchMethodException e) {
            throw new RepositoryException(e.toString());
        } catch (ClassNotFoundException e) {
            throw new RepositoryException(e.toString());
        } catch (InstantiationException e) {
            throw new RepositoryException(e.toString());
        } catch (IllegalAccessException e) {
            throw new RepositoryException(e.toString());
        } catch (InvocationTargetException e) {
            throw new RepositoryException(e.toString());
        }

        return stub;
    }

    /**
     * Returns a reference to the <code>RepositoryService</code> provided by this
     * <code>RepositoryServiceStub</code>.
     *
     * @return
     */
    public abstract RepositoryService getRepositoryService() throws RepositoryException;

    /**
     * Returns the property with the specified <code>name</code>. If a
     * property with the given name does not exist, <code>null</code> is
     * returned.
     * @param name the name of the property.
     * @return the property, or <code>null</code> if the property does not
     * exist.
     */
    public String getProperty(String name) {
        return environment.getProperty(name);
    }

    public abstract Credentials getAdminCredentials() throws RepositoryException;

    public abstract Credentials getReadOnlyCredentials() throws RepositoryException;
}