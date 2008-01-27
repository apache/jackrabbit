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



import org.apache.jackrabbit.ocm.manager.collectionconverter.impl.NTCollectionConverterImpl;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.Collection;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.Field;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.Node;

@Node(jcrType="ocm:ntmain")
public class NTMain
{

	@Field(path=true) private String path;
    @Collection(proxy=true, elementClassName=NTDetail.class,collectionConverter=NTCollectionConverterImpl.class)  private java.util.Collection proxyCollection;

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public java.util.Collection getProxyCollection() {
		return proxyCollection;
	}

	public void setProxyCollection(java.util.Collection proxyCollection) {
		this.proxyCollection = proxyCollection;
	}


}
