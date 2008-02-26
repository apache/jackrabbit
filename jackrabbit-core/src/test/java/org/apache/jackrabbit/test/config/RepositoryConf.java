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

import org.apache.jackrabbit.core.config.ClusterConfig;
import org.apache.jackrabbit.core.config.FileSystemConfig;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.apache.jackrabbit.core.config.RepositoryConfigurationParser;
import org.apache.jackrabbit.core.config.SearchConfig;
import org.apache.jackrabbit.core.config.SecurityConfig;
import org.apache.jackrabbit.core.config.VersioningConfig;
import org.apache.jackrabbit.core.fs.local.LocalFileSystem;
import org.apache.jackrabbit.core.persistence.bundle.DerbyPersistenceManager;
import org.apache.jackrabbit.core.query.lucene.SearchIndex;
import org.apache.jackrabbit.core.security.SimpleAccessManager;
import org.apache.jackrabbit.core.security.SimpleLoginModule;
import org.apache.jackrabbit.test.config.util.PrettyPrinter;
import org.apache.jackrabbit.test.config.util.Variables;
import org.apache.jackrabbit.test.config.xml.ConfException;
import org.apache.jackrabbit.test.config.xml.RepositoryConfWriter;
import org.apache.jackrabbit.test.config.xml.Xml;

public class RepositoryConf {

    private String home;

    private String defaultWorkspace;

    private String workspaceDirectory;

    private String workspaceConfigDirectory;

    private int workspaceMaxIdleTime;

    private SecurityConf sec;

    private FileSystemConf fsc;

    private VersioningConf vc;

    private SearchConf sc;

    private ClusterConf cc;
    
    private WorkspaceConf workspaceConfTemplate;
    
	public RepositoryConf(String home, SecurityConf securityConf,
			FileSystemConf fsc, String workspaceDirectory,
			String workspaceConfigDirectory, String defaultWorkspace,
			int workspaceMaxIdleTime, WorkspaceConf workspaceConfTemplate, VersioningConf vc,
			SearchConf sc, ClusterConf cc) {
		this.home = home;
		this.sec = securityConf;
		this.fsc = fsc;
		this.workspaceDirectory = workspaceDirectory;
		this.workspaceConfigDirectory = workspaceConfigDirectory;
		this.defaultWorkspace = defaultWorkspace;
		this.workspaceMaxIdleTime = workspaceMaxIdleTime;
		this.vc = vc;
		this.sc = sc;
		this.cc = cc;
		this.workspaceConfTemplate = workspaceConfTemplate;
	}
	
	/**
	 * The default configuration uses the bundle {@link DerbyPersistenceManager}
	 * (for versioning and workspaces), a {@link LocalFileSystem} (for repository,
	 * versioning and workspaces), a {@link SearchIndex} (for repository and
	 * workspaces) and a {@link SimpleAccessManager} as well as a
	 * {@link SimpleLoginModule} for security config.
	 */
	public RepositoryConf() {
        this.home = "${" + Xml.REPOSITORY_HOME_VARIABLE + "}";
        
        this.workspaceDirectory = this.home + "/workspaces";
        this.workspaceConfigDirectory = null;
        this.workspaceMaxIdleTime = 0;
        this.defaultWorkspace = "default";
        
        this.sec = new SecurityConf();
        
        this.fsc = new FileSystemConf();
        this.fsc.setParameter("path", this.home + "/repository");
        
        this.vc = new VersioningConf();
        
        this.sc = new SearchConf();
        this.sc.setParameter("path", this.home + "/repository/index");
        
        this.cc = null;
        this.workspaceConfTemplate = new WorkspaceConf();
	}
	
	/**
	 * Don't forget to call init() on the returned {@link RepositoryConfig}.
	 */
	public RepositoryConfig createConfig(String home) throws ConfException {
		Variables variables = new Variables();
		variables.setProperty(RepositoryConfigurationParser.REPOSITORY_HOME_VARIABLE, home);
		
		return createConfig(variables);
	}
	
	public RepositoryConfig createConfig(Variables variables) throws ConfException {
		ClusterConfig cc = null;
		if (getClusterConf() != null) {
			cc = getClusterConf().createClusterConfig(variables);
		}
		SecurityConfig sec = null;
		if (getSecurityConf() != null) {
			sec = getSecurityConf().createSecurityConfig(variables);
		}
		FileSystemConfig fsc = null;
		if (getFileSystemConf() != null) {
			fsc = getFileSystemConf().createFileSystemConfig(variables);
		}
		VersioningConfig vc = null;
		if (getVersioningConf() != null) {
			vc = getVersioningConf().createVersioningConfig(variables);
		}
		SearchConfig sc = null;
		if (getSearchConf() != null) {
			sc = getSearchConf().createSearchConfig(variables);
		}
    	return new RepositoryConfig(
    			variables.replaceVariables(getHomeDir()),
    			sec,
    			fsc,
    			variables.replaceVariables(getWorkspaceDirectory()),
    			getWorkspaceConfigDirectory(),
    			variables.replaceVariables(getDefaultWorkspaceName()),
    			getWorkspaceMaxIdleTime(),
    			RepositoryConfWriter.createWorkspaceConfElement(workspaceConfTemplate),
    			vc,
    			sc,
    			cc,
    			new RepositoryConfigurationParser(variables)
    		);
    }

    public String getHomeDir() {
        return home;
    }

    public FileSystemConf getFileSystemConf() {
        return fsc;
    }

    public SecurityConf getSecurityConf() {
        return sec;
    }

    public String getWorkspaceDirectory() {
        return workspaceDirectory;
    }

    public String getWorkspaceConfigDirectory() {
        return workspaceConfigDirectory;
    }

    public String getDefaultWorkspaceName() {
        return defaultWorkspace;
    }

    public int getWorkspaceMaxIdleTime() {
        return workspaceMaxIdleTime;
    }

    public VersioningConf getVersioningConf() {
        return vc;
    }

    public SearchConf getSearchConf() {
        return sc;
    }

    public ClusterConf getClusterConf() {
        return cc;
    }

	public void setHome(String home) {
		this.home = home;
	}

	public void setSec(SecurityConf sec) {
		this.sec = sec;
	}

	public void setFsc(FileSystemConf fsc) {
		this.fsc = fsc;
	}

	public void setDefaultWorkspace(String defaultWorkspace) {
		this.defaultWorkspace = defaultWorkspace;
	}

	public void setWorkspaceDirectory(String workspaceDirectory) {
		this.workspaceDirectory = workspaceDirectory;
	}

	public void setWorkspaceMaxIdleTime(int workspaceMaxIdleTime) {
		this.workspaceMaxIdleTime = workspaceMaxIdleTime;
	}

	public void setVersioningConf(VersioningConf vc) {
		this.vc = vc;
	}

	public void setSearchConf(SearchConf sc) {
		this.sc = sc;
	}

	public void setClusterConf(ClusterConf cc) {
		this.cc = cc;
	}

	public void setWorkspaceConfigDirectory(String workspaceConfigDirectory) {
		this.workspaceConfigDirectory = workspaceConfigDirectory;
	}

	public WorkspaceConf getWorkspaceConfTemplate() {
		return workspaceConfTemplate;
	}

	public void setWorkspaceConfTemplate(WorkspaceConf workspaceConfTemplate) {
		this.workspaceConfTemplate = workspaceConfTemplate;
	}

	public void print(PrettyPrinter pp) {
		pp.printlnIndent("[Repository");
		
		pp.increaseIndent();
		
		pp.printlnIndent("home=" + home + ", ");
		pp.printlnIndent("defaultWorkspace=" + defaultWorkspace + ", ");
		pp.printlnIndent("workspaceDirectory=" + workspaceDirectory + ", ");
		pp.printlnIndent("workspaceMaxIdleTime=" + workspaceMaxIdleTime + ", ");
		pp.printlnIndent("workspaceConfigDirectory=" + workspaceConfigDirectory);
		
		if (sec != null) sec.print(pp);
		if (fsc != null) fsc.print(pp);
		if (vc != null) vc.print(pp);
		if (sc != null) sc.print(pp);
		if (cc != null) cc.print(pp);
		if (workspaceConfTemplate != null) workspaceConfTemplate.print(pp);
		
		pp.decreaseIndent();
		
		pp.printlnIndent("]");
	}
}
