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

import java.util.Enumeration;
import java.util.Properties;

import org.apache.jackrabbit.core.config.BeanConfig;
import org.apache.jackrabbit.test.config.util.PrettyPrinter;
import org.apache.jackrabbit.test.config.util.Variables;
import org.apache.jackrabbit.test.config.xml.ConfException;

public class BeanConf {
	
    protected String className;

    protected Properties parameters;
    
    public BeanConf() {
    	this.className = "";
    	this.parameters = new Properties();
    }
    
	public BeanConf(String className, Properties parameters) {
		this.className = className;
		this.parameters = parameters;
	}

    public BeanConf(BeanConf config) {
        this(config.getClassName(), config.getParameters());
    }
    
	public BeanConfig createBeanConfig(Variables variables) throws ConfException {
		return new BeanConfig(getClassName(), variables.replaceVariables(getParameters()));
	}

    public String getClassName() {
        return className;
    }
    
    public void setClassName(String className) {
    	this.className = className;
    }
    
    public Properties getParameters() {
        return parameters;
    }

    public void setParameters(Properties params) {
    	this.parameters = params;
    }
    
    public void setParameter(String name, String value) {
    	this.parameters.setProperty(name, value);
    }

	public void printBeanConf(PrettyPrinter pp) {
		pp.increaseIndent();
		
		pp.printlnIndent("className=" + className);
		for (Enumeration keys = parameters.keys(); keys.hasMoreElements();) {
			String key = (String) keys.nextElement();
			pp.printlnIndent(key + "=" + parameters.getProperty(key));
		}

		pp.decreaseIndent();
	}
}
