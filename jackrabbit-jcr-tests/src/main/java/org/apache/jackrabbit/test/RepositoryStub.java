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

import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.jcr.SimpleCredentials;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Properties;
import java.util.Map;

/**
 * The <code>RepositoryStub</code> is the entry point to the JCR Repository
 * for the TCK Test harness.
 * <p/>
 * Implementors of the JCR specification need to provide an implementation
 * for the abstract methods defined in this class.
 */
public abstract class RepositoryStub {

    public static final String STUB_IMPL_PROPS = "repositoryStubImpl.properties";

    public static final String PROP_PREFIX = "javax.jcr.tck";

    public static final String STUB_IMPL_SYS_PROPS = PROP_PREFIX + ".properties";

    public static final String PROP_STUB_IMPL_CLASS = PROP_PREFIX + ".repository_stub_impl";

    public static final String PROP_SUPERUSER_PWD = "superuser.pwd";

    public static final String PROP_SUPERUSER_NAME = "superuser.name";

    public static final String PROP_READONLY_PWD = "readonly.pwd";

    public static final String PROP_READONLY_NAME = "readonly.name";

    public static final String PROP_READWRITE_PWD = "readwrite.pwd";

    public static final String PROP_READWRITE_NAME = "readwrite.name";

    public static final String PROP_NODETYPE = "nodetype";

    public static final String PROP_NODETYPENOCHILDREN = "nodetypenochildren";

    public static final String PROP_TESTROOT = "testroot";

    public static final String PROP_NODE_NAME1 = "nodename1";

    public static final String PROP_NODE_NAME2 = "nodename2";

    public static final String PROP_NODE_NAME3 = "nodename3";

    public static final String PROP_NODE_NAME4 = "nodename4";

    public static final String PROP_PROP_NAME1 = "propertyname1";

    public static final String PROP_PROP_NAME2 = "propertyname2";

    public static final String PROP_WORKSPACE_NAME = "workspacename";

    public static final String PROP_NAMESPACES = "namespaces";

    protected final Properties environment;

    protected SimpleCredentials superuser;

    protected SimpleCredentials readonly;

    protected SimpleCredentials readwrite;

    /**
     * Implementations of this class must overwrite this constructor.
     *
     * @param env the environment variables. This parameter must not be null.
     */
    protected RepositoryStub(Properties env) {
        if (env == null) {
            throw new IllegalArgumentException("Parameter 'env' must not be null!");
        }
        environment = env;
        superuser = new SimpleCredentials(env.getProperty(PROP_PREFIX + "." + PROP_SUPERUSER_NAME, ""),
                env.getProperty(PROP_PREFIX + "." + PROP_SUPERUSER_PWD, "").toCharArray());
        readonly = new SimpleCredentials(env.getProperty(PROP_PREFIX + "." + PROP_READONLY_NAME, ""),
                env.getProperty(PROP_PREFIX + "." + PROP_READONLY_PWD, "").toCharArray());
        readwrite = new SimpleCredentials(env.getProperty(PROP_PREFIX + "." + PROP_READWRITE_NAME, ""),
                env.getProperty(PROP_PREFIX + "." + PROP_READWRITE_PWD, "").toCharArray());
    }

    /**
     * Creates and/or returns the configured <code>RepositryStub</code>
     * implementation.
     * <p/>
     * The property file is located in the following sequence:
     * <ol>
     * <li>If the system property <code>-Djavax.jcr.tck.properties</code> is
     * set, then the accroding file is used as configuration.</li>
     * <li>If the system property <code>-Djavax.jcr.tck.properties</code> is
     * not set, then the TCK tries to load the file <code>repositoryStubImpl.properties</code>
     * as a resource from the ClassLoader of this <code>RepositryStub</code> class.</li>
     * </ol>
     * The properties are then overlayed with the the key / values from
     * <code>configuration</code> map.
     *
     * @param configuration a <code>Map</code> of additional configuration entries.
     * @return a <code>RepositoryStub</code> implementation.
     * @throws RepositoryStubException
     */
    static synchronized RepositoryStub getInstance(Map configuration)
        throws RepositoryStubException {
        Properties props = null;
        RepositoryStub stub = null;
        String implProp = System.getProperty(STUB_IMPL_SYS_PROPS);
        if (implProp != null) {
            File implPropFile = new File(implProp);
            if (implPropFile.exists()) {
                props = new Properties();
                try {
                    props.load(new FileInputStream(implPropFile));
                } catch (IOException e) {
                    throw new RepositoryStubException("Unable to load config file: "
                            + implProp + " " + e.toString());
                }
            } else {
                throw new RepositoryStubException("File does not exist: " + implProp);
            }
        }

        if (props == null) {
            props = new Properties();
            InputStream is = RepositoryStub.class.getClassLoader().getResourceAsStream(STUB_IMPL_PROPS);
            if (is != null) {
                try {
                    props.load(is);
                } catch (IOException e) {
                    throw new RepositoryStubException("Exception reading "
                            + STUB_IMPL_PROPS + ": " + e.toString());
                }
            }
        }

        // overlay with configuration parameter
        props.putAll(configuration);

        try {
            String className = props.getProperty(PROP_STUB_IMPL_CLASS);
            if (className == null || className.length() == 0) {
                throw new RepositoryStubException("Property " + PROP_STUB_IMPL_CLASS + " not defined!");
            }
            Class stubClass = Class.forName(className);
            Constructor constr = stubClass.getConstructor(new Class[]{Properties.class});
            stub = (RepositoryStub) constr.newInstance(new Object[]{props});
        } catch (ClassCastException e) {
            throw new RepositoryStubException(e.toString());
        } catch (NoSuchMethodException e) {
            throw new RepositoryStubException(e.toString());
        } catch (ClassNotFoundException e) {
            throw new RepositoryStubException(e.toString());
        } catch (InstantiationException e) {
            throw new RepositoryStubException(e.toString());
        } catch (IllegalAccessException e) {
            throw new RepositoryStubException(e.toString());
        } catch (InvocationTargetException e) {
            throw new RepositoryStubException(e.toString());
        }

        return stub;
    }

    /**
     * Returns a reference to the <code>Repository</code> provided by this
     * <code>RepositoryStub</code>.
     *
     * @return
     */
    public abstract Repository getRepository() throws RepositoryStubException;

    /**
     * Returns a <code>Credentials</code> object, that can be used to login
     * to the <code>Repository</code> returned by {@link #getRepository}.
     * <p/>
     * The <code>Credentials</code> returned has 'superuser' rights. That
     * is, the <code>Session</code> object returned by {@link Repository#login(Credentials)}
     * has read write access to the whole Content Repository.
     *
     * @return a <code>Credentials</code> object, that allows to login to the
     *         <code>Repository</code> as 'superuser'.
     */
    public Credentials getSuperuserCredentials() {
        return superuser;
    }

    /**
     * Returns a <code>Credentials</code> object, that can be used to login
     * to the <code>Repository</code> returned by {@link #getRepository}.
     * <p/>
     * The <code>Credentials</code> returned has read/write rights. That
     * is, the <code>Session</code> object returned by {@link Repository#login(Credentials)}
     * has read write access to the <code>Node</code> configured in the
     * JCR TCK Interview.
     * <p/>
     * For details, see: JCR TCK User Guide.
     *
     * @return a <code>Credentials</code> object, that allows to login to the
     *         <code>Repository</code> with read/write right.
     */
    public Credentials getReadWriteCredentials() {
        return readwrite;
    }

    /**
     * Returns a <code>Credentials</code> object, that can be used to login
     * to the <code>Repository</code> returned by {@link #getRepository}.
     * <p/>
     * The <code>Credentials</code> returned must have read-only rights. That
     * is, the <code>Session</code> object returned by {@link Repository#login()}
     * has read-only access to the <code>Node</code> configured in the
     * JCR TCK Interview.
     * <p/>
     * For details, see: JCR TCK User Guide.
     *
     * @return a <code>Credentials</code> object, that allows to login to the
     *         <code>Repository</code> with read-only right.
     */
    public Credentials getReadOnlyCredentials() {
        return readonly;
    }

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
}
