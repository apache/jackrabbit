/*
 * Copyright 2004 The Apache Software Foundation.
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
package org.apache.jackrabbit.test;

import javax.jcr.Repository;
import javax.jcr.Credentials;
import javax.jcr.SimpleCredentials;
import java.util.Properties;
import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * The <code>RepositoryStub</code> is the entry point to the JCR Repository
 * for the TCK Test harness.
 * <p>
 * Implementors of the JCR specification need to provide an implementation
 * for the abstract methods defined in this class.
 *
 * @version $Revision: 1.3 $, $Date: 2004/05/04 12:06:31 $
 * @author Marcel Reutegger
 */
public abstract class RepositoryStub {

    public static final String STUB_IMPL_PROPS = "repositoryStubImpl.properties";

    public static final String STUB_IMPL_SYS_PROPS = "javax.jcr.tck.properties";

    public static final String PROP_STUB_IMPL_CLASS = "javax.jcr.tck.repository_stub_impl";

    public static final String PROP_SUPERUSER_PWD = "javax.jcr.tck.superuser.pwd";

    public static final String PROP_SUPERUSER_NAME = "javax.jcr.tck.superuser.name";

    public static final String PROP_READONLY_PWD = "javax.jcr.tck.readonly.pwd";

    public static final String PROP_READONLY_NAME = "javax.jcr.tck.readonly.name";

    public static final String PROP_READWRITE_PWD = "javax.jcr.tck.readwrite.pwd";

    public static final String PROP_READWRITE_NAME = "javax.jcr.tck.readwrite.name";

    protected static RepositoryStub instance;

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
        if (env == null) throw new IllegalArgumentException("Parameter 'env' must not be null!");
        environment = env;
	superuser = new SimpleCredentials(env.getProperty(PROP_SUPERUSER_NAME, ""),
		env.getProperty(PROP_SUPERUSER_PWD, "").toCharArray());
	readonly = new SimpleCredentials(env.getProperty(PROP_READONLY_NAME, ""),
		env.getProperty(PROP_READONLY_PWD, "").toCharArray());
	readwrite = new SimpleCredentials(env.getProperty(PROP_READWRITE_NAME, ""),
		env.getProperty(PROP_READWRITE_PWD, "").toCharArray());
    }

    /**
     * Creates and/or returns the configured <code>RepositryStub</code>
     * implementation.
     * <p>
     * The property file is located in the following sequence:
     * <ol>
     * <li>If the system property <code>-Djavax.jcr.tck.properties</code> is
     * set, then the accroding file is used as configuration.</li>
     * <li>If the system property <code>-Djavax.jcr.tck.properties</code> is
     * not set, then the TCK tries to load the file <code>repositoryStubImpl.properties</code>
     * as a resource from the ClassLoader of this <code>RepositryStub</code> class.</li>
     * <li>If none of the above is found, a {@link RepositoryStubException} is thrown.
     * </ol>
     * @return a <code>RepositoryStub</code> implementation.
     * @throws RepositoryStubException
     */
    public static synchronized RepositoryStub getInstance() throws RepositoryStubException {
        if (instance == null) {
            Properties props = null;
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
		InputStream is = RepositoryStub.class.getClassLoader().getResourceAsStream(STUB_IMPL_PROPS);
		if (is == null) {
		    throw new RepositoryStubException(STUB_IMPL_PROPS + " not found in classpath!");
		}
		try {
		    props = new Properties();
		    props.load(is);
		} catch (IOException e) {
		    throw new RepositoryStubException("Exception reading "
			    + STUB_IMPL_PROPS + ": " + e.toString());
		}
	    }

            try {
                String className = props.getProperty(PROP_STUB_IMPL_CLASS);
                if (className == null || className.length() == 0) {
                    throw new RepositoryStubException("Property " + PROP_STUB_IMPL_CLASS + " not defined!");
                }
                Class stubClass = Class.forName(className);
                Constructor constr = stubClass.getConstructor(new Class[] {Properties.class});
                instance = (RepositoryStub)constr.newInstance(new Object[] {props});
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
        }
        return instance;
    }

    /**
     * Returns a reference to the <code>Repository</code> provided by this
     * <code>RepositoryStub</code>.
     * @return
     */
    public abstract Repository getRepository() throws RepositoryStubException;

    /**
     * Returns a <code>Credentials</code> object, that can be used to login
     * to the <code>Repository</code> returned by {@link #getRepository}.
     * <p>
     * The <code>Credentials</code> returned has 'superuser' rights. That
     * is, the <code>Ticket</code> object returned by {@link Repository#login}
     * has read write access to the whole Content Repository.
     *
     * @return a <code>Credentials</code> object, that allows to login to the
     *      <code>Repository</code> as 'superuser'.
     */
    public Credentials getSuperuserCredentials() {
	return superuser;
    }

    /**
     * Returns a <code>Credentials</code> object, that can be used to login
     * to the <code>Repository</code> returned by {@link #getRepository}.
     * <p>
     * The <code>Credentials</code> returned has read/write rights. That
     * is, the <code>Ticket</code> object returned by {@link Repository#login}
     * has read write access to the <code>Node</code> configured in the
     * JCR TCK Interview.
     * <p>
     * For details, see: JCR TCK User Guide.
     *
     * @return a <code>Credentials</code> object, that allows to login to the
     *      <code>Repository</code> with read/write right.
     */
    public Credentials getReadWriteCredentials() {
	return readwrite;
    }

    /**
     * Returns a <code>Credentials</code> object, that can be used to login
     * to the <code>Repository</code> returned by {@link #getRepository}.
     * <p>
     * The <code>Credentials</code> returned must have read-only rights. That
     * is, the <code>Ticket</code> object returned by {@link Repository#login}
     * has read-only access to the <code>Node</code> configured in the
     * JCR TCK Interview.
     * <p>
     * For details, see: JCR TCK User Guide.
     *
     * @return a <code>Credentials</code> object, that allows to login to the
     *      <code>Repository</code> with read-only right.
     */
    public Credentials getReadOnlyCredentials() {
	return readonly;
    }
}
