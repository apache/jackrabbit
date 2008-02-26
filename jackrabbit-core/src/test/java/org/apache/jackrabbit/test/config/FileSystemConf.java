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

import org.apache.jackrabbit.core.config.FileSystemConfig;
import org.apache.jackrabbit.core.fs.local.LocalFileSystem;
import org.apache.jackrabbit.test.config.util.PrettyPrinter;
import org.apache.jackrabbit.test.config.util.Variables;
import org.apache.jackrabbit.test.config.xml.ConfException;

public class FileSystemConf extends BeanConf {

	public FileSystemConf(BeanConf config) {
		super(config);
	}

	/**
	 * Uses {@link LocalFileSystem} by default.
	 */
	public FileSystemConf() {
		this.className = LocalFileSystem.class.getName();
	}

	public FileSystemConfig createFileSystemConfig(Variables variables) throws ConfException {
		return new FileSystemConfig(super.createBeanConfig(variables));
	}

	public void print(PrettyPrinter pp) {
		pp.printlnIndent("[FileSystem");
		
		printBeanConf(pp);
		
		pp.printlnIndent("]");
	}
}
