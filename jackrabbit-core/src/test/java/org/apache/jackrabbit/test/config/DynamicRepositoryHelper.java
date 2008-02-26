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

package org.apache.jackrabbit.test.config;

import java.io.IOException;

import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.jackrabbit.core.TransientRepository;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.apache.jackrabbit.test.RepositoryHelper;

/**
 * <code>DynamicRepositoryHelper</code> ...
 *
 */
public class DynamicRepositoryHelper extends RepositoryHelper {
	
	private TransientRepository repo;
	
	private RepositoryConf conf;

	private String home;
	
	private SimpleCredentials superuser;
	
	private SimpleCredentials readwrite;
	
	private SimpleCredentials readonly;

	public static RepositoryConf getSimpleRepositoryConf() {
		RepositoryConf conf = new RepositoryConf();
		return conf;
	}
	
	public DynamicRepositoryHelper(RepositoryConf conf, String home) {
		this.conf = conf;
		this.home = home;
		
		superuser = new SimpleCredentials("superuser", "".toCharArray());
		readwrite = new SimpleCredentials("user", "".toCharArray());
		readonly = new SimpleCredentials("anonymous", "".toCharArray());
	}

	public Repository getRepository() throws RepositoryException {
		if (repo == null) {
			RepositoryConfig config = conf.createConfig(home);
			config.init();
			try {
				repo = new TransientRepository(config);
			} catch (IOException e) {
				throw new RepositoryException("Cannot instantiate " +
						"TransientRepository at " + home, e);
			}
		}
		return repo;
	}

	public Session getSuperuserSession(String workspaceName)
			throws RepositoryException {
		return getRepository().login(getSuperuserCredentials(), workspaceName);
	}

	public Session getReadWriteSession(String workspaceName)
			throws RepositoryException {
		return getRepository().login(getReadWriteCredentials(), workspaceName);
	}
	
	public Session getReadOnlySession(String workspaceName)
			throws RepositoryException {
		return getRepository().login(getReadOnlyCredentials(), workspaceName);
	}
	
	/**
	 * Not implemented, always returns null.
	 */
	public String getProperty(String name) throws RepositoryException {
		return null;
	}

	public Credentials getReadOnlyCredentials() {
		return readonly;
	}

	public Credentials getReadWriteCredentials() {
		return readwrite;
	}

	public Credentials getSuperuserCredentials() {
		return superuser;
	}

	public void shutdown() {
		if (repo != null) {
			repo.shutdown();
		}
	}

}
