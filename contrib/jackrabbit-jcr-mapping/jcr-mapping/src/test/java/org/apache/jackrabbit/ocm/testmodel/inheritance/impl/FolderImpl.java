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

import java.util.ArrayList;
import java.util.List;

import org.apache.jackrabbit.ocm.testmodel.interfaces.CmsObject;
import org.apache.jackrabbit.ocm.testmodel.interfaces.Folder;




/**
 * CMS Folder Test
 *
 * @author <a href="mailto:christophe.lombart@gmail.com">Christophe Lombart</a>
 * @version $Id: Folder.java,v 1.1 2004/12/22 20:36:59 christophe Exp $
 */
public class FolderImpl extends CmsObjectImpl implements Folder 
{

    protected List children = new ArrayList();

	/**
	 * 
	 * @see org.apache.jackrabbit.ocm.testmodel.interfaces.Folder#getChildren()
	 */
	public List getChildren() {
		return children;
	}

	/**
	 * 
	 * @see org.apache.jackrabbit.ocm.testmodel.interfaces.Folder#setChildren(java.util.List)
	 */
	public void setChildren(List children) {
		this.children = children;
	}
    
    /**
     * 
     * @see org.apache.jackrabbit.ocm.testmodel.interfaces.Folder#addChild(org.apache.jackrabbit.ocm.testmodel.interfaces.CmsObject)
     */
    public void addChild(CmsObject child)
    {
    	    children.add(child);
    }
}

