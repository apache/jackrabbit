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
package org.apache.jackrabbit.ocm.testmodel.inheritance.impl;

import org.apache.jackrabbit.ocm.testmodel.interfaces.CmsObject;
import org.apache.jackrabbit.ocm.testmodel.interfaces.Folder;



/**
 * CmsObject test
 * 
 * @author <a href="mailto:christophe.lombart@gmail.com">Christophe Lombart</a>
 * 
 * 
 */
public class CmsObjectImpl implements CmsObject
{
    
    protected String path;        
    protected String name;
    protected Folder parentFolder;
    
	/* (non-Javadoc)
	 * @see org.apache.portals.graffito.jcr.testmodel.inheritance.impl.CmsObject#getName()
	 */
	public String getName() {
		return name;
	}
	/* (non-Javadoc)
	 * @see org.apache.portals.graffito.jcr.testmodel.inheritance.impl.CmsObject#setName(java.lang.String)
	 */
	public void setName(String name) {
		this.name = name;
	}
	/* (non-Javadoc)
	 * @see org.apache.portals.graffito.jcr.testmodel.inheritance.impl.CmsObject#getPath()
	 */
	public String getPath() {
		return path;
	}
	/* (non-Javadoc)
	 * @see org.apache.portals.graffito.jcr.testmodel.inheritance.impl.CmsObject#setPath(java.lang.String)
	 */
	public void setPath(String path) {
		this.path = path;
	}
	/* (non-Javadoc)
	 * @see org.apache.portals.graffito.jcr.testmodel.inheritance.impl.CmsObject#getParentFolder()
	 */
	public Folder getParentFolder() {
		return parentFolder;
	}
	/* (non-Javadoc)
	 * @see org.apache.portals.graffito.jcr.testmodel.inheritance.impl.CmsObject#setParentFolder(org.apache.portals.graffito.jcr.testmodel.inheritance.impl.FolderImpl)
	 */
	public void setParentFolder(Folder parentFolder) {
		this.parentFolder = parentFolder;
	}
	
	
        
        
}
