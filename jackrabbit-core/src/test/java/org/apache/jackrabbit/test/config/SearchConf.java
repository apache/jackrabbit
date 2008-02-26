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

import java.util.Properties;

import org.apache.jackrabbit.core.config.FileSystemConfig;
import org.apache.jackrabbit.core.config.SearchConfig;
import org.apache.jackrabbit.core.query.lucene.SearchIndex;
import org.apache.jackrabbit.test.config.util.PrettyPrinter;
import org.apache.jackrabbit.test.config.util.Variables;
import org.apache.jackrabbit.test.config.xml.ConfException;

public class SearchConf extends BeanConf {

    private FileSystemConf fsc;

	public SearchConf(String className, Properties parameters,
			FileSystemConf fsc) {
		super(className, parameters);
		this.fsc = fsc;
	}

	/**
	 * Uses {@link SearchIndex} by default.
	 */
	public SearchConf() {
		this.className = SearchIndex.class.getName();
	}

	public SearchConfig createSearchConfig(Variables variables) throws ConfException {
		FileSystemConfig fsc = null;
		if (getFileSystemConf() != null) {
			fsc = getFileSystemConf().createFileSystemConfig(variables);
		}
		return new SearchConfig(getClassName(), variables.replaceVariables(getParameters()), fsc);
	}
	
    public FileSystemConf getFileSystemConf() {
        return fsc;
    }
    
    public void setFileSystemConf(FileSystemConf fsc) {
    	this.fsc = fsc;
    }

	public void print(PrettyPrinter pp) {
		pp.printlnIndent("[SearchIndex");
		
		printBeanConf(pp);
		
		pp.increaseIndent();
		
		if (fsc != null) fsc.print(pp);
		
		pp.decreaseIndent();
		
		pp.printlnIndent("]");
	}
}
