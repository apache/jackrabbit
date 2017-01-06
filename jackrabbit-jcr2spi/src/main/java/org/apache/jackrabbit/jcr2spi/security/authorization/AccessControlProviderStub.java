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
package org.apache.jackrabbit.jcr2spi.security.authorization;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;

import org.apache.jackrabbit.jcr2spi.config.RepositoryConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stub class that provide clients with access to a concrete
 * AccessControlProvider implementation. TODO: Explain the way the concrete
 * provider is located, loaded and instantiated.
 * 
 */
public class AccessControlProviderStub {

	private static Logger log = LoggerFactory.getLogger(AccessControlProviderStub.class);

	/**
	 * The class property parameter determines the {@link AccessControlProvider}
	 * to load and instantiate. This is a fall-back parameter if the
	 * SYS_PROP_AC_PROVIDER_IMPL is not set.
	 */
	private static final String ACCESS_CONTROL_PROVIDER_PROPERTIES = "accessControlProvider.properties";

	/**
	 * Key look-up.
	 */
	private static final String PROPERTY_ACCESSCONTROL_PROVIDER_CLASS = "org.apache.jackrabbit.jcr2spi.AccessControlProvider.class";

	/**
	 * Avoid instantiation.
	 */
	private AccessControlProviderStub() {
	}

	/**
	 * Instantiates and returns a concrete AccessControlProvider implementation.
	 * 
	 * @param config
	 *            The RepositoryConfig to read configuration parameters.
	 * @return
	 * @throws RepositoryException
	 */
	public static AccessControlProvider newInstance(RepositoryConfig config) throws RepositoryException {

		String className = getProviderClass(config);
		if (className != null) {
			try {
				Class<?> acProviderClass = Class.forName(className);
				if (AccessControlProvider.class.isAssignableFrom(acProviderClass)) {
					AccessControlProvider acProvider = (AccessControlProvider) acProviderClass.newInstance();
					acProvider.init(config);
					return acProvider;
				} else {
					throw new RepositoryException("Fail to create AccessControlProvider from configuration.");
				}
			} catch (Exception e) {
				throw new RepositoryException("Fail to create AccessControlProvider from configuration.");
			}
		}

		// ac not supported in this setup.
		throw new UnsupportedRepositoryOperationException("Access control is not supported");
	}

	private static String getProviderClass(RepositoryConfig config) throws RepositoryException {

		String implClass = config.getConfiguration(PROPERTY_ACCESSCONTROL_PROVIDER_CLASS, null);

		if (implClass != null) {
			return implClass;
		} else {
			try {
				// not configured try to load as resource
				Properties prop = new Properties();
				InputStream is = AccessControlProviderStub.class.getClassLoader().getResourceAsStream(ACCESS_CONTROL_PROVIDER_PROPERTIES);
				if (is != null) {
					prop.load(is);
					// loads the concrete class to instantiate.
					if (prop.containsKey(PROPERTY_ACCESSCONTROL_PROVIDER_CLASS)) {
						return prop.getProperty(PROPERTY_ACCESSCONTROL_PROVIDER_CLASS);
					} else {
						log.debug("Missing AccessControlProvider configuration.");
					}
				} else {
					log.debug("Fail to locate the access control provider properties file.");
				}
			} catch (IOException e) {
				throw new RepositoryException("Fail to load AccessControlProvider configuration.");
			}
		}
		return null;
	}
}
