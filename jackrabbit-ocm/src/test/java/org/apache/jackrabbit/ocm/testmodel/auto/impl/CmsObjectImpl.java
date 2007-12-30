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
package org.apache.jackrabbit.ocm.testmodel.auto.impl;

import org.apache.jackrabbit.ocm.manager.beanconverter.impl.ParentBeanConverterImpl;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.Bean;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.Field;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.Node;
import org.apache.jackrabbit.ocm.testmodel.auto.CmsObject;
import org.apache.jackrabbit.ocm.testmodel.auto.Folder;



/**
 * CmsObject test
 * 
 * @author <a href="mailto:christophe.lombart@gmail.com">Christophe Lombart</a>
 * 
 * 
 */
@Node(jcrType="ocm:cmsobjectimpl", discriminator=false, isAbstract=true)
public abstract class CmsObjectImpl implements CmsObject
{
    
	@Field(path=true) protected String path;
    
    @Field(jcrName="ocm:name", id=true) protected String name;
  
    @Bean(converter=ParentBeanConverterImpl.class)
    protected Folder parentFolder;
    

    /**
     * 
     * @see org.apache.jackrabbit.ocm.testmodel.interfaces.CmsObject#getName()
     */    
	public String getName() {
		return name;
	}
	
	/**
	 * 
	 * @see org.apache.jackrabbit.ocm.testmodel.interfaces.CmsObject#setName(java.lang.String)
	 */
	public void setName(String name) {
		this.name = name;
	}
	
	/**
	 * 
	 * @see org.apache.jackrabbit.ocm.testmodel.interfaces.CmsObject#getPath()
	 */
	
	public String getPath() {
		return path;
	}
	/**
	 * 
	 * @see org.apache.jackrabbit.ocm.testmodel.interfaces.CmsObject#setPath(java.lang.String)
	 */
	public void setPath(String path) {
		this.path = path;
	}
	/**
	 * 
	 * @see org.apache.jackrabbit.ocm.testmodel.interfaces.CmsObject#getParentFolder()
	 */
	
	public Folder getParentFolder() {
		return parentFolder;
	}
	/**
	 * 
	 * @see org.apache.jackrabbit.ocm.testmodel.interfaces.CmsObject#setParentFolder(org.apache.jackrabbit.ocm.testmodel.interfaces.Folder)
	 */
	public void setParentFolder(Folder parentFolder) {
		this.parentFolder = parentFolder;
	}
}
