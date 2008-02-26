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

import org.apache.jackrabbit.test.config.util.PrettyPrinter;
import org.apache.jackrabbit.test.config.xml.Xml;

public class WorkspaceConf {

    private String name;

    private boolean clustered;

    private FileSystemConf fsc;

    private PersistenceManagerConf pmc;

    private SearchConf sc;

	public WorkspaceConf(String name, boolean clustered,
			FileSystemConf fsc, PersistenceManagerConf pmc, SearchConf sc) {
		this.name = name;
		this.clustered = clustered;
		this.fsc = fsc;
		this.pmc = pmc;
		this.sc = sc;
	}

	public WorkspaceConf() {
		this.name = "${" + Xml.WORKSPACE_NAME_VARIABLE + "}";
		this.clustered = true;
		
		this.fsc = new FileSystemConf();
		this.fsc.setParameter("path", "${" + Xml.WORKSPACE_HOME_VARIABLE + "}");
		
		this.pmc = new PersistenceManagerConf();
		this.pmc.setParameter("url", "jdbc:derby:${" + Xml.WORKSPACE_HOME_VARIABLE + "}/db/itemState;create=true");
		this.pmc.setParameter("schemaObjectPrefix", "${" + Xml.WORKSPACE_NAME_VARIABLE + "}_");
		
		this.sc = new SearchConf();
		this.sc.setParameter("path", "${" + Xml.WORKSPACE_HOME_VARIABLE + "}/index");
	}

	/**
	 * This method is probably never needed.
	 */
//	public WorkspaceConfig createWorkspaceConfig(String name, Variables parentVariables) throws ConfException {
//		Variables variables = new Variables();
//		variables.putAll(parentVariables);
//        variables.put(Xml.WORKSPACE_NAME_VARIABLE, name);
//        
//		FileSystemConfig fsc = null;
//		if (getFileSystemConf() != null) {
//			fsc = getFileSystemConf().createFileSystemConfig();
//		}
//		PersistenceManagerConfig pmc = null;
//		if (getPersistenceManagerConf() != null) {
//			pmc = getPersistenceManagerConf().createPersistenceManagerConfig();
//		}
//		SearchConfig sc = null;
//		if (getSearchConf() != null) {
//			sc = getSearchConf().createSearchConfig();
//		}
//		
//		return new WorkspaceConfig(variables.replaceVariables(getHome()),
//				getName(), isClustered(), fsc, pmc, sc);
//	}
	
    public String getName() {
        return name;
    }

    public boolean isClustered() {
        return clustered;
    }

    public FileSystemConf getFileSystemConf() {
        return fsc;
    }

    public PersistenceManagerConf getPersistenceManagerConf() {
        return pmc;
    }

    public SearchConf getSearchConf() {
        return sc;
    }

	public void setName(String name) {
		this.name = name;
	}

	public void setClustered(boolean clustered) {
		this.clustered = clustered;
	}

	public void setFileSystemConf(FileSystemConf fsc) {
		this.fsc = fsc;
	}

	public void setPersistenceManagerConf(PersistenceManagerConf pmc) {
		this.pmc = pmc;
	}

	public void setSearchConf(SearchConf sc) {
		this.sc = sc;
	}

	public void print(PrettyPrinter pp) {
		pp.printlnIndent("[Workspace");
		
		pp.increaseIndent();
		
		pp.printlnIndent("name=" + name);
		pp.printlnIndent("clustered=" + clustered);
		
		if (fsc != null) fsc.print(pp);
		if (pmc != null) pmc.print(pp);
		if (sc != null) sc.print(pp);
		
		pp.decreaseIndent();
		
		pp.printlnIndent("]");
	}

}
