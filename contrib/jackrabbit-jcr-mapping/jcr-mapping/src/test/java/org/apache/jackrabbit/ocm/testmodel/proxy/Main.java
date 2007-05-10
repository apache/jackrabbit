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
package org.apache.jackrabbit.ocm.testmodel.proxy;

import java.util.Collection;

public class Main 
{

	private String path;
	private Detail proxyDetail;
	private Detail nullDetail; 
    private Detail detail;
    private Collection proxyCollection;
    private Collection nullProxyCollection;

     
	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public Detail getDetail() {
		return detail;
	}

	public void setDetail(Detail detail) {
		this.detail = detail;
	}

	public Detail getProxyDetail() {
		return proxyDetail;
	}

	public void setProxyDetail(Detail proxyDetail) {
		this.proxyDetail = proxyDetail;
	}

	public Detail getNullDetail() {
		return nullDetail;
	}

	public void setNullDetail(Detail nullDetail) {
		this.nullDetail = nullDetail;
	}

	public Collection getProxyCollection() {
		return proxyCollection;
	}

	public void setProxyCollection(Collection proxyCollection) {
		this.proxyCollection = proxyCollection;
	}

	public Collection getNullProxyCollection() {
		return nullProxyCollection;
	}

	public void setNullProxyCollection(Collection nullProxyCollection) {
		this.nullProxyCollection = nullProxyCollection;
	}
	
	
     
}
