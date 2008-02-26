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

import junit.framework.TestCase;

import org.apache.jackrabbit.core.TransientRepository;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.apache.jackrabbit.test.config.util.PrettyPrinter;
import org.apache.jackrabbit.test.config.xml.ConfException;
import org.apache.jackrabbit.test.config.xml.RepositoryConfParser;
import org.apache.jackrabbit.test.config.xml.RepositoryConfWriter;

/**
 * <code>DynamicConfigTest</code> ...
 *
 */
public class DynamicConfigTest extends TestCase {

	private RepositoryConf conf;

	protected RepositoryConf getRepositoryConf() throws ConfException {
		if (conf == null) {
			conf = RepositoryConfParser.read("applications/test/repository.xml");
		}
		return conf;
	}
    
	public void testReadDynamicConfigFromXml() throws Exception {
		RepositoryConf conf = getRepositoryConf();
		System.out.println(">>>>>>>>>> Config read from 'applications/test/repository.xml'");
		conf.print(new PrettyPrinter(System.out));
		System.out.println();
	}

	public void testWriteDynamicConfigBackToXml() throws Exception {
		RepositoryConf conf = getRepositoryConf();
		RepositoryConfWriter.write(conf, "target/written-repository.xml");
	}

	public void testStartRepoWithDynamicConfigFromXml() throws Exception {
		RepositoryConf conf = getRepositoryConf();
		
		RepositoryConfig config = conf.createConfig("applications/test");
		config.init();
		TransientRepository repo = new TransientRepository(config);
		repo.login();
		repo.shutdown();
	}
	
	public void testStartRepoWithDefaultConfig() throws Exception {
		RepositoryConf conf = new RepositoryConf();
		System.out.println(">>>>>>>>>> Default config:");
		conf.print(new PrettyPrinter(System.out));
		System.out.println();
		
		RepositoryConfig config = conf.createConfig("applications/test2");
		config.init();
		TransientRepository repo = new TransientRepository(config);
		repo.login();
		repo.shutdown();
	}
}
