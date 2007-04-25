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
package org.apache.portals.graffito.jcr.testmodel;

import java.util.ArrayList;
import java.util.Collection;
/**
 * Java class used to map the jcr node type nt:folder
 * @author <a href="mailto:christophe.lombart@gmail.com">Lombart Christophe </a>
 *
 */
public class Folder extends HierarchyNode
{
    private String path;
    
    private Collection children;   // = a collection of HierarchyNodes 

    
	public String getPath() 
	{
		return path;
	}

	public void setPath(String path) 
	{
		this.path = path;
	}


	public Collection getChildren() 
	{
		return children;
	}

	public void setChildren(Collection children) 
	{
		this.children = children;
	}

	public void addChild(HierarchyNode node)
	{
		if (children == null)
		{
			children = new ArrayList();			
		}
		children.add(node);
	}
    
}
