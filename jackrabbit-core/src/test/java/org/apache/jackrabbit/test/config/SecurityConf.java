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

import org.apache.jackrabbit.core.config.AccessManagerConfig;
import org.apache.jackrabbit.core.config.LoginModuleConfig;
import org.apache.jackrabbit.core.config.SecurityConfig;
import org.apache.jackrabbit.test.config.util.PrettyPrinter;
import org.apache.jackrabbit.test.config.util.Variables;
import org.apache.jackrabbit.test.config.xml.ConfException;

public class SecurityConf {

    private String appName;

    private AccessManagerConf amc;

    private LoginModuleConf lmc;

	public SecurityConf(String appName, AccessManagerConf accessManagerConf,
			LoginModuleConf loginModuleConf) {
		this.appName = appName;
		this.amc = accessManagerConf;
		this.lmc = loginModuleConf;
	}

	public SecurityConf() {
		this.appName = "Jackrabbit";
		
		this.amc = new AccessManagerConf();
		this.lmc = new LoginModuleConf();
	}

	public SecurityConfig createSecurityConfig(Variables variables) throws ConfException {
		AccessManagerConfig amc = null;
		if (getAccessManagerConf() != null) {
			amc = getAccessManagerConf().createAccessManagerConfig(variables);
		}
		LoginModuleConfig lmc = null;
		if (getLoginModuleConf() != null) {
			lmc = getLoginModuleConf().createLoginModuleConfig(variables);
		}
		return new SecurityConfig(getAppName(), amc, lmc);
	}
	
    public String getAppName() {
        return appName;
    }

    public AccessManagerConf getAccessManagerConf() {
        return amc;
    }

    public LoginModuleConf getLoginModuleConf() {
        return lmc;
    }

	public void setName(String name) {
		this.appName = name;
	}

	public void setAccessManagerConf(AccessManagerConf amc) {
		this.amc = amc;
	}

	public void setLoginModuleConf(LoginModuleConf lmc) {
		this.lmc = lmc;
	}

	public void print(PrettyPrinter pp) {
		pp.printlnIndent("[Security");
		
		pp.increaseIndent();
		
		pp.printlnIndent("name=" + appName);
		
		if (amc != null) amc.print(pp);
		if (lmc != null) lmc.print(pp);
		
		pp.decreaseIndent();
		
		pp.printlnIndent("]");
	}

}
