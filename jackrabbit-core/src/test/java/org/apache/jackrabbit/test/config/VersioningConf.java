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
import org.apache.jackrabbit.core.config.PersistenceManagerConfig;
import org.apache.jackrabbit.core.config.VersioningConfig;
import org.apache.jackrabbit.test.config.util.PrettyPrinter;
import org.apache.jackrabbit.test.config.util.Variables;
import org.apache.jackrabbit.test.config.xml.ConfException;
import org.apache.jackrabbit.test.config.xml.Xml;

public class VersioningConf {

    private String homeDir;

    private FileSystemConf fsc;

    private PersistenceManagerConf pmc;

	public VersioningConf(String homeDir, FileSystemConf fsc,
			PersistenceManagerConf pmc) {
		this.homeDir = homeDir;
		this.fsc = fsc;
		this.pmc = pmc;
	}

	public VersioningConf() {
		this.homeDir = "${" + Xml.REPOSITORY_HOME_VARIABLE + "}/version";
		
		this.fsc = new FileSystemConf();
		this.fsc.setParameter("path", "${" + Xml.REPOSITORY_HOME_VARIABLE + "}/version");
		
		this.pmc = new PersistenceManagerConf();
		this.pmc.setParameter("url", "jdbc:derby:${" + Xml.REPOSITORY_HOME_VARIABLE + "}/version/db/itemState;create=true");
		this.pmc.setParameter("schemaObjectPrefix", "version_");
	}

	public VersioningConfig createVersioningConfig(Variables variables) throws ConfException {
		FileSystemConfig fsc = null;
		if (getFileSystemConf() != null) {
			fsc = getFileSystemConf().createFileSystemConfig(variables);
		}
		PersistenceManagerConfig pmc = null;
		if (getPersistenceManagerConf() != null) {
			pmc = getPersistenceManagerConf().createPersistenceManagerConfig(variables);
		}
		
		return new VersioningConfig(variables.replaceVariables(getHomeDir()), fsc, pmc);
	}
	
    public String getHomeDir() {
        return homeDir;
    }

    public FileSystemConf getFileSystemConf() {
        return fsc;
    }

    public PersistenceManagerConf getPersistenceManagerConf() {
        return pmc;
    }

	public void setHomeDir(String home) {
		this.homeDir = home;
	}

	public void setFileSystemConf(FileSystemConf fsc) {
		this.fsc = fsc;
	}

	public void setPersistenceManagerConf(PersistenceManagerConf pmc) {
		this.pmc = pmc;
	}

	public void print(PrettyPrinter pp) {
		pp.printlnIndent("[Versioning");
		
		pp.increaseIndent();
		
		pp.printlnIndent("homeDir=" + homeDir);
		
		if (fsc != null) fsc.print(pp);
		if (pmc != null) pmc.print(pp);
		
		pp.decreaseIndent();
		
		pp.printlnIndent("]");
	}

}
